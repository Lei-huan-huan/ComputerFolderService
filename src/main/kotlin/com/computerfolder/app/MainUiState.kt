package com.computerfolder.app

import java.io.File
import java.util.prefs.Preferences

internal data class PreviewSession(
    val fileAbsolutePaths: List<String>,
    val initialIndex: Int,
)

internal data class MainUiState(
    val lanIp: String,
    val host: String,
    val portText: String,
    val pathText: String,
    val browseDir: File,
    val serviceStatus: String,
    val lastError: String,
    /** 勾选变化时递增，驱动 LazyGrid 刷新选中态 */
    val selectionRevision: Int,
    val previewSession: PreviewSession?,
    val serverRunning: Boolean,
) {
    companion object {
        fun initial(prefs: Preferences): MainUiState {
            val lan = detectLanIpStatic()
            val savedPath = prefs.get(PREF_PATH_KEY, System.getProperty("user.home") ?: "")
            val savedPort = prefs.get(PREF_PORT_KEY, "8080")
            val home = File(System.getProperty("user.home")!!)
            val browse = File(savedPath).takeIf { it.isDirectory }?.absoluteFile ?: home
            return MainUiState(
                lanIp = lan,
                host = lan,
                portText = savedPort,
                pathText = savedPath,
                browseDir = browse,
                serviceStatus = "未启动",
                lastError = "",
                selectionRevision = 0,
                previewSession = null,
                serverRunning = false,
            )
        }
    }
}

private fun detectLanIpStatic(): String {
    return try {
        java.net.NetworkInterface.getNetworkInterfaces().toList()
            .asSequence()
            .filter { it.isUp && !it.isLoopback && !it.isVirtual }
            .flatMap { it.inetAddresses.toList().asSequence() }
            .filterIsInstance<java.net.Inet4Address>()
            .firstOrNull { !it.isLoopbackAddress && it.isSiteLocalAddress }
            ?.hostAddress
            ?: "0.0.0.0"
    } catch (_: Exception) {
        "0.0.0.0"
    }
}
