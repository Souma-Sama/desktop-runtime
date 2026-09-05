package com.nuvio.app.features.details.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.nuvio.app.core.ui.NuvioModalBottomSheet
import com.nuvio.app.core.ui.dismissNuvioBottomSheet
import com.nuvio.app.features.anilist.AnilistAuthRepository
import com.nuvio.app.features.anilist.AnilistFuzzyDate
import com.nuvio.app.features.anilist.AnilistMediaListStatus
import com.nuvio.app.features.anilist.AnilistTrackerCoordinator
import com.nuvio.app.features.anilist.AnilistTrackerTheme
import com.nuvio.app.isDesktop
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.rating_anilist
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

// Status Color Tokens
internal val StatusColorWatching = Color(0xFF00A2FF)
private val StatusColorPlanning = Color(0xFFA855F7)
private val StatusColorCompleted = Color(0xFF10B981)
private val StatusColorOnHold = Color(0xFFF59E0B)
private val StatusColorDropped = Color(0xFFEF4444)
private val StatusColorRepeating = Color(0xFF06B6D4)

fun getStatusColor(status: AnilistMediaListStatus?): Color = when (status) {
    AnilistMediaListStatus.CURRENT -> StatusColorWatching
    AnilistMediaListStatus.PLANNING -> StatusColorPlanning
    AnilistMediaListStatus.COMPLETED -> StatusColorCompleted
    AnilistMediaListStatus.PAUSED -> StatusColorOnHold
    AnilistMediaListStatus.DROPPED -> StatusColorDropped
    AnilistMediaListStatus.REPEATING -> StatusColorRepeating
    null -> Color(0xFF00A2FF)
}

// Static Shape Tokens to prevent recomposition allocations
internal val ShapePill = CircleShape
internal val ShapeSheetTop = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
internal val ShapeDialog = RoundedCornerShape(26.dp)
internal val ShapeModal = RoundedCornerShape(22.dp)
internal val ShapeCard = RoundedCornerShape(18.dp)
internal val ShapeTile = RoundedCornerShape(14.dp)
internal val ShapeStepper = RoundedCornerShape(13.dp)
internal val ShapePoster = RoundedCornerShape(12.dp)
internal val ShapeEmblem = RoundedCornerShape(11.dp)
internal val ShapeAction = RoundedCornerShape(10.dp)
internal val ShapeRoundCapsule = RoundedCornerShape(999.dp)
internal val ShapeProgressBar = RoundedCornerShape(3.dp)

// Static Cached Utility Brushes
private val PosterBorderBrush = Brush.verticalGradient(
    listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.10f)),
)
internal val AnilistEmblemBgBrush = Brush.linearGradient(
    listOf(Color(0xFF00B4D8), Color(0xFF0077B6)),
)
internal val AnilistEmblemBorderBrush = Brush.verticalGradient(
    listOf(Color.White.copy(alpha = 0.50f), Color.White.copy(alpha = 0.20f)),
)
private val DeleteNormalBgBrush = Brush.verticalGradient(
    listOf(Color(0xFFEF4444).copy(alpha = 0.14f), Color(0xFFEF4444).copy(alpha = 0.05f)),
)
private val DeleteConfirmBgBrush = Brush.verticalGradient(
    listOf(Color(0xFFEF4444).copy(alpha = 0.28f), Color(0xFFEF4444).copy(alpha = 0.16f)),
)
private val DeleteNormalBorderBrush = Brush.verticalGradient(
    listOf(Color(0xFFEF4444).copy(alpha = 0.40f), Color(0xFFEF4444).copy(alpha = 0.15f)),
)
private val DeleteConfirmBorderBrush = Brush.verticalGradient(
    listOf(Color(0xFFEF4444).copy(alpha = 0.85f), Color(0xFFEF4444).copy(alpha = 0.50f)),
)

private data class TrackerStatusItem(
    val status: AnilistMediaListStatus,
    val icon: ImageVector,
    val color: Color,
)

private val TrackerStatusItems = listOf(
    TrackerStatusItem(AnilistMediaListStatus.CURRENT, Icons.Outlined.PlayCircle, StatusColorWatching),
    TrackerStatusItem(AnilistMediaListStatus.PLANNING, Icons.Default.CalendarToday, StatusColorPlanning),
    TrackerStatusItem(AnilistMediaListStatus.COMPLETED, Icons.Outlined.CheckCircle, StatusColorCompleted),
    TrackerStatusItem(AnilistMediaListStatus.PAUSED, Icons.Outlined.PauseCircle, StatusColorOnHold),
    TrackerStatusItem(AnilistMediaListStatus.DROPPED, Icons.Outlined.RemoveCircleOutline, StatusColorDropped),
    TrackerStatusItem(AnilistMediaListStatus.REPEATING, Icons.Default.Replay, StatusColorRepeating),
)

@Immutable
data class TrackerThemeTokens(
    val theme: AnilistTrackerTheme,
    val dialogBackground: Color,
    val dialogBorder: Brush,
    val headerBackground: Brush,
    val headerHairline: Brush,
    val cardBackground: Brush?,
    val cardBackgroundColor: Color,
    val cardBorder: Brush,
    val cardGleam: Brush?,
    val subtleChipBackground: Color,
    val subtleChipBorder: Brush,
    val stepperBackground: Brush,
    val stepperBorder: Brush,
    val stepperDivider: Color,
    val actionButtonBackground: Brush,
    val actionButtonBorder: Brush,
    val statusUnselectedBg: Brush,
    val statusUnselectedBorder: Brush,
    val statusSelectedBgs: Map<Color, Brush>,
    val statusSelectedBorders: Map<Color, Brush>,
    val scrimAlpha: Float,
)

private val FrostedGlassTokens by lazy {
    val selectedBgs = TrackerStatusItems.associate { item ->
        item.color to Brush.verticalGradient(
            listOf(item.color.copy(alpha = 0.32f), item.color.copy(alpha = 0.15f)),
        )
    }
    val selectedBorders = TrackerStatusItems.associate { item ->
        item.color to Brush.verticalGradient(
            listOf(item.color.copy(alpha = 0.90f), item.color.copy(alpha = 0.55f)),
        )
    }
    TrackerThemeTokens(
        theme = AnilistTrackerTheme.FROSTED_GLASS,
        dialogBackground = Color(0xFF0C101C).copy(alpha = 0.94f),
        dialogBorder = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.40f),
                Color.White.copy(alpha = 0.14f),
                Color.White.copy(alpha = 0.05f),
                Color.White.copy(alpha = 0.18f),
            ),
        ),
        headerBackground = Brush.verticalGradient(
            listOf(
                Color(0xFF182034).copy(alpha = 0.88f),
                Color(0xFF0F1424).copy(alpha = 0.88f),
            ),
        ),
        headerHairline = Brush.horizontalGradient(
            listOf(
                Color.Transparent,
                Color.White.copy(alpha = 0.18f),
                Color.White.copy(alpha = 0.06f),
                Color.Transparent,
            ),
        ),
        cardBackground = Brush.verticalGradient(
            listOf(
                Color(0xFF1A2236).copy(alpha = 0.74f),
                Color(0xFF121726).copy(alpha = 0.74f),
            ),
        ),
        cardBackgroundColor = Color(0xFF182033).copy(alpha = 0.74f),
        cardBorder = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.20f),
                Color.White.copy(alpha = 0.07f),
            ),
        ),
        cardGleam = Brush.horizontalGradient(
            listOf(
                Color.Transparent,
                Color.White.copy(alpha = 0.16f),
                Color.White.copy(alpha = 0.05f),
                Color.Transparent,
            ),
        ),
        subtleChipBackground = Color(0xFF1E283E).copy(alpha = 0.70f),
        subtleChipBorder = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.24f),
                Color.White.copy(alpha = 0.08f),
            ),
        ),
        stepperBackground = Brush.verticalGradient(
            listOf(
                Color(0xFF1A2236).copy(alpha = 0.82f),
                Color(0xFF131828).copy(alpha = 0.82f),
            ),
        ),
        stepperBorder = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.24f),
                Color.White.copy(alpha = 0.08f),
            ),
        ),
        stepperDivider = Color.White.copy(alpha = 0.15f),
        actionButtonBackground = Brush.verticalGradient(
            listOf(
                Color(0xFF1E283E).copy(alpha = 0.85f),
                Color(0xFF151C2C).copy(alpha = 0.85f),
            ),
        ),
        actionButtonBorder = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.28f),
                Color.White.copy(alpha = 0.10f),
            ),
        ),
        statusUnselectedBg = Brush.verticalGradient(
            listOf(
                Color(0xFF1A2236).copy(alpha = 0.65f),
                Color(0xFF121726).copy(alpha = 0.65f),
            ),
        ),
        statusUnselectedBorder = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.16f),
                Color.White.copy(alpha = 0.05f),
            ),
        ),
        statusSelectedBgs = selectedBgs,
        statusSelectedBorders = selectedBorders,
        scrimAlpha = 0.55f,
    )
}

private val WaterGlassTokens by lazy {
    val selectedBgs = TrackerStatusItems.associate { item ->
        item.color to Brush.verticalGradient(
            listOf(item.color.copy(alpha = 0.36f), item.color.copy(alpha = 0.16f)),
        )
    }
    val selectedBorders = TrackerStatusItems.associate { item ->
        item.color to Brush.verticalGradient(
            listOf(item.color.copy(alpha = 0.95f), item.color.copy(alpha = 0.60f)),
        )
    }
    TrackerThemeTokens(
        theme = AnilistTrackerTheme.WATER_GLASS,
        dialogBackground = Color(0xFF0C101D).copy(alpha = 0.72f),
        dialogBorder = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.45f),
                Color.White.copy(alpha = 0.16f),
                Color.White.copy(alpha = 0.05f),
                Color.White.copy(alpha = 0.15f),
            ),
        ),
        headerBackground = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.02f),
            ),
        ),
        headerHairline = Brush.horizontalGradient(
            listOf(
                Color.Transparent,
                Color.White.copy(alpha = 0.16f),
                Color.White.copy(alpha = 0.06f),
                Color.Transparent,
            ),
        ),
        cardBackground = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.035f),
            ),
        ),
        cardBackgroundColor = Color.Transparent,
        cardBorder = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.32f),
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.02f),
            ),
        ),
        cardGleam = Brush.horizontalGradient(
            listOf(
                Color.Transparent,
                Color.White.copy(alpha = 0.25f),
                Color.White.copy(alpha = 0.08f),
                Color.Transparent,
            ),
        ),
        subtleChipBackground = Color.White.copy(alpha = 0.08f),
        subtleChipBorder = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.22f),
                Color.White.copy(alpha = 0.08f),
            ),
        ),
        stepperBackground = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.04f),
            ),
        ),
        stepperBorder = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.30f),
                Color.White.copy(alpha = 0.08f),
            ),
        ),
        stepperDivider = Color.White.copy(alpha = 0.14f),
        actionButtonBackground = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.10f),
                Color.White.copy(alpha = 0.04f),
            ),
        ),
        actionButtonBorder = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.32f),
                Color.White.copy(alpha = 0.08f),
            ),
        ),
        statusUnselectedBg = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.065f),
                Color.White.copy(alpha = 0.025f),
            ),
        ),
        statusUnselectedBorder = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.18f),
                Color.White.copy(alpha = 0.06f),
            ),
        ),
        statusSelectedBgs = selectedBgs,
        statusSelectedBorders = selectedBorders,
        scrimAlpha = 0.35f,
    )
}

private val MidnightGlassTokens by lazy {
    val selectedBgs = TrackerStatusItems.associate { item ->
        item.color to Brush.verticalGradient(
            listOf(item.color.copy(alpha = 0.28f), item.color.copy(alpha = 0.12f)),
        )
    }
    val selectedBorders = TrackerStatusItems.associate { item ->
        item.color to Brush.verticalGradient(
            listOf(item.color.copy(alpha = 0.85f), item.color.copy(alpha = 0.45f)),
        )
    }
    TrackerThemeTokens(
        theme = AnilistTrackerTheme.MIDNIGHT_GLASS,
        dialogBackground = Color(0xFF070910).copy(alpha = 0.97f),
        dialogBorder = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.25f),
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.02f),
                Color.White.copy(alpha = 0.08f),
            ),
        ),
        headerBackground = Brush.verticalGradient(
            listOf(
                Color(0xFF141824).copy(alpha = 0.92f),
                Color(0xFF0D101A).copy(alpha = 0.92f),
            ),
        ),
        headerHairline = Brush.horizontalGradient(
            listOf(
                Color.Transparent,
                Color.White.copy(alpha = 0.12f),
                Color.White.copy(alpha = 0.04f),
                Color.Transparent,
            ),
        ),
        cardBackground = Brush.verticalGradient(
            listOf(
                Color(0xFF131724),
                Color(0xFF0D101A),
            ),
        ),
        cardBackgroundColor = Color(0xFF121622),
        cardBorder = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.14f),
                Color.White.copy(alpha = 0.04f),
            ),
        ),
        cardGleam = Brush.horizontalGradient(
            listOf(
                Color.Transparent,
                Color.White.copy(alpha = 0.10f),
                Color.Transparent,
            ),
        ),
        subtleChipBackground = Color(0xFF1A2030),
        subtleChipBorder = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.18f),
                Color.White.copy(alpha = 0.05f),
            ),
        ),
        stepperBackground = Brush.verticalGradient(
            listOf(
                Color(0xFF151926),
                Color(0xFF0F121C),
            ),
        ),
        stepperBorder = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.18f),
                Color.White.copy(alpha = 0.05f),
            ),
        ),
        stepperDivider = Color.White.copy(alpha = 0.10f),
        actionButtonBackground = Brush.verticalGradient(
            listOf(
                Color(0xFF192030),
                Color(0xFF111520),
            ),
        ),
        actionButtonBorder = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.20f),
                Color.White.copy(alpha = 0.06f),
            ),
        ),
        statusUnselectedBg = Brush.verticalGradient(
            listOf(
                Color(0xFF141824),
                Color(0xFF0E111A),
            ),
        ),
        statusUnselectedBorder = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.12f),
                Color.White.copy(alpha = 0.03f),
            ),
        ),
        statusSelectedBgs = selectedBgs,
        statusSelectedBorders = selectedBorders,
        scrimAlpha = 0.65f,
    )
}

private val SmokedGlassTokens by lazy {
    val selectedBgs = TrackerStatusItems.associate { item ->
        item.color to Brush.verticalGradient(
            listOf(Color.White, Color(0xFFF2F4F7)),
        )
    }
    val selectedBorders = TrackerStatusItems.associate { item ->
        item.color to Brush.verticalGradient(
            listOf(Color.White, Color.White.copy(alpha = 0.85f)),
        )
    }
    TrackerThemeTokens(
        theme = AnilistTrackerTheme.SMOKED_GLASS,
        dialogBackground = Color(0xFF14151C).copy(alpha = 0.58f),
        dialogBorder = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.28f),
                Color.White.copy(alpha = 0.10f),
                Color.White.copy(alpha = 0.04f),
                Color.White.copy(alpha = 0.14f),
            ),
        ),
        headerBackground = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.09f),
                Color.White.copy(alpha = 0.02f),
            ),
        ),
        headerHairline = Brush.horizontalGradient(
            listOf(
                Color.Transparent,
                Color.White.copy(alpha = 0.16f),
                Color.Transparent,
            ),
        ),
        cardBackground = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.07f),
                Color.White.copy(alpha = 0.03f),
            ),
        ),
        cardBackgroundColor = Color.White.copy(alpha = 0.05f),
        cardBorder = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.18f),
                Color.White.copy(alpha = 0.06f),
            ),
        ),
        cardGleam = Brush.horizontalGradient(
            listOf(
                Color.Transparent,
                Color.White.copy(alpha = 0.16f),
                Color.Transparent,
            ),
        ),
        subtleChipBackground = Color.White.copy(alpha = 0.08f),
        subtleChipBorder = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.22f),
                Color.White.copy(alpha = 0.07f),
            ),
        ),
        stepperBackground = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.035f),
            ),
        ),
        stepperBorder = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.20f),
                Color.White.copy(alpha = 0.06f),
            ),
        ),
        stepperDivider = Color.White.copy(alpha = 0.12f),
        actionButtonBackground = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.12f),
                Color.White.copy(alpha = 0.05f),
            ),
        ),
        actionButtonBorder = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.26f),
                Color.White.copy(alpha = 0.08f),
            ),
        ),
        statusUnselectedBg = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.06f),
                Color.White.copy(alpha = 0.025f),
            ),
        ),
        statusUnselectedBorder = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.15f),
                Color.White.copy(alpha = 0.05f),
            ),
        ),
        statusSelectedBgs = selectedBgs,
        statusSelectedBorders = selectedBorders,
        scrimAlpha = 0.30f,
    )
}

fun getTrackerThemeTokens(theme: AnilistTrackerTheme): TrackerThemeTokens = when (theme) {
    AnilistTrackerTheme.FROSTED_GLASS -> FrostedGlassTokens
    AnilistTrackerTheme.WATER_GLASS -> WaterGlassTokens
    AnilistTrackerTheme.MIDNIGHT_GLASS -> MidnightGlassTokens
    AnilistTrackerTheme.SMOKED_GLASS -> SmokedGlassTokens
}

val LocalTrackerThemeTokens = staticCompositionLocalOf {
    FrostedGlassTokens
}

@Composable
internal fun TrackerGlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = ShapeCard,
    backgroundColor: Color? = null,
    backgroundBrush: Brush? = null,
    borderBrush: Brush? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = LocalTrackerThemeTokens.current
    val effectiveBgModifier = when {
        backgroundBrush != null -> Modifier.background(backgroundBrush)
        backgroundColor != null -> Modifier.background(backgroundColor)
        tokens.cardBackground != null -> Modifier.background(tokens.cardBackground)
        else -> Modifier.background(tokens.cardBackgroundColor)
    }

    val effectiveBorder = borderBrush ?: tokens.cardBorder
    val borderModifier = Modifier.border(1.dp, effectiveBorder, shape)

    val clickModifier = if (onClick != null) {
        Modifier.clickable(role = Role.Button, onClick = onClick)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .clip(shape)
            .then(effectiveBgModifier)
            .then(borderModifier)
            .then(clickModifier),
    ) {
        val gleam = tokens.cardGleam
        if (gleam != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(gleam),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            content = content,
        )
    }
}

@Composable
fun AnimeTrackerButton(
    modifier: Modifier = Modifier,
    meta: com.nuvio.app.features.details.MetaDetails? = null,
    title: String? = null,
    size: Dp = 52.dp,
) {
    val anilistPrefs by com.nuvio.app.features.anilist.AnilistPreferencesRepository.preferences.collectAsState()
    val isKaiItem = remember(meta?.id) {
        AnilistTrackerCoordinator.isKaiMedia(meta?.id)
    }
    if (!anilistPrefs.enabled || !isKaiItem) return

    var showSheet by remember { mutableStateOf(false) }
    val trackerState by AnilistTrackerCoordinator.trackerState.collectAsState()
    val effectiveTitle = title?.takeIf { it.isNotBlank() } ?: meta?.name.orEmpty()
    val hapticFeedback = LocalHapticFeedback.current

    val anilistId = remember(meta?.id) { AnilistTrackerCoordinator.extractAnilistId(meta?.id) }
    val currentStatus = trackerState.entry?.takeIf {
        trackerState.media != null && (
            (anilistId != null && trackerState.media?.id == anilistId) ||
            trackerState.lastLookupTitle.equals(effectiveTitle, ignoreCase = true)
        )
    }?.status ?: com.nuvio.app.features.anilist.AnilistLibraryRepository.getMediaStatusById(meta?.id.orEmpty(), effectiveTitle)

    val isTrackingActive = currentStatus != null
    val statusColor = getStatusColor(currentStatus)

    val containerColor by animateColorAsState(
        targetValue = when {
            isTrackingActive -> statusColor.copy(alpha = 0.22f)
            showSheet -> MaterialTheme.colorScheme.onBackground
            else -> Color(0xFF101524).copy(alpha = 0.82f)
        },
        animationSpec = tween(250),
        label = "TrackerButtonBg",
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isTrackingActive -> statusColor.copy(alpha = 0.85f)
            showSheet -> MaterialTheme.colorScheme.primary
            else -> Color.White.copy(alpha = 0.22f)
        },
        animationSpec = tween(250),
        label = "TrackerButtonBorder",
    )

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .border(
                    width = if (isTrackingActive || showSheet) 1.5.dp else 1.dp,
                    color = borderColor,
                    shape = CircleShape,
                ),
            shape = CircleShape,
            color = containerColor,
            tonalElevation = if (isTrackingActive) 8.dp else 4.dp,
        ) {
            Box(
                modifier = Modifier
                    .size(size)
                    .clickable(role = Role.Button) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showSheet = true
                        val metaYear = meta?.releaseInfo?.take(4)?.toIntOrNull()
                        AnilistTrackerCoordinator.loadForMedia(
                            title = effectiveTitle,
                            mediaId = meta?.id,
                            year = metaYear,
                            genres = meta?.genres.orEmpty(),
                            country = meta?.country,
                            language = meta?.language,
                            forceRefresh = false,
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (trackerState.isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = statusColor,
                    )
                } else {
                    Image(
                        painter = painterResource(Res.drawable.rating_anilist),
                        contentDescription = "AniList Tracker",
                        modifier = Modifier.size(26.dp),
                    )

                    // Active tracking indicator dot
                    if (isTrackingActive) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor),
                        )
                    }
                }
            }
        }

        if (showSheet) {
            AnimeTrackerSheet(
                meta = meta,
                title = effectiveTitle,
                onDismiss = { showSheet = false },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeTrackerSheet(
    meta: com.nuvio.app.features.details.MetaDetails? = null,
    preview: com.nuvio.app.features.home.MetaPreview? = null,
    title: String? = null,
    onDismiss: () -> Unit,
) {
    val effectiveTitle = title?.takeIf { it.isNotBlank() } ?: preview?.name ?: meta?.name.orEmpty()
    val mediaId = preview?.id ?: meta?.id
    val year = preview?.releaseInfo?.take(4)?.toIntOrNull() ?: meta?.releaseInfo?.take(4)?.toIntOrNull()
    val genres = preview?.genres ?: meta?.genres.orEmpty()
    val country = meta?.country
    val language = meta?.language

    val anilistPrefs by com.nuvio.app.features.anilist.AnilistPreferencesRepository.preferences.collectAsState()
    val currentTheme = anilistPrefs.trackerTheme
    val themeTokens = remember(currentTheme) { getTrackerThemeTokens(currentTheme) }

    LaunchedEffect(effectiveTitle, mediaId) {
        if (effectiveTitle.isNotBlank()) {
            AnilistTrackerCoordinator.loadForMedia(
                title = effectiveTitle,
                mediaId = mediaId,
                year = year,
                genres = genres,
                country = country,
                language = language,
                forceRefresh = false,
            )
        }
    }

    if (isDesktop) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.88f)
                    .widthIn(min = 480.dp, max = 530.dp)
                    .shadow(
                        elevation = 20.dp,
                        shape = ShapeDialog,
                        spotColor = Color.Black.copy(alpha = 0.50f),
                        ambientColor = Color.Black.copy(alpha = 0.30f),
                    )
                    .clip(ShapeDialog)
                    .background(themeTokens.dialogBackground)
                    .border(1.dp, themeTokens.dialogBorder, ShapeDialog),
            ) {
                // Subtle liquid glass top highlight sheen (identical to FloatingGlassDesktopSidebar)
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(ShapeDialog)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.14f),
                                    Color.White.copy(alpha = 0.03f),
                                    Color.Transparent,
                                ),
                            ),
                        ),
                )

                AnimeTrackerSheetContent(
                    meta = meta,
                    preview = preview,
                    title = effectiveTitle,
                    currentTheme = currentTheme,
                    themeTokens = themeTokens,
                    onClose = onDismiss,
                )
            }
        }
    } else {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val coroutineScope = rememberCoroutineScope()

        NuvioModalBottomSheet(
            onDismissRequest = {
                coroutineScope.launch {
                    dismissNuvioBottomSheet(sheetState = sheetState, onDismiss = onDismiss)
                }
            },
            sheetState = sheetState,
            containerColor = themeTokens.dialogBackground,
            shape = ShapeSheetTop,
            showDragHandle = true,
        ) {
            AnimeTrackerSheetContent(
                meta = meta,
                preview = preview,
                title = effectiveTitle,
                currentTheme = currentTheme,
                themeTokens = themeTokens,
                onClose = {
                    coroutineScope.launch {
                        dismissNuvioBottomSheet(sheetState = sheetState, onDismiss = onDismiss)
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeTrackerSheetContent(
    meta: com.nuvio.app.features.details.MetaDetails? = null,
    preview: com.nuvio.app.features.home.MetaPreview? = null,
    title: String? = null,
    currentTheme: AnilistTrackerTheme = AnilistTrackerTheme.FROSTED_GLASS,
    themeTokens: TrackerThemeTokens = remember(currentTheme) { getTrackerThemeTokens(currentTheme) },
    onClose: () -> Unit,
) {
    val trackerState by AnilistTrackerCoordinator.trackerState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    var tokenInput by remember { mutableStateOf("") }
    var showTokenInput by remember { mutableStateOf(false) }
    var manualSearchText by remember { mutableStateOf("") }
    val uriHandler = LocalUriHandler.current
    var showUserProfileSheet by rememberSaveable { mutableStateOf(false) }

    CompositionLocalProvider(LocalTrackerThemeTokens provides themeTokens) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
        ) {
            // --- 1. PINNED APPLE GLASS HEADER BAR ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(themeTokens.headerBackground)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(11.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(ShapeEmblem)
                                .background(AnilistEmblemBgBrush)
                                .border(1.dp, AnilistEmblemBorderBrush, ShapeEmblem),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(Res.drawable.rating_anilist),
                                contentDescription = "AniList",
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = "AniList Tracker",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    letterSpacing = (-0.2).sp,
                                ),
                                color = Color.White,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(ShapePill)
                                        .background(if (trackerState.isAuthenticated) Color(0xFF10B981) else Color(0xFFF59E0B)),
                                )
                                Text(
                                    text = if (trackerState.isAuthenticated) "Cloud Synced" else "Not Logged In",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                    ),
                                    color = Color.White.copy(alpha = 0.55f),
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (trackerState.isAuthenticated) {
                            val user = trackerState.user
                            Box(
                                modifier = Modifier
                                    .clip(ShapeRoundCapsule)
                                    .background(themeTokens.subtleChipBackground)
                                    .border(1.dp, themeTokens.subtleChipBorder, ShapeRoundCapsule)
                                    .clickable(role = Role.Button) { showUserProfileSheet = true }
                                    .padding(horizontal = 9.dp, vertical = 4.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    val avatarUrl = user?.avatarUrl
                                    if (avatarUrl != null) {
                                        AsyncImage(
                                            model = avatarUrl,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(17.dp)
                                                .clip(ShapePill),
                                            contentScale = ContentScale.Crop,
                                        )
                                    }
                                    Text(
                                        text = user?.name ?: "Connected",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.5.sp,
                                        ),
                                        color = Color.White.copy(alpha = 0.90f),
                                        maxLines = 1,
                                    )
                                }
                            }
                        }

                        // Theme switcher palette button
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(ShapePill)
                                .background(themeTokens.subtleChipBackground)
                                .border(1.dp, themeTokens.subtleChipBorder, ShapePill)
                                .clickable(role = Role.Button) {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val nextTheme = when (currentTheme) {
                                        AnilistTrackerTheme.FROSTED_GLASS -> AnilistTrackerTheme.WATER_GLASS
                                        AnilistTrackerTheme.WATER_GLASS -> AnilistTrackerTheme.MIDNIGHT_GLASS
                                        AnilistTrackerTheme.MIDNIGHT_GLASS -> AnilistTrackerTheme.SMOKED_GLASS
                                        AnilistTrackerTheme.SMOKED_GLASS -> AnilistTrackerTheme.FROSTED_GLASS
                                    }
                                    com.nuvio.app.features.anilist.AnilistPreferencesRepository.setTrackerTheme(nextTheme)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Palette,
                                contentDescription = "Theme: ${currentTheme.label}",
                                modifier = Modifier.size(15.dp),
                                tint = Color.White.copy(alpha = 0.85f),
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(ShapePill)
                                .background(themeTokens.subtleChipBackground)
                                .border(1.dp, themeTokens.subtleChipBorder, ShapePill)
                                .clickable(role = Role.Button) { onClose() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                modifier = Modifier.size(15.dp),
                                tint = Color.White.copy(alpha = 0.85f),
                            )
                        }
                    }
                }
            }

            // Hairline divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(themeTokens.headerHairline),
            )

        // --- 2. SCROLLABLE WATER GLASS BODY ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            // --- AUTHENTICATION PROMPT (If not logged in) ---
            if (!trackerState.isAuthenticated) {
                TrackerGlassCard {
                    Text(
                        text = "Connect your AniList account to synchronize your watch history, scores, dates, notes, and custom lists across all your devices.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                        ),
                        color = Color.White.copy(alpha = 0.70f),
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    if (!showTokenInput) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Button(
                                onClick = {
                                    uriHandler.openUri(AnilistAuthRepository.OAUTH_AUTHORIZE_URL)
                                },
                                shape = ShapePoster,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00A2FF),
                                    contentColor = Color.White,
                                ),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Authorize", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { showTokenInput = true },
                                shape = ShapePoster,
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                            ) {
                                Text("Paste Token", fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))
                            }
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = tokenInput,
                                onValueChange = { tokenInput = it },
                                placeholder = { Text("Paste AniList Token / Pin URL...", fontSize = 12.sp, color = Color.White.copy(0.35f)) },
                                singleLine = true,
                                shape = ShapePoster,
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, color = Color.White),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00A2FF),
                                    unfocusedBorderColor = Color.White.copy(0.18f),
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Button(
                                    onClick = {
                                        if (tokenInput.isNotBlank()) {
                                            coroutineScope.launch {
                                                val token = tokenInput.substringAfter("access_token=").substringBefore("&").trim()
                                                AnilistAuthRepository.loginWithToken(token)
                                                showTokenInput = false
                                            }
                                        }
                                    },
                                    enabled = tokenInput.isNotBlank(),
                                    shape = ShapeAction,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00A2FF),
                                        contentColor = Color.White,
                                    ),
                                ) {
                                    Text("Save Token", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                TextButton(
                                    onClick = { showTokenInput = false },
                                ) {
                                    Text("Cancel", fontSize = 13.sp, color = Color.White.copy(alpha = 0.60f))
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                return@Column
            }

            // --- LOADING STATE ---
            if (trackerState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 2.5.dp,
                            color = Color(0xFF00A2FF),
                        )
                        Text(
                            text = "Matching anime on AniList...",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            color = Color.White.copy(alpha = 0.65f),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                return@Column
            }

            // --- UNMATCHED ANIME STATE ---
            if (trackerState.media == null) {
                TrackerGlassCard {
                    Text(
                        text = trackerState.error ?: "Could not automatically match this anime on AniList.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val effectiveRetryTitle = title?.takeIf { it.isNotBlank() } ?: preview?.name ?: meta?.name.orEmpty()
                    if (effectiveRetryTitle.isNotBlank() || preview != null || meta != null) {
                        Button(
                            onClick = {
                                val metaYear = preview?.releaseInfo?.take(4)?.toIntOrNull() ?: meta?.releaseInfo?.take(4)?.toIntOrNull()
                                AnilistTrackerCoordinator.loadForMedia(
                                    title = effectiveRetryTitle,
                                    mediaId = preview?.id ?: meta?.id,
                                    year = metaYear,
                                    genres = preview?.genres ?: meta?.genres.orEmpty(),
                                    country = meta?.country,
                                    language = meta?.language,
                                    forceRefresh = true,
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = ShapePoster,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00A2FF),
                                contentColor = Color.White,
                            ),
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry Auto-Detect", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = manualSearchText,
                        onValueChange = { manualSearchText = it },
                        placeholder = { Text("Search title or enter AniList ID...", fontSize = 12.sp, color = Color.White.copy(0.35f)) },
                        singleLine = true,
                        shape = ShapePoster,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00A2FF),
                            unfocusedBorderColor = Color.White.copy(0.18f),
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (manualSearchText.isNotBlank()) {
                                val query = manualSearchText.trim()
                                val numId = query.toIntOrNull()
                                if (numId != null) {
                                    AnilistTrackerCoordinator.loadForMedia(
                                        title = query,
                                        mediaId = "anilist:$numId",
                                        forceRefresh = true,
                                    )
                                } else {
                                    AnilistTrackerCoordinator.loadForMedia(
                                        title = query,
                                        forceRefresh = true,
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = ShapePoster,
                        enabled = manualSearchText.isNotBlank(),
                    ) {
                        Text("Search AniList", fontSize = 13.sp)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                return@Column
            }

            val media = trackerState.media!!
            val entry = trackerState.entry
            val isReading = remember(media.format) {
                com.nuvio.app.features.anilist.KaiHooks.isNonVideoMedia(media.format)
            }

            // --- 3. MATCHED ANIME / MANGA HERO CARD ---
            TrackerGlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // Cover Poster
                    val cover = media.coverImage?.large ?: media.coverImage?.medium
                    Box(
                        modifier = Modifier
                            .size(width = 54.dp, height = 76.dp)
                            .clip(ShapePoster)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, PosterBorderBrush, ShapePoster),
                    ) {
                        if (cover != null) {
                            AsyncImage(
                                model = cover,
                                contentDescription = media.title?.displayTitle,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }

                    // Metadata Details
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = media.title?.displayTitle ?: if (isReading) "Manga" else "Anime",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                lineHeight = 18.sp,
                            ),
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )

                        val metaLine = if (isReading) {
                            listOfNotNull(
                                media.format?.replace("_", " ")?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Manga",
                                media.chapters?.let { "$it ch" },
                                media.volumes?.let { "$it vol" },
                                media.startDateYear?.toString(),
                            ).joinToString(" • ")
                        } else {
                            listOfNotNull(
                                media.format?.takeIf { it.isNotBlank() },
                                media.episodes?.let { "$it eps" },
                                media.startDateYear?.toString(),
                            ).joinToString(" • ")
                        }

                        if (metaLine.isNotBlank()) {
                            Text(
                                text = metaLine,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = Color.White.copy(alpha = 0.55f),
                                maxLines = 1,
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (media.averageScore != null && media.averageScore > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(ShapeRoundCapsule)
                                        .background(Color(0xFFFFB800).copy(alpha = 0.14f))
                                        .border(1.dp, Color(0xFFFFB800).copy(alpha = 0.35f), ShapeRoundCapsule)
                                        .padding(horizontal = 7.dp, vertical = 2.dp),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color(0xFFFFB800),
                                            modifier = Modifier.size(11.dp),
                                        )
                                        Text(
                                            text = "${media.averageScore}% Score",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                            ),
                                            color = Color(0xFFFFB800),
                                        )
                                    }
                                }
                            }

                            val mediaWebUrl = "https://anilist.co/${if (isReading) "manga" else "anime"}/${media.id}"
                            Box(
                                modifier = Modifier
                                    .clip(ShapeRoundCapsule)
                                    .background(themeTokens.subtleChipBackground)
                                    .border(1.dp, themeTokens.subtleChipBorder, ShapeRoundCapsule)
                                    .clickable(role = Role.Button) { uriHandler.openUri(mediaWebUrl) }
                                    .padding(horizontal = 7.dp, vertical = 2.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    Text(
                                        text = "AniList",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        ),
                                        color = Color.White.copy(alpha = 0.80f),
                                    )
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.80f),
                                        modifier = Modifier.size(10.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- 4. STATUS CAPSULES (2x3 TILES) ---
            Text(
                text = if (isReading) "Reading Status" else "Watch Status",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 0.2.sp,
                ),
                color = Color.White.copy(alpha = 0.90f),
            )

            Spacer(modifier = Modifier.height(8.dp))

            val currentStatus = entry?.status

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TrackerStatusItems.chunked(2).forEach { rowStatuses ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowStatuses.forEach { item ->
                            val isSelected = currentStatus == item.status
                            val bgBrush = if (isSelected) {
                                themeTokens.statusSelectedBgs[item.color] ?: themeTokens.statusUnselectedBg
                            } else {
                                themeTokens.statusUnselectedBg
                            }
                            val borderBrush = if (isSelected) {
                                themeTokens.statusSelectedBorders[item.color] ?: themeTokens.statusUnselectedBorder
                            } else {
                                themeTokens.statusUnselectedBorder
                            }

                            val isSmokedGlass = themeTokens.theme == AnilistTrackerTheme.SMOKED_GLASS
                            val tileModifier = if (isSelected && isSmokedGlass) {
                                Modifier
                                    .weight(1f)
                                    .shadow(
                                        elevation = 6.dp,
                                        shape = ShapeTile,
                                        spotColor = Color.White.copy(alpha = 0.30f),
                                    )
                                    .clip(ShapeTile)
                                    .background(bgBrush)
                                    .border(1.dp, borderBrush, ShapeTile)
                            } else {
                                Modifier
                                    .weight(1f)
                                    .clip(ShapeTile)
                                    .background(bgBrush)
                                    .border(if (isSelected) 1.5.dp else 1.dp, borderBrush, ShapeTile)
                            }

                            Box(
                                modifier = tileModifier
                                    .clickable(role = Role.Button) {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        AnilistTrackerCoordinator.updateStatus(item.status)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 11.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f, fill = false),
                                    ) {
                                        val iconColor = when {
                                            isSelected && isSmokedGlass -> Color(0xFF12131A)
                                            isSelected -> item.color
                                            else -> Color.White.copy(alpha = 0.65f)
                                        }
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = iconColor,
                                        )
                                        val statusLabel = when (item.status) {
                                            AnilistMediaListStatus.CURRENT -> if (isReading) "Reading" else "Watching"
                                            AnilistMediaListStatus.PLANNING -> if (isReading) "Plan to Read" else "Plan to Watch"
                                            AnilistMediaListStatus.REPEATING -> if (isReading) "Rereading" else "Rewatching"
                                            else -> item.status.label
                                        }
                                        val textColor = when {
                                            isSelected && isSmokedGlass -> Color(0xFF12131A)
                                            isSelected -> item.color
                                            else -> Color.White.copy(alpha = 0.82f)
                                        }
                                        Text(
                                            text = statusLabel,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 12.5.sp,
                                            ),
                                            color = textColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }

                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(ShapePill)
                                                .background(item.color)
                                                .border(1.dp, if (isSmokedGlass) Color.Black.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.50f), ShapePill),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- 5. PROGRESS STEPPERS (CONTINUOUS WATER GLASS PILL) ---
            val activeColor = getStatusColor(currentStatus)
            if (isReading) {
                // Chapters Stepper
                TrackerProgressStepperSection(
                    title = "Chapter Progress",
                    icon = Icons.Default.EditNote,
                    unitLabel = "Ch",
                    currentUnits = entry?.progress ?: 0,
                    totalUnits = media.chapters,
                    accentColor = activeColor,
                    onIncrement = { AnilistTrackerCoordinator.incrementProgress() },
                    onDecrement = { AnilistTrackerCoordinator.decrementProgress() },
                    onSetExact = { target -> AnilistTrackerCoordinator.updateProgress(target) },
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Volumes Stepper
                val currentVolumes = entry?.progressVolumes ?: 0
                val totalVolumes = media.volumes
                TrackerProgressStepperSection(
                    title = "Volume Progress",
                    icon = Icons.Default.ContentCopy,
                    unitLabel = "Vol",
                    currentUnits = currentVolumes,
                    totalUnits = totalVolumes,
                    accentColor = Color(0xFFA855F7),
                    onIncrement = { AnilistTrackerCoordinator.updateProgressVolumes(currentVolumes + 1) },
                    onDecrement = { if (currentVolumes > 0) AnilistTrackerCoordinator.updateProgressVolumes(currentVolumes - 1) },
                    onSetExact = { target -> AnilistTrackerCoordinator.updateProgressVolumes(target) },
                )
            } else {
                // Episode Stepper (Anime)
                TrackerProgressStepperSection(
                    title = "Episode Progress",
                    icon = Icons.Default.EditNote,
                    unitLabel = "Ep",
                    currentUnits = entry?.progress ?: 0,
                    totalUnits = media.episodes,
                    accentColor = activeColor,
                    onIncrement = { AnilistTrackerCoordinator.incrementProgress() },
                    onDecrement = { AnilistTrackerCoordinator.decrementProgress() },
                    onSetExact = { target -> AnilistTrackerCoordinator.updateProgress(target) },
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- 6. SCORE RATING SECTION (1-10 STARS APPLE DIAL) ---
            val rawScore = entry?.score ?: 0.0
            val currentScore = if (rawScore >= 10.0) rawScore / 10.0 else rawScore
            val scoreInt = ((currentScore * 10.0).roundToInt() / 10.0).roundToInt()

            TrackerGlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB800),
                            modifier = Modifier.size(17.dp),
                        )
                        Text(
                            text = "Score Rating",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                            ),
                            color = Color.White,
                        )
                    }

                    if (scoreInt > 0) {
                        val ratingLabel = when (scoreInt) {
                            10 -> "10 • Masterpiece"
                            9 -> "9 • Great"
                            8 -> "8 • Very Good"
                            7 -> "7 • Good"
                            6 -> "6 • Fine"
                            5 -> "5 • Average"
                            4 -> "4 • Bad"
                            3 -> "3 • Very Bad"
                            2 -> "2 • Horrible"
                            1 -> "1 • Appalling"
                            else -> "$scoreInt • Rated"
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = ratingLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                ),
                                color = Color(0xFFFFB800),
                            )
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(ShapePill)
                                    .background(themeTokens.subtleChipBackground)
                                    .clickable(role = Role.Button) {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        AnilistTrackerCoordinator.updateScore(0.0)
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear Score",
                                    tint = Color.White.copy(alpha = 0.65f),
                                    modifier = Modifier.size(11.dp),
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Unrated",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                            ),
                            color = Color.White.copy(alpha = 0.45f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 10-Star Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    (1..10).forEach { starIndex ->
                        val isFilled = starIndex <= scoreInt
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(ShapePill)
                                .clickable(role = Role.Button) {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val newScore = if (scoreInt == starIndex) 0.0 else starIndex.toDouble()
                                    AnilistTrackerCoordinator.updateScore(newScore)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (isFilled) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Rate $starIndex",
                                modifier = Modifier.size(22.dp),
                                tint = if (isFilled) Color(0xFFFFB800) else Color.White.copy(alpha = 0.22f),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- 7. TRACKING DATES (APPLE GROUPED INSET CARD) ---
            TrackerGlassCard {
                // Start Date
                val startedAt = entry?.startedAt
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp),
                            tint = Color.White.copy(alpha = 0.65f),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = "Start Date",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.5.sp,
                                ),
                                color = Color.White,
                            )
                            Text(
                                text = startedAt?.formatted() ?: "Not set",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = if (startedAt?.isSet == true) Color(0xFF00A2FF) else Color.White.copy(alpha = 0.45f),
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(ShapeRoundCapsule)
                                .background(themeTokens.subtleChipBackground)
                                .border(1.dp, themeTokens.subtleChipBorder, ShapeRoundCapsule)
                                .clickable(role = Role.Button) {
                                    val now = io.ktor.util.date.GMTDate()
                                    val today = AnilistFuzzyDate(
                                        year = now.year,
                                        month = now.month.ordinal + 1,
                                        day = now.dayOfMonth,
                                    )
                                    AnilistTrackerCoordinator.updateStartedAt(today)
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "Set Today",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                ),
                                color = Color.White.copy(alpha = 0.88f),
                            )
                        }
                        if (startedAt?.isSet == true) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(ShapePill)
                                    .background(themeTokens.subtleChipBackground)
                                    .clickable(role = Role.Button) { AnilistTrackerCoordinator.updateStartedAt(null) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(13.dp),
                                    tint = Color.White.copy(alpha = 0.65f),
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.08f)),
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Finish / End Date
                val completedAt = entry?.completedAt
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp),
                            tint = Color.White.copy(alpha = 0.65f),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = "Finish Date",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.5.sp,
                                ),
                                color = Color.White,
                            )
                            Text(
                                text = completedAt?.formatted() ?: "Not set",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = if (completedAt?.isSet == true) Color(0xFF10B981) else Color.White.copy(alpha = 0.45f),
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(ShapeRoundCapsule)
                                .background(themeTokens.subtleChipBackground)
                                .border(1.dp, themeTokens.subtleChipBorder, ShapeRoundCapsule)
                                .clickable(role = Role.Button) {
                                    val now = io.ktor.util.date.GMTDate()
                                    val today = AnilistFuzzyDate(
                                        year = now.year,
                                        month = now.month.ordinal + 1,
                                        day = now.dayOfMonth,
                                    )
                                    AnilistTrackerCoordinator.updateCompletedAt(today)
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "Set Today",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                ),
                                color = Color.White.copy(alpha = 0.88f),
                            )
                        }
                        if (completedAt?.isSet == true) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(ShapePill)
                                    .background(themeTokens.subtleChipBackground)
                                    .clickable(role = Role.Button) { AnilistTrackerCoordinator.updateCompletedAt(null) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(13.dp),
                                    tint = Color.White.copy(alpha = 0.65f),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- 8. ADVANCED DETAILS (APPLE ACCORDION) ---
            var showAdvancedOptions by remember { mutableStateOf(false) }

            TrackerGlassCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ShapeAction)
                        .clickable(role = Role.Button) { showAdvancedOptions = !showAdvancedOptions },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp),
                            tint = Color(0xFF00A2FF),
                        )
                        Text(
                            text = "Advanced Tracking Details",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                            ),
                            color = Color.White,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(ShapeRoundCapsule)
                            .background(themeTokens.subtleChipBackground)
                            .border(1.dp, themeTokens.subtleChipBorder, ShapeRoundCapsule)
                            .padding(horizontal = 9.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = if (showAdvancedOptions) "Hide ▲" else "Expand ▼",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                            ),
                            color = Color(0xFF00A2FF),
                        )
                    }
                }

                AnimatedVisibility(
                    visible = showAdvancedOptions,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Repeat Stepper
                        val currentRepeat = entry?.repeat ?: 0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Replay,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp),
                                    tint = Color.White.copy(alpha = 0.65f),
                                )
                                Column {
                                    Text(
                                        text = if (isReading) "Reread Count" else "Rewatch Count",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.5.sp,
                                        ),
                                        color = Color.White,
                                    )
                                    Text(
                                        text = if (isReading) "$currentRepeat times reread" else "$currentRepeat times rewatched",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color.White.copy(alpha = 0.45f),
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(ShapePill)
                                        .background(themeTokens.subtleChipBackground)
                                        .clickable(
                                            enabled = currentRepeat > 0,
                                            role = Role.Button,
                                        ) {
                                            if (currentRepeat > 0) {
                                                AnilistTrackerCoordinator.updateRepeat(currentRepeat - 1)
                                            }
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (currentRepeat > 0) Color.White else Color.White.copy(alpha = 0.20f),
                                    )
                                }

                                Text(
                                    text = "$currentRepeat",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                    ),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp),
                                )

                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(ShapePill)
                                        .background(themeTokens.subtleChipBackground)
                                        .clickable(role = Role.Button) {
                                            AnilistTrackerCoordinator.updateRepeat(currentRepeat + 1)
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = Color.White,
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.08f)),
                        )

                        // Private Entry Switch
                        val isPrivate = entry?.private ?: false
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp),
                                    tint = Color.White.copy(alpha = 0.65f),
                                )
                                Column {
                                    Text(
                                        text = "Private Entry",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.5.sp,
                                        ),
                                        color = Color.White,
                                    )
                                    Text(
                                        text = "Hide this entry from your public profile",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color.White.copy(alpha = 0.45f),
                                    )
                                }
                            }

                            Switch(
                                checked = isPrivate,
                                onCheckedChange = { AnilistTrackerCoordinator.updatePrivate(it) },
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.08f)),
                        )

                        // Hidden from Status Lists Switch
                        val isHidden = entry?.hiddenFromStatusLists ?: false
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp),
                                    tint = Color.White.copy(alpha = 0.65f),
                                )
                                Column {
                                    Text(
                                        text = "Hide from Status Lists",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.5.sp,
                                        ),
                                        color = Color.White,
                                    )
                                    Text(
                                        text = "Only show in custom lists",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color.White.copy(alpha = 0.45f),
                                    )
                                }
                            }

                            Switch(
                                checked = isHidden,
                                onCheckedChange = { AnilistTrackerCoordinator.updateHiddenFromStatusLists(it) },
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.08f)),
                        )

                        // Personal Notes Text Field
                        var notesText by remember(entry?.notes) { mutableStateOf(entry?.notes.orEmpty()) }
                        var isNotesDirty by remember(entry?.notes) { mutableStateOf(false) }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EditNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp),
                                    tint = Color.White.copy(alpha = 0.65f),
                                )
                                Text(
                                    text = "Personal Notes",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.5.sp,
                                    ),
                                    color = Color.White,
                                )
                            }

                            OutlinedTextField(
                                value = notesText,
                                onValueChange = {
                                    notesText = it
                                    isNotesDirty = true
                                },
                                placeholder = { Text("Write personal thoughts, reminders, or tags...", fontSize = 12.sp, color = Color.White.copy(0.35f)) },
                                minLines = 2,
                                maxLines = 4,
                                shape = ShapePoster,
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp, color = Color.White),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00A2FF),
                                    unfocusedBorderColor = Color.White.copy(0.18f),
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )

                            if (isNotesDirty) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                ) {
                                    Button(
                                        onClick = {
                                            AnilistTrackerCoordinator.updateNotes(notesText)
                                            isNotesDirty = false
                                        },
                                        shape = ShapeAction,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF00A2FF),
                                            contentColor = Color.White,
                                        ),
                                    ) {
                                        Text("Save Notes", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- 9. DESTRUCTIVE ACTION (GUARDED 2-STEP CONFIRMATION) ---
            if (entry != null) {
                Spacer(modifier = Modifier.height(14.dp))

                var confirmDelete by remember { mutableStateOf(false) }
                LaunchedEffect(confirmDelete) {
                    if (confirmDelete) {
                        delay(4000)
                        confirmDelete = false
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(ShapeTile)
                        .background(if (confirmDelete) DeleteConfirmBgBrush else DeleteNormalBgBrush)
                        .border(
                            1.dp,
                            if (confirmDelete) DeleteConfirmBorderBrush else DeleteNormalBorderBrush,
                            ShapeTile,
                        )
                        .clickable(role = Role.Button) {
                            if (!confirmDelete) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                confirmDelete = true
                            } else {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                AnilistTrackerCoordinator.deleteEntry()
                                onClose()
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFFEF4444),
                        )
                        Text(
                            text = if (confirmDelete) "Tap Again to Confirm Removal" else "Remove from AniList",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                            ),
                            color = Color(0xFFEF4444),
                        )
                    }
                }
            }

            // Expandable Match Diagnostics
            if (!trackerState.debugInfo.isNullOrBlank()) {
                var showDiagnostics by remember { mutableStateOf(false) }
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ShapeAction)
                        .clickable(role = Role.Button) { showDiagnostics = !showDiagnostics },
                    color = Color.Transparent,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Match Diagnostics",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                            color = Color.White.copy(alpha = 0.40f),
                        )
                        Text(
                            text = if (showDiagnostics) "Hide ▲" else "View ▼",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = Color(0xFF00A2FF),
                        )
                    }
                }

                AnimatedVisibility(
                    visible = showDiagnostics,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    val clipboardManager = LocalClipboardManager.current
                    var copied by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ShapePoster)
                            .background(Color.White.copy(alpha = 0.04f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), ShapePoster)
                            .padding(10.dp),
                    ) {
                        Text(
                            text = trackerState.debugInfo.orEmpty(),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            ),
                            color = Color.White.copy(alpha = 0.85f),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(trackerState.debugInfo.orEmpty()))
                                copied = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = ShapeAction,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Log",
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (copied) "✓ Log Copied to Clipboard!" else "Copy Diagnostics Log",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.80f),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

    if (showUserProfileSheet && trackerState.user != null) {
        com.nuvio.app.features.anilist.profile.AnilistUserProfileSheet(
            userId = trackerState.user?.id,
            username = trackerState.user?.name,
            onDismiss = { showUserProfileSheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackerProgressStepperSection(
    title: String,
    icon: ImageVector,
    unitLabel: String,
    currentUnits: Int,
    totalUnits: Int?,
    accentColor: Color,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onSetExact: (Int) -> Unit,
) {
    val tokens = LocalTrackerThemeTokens.current
    var showExactEditDialog by remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current

    val fraction = if (totalUnits != null && totalUnits > 0) {
        (currentUnits.toFloat() / totalUnits).coerceIn(0f, 1f)
    } else 0f

    val progressTrackBrush = remember(accentColor) {
        Brush.horizontalGradient(
            listOf(accentColor, Color(0xFF38BDF8)),
        )
    }

    TrackerGlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                    tint = accentColor,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    ),
                    color = Color.White,
                )
            }

            Text(
                text = if (totalUnits != null) {
                    "$unitLabel $currentUnits of $totalUnits"
                } else {
                    "$unitLabel $currentUnits"
                },
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.5.sp,
                ),
                color = accentColor,
            )
        }

        if (totalUnits != null && totalUnits > 0) {
            Spacer(modifier = Modifier.height(10.dp))
            val animatedProgress by animateFloatAsState(
                targetValue = fraction,
                animationSpec = tween(300),
                label = "ProgressPillTrack",
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(ShapeProgressBar)
                    .background(Color.White.copy(alpha = 0.08f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(ShapeProgressBar)
                        .background(progressTrackBrush),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Continuous Segmented Stepper Pill
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(ShapeStepper)
                    .background(tokens.stepperBackground)
                    .border(
                        1.dp,
                        tokens.stepperBorder,
                        ShapeStepper,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Minus segment
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            enabled = currentUnits > 0,
                            role = Role.Button,
                        ) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDecrement()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrease",
                        modifier = Modifier.size(16.dp),
                        tint = if (currentUnits > 0) Color.White else Color.White.copy(alpha = 0.25f),
                    )
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(20.dp)
                        .background(tokens.stepperDivider),
                )

                // Middle: Direct input / badge segment
                Box(
                    modifier = Modifier
                        .weight(1.4f)
                        .fillMaxHeight()
                        .clickable(role = Role.Button) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showExactEditDialog = true
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "$unitLabel $currentUnits",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                            ),
                            color = accentColor,
                        )
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Exact",
                            modifier = Modifier.size(12.dp),
                            tint = accentColor.copy(alpha = 0.70f),
                        )
                    }
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(20.dp)
                        .background(tokens.stepperDivider),
                )

                // Plus segment
                val canIncrement = totalUnits == null || currentUnits < totalUnits
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            enabled = canIncrement,
                            role = Role.Button,
                        ) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onIncrement()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase",
                        modifier = Modifier.size(16.dp),
                        tint = if (canIncrement) Color.White else Color.White.copy(alpha = 0.25f),
                    )
                }
            }

            // Quick Max completion button if total is known and not reached
            if (totalUnits != null && currentUnits < totalUnits) {
                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(ShapeStepper)
                        .background(tokens.actionButtonBackground)
                        .border(
                            1.dp,
                            tokens.actionButtonBorder,
                            ShapeStepper,
                        )
                        .clickable(role = Role.Button) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSetExact(totalUnits)
                        }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        val allColor = if (tokens.theme == AnilistTrackerTheme.SMOKED_GLASS) Color.White else accentColor
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = allColor,
                        )
                        Text(
                            text = "All",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                            ),
                            color = allColor,
                        )
                    }
                }
            }
        }
    }

    if (showExactEditDialog) {
        var textInput by remember { mutableStateOf(currentUnits.toString()) }
        BasicAlertDialog(
            onDismissRequest = { showExactEditDialog = false },
        ) {
            Surface(
                shape = ShapeModal,
                color = tokens.dialogBackground,
                border = BorderStroke(
                    1.dp,
                    tokens.dialogBorder,
                ),
                shadowElevation = 20.dp,
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        text = "Set $unitLabel Progress",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        ),
                        color = Color.White,
                    )

                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it.filter { ch -> ch.isDigit() } },
                        singleLine = true,
                        shape = ShapePoster,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { Text("0", color = Color.White.copy(0.4f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = Color.White.copy(0.18f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = { showExactEditDialog = false },
                            shape = ShapePoster,
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                        ) {
                            Text("Cancel", color = Color.White.copy(alpha = 0.80f), fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                val target = textInput.toIntOrNull() ?: currentUnits
                                val clamped = if (totalUnits != null) target.coerceIn(0, totalUnits) else target.coerceAtLeast(0)
                                onSetExact(clamped)
                                showExactEditDialog = false
                            },
                            shape = ShapePoster,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = Color.White,
                            ),
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
