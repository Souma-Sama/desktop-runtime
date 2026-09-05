package com.nuvio.app.features.anilist.streams

import com.nuvio.app.features.anilist.catalog.AnilistMetaDetailsResolver.ArmMapping

internal expect object ArmLocalCache {
    fun get(anilistId: Int): ArmMapping?
    fun put(anilistId: Int, mapping: ArmMapping)
}
