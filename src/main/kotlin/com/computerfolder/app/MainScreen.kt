package com.computerfolder.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.io.File
import java.nio.file.Path
import java.util.Locale

@Composable
private fun compactFieldTextStyle() =
    MaterialTheme.typography.body2.copy(fontSize = 12.sp, lineHeight = 14.sp)

@Composable
internal fun MainScreen(viewModel: MainViewModel) {
    val state by viewModel.uiState
    val serviceRoot = remember(state.pathText) { viewModel.serviceRootPath() }
    val fieldStyle = compactFieldTextStyle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "服务\n${state.lanIp}",
                style = MaterialTheme.typography.caption.copy(fontSize = 10.sp, lineHeight = 12.sp),
                modifier = Modifier.widthIn(min = 72.dp, max = 140.dp),
                maxLines = 3,
            )
            OutlinedTextField(
                value = state.host,
                onValueChange = viewModel::setHost,
                label = { Text("IP", style = MaterialTheme.typography.caption) },
                singleLine = true,
                textStyle = fieldStyle,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
            )
            OutlinedTextField(
                value = state.portText,
                onValueChange = viewModel::setPortText,
                label = { Text("端口", style = MaterialTheme.typography.caption) },
                singleLine = true,
                textStyle = fieldStyle,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .width(76.dp)
                    .height(52.dp),
            )
            Button(
                enabled = !state.serverRunning,
                modifier = Modifier.height(36.dp),
                onClick = { viewModel.startServer() },
            ) {
                Text("启动", style = MaterialTheme.typography.caption)
            }

            Button(
                enabled = state.serverRunning,
                modifier = Modifier.height(36.dp),
                onClick = { viewModel.stopServer() },
            ) {
                Text("停止", style = MaterialTheme.typography.caption)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = state.pathText,
                onValueChange = viewModel::setPathText,
                label = { Text("根目录", style = MaterialTheme.typography.caption) },
                singleLine = false,
                maxLines = 3,
                textStyle = fieldStyle,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp, max = 76.dp),
            )
            Button(
                modifier = Modifier.height(36.dp),
                onClick = { viewModel.pickFolderWithSwingChooser() },
            ) {
                Text("目录", style = MaterialTheme.typography.caption)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = state.serviceStatus,
                style = MaterialTheme.typography.caption,
                maxLines = 3,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "/selected\n/image 原图",
                style = MaterialTheme.typography.caption.copy(fontSize = 10.sp, lineHeight = 11.sp),
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.55f),
            )
        }
        if (state.lastError.isNotBlank()) {
            Text(
                text = state.lastError,
                color = MaterialTheme.colors.error,
                style = MaterialTheme.typography.caption,
                maxLines = 4,
            )
        }

        Divider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "已选",
                style = MaterialTheme.typography.caption,
                modifier = Modifier.padding(end = 6.dp),
            )
            SelectedStrip(
                selectionRevision = state.selectionRevision,
                selectionStore = viewModel.selectionStore,
                serviceRoot = serviceRoot,
                onRemove = viewModel::removeSelection,
                onPreviewFile = viewModel::openPreviewSingle,
                modifier = Modifier.weight(1f),
            )
        }

        Divider()

        Column(
            modifier = Modifier
                .weight(1f, fill = true)
                .fillMaxWidth(),
        ) {
            ImagePickerPanel(
                modifier = Modifier.fillMaxSize(),
                browseDir = state.browseDir,
                onBrowseDirChange = viewModel::setBrowseDir,
                serviceRoot = serviceRoot,
                selectionStore = viewModel.selectionStore,
                selectionRevision = state.selectionRevision,
                onToggleSelection = viewModel::toggleSelection,
                onPreviewInFolder = viewModel::openPreviewFolder,
            )
        }

        Text(
            text = "API: /health /images?offset= /selected /image?name=相对路径。界面仅为小缩略图；HTTP /image 与以前一样返回磁盘上的原文件，客户端地址未改。",
            style = MaterialTheme.typography.caption.copy(fontSize = 10.sp, lineHeight = 12.sp),
            maxLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    state.previewSession?.let { session ->
        ImagePreviewDialog(
            files = session.fileAbsolutePaths.map { File(it) },
            initialIndex = session.initialIndex,
            onDismiss = viewModel::dismissPreview,
        )
    }
}

@Composable
private fun SelectedStrip(
    selectionRevision: Int,
    selectionStore: SelectionStore,
    serviceRoot: Path?,
    onRemove: (String) -> Unit,
    onPreviewFile: (File) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = remember(selectionRevision) { selectionStore.snapshot() }
    if (selected.isEmpty()) {
        Text(
            text = "下方勾选",
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
            modifier = modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(vertical = 4.dp),
        )
        return
    }

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(selected, key = { it }) { rel ->
            val thumbFile: File? =
                serviceRoot?.let { root ->
                    val f = root.resolve(rel).normalize().toFile()
                    if (f.toPath().toAbsolutePath().normalize().startsWith(root) && f.isFile) f else null
                }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(52.dp),
            ) {
                Box(
                    modifier = Modifier.size(48.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(4.dp))
                            .border(1.dp, MaterialTheme.colors.primary.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .clickable(enabled = thumbFile != null) {
                                if (thumbFile != null) onPreviewFile(thumbFile)
                            },
                    ) {
                        if (thumbFile != null) {
                            Thumbnail(
                                file = thumbFile,
                                maxDecodeSide = THUMB_STRIP_MAX_PX,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.LightGray.copy(alpha = 0.35f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("?", style = MaterialTheme.typography.caption, fontSize = 9.sp)
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(16.dp)
                            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(2.dp))
                            .clickable { onRemove(rel) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("×", color = Color.White, fontSize = 10.sp, lineHeight = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ImagePickerPanel(
    modifier: Modifier = Modifier,
    browseDir: File,
    onBrowseDirChange: (File) -> Unit,
    serviceRoot: Path?,
    selectionStore: SelectionStore,
    selectionRevision: Int,
    onToggleSelection: (String) -> Unit,
    onPreviewInFolder: (files: List<File>, index: Int) -> Unit,
) {
    val rootFile = serviceRoot?.toFile()
    val canGoUp =
        rootFile != null &&
            browseDir.absoluteFile != rootFile.absoluteFile &&
            browseDir.absolutePath.startsWith(rootFile.absolutePath)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = {
                    val parent = browseDir.parentFile
                    if (parent != null && rootFile != null && parent.absolutePath.startsWith(rootFile.absolutePath)) {
                        onBrowseDirChange(parent.absoluteFile)
                    }
                },
                enabled = canGoUp,
                modifier = Modifier.height(28.dp),
            ) {
                Text("↑", style = MaterialTheme.typography.caption)
            }
            Text(
                text = browseDir.absolutePath,
                style = MaterialTheme.typography.caption.copy(fontSize = 10.sp, lineHeight = 12.sp),
                maxLines = 3,
                modifier = Modifier.weight(1f),
            )
        }

        val subDirs = remember(browseDir.absolutePath) {
            browseDir.listFiles()
                ?.filter { it.isDirectory && !it.name.startsWith(".") }
                ?.sortedBy { it.name.lowercase(Locale.getDefault()) }
                ?: emptyList()
        }

        if (subDirs.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                items(subDirs, key = { it.absolutePath }) { dir ->
                    Button(
                        onClick = { onBrowseDirChange(dir.absoluteFile) },
                        modifier = Modifier
                            .defaultMinSize(minHeight = 40.dp)
                            .heightIn(min = 40.dp)
                            .widthIn(max = 240.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Text(
                            text = dir.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false,
                            style = MaterialTheme.typography.caption.copy(
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                            ),
                        )
                    }
                }
            }
        }

        val imagesInFolder = remember(browseDir.absolutePath) {
            browseDir.listFiles()
                ?.filter { it.isFile && isImageFile(it) }
                ?.sortedBy { it.name.lowercase(Locale.getDefault()) }
                ?: emptyList()
        }

        if (serviceRoot == null) {
            Text("请先填写有效的服务根目录", color = MaterialTheme.colors.error, style = MaterialTheme.typography.caption)
        } else {
            val selectedSet = remember(selectionRevision) { selectionStore.snapshot().toSet() }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 72.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                itemsIndexed(imagesInFolder, key = { _, f -> f.absolutePath }) { index, file ->
                    val rel = try {
                        serviceRoot.relativize(file.toPath().toAbsolutePath().normalize()).toString()
                            .replace(File.separatorChar, '/')
                    } catch (_: Exception) {
                        null
                    }
                    val checked = rel != null && rel in selectedSet
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .border(
                                1.dp,
                                if (checked) MaterialTheme.colors.primary else Color.LightGray.copy(alpha = 0.4f),
                                RoundedCornerShape(6.dp),
                            )
                            .padding(2.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFFF5F5F5))
                                .clickable { onPreviewInFolder(imagesInFolder, index) },
                        ) {
                            Thumbnail(
                                file = file,
                                maxDecodeSide = THUMB_GRID_MAX_PX,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Checkbox(
                                checked = checked,
                                enabled = rel != null,
                                modifier = Modifier.height(32.dp),
                                onCheckedChange = {
                                    if (rel != null) onToggleSelection(rel)
                                },
                            )
                            Text(
                                text = file.name,
                                style = MaterialTheme.typography.caption.copy(fontSize = 10.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImagePreviewDialog(
    files: List<File>,
    initialIndex: Int,
    onDismiss: () -> Unit,
) {
    if (files.isEmpty()) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }
    var index by remember(files, initialIndex) {
        mutableStateOf(initialIndex.coerceIn(0, files.lastIndex))
    }
    val file = files[index]
    var bitmap by remember(file.absolutePath, file.lastModified()) {
        mutableStateOf<ImageBitmap?>(null)
    }
    LaunchedEffect(file.absolutePath, file.lastModified()) {
        bitmap = null
        bitmap = decodeThumbnailScaled(file, PREVIEW_MAX_PX)
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .background(Color(0xEE000000))
                .padding(10.dp),
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = { if (index > 0) index-- },
                        enabled = index > 0,
                        modifier = Modifier.height(34.dp),
                    ) {
                        Text("上一张", style = MaterialTheme.typography.caption)
                    }
                    Text(
                        text = "${index + 1}/${files.size}\n${file.name}",
                        style = MaterialTheme.typography.caption.copy(color = Color.White, lineHeight = 14.sp),
                        maxLines = 3,
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = { if (index < files.lastIndex) index++ },
                        enabled = index < files.lastIndex,
                        modifier = Modifier.height(34.dp),
                    ) {
                        Text("下一张", style = MaterialTheme.typography.caption)
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.height(34.dp),
                    ) {
                        Text("关闭", style = MaterialTheme.typography.caption)
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val b = bitmap
                    if (b != null) {
                        Image(
                            bitmap = b,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        Text("加载中…", color = Color.White, style = MaterialTheme.typography.body2)
                    }
                }
                Text(
                    text = "阅览为缩放图(最长边 ${PREVIEW_MAX_PX}px)；原图仍以 GET /image?name= 访问",
                    style = MaterialTheme.typography.caption.copy(fontSize = 10.sp, color = Color.White.copy(alpha = 0.65f)),
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun Thumbnail(
    file: File,
    maxDecodeSide: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale,
) {
    var bitmap by remember(file.absolutePath, file.lastModified(), maxDecodeSide) {
        mutableStateOf<ImageBitmap?>(null)
    }
    LaunchedEffect(file.absolutePath, file.lastModified(), maxDecodeSide) {
        bitmap = decodeThumbnailScaled(file, maxDecodeSide)
    }
    val b = bitmap
    if (b != null) {
        Image(
            bitmap = b,
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale,
        )
    } else {
        Box(modifier = modifier.background(Color(0xFFE8E8E8)), contentAlignment = Alignment.Center) {
            Text(
                text = file.extension.uppercase(Locale.getDefault()).take(4),
                style = MaterialTheme.typography.caption.copy(fontSize = 9.sp),
                color = Color.DarkGray,
            )
        }
    }
}
