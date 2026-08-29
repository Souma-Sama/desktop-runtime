package com.nuvio.app.features.details.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.ui.NuvioAsyncImage as AsyncImage
import com.nuvio.app.features.details.MetaCompany
import com.nuvio.app.features.details.MetaDetails
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailProductionSection(
    meta: MetaDetails,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    onCompanyClick: ((MetaCompany, String) -> Unit)? = null,
) {
    val isSeriesLike = meta.type == "series" || meta.videos.any { it.season != null || it.episode != null }
    val isAnilist = meta.id.startsWith("ani_", ignoreCase = true) || meta.id.startsWith("anilist:", ignoreCase = true)

    val sourceItems = if (isAnilist) {
        (meta.productionCompanies + meta.networks).distinctBy { it.name.trim().lowercase() }
    } else if (meta.productionCompanies.isNotEmpty() && meta.networks.isNotEmpty()) {
        (meta.productionCompanies + meta.networks).distinctBy { it.name.trim().lowercase() }
    } else if (isSeriesLike) {
        meta.networks.ifEmpty { meta.productionCompanies }
    } else {
        meta.productionCompanies.ifEmpty { meta.networks }
    }
    if (sourceItems.isEmpty()) return

    val displayItems = sourceItems.take(10)
    if (displayItems.isEmpty()) return

    val sectionTitle = if (isAnilist) {
        "Studios & Networks"
    } else if (meta.productionCompanies.isNotEmpty() && meta.networks.isNotEmpty()) {
        "Production & Networks"
    } else if (isSeriesLike) {
        stringResource(Res.string.details_networks)
    } else {
        stringResource(Res.string.meta_section_production_title)
    }

    DetailSection(
        title = sectionTitle,
        modifier = modifier,
        showHeader = showHeader,
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            displayItems.forEach { item ->
                val entityKind = if (meta.networks.contains(item)) "network" else "company"
                ProductionChip(
                    item = item,
                    onClick = if (onCompanyClick != null && item.name.isNotBlank()) {
                        { onCompanyClick(item, entityKind) }
                    } else null,
                )
            }
        }
    }
}

@Composable
private fun ProductionChip(
    item: MetaCompany,
    onClick: (() -> Unit)? = null,
) {
    val localLogo = com.nuvio.app.features.anilist.catalog.AnimeStudioLogos.findLogoResource(item.name)
    var hasLogoError by remember(item.logo) { mutableStateOf(false) }
    val hasLogo = localLogo != null || (!item.logo.isNullOrBlank() && !hasLogoError)

    val chipBackground = if (hasLogo) {
        Color(0xFFF5F5F5)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f)
    }

    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .width(132.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(color = chipBackground)
            .border(
                width = 1.dp,
                color = if (hasLogo) Color(0x33000000) else MaterialTheme.colorScheme.outline.copy(alpha = 0.20f),
                shape = RoundedCornerShape(14.dp),
            )
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (localLogo != null) {
                Image(
                    painter = painterResource(localLogo),
                    contentDescription = item.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    contentScale = ContentScale.Fit,
                )
            } else if (!item.logo.isNullOrBlank() && !hasLogoError) {
                AsyncImage(
                    model = item.logo,
                    contentDescription = item.name,
                    onError = { hasLogoError = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
        }

        if (hasLogo) {
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.name,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = Color(0xFF333333),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
    }
}
