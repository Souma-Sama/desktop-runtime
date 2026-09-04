package com.nuvio.app.features.anilist.community

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.nuvio.app.core.ui.NuvioAsyncImage as AsyncImage
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import javax.imageio.metadata.IIOMetadataNode

private val gifFrameCache = mutableMapOf<String, List<Pair<ImageBitmap, Long>>>()
private val downloadSemaphore = Semaphore(4)
private val desktopGifHttpClient by lazy {
    HttpClient(CIO) {
        followRedirects = true
        engine {
            requestTimeout = 25_000
        }
    }
}

@Composable
internal actual fun ReviewMediaImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale,
) {
    val cleanUrl = remember(url) { url.trim() }
    if (cleanUrl.isBlank()) return

    val isGif = remember(cleanUrl) {
        cleanUrl.endsWith(".gif", ignoreCase = true) ||
            cleanUrl.contains(".gif?", ignoreCase = true) ||
            cleanUrl.contains("tenor.com", ignoreCase = true) ||
            cleanUrl.contains("giphy.com", ignoreCase = true)
    }

    if (!isGif) {
        AsyncImage(
            model = cleanUrl,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
        )
        return
    }

    var frames by remember(cleanUrl) { mutableStateOf<List<Pair<ImageBitmap, Long>>?>(gifFrameCache[cleanUrl]) }
    var currentFrameIndex by remember(cleanUrl) { mutableIntStateOf(0) }

    LaunchedEffect(cleanUrl) {
        if (frames != null) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            downloadSemaphore.withPermit {
                for (attempt in 0..2) {
                    try {
                        val bytes = desktopGifHttpClient.get(cleanUrl) {
                            header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                            header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                        }.readBytes()

                        if (bytes.isNotEmpty()) {
                            val decoded = decodeGifFrames(bytes)
                            if (decoded.isNotEmpty()) {
                                gifFrameCache[cleanUrl] = decoded
                                frames = decoded
                                break
                            }
                        }
                    } catch (_: Exception) {
                        if (attempt < 2) delay(350)
                    }
                }
            }
        }
    }

    val currentFrames = frames
    if (currentFrames != null && currentFrames.isNotEmpty()) {
        LaunchedEffect(currentFrames) {
            if (currentFrames.size <= 1) return@LaunchedEffect
            while (isActive) {
                val delayMs = currentFrames[currentFrameIndex].second.coerceAtLeast(20L)
                delay(delayMs)
                currentFrameIndex = (currentFrameIndex + 1) % currentFrames.size
            }
        }

        Image(
            bitmap = currentFrames[currentFrameIndex].first,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
        )
    } else {
        AsyncImage(
            model = cleanUrl,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
        )
    }
}

private data class GifFrameMeta(
    val delayMs: Long,
    val disposalMethod: String,
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
)

private fun decodeGifFrames(bytes: ByteArray): List<Pair<ImageBitmap, Long>> {
    val result = mutableListOf<Pair<ImageBitmap, Long>>()
    val stream = ImageIO.createImageInputStream(ByteArrayInputStream(bytes)) ?: return emptyList()
    val readers = ImageIO.getImageReadersByFormatName("gif")
    if (!readers.hasNext()) return emptyList()
    val reader = readers.next()
    reader.input = stream

    try {
        val numImages = reader.getNumImages(true)
        if (numImages <= 0) return emptyList()

        val masterWidth = maxOf(1, reader.getWidth(0))
        val masterHeight = maxOf(1, reader.getHeight(0))

        val masterImage = java.awt.image.BufferedImage(
            masterWidth,
            masterHeight,
            java.awt.image.BufferedImage.TYPE_INT_ARGB,
        )
        val masterGraphics = masterImage.createGraphics()
        masterGraphics.background = java.awt.Color(0, 0, 0, 0)

        var previousSnapshot: java.awt.image.BufferedImage? = null
        var prevDisposal = "none"
        var prevLeft = 0
        var prevTop = 0
        var prevWidth = masterWidth
        var prevHeight = masterHeight

        for (i in 0 until numImages) {
            val frameImage = reader.read(i)
            val meta = getFrameMeta(reader.getImageMetadata(i), frameImage.width, frameImage.height)

            // Apply previous frame disposal
            when (prevDisposal.lowercase()) {
                "restoretobackgroundcolor" -> {
                    masterGraphics.composite = java.awt.AlphaComposite.Clear
                    masterGraphics.fillRect(prevLeft, prevTop, prevWidth, prevHeight)
                    masterGraphics.composite = java.awt.AlphaComposite.SrcOver
                }
                "restoretoprevious" -> {
                    if (previousSnapshot != null) {
                        masterGraphics.composite = java.awt.AlphaComposite.Src
                        masterGraphics.drawImage(previousSnapshot, 0, 0, null)
                        masterGraphics.composite = java.awt.AlphaComposite.SrcOver
                    }
                }
            }

            // Save snapshot if current frame requests restoreToPrevious
            if (meta.disposalMethod.equals("restoreToPrevious", ignoreCase = true)) {
                val snap = java.awt.image.BufferedImage(masterWidth, masterHeight, java.awt.image.BufferedImage.TYPE_INT_ARGB)
                val sg = snap.createGraphics()
                sg.drawImage(masterImage, 0, 0, null)
                sg.dispose()
                previousSnapshot = snap
            }

            // Draw frame image with its offset
            masterGraphics.drawImage(frameImage, meta.left, meta.top, null)

            // Create frame snapshot for Compose
            val frameSnapshot = java.awt.image.BufferedImage(masterWidth, masterHeight, java.awt.image.BufferedImage.TYPE_INT_ARGB)
            val fg = frameSnapshot.createGraphics()
            fg.drawImage(masterImage, 0, 0, null)
            fg.dispose()

            result.add(Pair(frameSnapshot.toComposeImageBitmap(), meta.delayMs))

            prevDisposal = meta.disposalMethod
            prevLeft = meta.left
            prevTop = meta.top
            prevWidth = meta.width
            prevHeight = meta.height
        }
        masterGraphics.dispose()
    } catch (_: Exception) {
    } finally {
        reader.dispose()
    }

    return result
}

private fun getFrameMeta(metadata: javax.imageio.metadata.IIOMetadata?, defaultWidth: Int, defaultHeight: Int): GifFrameMeta {
    var delayMs = 100L
    var disposalMethod = "none"
    var left = 0
    var top = 0
    var width = defaultWidth
    var height = defaultHeight

    if (metadata == null) return GifFrameMeta(delayMs, disposalMethod, left, top, width, height)

    try {
        val root = metadata.getAsTree(metadata.nativeMetadataFormatName)
        for (i in 0 until root.childNodes.length) {
            val node = root.childNodes.item(i)
            if (node.nodeName.equals("GraphicControlExtension", ignoreCase = true)) {
                val delayTime = (node as? IIOMetadataNode)?.getAttribute("delayTime")?.toIntOrNull()
                if (delayTime != null && delayTime > 0) {
                    delayMs = delayTime * 10L
                }
                val disposal = (node as? IIOMetadataNode)?.getAttribute("disposalMethod")
                if (!disposal.isNullOrBlank()) {
                    disposalMethod = disposal
                }
            } else if (node.nodeName.equals("ImageDescriptor", ignoreCase = true)) {
                val elem = node as? IIOMetadataNode
                left = elem?.getAttribute("imageLeftPosition")?.toIntOrNull() ?: 0
                top = elem?.getAttribute("imageTopPosition")?.toIntOrNull() ?: 0
                width = elem?.getAttribute("imageWidth")?.toIntOrNull() ?: defaultWidth
                height = elem?.getAttribute("imageHeight")?.toIntOrNull() ?: defaultHeight
            }
        }
    } catch (_: Exception) {
    }

    return GifFrameMeta(delayMs, disposalMethod, left, top, width, height)
}
