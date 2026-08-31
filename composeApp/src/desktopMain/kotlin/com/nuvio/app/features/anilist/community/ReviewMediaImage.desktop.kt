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
import coil3.compose.AsyncImage
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import javax.imageio.metadata.IIOMetadataNode

private val gifFrameCache = mutableMapOf<String, List<Pair<ImageBitmap, Long>>>()
private val httpClient by lazy { HttpClient() }

@Composable
internal actual fun ReviewMediaImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale,
) {
    val isGif = remember(url) {
        url.endsWith(".gif", ignoreCase = true) ||
            url.contains(".gif?", ignoreCase = true) ||
            url.contains("tenor.com", ignoreCase = true) ||
            url.contains("giphy.com", ignoreCase = true)
    }

    if (!isGif) {
        AsyncImage(
            model = url,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
        )
        return
    }

    var frames by remember(url) { mutableStateOf<List<Pair<ImageBitmap, Long>>?>(gifFrameCache[url]) }
    var currentFrameIndex by remember(url) { mutableIntStateOf(0) }

    LaunchedEffect(url) {
        if (frames != null) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try {
                val bytes = httpClient.get(url).readBytes()
                val decoded = decodeGifFrames(bytes)
                if (decoded.isNotEmpty()) {
                    gifFrameCache[url] = decoded
                    frames = decoded
                }
            } catch (_: Exception) {
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
            model = url,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
        )
    }
}

private fun decodeGifFrames(bytes: ByteArray): List<Pair<ImageBitmap, Long>> {
    val result = mutableListOf<Pair<ImageBitmap, Long>>()
    val stream = ImageIO.createImageInputStream(ByteArrayInputStream(bytes)) ?: return emptyList()
    val readers = ImageIO.getImageReadersByFormatName("gif")
    if (!readers.hasNext()) return emptyList()
    val reader = readers.next()
    reader.input = stream

    try {
        val numImages = reader.getNumImages(true)
        var masterImage: java.awt.image.BufferedImage? = null

        for (i in 0 until numImages) {
            val frameImage = reader.read(i)
            val metadata = reader.getImageMetadata(i)
            val delayMs = getFrameDelayMs(metadata)

            if (masterImage == null) {
                masterImage = java.awt.image.BufferedImage(
                    frameImage.width,
                    frameImage.height,
                    java.awt.image.BufferedImage.TYPE_INT_ARGB,
                )
            }

            val g = masterImage.createGraphics()
            g.drawImage(frameImage, 0, 0, null)
            g.dispose()

            val snapshot = java.awt.image.BufferedImage(
                masterImage.width,
                masterImage.height,
                java.awt.image.BufferedImage.TYPE_INT_ARGB,
            )
            val sg = snapshot.createGraphics()
            sg.drawImage(masterImage, 0, 0, null)
            sg.dispose()

            result.add(Pair(snapshot.toComposeImageBitmap(), delayMs))
        }
    } catch (_: Exception) {
    } finally {
        reader.dispose()
    }

    return result
}

private fun getFrameDelayMs(metadata: javax.imageio.metadata.IIOMetadata?): Long {
    if (metadata == null) return 100L
    try {
        val root = metadata.getAsTree(metadata.nativeMetadataFormatName)
        for (i in 0 until root.childNodes.length) {
            val node = root.childNodes.item(i)
            if (node.nodeName.equals("GraphicControlExtension", ignoreCase = true)) {
                val delayTime = (node as? IIOMetadataNode)?.getAttribute("delayTime")?.toIntOrNull()
                if (delayTime != null && delayTime > 0) {
                    return delayTime * 10L
                }
            }
        }
    } catch (_: Exception) {
    }
    return 100L
}
