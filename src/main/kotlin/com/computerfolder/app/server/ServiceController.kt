package com.computerfolder.app.server

import com.computerfolder.app.SelectionStore
import com.computerfolder.app.imageExtensions
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.File
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.stream.Collectors

internal class ServiceController(
    val host: String,
    val port: Int,
    private val imageRoot: Path,
    private val selectionStore: SelectionStore,
) {
    private var server: HttpServer? = null
    private var requestExecutor: ExecutorService? = null

    private val listCacheLock = Any()
    private var cachedSortedPaths: List<Path>? = null
    private var listCacheExpiresAtMs: Long = 0

    fun start() {
        if (server != null) return

        val created = HttpServer.create(InetSocketAddress(host, port), HTTP_BACKLOG)
        val poolSize = (Runtime.getRuntime().availableProcessors() * 8).coerceIn(16, 256)
        val exec = Executors.newFixedThreadPool(poolSize) { r ->
            Thread(r, "folder-http").apply { isDaemon = true }
        }
        created.executor = exec
        requestExecutor = exec

        created.createContext("/health") { exchange ->
            if (handleOptions(exchange)) return@createContext
            sendJson(exchange, 200, "{\"status\":\"ok\"}")
        }

        created.createContext("/selected") { exchange ->
            if (handleOptions(exchange)) return@createContext
            if (exchange.requestMethod != "GET") {
                sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}")
                return@createContext
            }
            if (!Files.exists(imageRoot) || !Files.isDirectory(imageRoot)) {
                sendJson(
                    exchange,
                    500,
                    "{\"error\":\"image dir missing\",\"imageDir\":${jsonString(imageRoot.toString())}}",
                )
                return@createContext
            }
            val baseUrl = getBaseUrl(exchange)
            val paths = selectionStore.snapshot()
            val body = buildString {
                append("{")
                append("\"imageDir\":")
                append(jsonString(imageRoot.toString()))
                append(",\"count\":")
                append(paths.size)
                append(",\"items\":[")
                paths.forEachIndexed { index, rel ->
                    if (index > 0) append(",")
                    val path = imageRoot.resolve(rel).normalize()
                    if (!path.startsWith(imageRoot) || !Files.isRegularFile(path) || !isImageFile(path.toFile())) {
                        append("{\"error\":")
                        append(jsonString("missing or not image"))
                        append(",\"relativePath\":")
                        append(jsonString(rel))
                        append("}")
                    } else {
                        val fileName = path.fileName.toString()
                        val size = Files.size(path)
                        val modified = Files.getLastModifiedTime(path).toInstant().toString()
                        append("{")
                        append("\"name\":")
                        append(jsonString(fileName))
                        append(",\"relativePath\":")
                        append(jsonString(rel))
                        append(",\"size\":")
                        append(size)
                        append(",\"lastModified\":")
                        append(jsonString(modified))
                        append(",\"url\":")
                        append(jsonString("$baseUrl/image?name=${urlEncode(rel)}"))
                        append("}")
                    }
                }
                append("]}")
            }
            sendJson(exchange, 200, body)
        }

        created.createContext("/images") { exchange ->
            if (handleOptions(exchange)) return@createContext

            if (exchange.requestMethod != "GET") {
                sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}")
                return@createContext
            }

            if (!Files.exists(imageRoot) || !Files.isDirectory(imageRoot)) {
                sendJson(
                    exchange,
                    500,
                    "{\"error\":\"image dir missing\",\"imageDir\":${jsonString(imageRoot.toString())}}",
                )
                return@createContext
            }

            val rawQuery = exchange.requestURI.rawQuery ?: ""
            val params = parseQuery(rawQuery)
            val forceRefresh =
                params["refresh"] == "1" ||
                    params["refresh"]?.equals("true", ignoreCase = true) == true
            val limitRaw = params["limit"]?.trim().orEmpty()
            val parsedLimit = limitRaw.takeIf { it.isNotEmpty() }?.toIntOrNull()
            val pageLimit =
                (parsedLimit ?: IMAGES_PAGE_SIZE).coerceIn(1, IMAGES_MAX_PAGE_SIZE)
            val pageOffset =
                params["offset"]?.trim().orEmpty().takeIf { it.isNotEmpty() }?.toIntOrNull()
                    ?.coerceAtLeast(0) ?: 0

            val allFiles = getSortedImagePaths(forceRefresh)
            val total = allFiles.size
            val files =
                if (pageOffset >= total) {
                    emptyList()
                } else {
                    val end = (pageOffset + pageLimit).coerceAtMost(total)
                    ArrayList(allFiles.subList(pageOffset, end))
                }

            val baseUrl = getBaseUrl(exchange)
            val body = buildString {
                append("{")
                append("\"imageDir\":")
                append(jsonString(imageRoot.toString()))
                append(",\"count\":")
                append(total)
                append(",\"offset\":")
                append(pageOffset)
                append(",\"limit\":")
                append(pageLimit)
                append(",\"hasMore\":")
                append(pageOffset + files.size < total)
                append(",\"items\":[")

                files.forEachIndexed { index, path ->
                    if (index > 0) append(",")

                    val relativePath = imageRoot.relativize(path).toString().replace(File.separatorChar, '/')
                    val fileName = path.fileName.toString()
                    val size = Files.size(path)
                    val modified = Files.getLastModifiedTime(path).toInstant().toString()

                    append("{")
                    append("\"name\":")
                    append(jsonString(fileName))
                    append(",\"relativePath\":")
                    append(jsonString(relativePath))
                    append(",\"size\":")
                    append(size)
                    append(",\"lastModified\":")
                    append(jsonString(modified))
                    append(",\"url\":")
                    append(jsonString("$baseUrl/image?name=${urlEncode(relativePath)}"))
                    append("}")
                }
                append("]}")
            }

            sendJson(exchange, 200, body)
        }

        created.createContext("/image") { exchange ->
            if (handleOptions(exchange)) return@createContext

            if (exchange.requestMethod != "GET") {
                sendJson(exchange, 405, "{\"error\":\"Method Not Allowed\"}")
                return@createContext
            }

            val rawQuery = exchange.requestURI.rawQuery ?: ""
            val params = parseQuery(rawQuery)
            val name = params["name"]

            if (name.isNullOrBlank()) {
                sendJson(exchange, 400, "{\"error\":\"Missing query param: name\"}")
                return@createContext
            }

            val requested = imageRoot.resolve(name).normalize()
            if (!requested.startsWith(imageRoot)) {
                sendJson(exchange, 403, "{\"error\":\"Forbidden path\"}")
                return@createContext
            }

            if (!Files.exists(requested) || !Files.isRegularFile(requested) || !isImageFile(requested.toFile())) {
                sendJson(exchange, 404, "{\"error\":\"Image not found\"}")
                return@createContext
            }

            val mimeType = Files.probeContentType(requested) ?: "application/octet-stream"
            val size = Files.size(requested)
            val lastModifiedTime: FileTime = Files.getLastModifiedTime(requested)
            val etag = imageEtag(size, lastModifiedTime)

            val inm = exchange.requestHeaders.getFirst("If-None-Match")?.trim()
            if (inm != null && inm == etag) {
                sendImageNotModified(exchange, etag, lastModifiedTime)
                return@createContext
            }

            exchange.responseHeaders.add("Access-Control-Allow-Origin", "*")
            exchange.responseHeaders.add("Access-Control-Allow-Methods", "GET, OPTIONS")
            exchange.responseHeaders.add("Access-Control-Allow-Headers", "Content-Type")
            exchange.responseHeaders.add("Content-Type", mimeType)
            exchange.responseHeaders.add("Cache-Control", "public, max-age=86400")
            exchange.responseHeaders.add("ETag", etag)
            exchange.responseHeaders.add("Last-Modified", formatHttpLastModified(lastModifiedTime))

            exchange.sendResponseHeaders(200, size)
            exchange.responseBody.use { out -> Files.copy(requested, out) }
        }

        created.start()
        server = created
    }

    private fun getSortedImagePaths(forceRefresh: Boolean): List<Path> {
        synchronized(listCacheLock) {
            val now = System.currentTimeMillis()
            if (!forceRefresh && cachedSortedPaths != null && now < listCacheExpiresAtMs) {
                return cachedSortedPaths!!
            }
            val list =
                Files.walk(imageRoot).use { stream ->
                    stream.filter { Files.isRegularFile(it) }
                        .filter { isImageFile(it.toFile()) }
                        .sorted(compareBy { it.fileName.toString().lowercase() })
                        .collect(Collectors.toList())
                }
            cachedSortedPaths = list
            listCacheExpiresAtMs = now + IMAGE_LIST_CACHE_TTL_MS
            return list
        }
    }

    fun stop() {
        server?.stop(0)
        server = null
        requestExecutor?.shutdown()
        requestExecutor = null
        synchronized(listCacheLock) {
            cachedSortedPaths = null
            listCacheExpiresAtMs = 0
        }
    }

    companion object {
        private const val HTTP_BACKLOG = 512
        private const val IMAGE_LIST_CACHE_TTL_MS = 15_000L
        private const val IMAGES_PAGE_SIZE = 50
        private const val IMAGES_MAX_PAGE_SIZE = 2_000

        private val httpLastModifiedFormatter: DateTimeFormatter =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US)
                .withZone(ZoneOffset.UTC)

        private fun formatHttpLastModified(fileTime: FileTime): String {
            return httpLastModifiedFormatter.format(fileTime.toInstant())
        }

        private fun imageEtag(size: Long, lastModified: FileTime): String {
            return "\"${size}-${lastModified.toMillis()}\""
        }

        private fun sendImageNotModified(
            exchange: HttpExchange,
            etag: String,
            lastModified: FileTime,
        ) {
            exchange.responseHeaders.add("Access-Control-Allow-Origin", "*")
            exchange.responseHeaders.add("Access-Control-Allow-Methods", "GET, OPTIONS")
            exchange.responseHeaders.add("Access-Control-Allow-Headers", "Content-Type")
            exchange.responseHeaders.add("ETag", etag)
            exchange.responseHeaders.add("Last-Modified", formatHttpLastModified(lastModified))
            exchange.responseHeaders.add("Cache-Control", "public, max-age=86400")
            exchange.sendResponseHeaders(304, -1)
            exchange.close()
        }
    }
}

private fun handleOptions(exchange: HttpExchange): Boolean {
    if (exchange.requestMethod == "OPTIONS") {
        exchange.responseHeaders.add("Access-Control-Allow-Origin", "*")
        exchange.responseHeaders.add("Access-Control-Allow-Methods", "GET, OPTIONS")
        exchange.responseHeaders.add("Access-Control-Allow-Headers", "Content-Type")
        exchange.sendResponseHeaders(204, -1)
        exchange.close()
        return true
    }
    return false
}

private fun sendJson(exchange: HttpExchange, code: Int, body: String) {
    val data = body.toByteArray(StandardCharsets.UTF_8)
    exchange.responseHeaders.add("Access-Control-Allow-Origin", "*")
    exchange.responseHeaders.add("Access-Control-Allow-Methods", "GET, OPTIONS")
    exchange.responseHeaders.add("Access-Control-Allow-Headers", "Content-Type")
    exchange.responseHeaders.add("Cache-Control", "no-store")
    exchange.responseHeaders.add("Content-Type", "application/json; charset=utf-8")
    exchange.sendResponseHeaders(code, data.size.toLong())
    exchange.responseBody.use { it.write(data) }
}

private fun parseQuery(rawQuery: String): Map<String, String> {
    if (rawQuery.isBlank()) return emptyMap()

    return rawQuery.split("&")
        .mapNotNull {
            val idx = it.indexOf("=")
            if (idx <= 0) return@mapNotNull null
            val key = URLDecoder.decode(it.substring(0, idx), StandardCharsets.UTF_8)
            val value = URLDecoder.decode(it.substring(idx + 1), StandardCharsets.UTF_8)
            key to value
        }
        .toMap()
}

private fun isImageFile(file: File): Boolean {
    return file.extension.lowercase() in imageExtensions
}

private fun jsonString(value: String): String {
    return buildString {
        append('"')
        value.forEach { ch ->
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> {
                    if (ch.code < 32) {
                        append("\\u")
                        append(ch.code.toString(16).padStart(4, '0'))
                    } else {
                        append(ch)
                    }
                }
            }
        }
        append('"')
    }
}

private fun urlEncode(value: String): String {
    return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8)
}

private fun getBaseUrl(exchange: HttpExchange): String {
    val hostHeader = exchange.requestHeaders.getFirst("Host") ?: "127.0.0.1:8080"
    return "http://$hostHeader"
}
