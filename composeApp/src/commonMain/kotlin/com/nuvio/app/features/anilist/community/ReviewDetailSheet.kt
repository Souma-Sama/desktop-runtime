package com.nuvio.app.features.anilist.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nuvio.app.features.anilist.AnilistAuthRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewDetailSheet(
    review: AnilistReview,
    onDismiss: () -> Unit,
    onVote: (AnilistReview, AnilistReviewVote) -> Unit,
    onEdit: ((AnilistReview) -> Unit)? = null,
    onDelete: ((AnilistReview) -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uriHandler = LocalUriHandler.current
    val currentUserId = AnilistAuthRepository.currentUser.value?.id
    val isAuthor = currentUserId != null && currentUserId == review.userId
    var userVoted by remember(review.id, review.userRating) {
        mutableStateOf(review.userRating == "UP_VOTE")
    }
    var localRating by remember(review.id, review.rating) {
        mutableStateOf(review.rating)
    }

    var showUserProfile by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(680.dp)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            // Header: User Profile & Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showUserProfile = true }
                        .padding(4.dp),
                ) {
                    val avatarUrl = review.user?.avatarLarge ?: review.user?.avatarMedium
                    if (avatarUrl != null) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = review.user?.name,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = review.user?.name?.take(1)?.uppercase() ?: "A",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = review.user?.name ?: "Anonymous",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            review.user?.donatorBadge?.let { badge ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFE50914).copy(alpha = 0.16f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        text = badge,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = Color(0xFFFF5252),
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }

                        Text(
                            text = formatReviewDate(review.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Score Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (review.score >= 75) Color(0xFF10B981)
                                else if (review.score >= 50) Color(0xFFF59E0B)
                                else Color(0xFFEF4444)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = "${review.score}%",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                            ),
                            color = Color.White,
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Summary Headline
            if (review.summary.isNotBlank()) {
                Text(
                    text = review.summary,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 28.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Scrollable Lazy Body Content
            AnilistRichContentRenderer(
                body = review.body,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Actions Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Helpful Voting Button
                OutlinedButton(
                    onClick = {
                        val newVoted = !userVoted
                        userVoted = newVoted
                        localRating = maxOf(0, localRating + if (newVoted) 1 else -1)
                        onVote(review, if (newVoted) AnilistReviewVote.UP_VOTE else AnilistReviewVote.NO_VOTE)
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (userVoted) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        contentColor = if (userVoted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        imageVector = if (userVoted) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                        contentDescription = "Helpful",
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Helpful ($localRating)",
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isAuthor) {
                        onEdit?.let { editAction ->
                            IconButton(onClick = { editAction(review) }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Review",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        onDelete?.let { deleteAction ->
                            IconButton(onClick = { deleteAction(review) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Review",
                                    tint = Color(0xFFEF4444),
                                )
                            }
                        }
                    }

                    review.siteUrl?.let { url ->
                        OutlinedButton(
                            onClick = { uriHandler.openUri(url) },
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text("AniList", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    if (showUserProfile) {
        com.nuvio.app.features.anilist.profile.AnilistUserProfileSheet(
            userId = review.user?.id,
            username = review.user?.name,
            onDismiss = { showUserProfile = false },
        )
    }
}

private fun formatReviewDate(timestampSeconds: Long): String {
    if (timestampSeconds <= 0L) return "Recently"
    val now = io.ktor.util.date.GMTDate().timestamp / 1000
    val diffSecs = (now - timestampSeconds).coerceAtLeast(0L)
    val days = diffSecs / 86400
    val months = days / 30
    val years = days / 365

    return when {
        years > 0 -> "$years ${if (years == 1L) "year" else "years"} ago"
        months > 0 -> "$months ${if (months == 1L) "month" else "months"} ago"
        days > 0 -> "$days ${if (days == 1L) "day" else "days"} ago"
        diffSecs >= 3600 -> "${diffSecs / 3600}h ago"
        diffSecs >= 60 -> "${diffSecs / 60}m ago"
        else -> "Just now"
    }
}
