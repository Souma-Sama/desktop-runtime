package com.nuvio.app.features.anilist.threads

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nuvio.app.core.ui.NuvioDesktopVerticalScrollbar
import com.nuvio.app.features.anilist.AnilistAuthRepository
import com.nuvio.app.features.anilist.community.AnilistContentBlockItem
import com.nuvio.app.features.anilist.community.parseAnilistRichContent
import com.nuvio.app.features.anilist.profile.AnilistUserProfileSheet
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadDetailSheet(
    thread: AnilistThread,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val primaryColor = MaterialTheme.colorScheme.primary

    var comments by remember(thread.id) { mutableStateOf<List<AnilistThreadComment>>(emptyList()) }
    var isLoadingComments by remember(thread.id) { mutableStateOf(true) }
    var replyText by remember { mutableStateOf("") }
    var isPostingReply by remember { mutableStateOf(false) }

    var isLiked by remember(thread.id, thread.isLiked) { mutableStateOf(thread.isLiked) }
    var localLikeCount by remember(thread.id, thread.likeCount) { mutableIntStateOf(thread.likeCount) }

    var profileToViewId by remember { mutableStateOf<Int?>(null) }
    var profileToViewName by remember { mutableStateOf<String?>(null) }

    val isLoggedIn = AnilistAuthRepository.isAuthenticated.value
    var currentBody by remember(thread.id, thread.body) { mutableStateOf(thread.body) }
    var isLoadingBody by remember(thread.id) { mutableStateOf(thread.body.isBlank()) }

    LaunchedEffect(thread.id) {
        if (currentBody.isBlank()) {
            isLoadingBody = true
            val body = com.nuvio.app.features.anilist.AnilistApi.getThreadBody(thread.id)
            if (!body.isNullOrBlank()) {
                currentBody = body
            }
            isLoadingBody = false
        }
    }

    val threadBodyBlocks = remember(currentBody, primaryColor) {
        parseAnilistRichContent(currentBody, primaryColor)
    }

    LaunchedEffect(thread.id) {
        isLoadingComments = true
        val result = AnilistThreadsRepository.getThreadComments(threadId = thread.id)
        result.onSuccess {
            comments = it
            isLoadingComments = false
        }.onFailure {
            isLoadingComments = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null,
    ) {
        val listState = rememberLazyListState()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(720.dp),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        if (thread.isSticky) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        if (thread.isLocked) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Text(
                            text = "Discussion Forum",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                // Scrollable Content
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        // 1. Thread Title
                        item {
                            Text(
                                text = thread.title,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 22.sp,
                                    lineHeight = 28.sp,
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }

                        // 2. Author Header
                        item {
                            val author = thread.user
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (author != null) {
                                            profileToViewId = author.id
                                            profileToViewName = author.name
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                ) {
                                    val avatar = author?.avatarMedium ?: author?.avatarLarge
                                    if (!avatar.isNullOrBlank()) {
                                        AsyncImage(
                                            model = avatar,
                                            contentDescription = author?.name,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop,
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(
                                            text = author?.name ?: "Anonymous",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        if (!author?.donatorBadge.isNullOrBlank()) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                                                shape = RoundedCornerShape(4.dp),
                                            ) {
                                                Text(
                                                    text = author.donatorBadge,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = formatEpochSeconds(thread.createdAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }

                                // Likes Button
                                OutlinedButton(
                                    onClick = {
                                        if (isLoggedIn) {
                                            val newLiked = !isLiked
                                            isLiked = newLiked
                                            localLikeCount += if (newLiked) 1 else -1
                                            scope.launch {
                                                AnilistThreadsRepository.toggleLike(thread.id, "THREAD")
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isLiked) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    ),
                                ) {
                                    Icon(
                                        imageVector = if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "$localLikeCount",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    )
                                }
                            }
                        }

                        // 3. Thread Body AST Blocks
                        if (isLoadingBody && threadBodyBlocks.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(60.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = primaryColor,
                                    )
                                }
                            }
                        }

                        items(
                            count = threadBodyBlocks.size,
                            key = { index -> "thread-body-$index" },
                        ) { index ->
                            AnilistContentBlockItem(
                                block = threadBodyBlocks[index],
                                primaryColor = primaryColor,
                                uriHandler = uriHandler,
                            )
                        }

                        // 4. Divider & Comments Header
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Comments (${comments.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }

                        // 5. Comments List
                        if (isLoadingComments) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                                }
                            }
                        } else if (comments.isEmpty()) {
                            item {
                                Text(
                                    text = "No replies yet. Be the first to join the conversation!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 12.dp),
                                )
                            }
                        } else {
                            items(comments, key = { it.id }) { comment ->
                                ThreadCommentCard(
                                    comment = comment,
                                    primaryColor = primaryColor,
                                    uriHandler = uriHandler,
                                    isLoggedIn = isLoggedIn,
                                    onAuthorClick = {
                                        comment.user?.let { u ->
                                            profileToViewId = u.id
                                            profileToViewName = u.name
                                        }
                                    },
                                    onLikeToggle = {
                                        scope.launch {
                                            AnilistThreadsRepository.toggleLike(comment.id, "THREAD_COMMENT")
                                        }
                                    },
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }

                    NuvioDesktopVerticalScrollbar(
                        state = listState,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(vertical = 4.dp),
                    )
                }

                // 6. Bottom Reply Bar (if logged in and not locked)
                if (isLoggedIn && !thread.isLocked) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            placeholder = { Text("Write a reply...") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            maxLines = 3,
                        )

                        IconButton(
                            onClick = {
                                if (replyText.isNotBlank() && !isPostingReply) {
                                    isPostingReply = true
                                    scope.launch {
                                        val result = AnilistThreadsRepository.postComment(thread.id, replyText.trim())
                                        result.onSuccess { newComment ->
                                            comments = comments + newComment
                                            replyText = ""
                                        }
                                        isPostingReply = false
                                    }
                                }
                            },
                            enabled = replyText.isNotBlank() && !isPostingReply,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (replyText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            if (isPostingReply) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Send",
                                    tint = if (replyText.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Nested Profile Sheet
    if (profileToViewId != null || profileToViewName != null) {
        AnilistUserProfileSheet(
            userId = profileToViewId,
            username = profileToViewName,
            onDismiss = {
                profileToViewId = null
                profileToViewName = null
            },
        )
    }
}

@Composable
private fun ThreadCommentCard(
    comment: AnilistThreadComment,
    primaryColor: Color,
    uriHandler: androidx.compose.ui.platform.UriHandler,
    isLoggedIn: Boolean,
    onAuthorClick: () -> Unit,
    onLikeToggle: () -> Unit,
) {
    val author = comment.user
    var isLiked by remember(comment.id, comment.isLiked) { mutableStateOf(comment.isLiked) }
    var localLikeCount by remember(comment.id, comment.likeCount) { mutableIntStateOf(comment.likeCount) }
    val commentBlocks = remember(comment.comment, primaryColor) {
        parseAnilistRichContent(comment.comment, primaryColor)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable(onClick = onAuthorClick),
                ) {
                    val avatar = author?.avatarMedium ?: author?.avatarLarge
                    if (!avatar.isNullOrBlank()) {
                        AsyncImage(
                            model = avatar,
                            contentDescription = author?.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = author?.name ?: "Anonymous",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.clickable(onClick = onAuthorClick),
                    )
                    Text(
                        text = formatEpochSeconds(comment.createdAt),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Comment Like Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = isLoggedIn) {
                            val newLiked = !isLiked
                            isLiked = newLiked
                            localLikeCount += if (newLiked) 1 else -1
                            onLikeToggle()
                        }
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                        contentDescription = null,
                        tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp),
                    )
                    if (localLikeCount > 0) {
                        Text(
                            text = "$localLikeCount",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Comment text blocks
            commentBlocks.forEach { block ->
                AnilistContentBlockItem(
                    block = block,
                    primaryColor = primaryColor,
                    uriHandler = uriHandler,
                )
            }
        }
    }
}

private fun formatEpochSeconds(epochSec: Long): String {
    if (epochSec <= 0) return ""
    return try {
        val instant = Instant.fromEpochSeconds(epochSec)
        val ldt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val month = ldt.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
        "$month ${ldt.dayOfMonth}, ${ldt.year}"
    } catch (_: Exception) {
        ""
    }
}
