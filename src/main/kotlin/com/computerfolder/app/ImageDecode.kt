package com.computerfolder.app

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

internal fun scaleBufferedImage(src: BufferedImage, maxSide: Int): BufferedImage {
    val w = src.width
    val h = src.height
    if (w <= 0 || h <= 0) return src
    if (w <= maxSide && h <= maxSide) return src
    val scale = maxSide.toDouble() / maxOf(w, h)
    val nw = (w * scale).toInt().coerceAtLeast(1)
    val nh = (h * scale).toInt().coerceAtLeast(1)
    val type =
        if (src.colorModel.hasAlpha()) {
            BufferedImage.TYPE_INT_ARGB
        } else {
            BufferedImage.TYPE_INT_RGB
        }
    val dst = BufferedImage(nw, nh, type)
    val g = dst.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    g.drawImage(src, 0, 0, nw, nh, null)
    g.dispose()
    return dst
}

internal suspend fun decodeThumbnailScaled(file: File, maxSide: Int): ImageBitmap? =
    withContext(Dispatchers.IO) {
        runCatching {
            val src = ImageIO.read(file) ?: return@runCatching null
            val scaled = scaleBufferedImage(src, maxSide)
            scaled.toComposeImageBitmap()
        }.getOrNull()
    }
