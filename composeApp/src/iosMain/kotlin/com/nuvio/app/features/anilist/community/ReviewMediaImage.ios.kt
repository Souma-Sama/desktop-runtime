package com.nuvio.app.features.anilist.community

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

@Composable
internal actual fun ReviewMediaImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale,
) {
    AsyncImage(
        model = url,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
    )
}
