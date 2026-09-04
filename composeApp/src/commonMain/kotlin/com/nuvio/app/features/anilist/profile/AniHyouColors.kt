package com.nuvio.app.features.anilist.profile

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

// Exact AniHyou stat colors
val stat_dark_blue = Color(0xFFA9C7FF)
val stat_dark_onBlue = Color(0xFF003063)
val stat_dark_green = Color(0xFF45E267)
val stat_dark_onGreen = Color(0xFF003910)
val stat_dark_red = Color(0xFFFFB4AA)
val stat_dark_onRed = Color(0xFF690004)
val stat_dark_yellow = Color(0xFFEAC300)
val stat_dark_onYellow = Color(0xFF3B2F00)

val stat_dark_10 = Color(0xFFFFB3AE)
val stat_dark_on10 = Color(0xFF68000C)
val stat_dark_20 = Color(0xFFFFB694)
val stat_dark_on20 = Color(0xFF571F00)
val stat_dark_30 = Color(0xFFFFB695)
val stat_dark_on30 = Color(0xFF571E00)
val stat_dark_40 = Color(0xFFFFB871)
val stat_dark_on40 = Color(0xFF4A2800)
val stat_dark_50 = Color(0xFFF2BF48)
val stat_dark_on50 = Color(0xFF402D00)
val stat_dark_60 = Color(0xFF71D2FF)
val stat_dark_on60 = Color(0xFF003547)
val stat_dark_70 = Color(0xFF88CEFF)
val stat_dark_on70 = Color(0xFF00344D)
val stat_dark_80 = Color(0xFF4DDAD9)
val stat_dark_on80 = Color(0xFF003736)
val stat_dark_90 = Color(0xFF64DBB4)
val stat_dark_on90 = Color(0xFF003829)
val stat_dark_100 = Color(0xFFA0D57B)
val stat_dark_on100 = Color(0xFF163800)

@Composable
fun Int.point100PrimaryColor(): Color {
    return when {
        this == 0 -> MaterialTheme.colorScheme.outline
        this < 20 -> stat_dark_10
        this < 30 -> stat_dark_20
        this < 40 -> stat_dark_30
        this < 50 -> stat_dark_40
        this < 60 -> stat_dark_50
        this < 70 -> stat_dark_60
        this < 80 -> stat_dark_70
        this < 90 -> stat_dark_80
        this < 100 -> stat_dark_90
        this == 100 -> stat_dark_100
        else -> MaterialTheme.colorScheme.outline
    }
}

@Composable
fun Int.point100OnPrimaryColor(): Color {
    return when {
        this == 0 -> MaterialTheme.colorScheme.onSurface
        this < 20 -> stat_dark_on10
        this < 30 -> stat_dark_on20
        this < 40 -> stat_dark_on30
        this < 50 -> stat_dark_on40
        this < 60 -> stat_dark_on50
        this < 70 -> stat_dark_on60
        this < 80 -> stat_dark_on70
        this < 90 -> stat_dark_on80
        this < 100 -> stat_dark_on90
        this == 100 -> stat_dark_on100
        else -> MaterialTheme.colorScheme.onSurface
    }
}

@Composable
fun Double.point10DecimalPrimaryColor(): Color {
    return (this.roundToInt() * 10).point100PrimaryColor()
}

@Composable
fun Double.point10DecimalOnPrimaryColor(): Color {
    return (this.roundToInt() * 10).point100OnPrimaryColor()
}

@Composable
fun statusToPrimaryColor(status: String): Color {
    return when (status.uppercase()) {
        "CURRENT" -> stat_dark_green
        "COMPLETED" -> stat_dark_blue
        "PAUSED" -> stat_dark_yellow
        "DROPPED" -> stat_dark_red
        "PLANNING" -> MaterialTheme.colorScheme.outline
        "REPEATING" -> stat_dark_blue
        else -> MaterialTheme.colorScheme.outline
    }
}

@Composable
fun statusToOnPrimaryColor(status: String): Color {
    return when (status.uppercase()) {
        "CURRENT" -> stat_dark_onGreen
        "COMPLETED" -> stat_dark_onBlue
        "PAUSED" -> stat_dark_onYellow
        "DROPPED" -> stat_dark_onRed
        "PLANNING" -> MaterialTheme.colorScheme.onSurface
        "REPEATING" -> stat_dark_onBlue
        else -> MaterialTheme.colorScheme.onSurface
    }
}

@Composable
fun formatToPrimaryColor(format: String): Color {
    return when (format.uppercase()) {
        "TV" -> stat_dark_blue
        "TV_SHORT" -> stat_dark_10
        "MOVIE" -> stat_dark_80
        "SPECIAL" -> stat_dark_50
        "OVA" -> stat_dark_40
        "ONA" -> stat_dark_20
        "MUSIC" -> stat_dark_60
        "MANGA" -> stat_dark_blue
        "NOVEL" -> stat_dark_90
        "ONE_SHOT" -> stat_dark_70
        else -> MaterialTheme.colorScheme.outline
    }
}

@Composable
fun formatToOnPrimaryColor(format: String): Color {
    return when (format.uppercase()) {
        "TV" -> stat_dark_onBlue
        "TV_SHORT" -> stat_dark_on10
        "MOVIE" -> stat_dark_on80
        "SPECIAL" -> stat_dark_on50
        "OVA" -> stat_dark_on40
        "ONA" -> stat_dark_on20
        "MUSIC" -> stat_dark_on60
        "MANGA" -> stat_dark_onBlue
        "NOVEL" -> stat_dark_on90
        "ONE_SHOT" -> stat_dark_on70
        else -> MaterialTheme.colorScheme.onSurface
    }
}

@Composable
fun countryToPrimaryColor(country: String): Color {
    return when (country.uppercase()) {
        "JP" -> stat_dark_blue
        "KR" -> stat_dark_60
        "CN" -> stat_dark_red
        "TW" -> stat_dark_yellow
        else -> stat_dark_80
    }
}

@Composable
fun countryToOnPrimaryColor(country: String): Color {
    return when (country.uppercase()) {
        "JP" -> stat_dark_onBlue
        "KR" -> stat_dark_on60
        "CN" -> stat_dark_onRed
        "TW" -> stat_dark_onYellow
        else -> stat_dark_on80
    }
}

@Composable
fun yearToPrimaryColor(year: Int): Color {
    val mod = (year % 10)
    return when (mod) {
        0 -> stat_dark_10
        1 -> stat_dark_20
        2 -> stat_dark_30
        3 -> stat_dark_40
        4 -> stat_dark_50
        5 -> stat_dark_60
        6 -> stat_dark_70
        7 -> stat_dark_80
        8 -> stat_dark_90
        else -> stat_dark_100
    }
}
