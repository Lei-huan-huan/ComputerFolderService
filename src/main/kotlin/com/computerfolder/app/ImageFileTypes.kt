package com.computerfolder.app

import java.io.File

internal fun isImageFile(file: File): Boolean =
    file.extension.lowercase() in imageExtensions
