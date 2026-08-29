package com.nuvio.app.features.anilist.catalog

import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource

object AnimeStudioLogos {
    fun findLogoResource(rawName: String?): DrawableResource? {
        if (rawName.isNullOrBlank()) return null
        val normalized = rawName.trim().lowercase()
        return when {
            normalized.contains("toho animation") -> Res.drawable.studio_toho_animation
            normalized.contains("toho") -> Res.drawable.studio_toho
            normalized.contains("aniplex") -> Res.drawable.studio_aniplex
            normalized.contains("mappa") -> Res.drawable.studio_mappa
            normalized.contains("ufotable") -> Res.drawable.studio_ufotable
            normalized.contains("kyoto animation") || normalized == "kyoani" -> Res.drawable.studio_kyoto_animation
            normalized.contains("bones") -> Res.drawable.studio_bones
            normalized.contains("a-1 pictures") || normalized.contains("a 1 pictures") || normalized.contains("a1 pictures") -> Res.drawable.studio_a_1_pictures
            normalized.contains("toei animation") || normalized.contains("toei") -> Res.drawable.studio_toei_animation
            normalized.contains("david production") -> Res.drawable.studio_david_production
            normalized.contains("studio ghibli") || normalized == "ghibli" -> Res.drawable.studio_studio_ghibli
            normalized.contains("sunrise") || normalized.contains("bandai namco filmworks") -> Res.drawable.studio_sunrise
            normalized.contains("mbs") || normalized.contains("mainichi broadcasting") -> Res.drawable.studio_mbs
            normalized.contains("fuji tv") || normalized.contains("fuji television") -> Res.drawable.studio_fuji_tv
            normalized.contains("netflix") -> Res.drawable.studio_netflix
            normalized.contains("nhk") -> Res.drawable.studio_nhk
            normalized.contains("at-x") || normalized == "atx" -> Res.drawable.studio_at_x
            normalized.contains("bs11") -> Res.drawable.studio_bs11
            normalized.contains("p.a. works") || normalized.contains("pa works") -> Res.drawable.studio_pa_works
            normalized.contains("science saru") -> Res.drawable.studio_science_saru
            normalized.contains("studio bind") -> Res.drawable.studio_studio_bind
            normalized.contains("studio deen") || normalized == "deen" -> Res.drawable.studio_studio_deen
            normalized.contains("kinema citrus") -> Res.drawable.studio_kinema_citrus
            normalized.contains("crunchyroll") -> Res.drawable.studio_crunchyroll
            else -> null
        }
    }
}
