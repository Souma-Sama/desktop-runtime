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

fun buildAddonCatalogRefreshSignature(addons: List<ManagedAddon>): List<String> =
    buildHomeCatalogRefreshSignature(addons)

fun buildHomeCatalogRefreshSignature(addons: List<ManagedAddon>): List<String> {
    val isAnilistEnabled = com.nuvio.app.features.anilist.KaiHooks.isKaiEnabled() &&
        (addons.isEmpty() || addons.any { com.nuvio.app.features.anilist.KaiHooks.isKaiAddon(it) && it.enabled })
    val anilistSignatures = if (isAnilistEnabled) {
        com.nuvio.app.features.anilist.catalog.AnilistCatalogRepository.getCatalogDefinitions()
            .map { it.descriptorSignature }
    } else listOf("anilist:disabled")
    val addonSignatures = com.nuvio.app.features.anilist.KaiHooks.filterExternalAddons(addons.enabledAddons())
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
    val isAnilistEnabled = com.nuvio.app.features.anilist.KaiHooks.isKaiEnabled() &&
        (addons.isEmpty() || addons.any { com.nuvio.app.features.anilist.KaiHooks.isKaiAddon(it) && it.enabled })
    val anilistCatalogs = if (isAnilistEnabled) {
        com.nuvio.app.features.anilist.catalog.AnilistCatalogRepository.getCatalogDefinitions()
    } else emptyList()
    val addonCatalogs = com.nuvio.app.features.anilist.KaiHooks.filterExternalAddons(addons.enabledAddons())
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
    signature.addAddon(addon)
    signature.addManifest(manifest)
    signature.addCatalog(catalog)
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

    fun addAddon(addon: ManagedAddon) {
        add(addon.userSetName)
        add(addon.enabled)
        add(addon.isRefreshing)
        add(addon.errorMessage)
        add(addon.manifestUrl)
    }

    fun addManifest(manifest: AddonManifest) {
        add(manifest.id)
        add(manifest.name)
        add(manifest.version)
        add(manifest.description)
        add(manifest.logoUrl)
        add(manifest.transportUrl)
        manifest.types.forEach(::add)
        manifest.idPrefixes.forEach(::add)
        manifest.resources.forEach { resource ->
            add(resource.name)
            resource.types.forEach(::add)
            resource.idPrefixes.forEach(::add)
        }
        add(manifest.behaviorHints.configurable)
        add(manifest.behaviorHints.configurationRequired)
        add(manifest.behaviorHints.adult)
        add(manifest.behaviorHints.p2p)
    }

    fun addCatalog(catalog: AddonCatalog) {
        add(catalog.type)
        add(catalog.id)
        add(catalog.name)
        add(catalog.supportsPagination())
        catalog.extra.forEach { extra ->
            add(extra.name)
            add(extra.isRequired)
            extra.options.forEach(::add)
            add(extra.optionsLimit)
        }
    }

    fun value(): String = hash.toULong().toString(16)

    private fun mix(value: Int) {
        hash = (hash xor value.toLong()) * 1099511628211L
    }
}

internal fun String.displayLabel(): String = localizedMediaTypeLabel(this)
