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
                            .padding(top = 6.dp),
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
                        AsyncImage(
                            model = block.url,
                            contentDescription = "Review image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 400.dp)
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
 * Parses raw AniList markdown and HTML into visual blocks.
 */
internal fun parseAnilistRichContent(rawText: String, primaryColor: Color): List<AnilistContentBlock> {
    if (rawText.isBlank()) return emptyList()

    val rawBlocks = rawText.split(Regex("(?:\r?\n){2,}")).map { it.trim() }.filter { it.isNotBlank() }
    val result = mutableListOf<AnilistContentBlock>()

    for (rawBlock in rawBlocks) {
        // 1. Check for standalone dividers: <hr>, <hr/>, ---, ***, ___
        if (rawBlock.matches(Regex("^(?:<hr\\s*/?>|[-*_~]{3,})$", RegexOption.IGNORE_CASE))) {
            result.add(AnilistContentBlock.Divider)
            continue
        }

        // 2. Check for AniList img tag: img900(url), img(url), img100%(url), or <img src="...">
        val anilistImgMatch = Regex("^(?:<center>|~~~)?\\s*img(?:\\d+%?)?\\((https?://[^)]+)\\)\\s*(?:</center>|~~~)?$", RegexOption.IGNORE_CASE).find(rawBlock)
        if (anilistImgMatch != null) {
            val url = anilistImgMatch.groupValues[1]
            result.add(AnilistContentBlock.Image(url = url))
            continue
        }

        val htmlImgMatch = Regex("^(?:<center>|~~~)?\\s*<img[^>]*src=[\"'](https?://[^\"']+)[\"'][^>]*>\\s*(?:</center>|~~~)?$", RegexOption.IGNORE_CASE).find(rawBlock)
        if (htmlImgMatch != null) {
            val url = htmlImgMatch.groupValues[1]
            result.add(AnilistContentBlock.Image(url = url))
            continue
        }

        // 3. Check for standalone spoiler block: ~! ... !~
        if (rawBlock.startsWith("~!") && rawBlock.endsWith("!~") && rawBlock.length > 4) {
            val inner = rawBlock.removePrefix("~!").removeSuffix("!~").trim()
            val isCentered = inner.startsWith("~~~") && inner.endsWith("~~~") ||
                inner.startsWith("<center>", ignoreCase = true) && inner.endsWith("</center>", ignoreCase = true)
            val cleaned = inner
                .removePrefix("~~~").removeSuffix("~~~")
                .replace(Regex("^<center>", RegexOption.IGNORE_CASE), "")
                .replace(Regex("</center>$", RegexOption.IGNORE_CASE), "")
                .trim()
            result.add(AnilistContentBlock.Spoiler(content = cleaned, isCentered = isCentered))
            continue
        }

        // 4. Check for headers: # Title, ## Title, #<center>Title</center>, #<center><a>Title</a></center>
        val headerMatch = Regex("^(#{1,6})\\s*(.*)$", RegexOption.DOT_MATCHES_ALL).find(rawBlock)
        if (headerMatch != null) {
            val level = headerMatch.groupValues[1].length
            var headerContent = headerMatch.groupValues[2].trim()
            val isCentered = headerContent.startsWith("~~~") ||
                headerContent.startsWith("<center>", ignoreCase = true)

            headerContent = headerContent
                .replace(Regex("^<center>", RegexOption.IGNORE_CASE), "")
                .replace(Regex("</center>$", RegexOption.IGNORE_CASE), "")
                .removePrefix("~~~").removeSuffix("~~~")
                .trim()

            val parsedHeader = parseInlineAnilistMarkdown(headerContent, primaryColor)
            result.add(AnilistContentBlock.Header(text = parsedHeader, level = level, isCentered = isCentered))
            continue
        }

        // 5. Check for Blockquotes: > Quote or ># Quote
        if (rawBlock.startsWith(">")) {
            val isCentered = rawBlock.contains("<center>", ignoreCase = true) || rawBlock.contains("~~~")
            val cleanQuote = rawBlock
                .lines()
                .joinToString(" ") { line ->
                    line.trim()
                        .replace(Regex("^>+\\s*"), "")
                        .replace(Regex("^#+\\s*"), "")
                }
                .replace(Regex("^<center>", RegexOption.IGNORE_CASE), "")
                .replace(Regex("</center>$", RegexOption.IGNORE_CASE), "")
                .removePrefix("~~~").removeSuffix("~~~")
                .trim()

            val parsedQuote = parseInlineAnilistMarkdown(cleanQuote, primaryColor)
            result.add(AnilistContentBlock.Quote(text = parsedQuote, isCentered = isCentered))
            continue
        }

        // 6. Check for Centered Paragraphs: ~~~Text~~~ or <center>Text</center>
        val isExplicitCenter = (rawBlock.startsWith("~~~") && rawBlock.endsWith("~~~")) ||
            (rawBlock.startsWith("<center>", ignoreCase = true) && rawBlock.endsWith("</center>", ignoreCase = true))

        var cleanParagraph = rawBlock
            .replace(Regex("^<center>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("</center>$", RegexOption.IGNORE_CASE), "")
            .removePrefix("~~~").removeSuffix("~~~")
            .trim()

        // Handle inline <hr> if present inside <center> block
        if (cleanParagraph.contains("<hr>", ignoreCase = true) || cleanParagraph.contains("<hr/>", ignoreCase = true)) {
            val parts = cleanParagraph.split(Regex("<hr\\s*/?>", RegexOption.IGNORE_CASE))
            parts.forEachIndexed { index, part ->
                val trimmedPart = part.trim()
                if (trimmedPart.isNotBlank()) {
                    val parsed = parseInlineAnilistMarkdown(trimmedPart, primaryColor)
                    result.add(AnilistContentBlock.Paragraph(text = parsed, isCentered = isExplicitCenter))
                }
                if (index < parts.size - 1) {
                    result.add(AnilistContentBlock.Divider)
                }
            }
            continue
        }

        val parsedText = parseInlineAnilistMarkdown(cleanParagraph, primaryColor)
        result.add(AnilistContentBlock.Paragraph(text = parsedText, isCentered = isExplicitCenter))
    }

    return result
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
        .replace("&quot;", "\"")
        .replace("&amp;", "&")
        .replace("&#039;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&nbsp;", " ")

    return buildAnnotatedString {
        val inlineImgRegex = Regex("img(?:\\d+%?)?\\((https?://[^)]+)\\)", RegexOption.IGNORE_CASE)

        // Replace inline img with clean label
        text = inlineImgRegex.replace(text) { match -> "[Image: ${match.groupValues[1]}]" }

        // Tokenize and format
        var remaining = text
        while (remaining.isNotEmpty()) {
            val boldMatch = Regex("^(?:\\*\\*(.*?)\\*\\*|<b>(.*?)</b>|<strong>(.*?)</strong>)", RegexOption.DOT_MATCHES_ALL).find(remaining)
            val italicMatch = Regex("^(?:\\*(.*?)\\*|_(.*?)_|<i>(.*?)</i>|<em>(.*?)</em>)", RegexOption.DOT_MATCHES_ALL).find(remaining)
            val underlineMatch = Regex("^<u>(.*?)</u>", RegexOption.DOT_MATCHES_ALL).find(remaining)
            val strikeMatch = Regex("^(?:~~(.*?)~~|~(.*?)~|<s>(.*?)</s>|<del>(.*?)</del>)", RegexOption.DOT_MATCHES_ALL).find(remaining)
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
