package com.computerfolder.app

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.computerfolder.app.server.ServiceController
import java.io.File
import java.nio.file.Path
import java.util.prefs.Preferences
import javax.swing.JFileChooser
import javax.swing.filechooser.FileSystemView

internal class MainViewModel(
    private val prefs: Preferences = Preferences.userRoot().node("computer-folder-service"),
) {
    val selectionStore = SelectionStore()
    private var httpServer: ServiceController? = null

    private val _uiState = mutableStateOf(MainUiState.initial(prefs))
    val uiState: State<MainUiState> get() = _uiState

    private fun setState(reduce: MainUiState.() -> MainUiState) {
        _uiState.value = _uiState.value.reduce()
    }

    fun setHost(value: String) = setState { copy(host = value) }

    fun setPortText(value: String) = setState { copy(portText = value) }

    fun setPathText(value: String) {
        val f = File(value)
        val browse = if (f.isDirectory) f.absoluteFile else _uiState.value.browseDir
        setState { copy(pathText = value, browseDir = browse) }
    }

    fun setBrowseDir(dir: File) = setState { copy(browseDir = dir.absoluteFile) }

    fun pickFolderWithSwingChooser(): Boolean {
        val chooser = JFileChooser(FileSystemView.getFileSystemView().homeDirectory)
        chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return false
        val sel = chooser.selectedFile
        setPathText(sel.absolutePath)
        setBrowseDir(sel.absoluteFile)
        return true
    }

    fun startServer() {
        setState { copy(lastError = "") }
        val s = _uiState.value
        val port = s.portText.toIntOrNull()
        if (port == null || port !in 1..65535) {
            setState { copy(lastError = "端口必须是 1-65535 的数字") }
            return
        }
        val root = File(s.pathText)
        if (!root.exists() || !root.isDirectory) {
            setState { copy(lastError = "文件路径不存在或不是目录") }
            return
        }
        try {
            val controller = ServiceController(
                host = s.host.ifBlank { "0.0.0.0" },
                port = port,
                imageRoot = root.toPath().toAbsolutePath().normalize(),
                selectionStore = selectionStore,
            )
            controller.start()
            httpServer = controller
            prefs.put(PREF_PATH_KEY, s.pathText)
            prefs.put(PREF_PORT_KEY, port.toString())
            setState {
                copy(
                    lastError = "",
                    serviceStatus = "运行中: http://${controller.host}:${controller.port}",
                    serverRunning = true,
                )
            }
        } catch (ex: Exception) {
            setState {
                copy(lastError = "启动失败: ${ex.message ?: ex.javaClass.simpleName}")
            }
        }
    }

    fun stopServer() {
        httpServer?.stop()
        httpServer = null
        setState { copy(serviceStatus = "已停止", serverRunning = false) }
    }

    fun toggleSelection(relativePath: String) {
        selectionStore.toggle(relativePath)
        setState { copy(selectionRevision = selectionRevision + 1) }
    }

    fun removeSelection(relativePath: String) {
        selectionStore.remove(relativePath)
        setState { copy(selectionRevision = selectionRevision + 1) }
    }

    fun openPreviewSingle(file: File) {
        setState {
            copy(
                previewSession = PreviewSession(
                    fileAbsolutePaths = listOf(file.absolutePath),
                    initialIndex = 0,
                ),
            )
        }
    }

    fun openPreviewFolder(files: List<File>, index: Int) {
        if (files.isEmpty()) return
        val i = index.coerceIn(0, files.lastIndex)
        setState {
            copy(
                previewSession = PreviewSession(
                    fileAbsolutePaths = files.map { it.absolutePath },
                    initialIndex = i,
                ),
            )
        }
    }

    fun dismissPreview() = setState { copy(previewSession = null) }

    /** 用于网格/列表：当前根目录下的绝对路径 */
    fun serviceRootPath(): Path? {
        val f = File(_uiState.value.pathText)
        return if (f.isDirectory) f.toPath().toAbsolutePath().normalize() else null
    }
}
