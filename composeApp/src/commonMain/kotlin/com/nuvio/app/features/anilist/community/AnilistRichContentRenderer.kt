package com.nuvio.app.features.anilist.community

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

/**
 * Visual AST block elements for AniList rich markdown & HTML.
 */
sealed interface AnilistContentBlock {
    data class Header(val text: AnnotatedString, val level: Int, val isCentered: Boolean) : AnilistContentBlock
    data class Image(val url: String, val widthConstraint: Int? = null) : AnilistContentBlock
    data class Quote(val text: AnnotatedString, val isCentered: Boolean) : AnilistContentBlock
    data class Paragraph(val text: AnnotatedString, val isCentered: Boolean) : AnilistContentBlock
    data class Spoiler(val content: String, val isCentered: Boolean) : AnilistContentBlock
    object Divider : AnilistContentBlock
}

@Composable
fun AnilistRichContentRenderer(
    body: String,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val blocks = remember(body, primaryColor) {
        parseAnilistRichContent(body, primaryColor)
    }
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        blocks.forEach { block ->
            when (block) {
                is AnilistContentBlock.Header -> {
                    val fontSize = when (block.level) {
                        1 -> 20.sp
                        2 -> 18.sp
                        3 -> 16.sp
                        else -> 15.sp
                    }
                    val alignment = if (block.isCentered) TextAlign.Center else TextAlign.Start

                    Text(
                        text = block.text,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = fontSize,
                            fontWeight = FontWeight.Bold,
                            lineHeight = (fontSize.value * 1.35f).sp,
                            textAlign = alignment,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                is AnilistContentBlock.Image -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        val shape = RoundedCornerShape(12.dp)
                        ReviewMediaImage(
                            url = block.url,
                            contentDescription = "Review media",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 420.dp)
                                .clip(shape)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), shape),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }

                is AnilistContentBlock.Quote -> {
                    val shape = RoundedCornerShape(8.dp)
                    val alignment = if (block.isCentered) TextAlign.Center else TextAlign.Start

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(12.dp)
                            .height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.5.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.primary),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = block.text,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic,
                                lineHeight = 22.sp,
                                textAlign = alignment,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                is AnilistContentBlock.Divider -> {
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    )
                }

                is AnilistContentBlock.Spoiler -> {
                    InteractiveSpoilerBlock(
                        rawContent = block.content,
                        isCentered = block.isCentered,
                        primaryColor = primaryColor,
                    )
                }

                is AnilistContentBlock.Paragraph -> {
                    val alignment = if (block.isCentered) TextAlign.Center else TextAlign.Start
                    ClickableRichText(
                        annotatedString = block.text,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 24.sp,
                            textAlign = alignment,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f),
                        ),
                        onUrlClick = { url ->
                            uriHandler.openUri(url)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun InteractiveSpoilerBlock(
    rawContent: String,
    isCentered: Boolean,
    primaryColor: Color,
) {
    var revealed by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val parsedContent = remember(rawContent, primaryColor) {
        parseInlineAnilistMarkdown(rawContent, primaryColor)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            .clickable { revealed = !revealed }
            .padding(14.dp),
    ) {
        val alignment = if (isCentered) TextAlign.Center else TextAlign.Start
        ClickableRichText(
            annotatedString = parsedContent,
            style = MaterialTheme.typography.bodyLarge.copy(
                lineHeight = 24.sp,
                textAlign = alignment,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            modifier = if (!revealed) Modifier.blur(10.dp) else Modifier,
            onUrlClick = { url ->
                if (revealed) uriHandler.openUri(url)
            },
        )

        if (!revealed) {
            Surface(
                color = Color.Black.copy(alpha = 0.78f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.align(Alignment.Center),
            ) {
                Text(
                    text = "⚠️ Spoiler (Tap to reveal)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
        }
    }
}

@Composable
private fun ClickableRichText(
    annotatedString: AnnotatedString,
    style: TextStyle,
    modifier: Modifier = Modifier,
    onUrlClick: (String) -> Unit,
) {
    ClickableText(
        text = annotatedString,
        style = style,
        modifier = modifier.fillMaxWidth(),
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    onUrlClick(annotation.item)
                }
        },
    )
}

/**
 * Decodes all HTML entities including decimal (e.g. &#120328; for Unicode Math symbols) and hex.
 */
fun decodeHtmlEntities(input: String): String {
    if (input.isBlank()) return ""

    var text = input
        .replace("&quot;", "\"")
        .replace("&amp;", "&")
        .replace("&apos;", "'")
        .replace("&#039;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&nbsp;", " ")

    // Decimal entities: &#120328; -> Unicode Character
    text = Regex("&#(\\d+);").replace(text) { match ->
        val code = match.groupValues[1].toIntOrNull()
        if (code != null && code in 1..0x10FFFF) {
            try {
                if (code <= 0xFFFF) {
                    code.toChar().toString()
                } else {
                    val high = ((code - 0x10000) ushr 10) + 0xD800
                    val low = ((code - 0x10000) and 0x3FF) + 0xDC00
                    charArrayOf(high.toChar(), low.toChar()).concatToString()
                }
            } catch (e: Exception) {
                match.value
            }
        } else {
            match.value
        }
    }

    // Hex entities: &#x1D608; -> Unicode Character
    text = Regex("&#x([0-9a-fA-F]+);", RegexOption.IGNORE_CASE).replace(text) { match ->
        val code = match.groupValues[1].toIntOrNull(16)
        if (code != null && code in 1..0x10FFFF) {
            try {
                if (code <= 0xFFFF) {
                    code.toChar().toString()
                } else {
                    val high = ((code - 0x10000) ushr 10) + 0xD800
                    val low = ((code - 0x10000) and 0x3FF) + 0xDC00
                    charArrayOf(high.toChar(), low.toChar()).concatToString()
                }
            } catch (e: Exception) {
                match.value
            }
        } else {
            match.value
        }
    }

    return text
}

/**
 * Parses raw AniList markdown and HTML into structured visual blocks.
 */
internal fun parseAnilistRichContent(rawText: String, primaryColor: Color): List<AnilistContentBlock> {
    if (rawText.isBlank()) return emptyList()

    val decodedText = decodeHtmlEntities(rawText)
    val lines = decodedText.lines()
    val blocks = mutableListOf<AnilistContentBlock>()

    val currentParagraphLines = mutableListOf<String>()
    var currentParagraphCentered = false

    fun flushParagraph() {
        if (currentParagraphLines.isNotEmpty()) {
            val content = currentParagraphLines.joinToString("\n").trim()
            if (content.isNotBlank()) {
                val parsed = parseInlineAnilistMarkdown(content, primaryColor)
                blocks.add(AnilistContentBlock.Paragraph(text = parsed, isCentered = currentParagraphCentered))
            }
            currentParagraphLines.clear()
            currentParagraphCentered = false
        }
    }

    var i = 0
    while (i < lines.size) {
        val rawLine = lines[i]
        val trimmed = rawLine.trim()

        if (trimmed.isBlank()) {
            flushParagraph()
            i++
            continue
        }

        // 1. Divider line (e.g. ***, ---, ____, **********, <hr>, <hr/>, <center><hr></center>)
        if (trimmed.matches(Regex("^(?:<center>)?\\s*(?:<hr\\s*/?>|[-*_~]{3,})\\s*(?:</center>)?$", RegexOption.IGNORE_CASE))) {
            flushParagraph()
            blocks.add(AnilistContentBlock.Divider)
            i++
            continue
        }

        // 2. Standalone image line: img(url), img900(url), ~~~img(url)~~~, img(url)~~~, <center>img(url)</center>, <img src="...">
        val anilistImgMatch = Regex("^(?:<center>|~~~)?\\s*img(?:\\d+%?)?\\((https?://[^)]+)\\)\\s*(?:</center>|~~~)?$", RegexOption.IGNORE_CASE).find(trimmed)
        if (anilistImgMatch != null) {
            flushParagraph()
            val url = anilistImgMatch.groupValues[1]
            blocks.add(AnilistContentBlock.Image(url = url))
            i++
            continue
        }

        val htmlImgMatch = Regex("^(?:<center>|~~~)?\\s*<img[^>]*src=[\"'](https?://[^\"']+)[\"'][^>]*>\\s*(?:</center>|~~~)?$", RegexOption.IGNORE_CASE).find(trimmed)
        if (htmlImgMatch != null) {
            flushParagraph()
            val url = htmlImgMatch.groupValues[1]
            blocks.add(AnilistContentBlock.Image(url = url))
            i++
            continue
        }

        // 3. Multi-line or single-line spoiler: ~! ... !~
        if (trimmed.startsWith("~!")) {
            flushParagraph()
            val spoilerLines = mutableListOf<String>()
            if (trimmed.endsWith("!~") && trimmed.length > 4) {
                spoilerLines.add(trimmed.removePrefix("~!").removeSuffix("!~"))
            } else {
                spoilerLines.add(trimmed.removePrefix("~!"))
                i++
                while (i < lines.size && !lines[i].contains("!~")) {
                    spoilerLines.add(lines[i])
                    i++
                }
                if (i < lines.size) {
                    spoilerLines.add(lines[i].substringBefore("!~"))
                }
            }
            val spoilerBody = spoilerLines.joinToString("\n").trim()
            val isCentered = (spoilerBody.startsWith("~~~") && spoilerBody.endsWith("~~~")) ||
                (spoilerBody.startsWith("<center>", ignoreCase = true) && spoilerBody.endsWith("</center>", ignoreCase = true))
            val cleanSpoiler = spoilerBody
                .replace(Regex("^<center>", RegexOption.IGNORE_CASE), "")
                .replace(Regex("</center>$", RegexOption.IGNORE_CASE), "")
                .removePrefix("~~~").removeSuffix("~~~")
                .trim()
            blocks.add(AnilistContentBlock.Spoiler(content = cleanSpoiler, isCentered = isCentered))
            i++
            continue
        }

        // 4. Header line: # Heading, ## Heading, # <a>~~~Heading~~~</a>, #<center>Heading</center>
        val headerMatch = Regex("^(#{1,6})\\s*(.*)$").find(trimmed)
        if (headerMatch != null) {
            flushParagraph()
            val level = headerMatch.groupValues[1].length
            val rawHeaderText = headerMatch.groupValues[2].trim()
            val isCentered = rawHeaderText.contains("<center>", ignoreCase = true) ||
                rawHeaderText.contains("~~~")

            val cleanHeaderText = rawHeaderText
                .replace(Regex("</?[a-zA-Z0-9]+[^>]*>"), "")
                .replace(Regex("~+"), "")
                .trim()

            val parsed = parseInlineAnilistMarkdown(cleanHeaderText, primaryColor)
            blocks.add(AnilistContentBlock.Header(text = parsed, level = level, isCentered = isCentered))
            i++
            continue
        }

        // 5. Quote line: > Quote or ># Quote
        if (trimmed.startsWith(">")) {
            flushParagraph()
            val quoteLines = mutableListOf<String>()
            while (i < lines.size && lines[i].trim().startsWith(">")) {
                val qLine = lines[i].trim()
                    .replace(Regex("^>+\\s*"), "")
                    .replace(Regex("^#+\\s*"), "")
                quoteLines.add(qLine)
                i++
            }
            val quoteText = quoteLines.joinToString(" ").trim()
            val isCentered = quoteText.contains("<center>", ignoreCase = true) || quoteText.contains("~~~")
            val cleanQuote = quoteText
                .replace(Regex("</?[a-zA-Z0-9]+[^>]*>"), "")
                .replace(Regex("~+"), "")
                .trim()
            val parsed = parseInlineAnilistMarkdown(cleanQuote, primaryColor)
            blocks.add(AnilistContentBlock.Quote(text = parsed, isCentered = isCentered))
            continue
        }

        // 6. Regular Paragraph Line (Check if centered with ~~~ or <center>)
        val lineIsCentered = (trimmed.startsWith("~~~") && trimmed.endsWith("~~~")) ||
            (trimmed.startsWith("<center>", ignoreCase = true) && trimmed.endsWith("</center>", ignoreCase = true))

        var cleanLine = trimmed
            .replace(Regex("^<center>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("</center>$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^~{3,}|~{3,}$"), "")
            .trim()

        if (cleanLine.isNotBlank()) {
            if (currentParagraphLines.isEmpty()) {
                currentParagraphCentered = lineIsCentered
            }
            currentParagraphLines.add(cleanLine)
        }
        i++
    }

    flushParagraph()
    return blocks
}

/**
 * Parses inline markdown and HTML formatting into an AnnotatedString.
 */
internal fun parseInlineAnilistMarkdown(input: String, linkColor: Color): AnnotatedString {
    if (input.isBlank()) return AnnotatedString("")

    var text = input
        .replace("<br>", "\n")
        .replace("<br/>", "\n")
        .replace("<br />", "\n")

    return buildAnnotatedString {
        var remaining = text
        while (remaining.isNotEmpty()) {
            val boldMatch = Regex("^(?:\\*\\*(.*?)\\*\\*|<b>(.*?)</b>|<strong>(.*?)</strong>)", RegexOption.DOT_MATCHES_ALL).find(remaining)
            val italicMatch = Regex("^(?:\\*(.*?)\\*|_(.*?)_|<i>(.*?)</i>|<em>(.*?)</em>)", RegexOption.DOT_MATCHES_ALL).find(remaining)
            val underlineMatch = Regex("^<u>(.*?)</u>", RegexOption.DOT_MATCHES_ALL).find(remaining)
            val strikeMatch = Regex("^(?:~~(.*?)~~|<s>(.*?)</s>|<del>(.*?)</del>)", RegexOption.DOT_MATCHES_ALL).find(remaining)
            val linkMatch = Regex("^\\[(.*?)\\]\\((https?://.*?)\\)").find(remaining)
            val htmlLinkMatch = Regex("^<a\\s+href=[\"'](https?://[^\"']+)[\"'][^>]*>(.*?)</a>", RegexOption.IGNORE_CASE).find(remaining)
            val plainHtmlMatch = Regex("^<(/?[a-zA-Z0-9]+)[^>]*>").find(remaining)

            when {
                boldMatch != null -> {
                    val inner = boldMatch.groupValues.drop(1).firstOrNull { it.isNotEmpty() } ?: ""
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(inner)
                    pop()
                    remaining = remaining.substring(boldMatch.range.last + 1)
                }

                italicMatch != null -> {
                    val inner = italicMatch.groupValues.drop(1).firstOrNull { it.isNotEmpty() } ?: ""
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(inner)
                    pop()
                    remaining = remaining.substring(italicMatch.range.last + 1)
                }

                underlineMatch != null -> {
                    val inner = underlineMatch.groupValues[1]
                    pushStyle(SpanStyle(textDecoration = TextDecoration.Underline))
                    append(inner)
                    pop()
                    remaining = remaining.substring(underlineMatch.range.last + 1)
                }

                strikeMatch != null -> {
                    val inner = strikeMatch.groupValues.drop(1).firstOrNull { it.isNotEmpty() } ?: ""
                    pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                    append(inner)
                    pop()
                    remaining = remaining.substring(strikeMatch.range.last + 1)
                }

                linkMatch != null -> {
                    val label = linkMatch.groupValues[1]
                    val url = linkMatch.groupValues[2]
                    pushStringAnnotation(tag = "URL", annotation = url)
                    pushStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Medium))
                    append(label)
                    pop()
                    pop()
                    remaining = remaining.substring(linkMatch.range.last + 1)
                }

                htmlLinkMatch != null -> {
                    val url = htmlLinkMatch.groupValues[1]
                    val label = htmlLinkMatch.groupValues[2]
                    pushStringAnnotation(tag = "URL", annotation = url)
                    pushStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Medium))
                    append(label)
                    pop()
                    pop()
                    remaining = remaining.substring(htmlLinkMatch.range.last + 1)
                }

                plainHtmlMatch != null -> {
                    // Skip unstyled plain HTML wrapper tags like <center>, </center>, <div>, etc.
                    remaining = remaining.substring(plainHtmlMatch.range.last + 1)
                }

                else -> {
                    append(remaining.first())
                    remaining = remaining.substring(1)
                }
            }
        }
    }
}
