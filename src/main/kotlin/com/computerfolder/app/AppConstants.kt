package com.computerfolder.app

internal val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif")

internal const val PREF_PATH_KEY = "last_file_path"
internal const val PREF_PORT_KEY = "last_port"

/** 网格缩略图解码最大边长（像素） */
internal const val THUMB_GRID_MAX_PX = 88

/** 顶部已选条缩略图最大边长 */
internal const val THUMB_STRIP_MAX_PX = 64

/** 阅览对话框解码最长边 */
internal const val PREVIEW_MAX_PX = 2048
