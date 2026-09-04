package com.nuvio.app.features.details.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    val isAnilist = meta.id.startsWith("ani_", ignoreCase = true) || meta.id.startsWith("anilist:", ignoreCase = true)

    fun hasLogo(item: MetaCompany): Boolean {
        return com.nuvio.app.features.anilist.catalog.AnimeStudioLogos.findLogoResource(item.name) != null ||
            !item.logo.isNullOrBlank()
    }

    // Sort logos first, then text-only items!
    val companies = meta.productionCompanies
        .distinctBy { it.name.trim().lowercase() }
        .sortedByDescending { hasLogo(it) }
        .take(12)

    val networks = meta.networks
        .distinctBy { it.name.trim().lowercase() }
        .sortedByDescending { hasLogo(it) }
        .take(12)

    if (companies.isEmpty() && networks.isEmpty()) return

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (companies.isNotEmpty()) {
            DetailSection(
                title = if (isAnilist) "Animation Studios" else stringResource(Res.string.meta_section_production_title),
                showHeader = showHeader,
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    companies.forEach { item ->
                        ProductionChip(
                            item = item,
                            onClick = if (onCompanyClick != null && item.name.isNotBlank()) {
                                { onCompanyClick(item, "company") }
                            } else null,
                        )
                    }
                }
            }
        }

        if (networks.isNotEmpty()) {
            DetailSection(
                title = stringResource(Res.string.details_networks),
                showHeader = showHeader,
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    networks.forEach { item ->
                        ProductionChip(
                            item = item,
                            onClick = if (onCompanyClick != null && item.name.isNotBlank()) {
                                { onCompanyClick(item, "network") }
                            } else null,
                        )
                    }
                }
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
    val hasValidLogo = localLogo != null || (!item.logo.isNullOrBlank() && !hasLogoError)

    Column(
        modifier = Modifier
            .width(150.dp)
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Logo image badge / text card (bigger and more spacious)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (hasValidLogo) Color(0xFFF5F5F5) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                )
                .border(
                    width = 1.dp,
                    color = if (hasValidLogo) Color(0x26000000) else MaterialTheme.colorScheme.outline.copy(alpha = 0.20f),
                    shape = RoundedCornerShape(14.dp),
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (localLogo != null) {
                Image(
                    painter = painterResource(localLogo),
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            } else if (!item.logo.isNullOrBlank() && !hasLogoError) {
                AsyncImage(
                    model = item.logo,
                    contentDescription = item.name,
                    onError = { hasLogoError = true },
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // Caption UNDER the image ONLY when a logo image is present!
        if (hasValidLogo) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.name,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            )
        }
    }
}
