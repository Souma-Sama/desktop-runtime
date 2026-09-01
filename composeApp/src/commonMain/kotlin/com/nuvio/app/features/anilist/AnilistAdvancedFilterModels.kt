package com.nuvio.app.features.anilist

import kotlinx.serialization.Serializable

@Serializable
data class AnilistAdvancedFilterState(
    val includedGenres: Set<String> = emptySet(),
    val excludedGenres: Set<String> = emptySet(),
    val includedTags: Set<String> = emptySet(),
    val excludedTags: Set<String> = emptySet(),
    val formats: Set<String> = emptySet(),
    val statuses: Set<String> = emptySet(),
    val season: String? = null,
    val seasonYear: Int? = null,
    val countryOfOrigin: String? = null,
    val sources: Set<String> = emptySet(),
    val minScore: Int? = null, // 0..100
    val sort: AnilistSortOption = AnilistSortOption.POPULARITY,
) {
    val activeFilterCount: Int
        get() = includedGenres.size + excludedGenres.size + includedTags.size + excludedTags.size +
                formats.size + statuses.size + (if (season != null) 1 else 0) +
                (if (seasonYear != null) 1 else 0) + (if (countryOfOrigin != null) 1 else 0) +
                sources.size + (if (minScore != null && minScore > 0) 1 else 0)

    val hasFilters: Boolean
        get() = activeFilterCount > 0

    fun reset(): AnilistAdvancedFilterState = AnilistAdvancedFilterState(sort = sort)
}
