package com.nuvio.app.core.format

import com.nuvio.app.core.i18n.localizedMonthName
import com.nuvio.app.core.time.parseEpisodeReleaseLocalDate

/**
 * Formats ISO calendar dates (yyyy-MM-dd or yyyy-MM-ddTHH:mm:ss…) for UI as "2025 February 1".
 * Other strings (e.g. year-only "2024", human text from addons) are returned unchanged.
 */
fun formatReleaseDateForDisplay(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return raw
    val datePart = parseEpisodeReleaseLocalDate(trimmed) ?: return raw
    val parts = datePart.split('-')
    if (parts.size != 3) return raw
    val year = parts[0].toIntOrNull() ?: return raw
    val month = parts[1].toIntOrNull()?.takeIf { it in 1..12 } ?: return raw
    val day = parts[2].toIntOrNull()?.takeIf { it in 1..31 } ?: return raw
    return "$year ${localizedMonthName(month)} $day"
}

fun formatReleaseDateWithoutYear(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return raw
    val datePart = parseEpisodeReleaseLocalDate(trimmed) ?: return raw
    val parts = datePart.split('-')
    if (parts.size != 3) return raw
    val month = parts[1].toIntOrNull()?.takeIf { it in 1..12 } ?: return raw
    val day = parts[2].toIntOrNull()?.takeIf { it in 1..31 } ?: return raw
    return "${localizedMonthName(month)} $day"
}

/**
 * Parses a release/air string (ISO date, year-only, or timestamp prefix) for compact UI (e.g. year chips).
 */
fun extractReleaseYearForDisplay(raw: String): Int? {
    val t = raw.trim()
    if (t.isEmpty()) return null
    if (t.length == 4 && t.all { it.isDigit() }) {
        return t.toIntOrNull()?.takeIf { it in 1000..9999 }
    }
    val datePart = parseEpisodeReleaseLocalDate(t) ?: return null
    val yearStr = datePart.split('-').firstOrNull() ?: return null
    return yearStr.toIntOrNull()?.takeIf { it in 1000..9999 }
}

fun formatYearRange(startYear: Int?, endYear: Int?, status: String? = null): String? {
    if (startYear == null) return endYear?.toString()
    val isOngoing = status?.trim()?.uppercase() in setOf("RELEASING", "ONGOING", "RETURNING SERIES", "AIRING")
    if (isOngoing) {
        return "$startYear -"
    }
    if (endYear == null || startYear == endYear) return "$startYear"
    return "$startYear - $endYear"
}

/**
 * Strips HTML formatting tags, entity escapes, and source attributions from descriptions.
 */
fun cleanHtmlDescription(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    return raw
        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("<[^>]*>"), "")
        .replace(Regex("~!.*?!~", RegexOption.DOT_MATCHES_ALL), "")
        .replace(Regex("\\|\\|.*?\\|\\|", RegexOption.DOT_MATCHES_ALL), "")
        // Strip lines that are just social/profile links: e.g. "[Profile](url) | [Twitter](url)"
        .replace(Regex("(?m)^\\s*\\[[^\\]]+\\]\\([^)]+\\)(\\s*\\|\\s*\\[[^\\]]+\\]\\([^)]+\\))*\\s*$"), "")
        // Replace remaining markdown links [Label](url) with just Label
        .replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1")
        // Clean bold/italic markdown underscores and asterisks
        .replace(Regex("__(.*?)__"), "$1")
        .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
        .replace(Regex("(?m)(^|\\s)_(.*?)_(?=\\s|:|\$)"), "$1$2")
        .replace(Regex("(?m)(^|\\s)\\*(.*?)\\*(?=\\s|:|\$)"), "$1$2")
        .replace("&quot;", "\"")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&#039;", "'")
        .replace("&apos;", "'")
        .replace("&nbsp;", " ")
        .replace("&mdash;", "—")
        .replace("&ndash;", "–")
        .replace("&hellip;", "…")
        .replace(Regex("(?i)\\(Source:.*?\\)"), "")
        .replace(Regex("(?i)\\[Written by.*?\\]"), "")
        .replace(Regex("(?i)^Source:.*$", RegexOption.MULTILINE), "")
        .replace(Regex("(?i)^Note:.*$", RegexOption.MULTILINE), "")
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
        .takeIf { it.isNotEmpty() }
}
