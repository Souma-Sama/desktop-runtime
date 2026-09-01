package com.nuvio.app.features.home

import com.nuvio.app.core.i18n.localizedMediaTypeLabel
import com.nuvio.app.features.addons.AddonCatalog
import com.nuvio.app.features.addons.AddonManifest
import com.nuvio.app.features.addons.ManagedAddon
import com.nuvio.app.features.addons.enabledAddons
import com.nuvio.app.features.catalog.supportsPagination
import kotlinx.coroutines.runBlocking
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.home_catalog_default_title
import org.jetbrains.compose.resources.getString

data class HomeCatalogDefinition(
    val key: String,
    val defaultTitle: String,
    val catalogName: String,
    val addonName: String,
    val manifestUrl: String,
    val type: String,
    val catalogId: String,
    val supportsPagination: Boolean,
    val descriptorSignature: String,
) {
    val cacheKey: String
        get() = "$key|$descriptorSignature"

    fun titleFor(showCatalogType: Boolean): String =
        if (showCatalogType) defaultTitle else catalogName
}

fun buildHomeCatalogRefreshSignature(addons: List<ManagedAddon>): List<String> {
    val prefs = com.nuvio.app.features.anilist.AnilistPreferencesRepository.snapshot()
    val isAnilistEnabled = prefs.enabled && (addons.isEmpty() || addons.any { (it.manifestUrl.startsWith("native://anilist") || it.manifestUrl.startsWith("builtin://anilist") || it.manifest?.id?.contains("anilist", ignoreCase = true) == true) && it.enabled })
    val anilistSignatures = if (isAnilistEnabled) {
        com.nuvio.app.features.anilist.catalog.AnilistCatalogRepository.getCatalogDefinitions()
            .map { it.descriptorSignature }
    } else listOf("anilist:disabled")
    val addonSignatures = addons.enabledAddons()
        .filter { !it.manifestUrl.startsWith("native://anilist") && !it.manifestUrl.startsWith("builtin://anilist") && it.manifest?.id?.contains("anilist", ignoreCase = true) != true }
        .mapNotNull { addon ->
            val manifest = addon.manifest ?: return@mapNotNull null
            addon to manifest
        }.flatMap { (addon, manifest) ->
            manifest.catalogs.map { catalog ->
                buildHomeCatalogDescriptorSignature(addon, manifest, catalog)
            }
        }
    return (anilistSignatures + addonSignatures).sorted()
}

fun buildHomeCatalogDefinitions(addons: List<ManagedAddon>): List<HomeCatalogDefinition> {
    val prefs = com.nuvio.app.features.anilist.AnilistPreferencesRepository.snapshot()
    val isAnilistEnabled = prefs.enabled && (addons.isEmpty() || addons.any { (it.manifestUrl.startsWith("native://anilist") || it.manifestUrl.startsWith("builtin://anilist") || it.manifest?.id?.contains("anilist", ignoreCase = true) == true) && it.enabled })
    val anilistCatalogs = if (isAnilistEnabled) {
        com.nuvio.app.features.anilist.catalog.AnilistCatalogRepository.getCatalogDefinitions()
    } else emptyList()
    val addonCatalogs = addons.enabledAddons()
        .filter { !it.manifestUrl.startsWith("native://anilist") && !it.manifestUrl.startsWith("builtin://anilist") && it.manifest?.id?.contains("anilist", ignoreCase = true) != true }
        .mapNotNull { addon ->
            val manifest = addon.manifest ?: return@mapNotNull null
            addon to manifest
        }.flatMap { (addon, manifest) ->
            manifest.catalogs
                .filter { catalog -> catalog.extra.none { it.isRequired } }
                .map { catalog ->
                    HomeCatalogDefinition(
                        key = "${manifest.id}:${catalog.type}:${catalog.id}",
                        defaultTitle = buildDefaultCatalogTitle(catalog.name, catalog.type),
                        catalogName = catalog.name,
                        addonName = addon.displayTitle,
                        manifestUrl = addon.manifestUrl,
                        type = catalog.type,
                        catalogId = catalog.id,
                        supportsPagination = catalog.supportsPagination(),
                        descriptorSignature = buildHomeCatalogDescriptorSignature(addon, manifest, catalog),
                    )
                }
        }
    return (anilistCatalogs + addonCatalogs).distinctBy(HomeCatalogDefinition::key)
}

fun buildDefaultCatalogTitle(catalogName: String, type: String): String {
    val cleanName = catalogName.trim()
    return when (type.lowercase()) {
        "movie" -> {
            if (cleanName.endsWith("movie", ignoreCase = true) || cleanName.endsWith("movies", ignoreCase = true)) {
                cleanName
            } else {
                "$cleanName Movies"
            }
        }
        "series", "tv" -> {
            if (cleanName.endsWith("series", ignoreCase = true) || cleanName.endsWith("shows", ignoreCase = true) || cleanName.endsWith("tv", ignoreCase = true)) {
                cleanName
            } else {
                "$cleanName Series"
            }
        }
        else -> cleanName
    }
}

private fun buildHomeCatalogDescriptorSignature(
    addon: ManagedAddon,
    manifest: AddonManifest,
    catalog: AddonCatalog,
): String {
    val signature = CatalogDescriptorSignature()
    signature.add(addon.displayTitle)
    signature.add(addon.enabled)
    signature.add(addon.isRefreshing)
    signature.add(addon.errorMessage)
    signature.add(addon.manifestUrl)
    signature.add(manifest.id)
    signature.add(manifest.name)
    signature.add(manifest.version)
    signature.add(manifest.description)
    signature.add(manifest.logoUrl)
    signature.add(manifest.transportUrl)
    manifest.types.forEach(signature::add)
    manifest.idPrefixes.forEach(signature::add)
    manifest.resources.forEach { resource ->
        signature.add(resource.name)
        resource.types.forEach(signature::add)
        resource.idPrefixes.forEach(signature::add)
    }
    signature.add(manifest.behaviorHints.configurable)
    signature.add(manifest.behaviorHints.configurationRequired)
    signature.add(manifest.behaviorHints.adult)
    signature.add(manifest.behaviorHints.p2p)
    signature.add(catalog.type)
    signature.add(catalog.id)
    signature.add(catalog.name)
    signature.add(catalog.supportsPagination())
    catalog.extra.forEach { extra ->
        signature.add(extra.name)
        signature.add(extra.isRequired)
        extra.options.forEach(signature::add)
        signature.add(extra.optionsLimit)
    }
    return signature.value()
}

private class CatalogDescriptorSignature {
    private var hash = -3750763034362895579L

    fun add(value: String?) {
        val text = value.orEmpty()
        mix(text.length)
        text.forEach { character -> mix(character.code) }
    }

    fun add(value: Boolean) {
        mix(if (value) 1 else 0)
    }

    fun add(value: Int?) {
        mix(value ?: Int.MIN_VALUE)
    }

    fun value(): String = hash.toULong().toString(16)

    private fun mix(value: Int) {
        hash = (hash xor value.toLong()) * 1099511628211L
    }
}

internal fun String.displayLabel(): String = localizedMediaTypeLabel(this)
