package com.nuvio.app.features.anilist.catalog

import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource

object AnimeStudioLogos {
    fun findLogoResource(rawName: String?): DrawableResource? = runCatching {
        if (rawName.isNullOrBlank()) return null
        val normalized = rawName.trim().lowercase()
        when {
            normalized.contains("toho animation") -> Res.drawable.logo_studio_toho_animation
            normalized.contains("toho") -> Res.drawable.logo_studio_toho
            normalized.contains("aniplex") -> Res.drawable.logo_studio_aniplex
            normalized.contains("kadokawa") -> Res.drawable.logo_studio_kadokawa
            normalized.contains("mappa") -> Res.drawable.logo_studio_mappa
            normalized.contains("wit studio") || (normalized.contains("wit") && !normalized.contains("with")) -> Res.drawable.logo_studio_wit
            normalized.contains("cloverworks") || normalized.contains("clover works") -> Res.drawable.logo_studio_cloverworks
            normalized.contains("ufotable") -> Res.drawable.logo_studio_ufotable
            normalized.contains("kyoto animation") || normalized == "kyoani" -> Res.drawable.logo_studio_kyoto_animation
            normalized.contains("bones") -> Res.drawable.logo_studio_bones
            normalized.contains("madhouse") -> Res.drawable.logo_studio_madhouse
            normalized.contains("pierrot") -> Res.drawable.logo_studio_pierrot
            normalized.contains("production i.g") || normalized.contains("production ig") -> Res.drawable.logo_studio_production_ig
            normalized.contains("j.c.staff") || normalized.contains("jc staff") || normalized.contains("j.c. staff") || normalized.contains("jcstaff") -> Res.drawable.logo_studio_jc_staff
            normalized.contains("trigger") -> Res.drawable.logo_studio_trigger
            normalized.contains("white fox") || normalized.contains("whitefox") -> Res.drawable.logo_studio_white_fox
            normalized.contains("a-1 pictures") || normalized.contains("a 1 pictures") || normalized.contains("a1 pictures") -> Res.drawable.logo_studio_a_1_pictures
            normalized.contains("toei animation") || normalized.contains("toei") -> Res.drawable.logo_studio_toei_animation
            normalized.contains("david production") -> Res.drawable.logo_studio_david_production
            normalized.contains("studio ghibli") || normalized == "ghibli" -> Res.drawable.logo_studio_studio_ghibli
            normalized.contains("sunrise") || normalized.contains("bandai namco filmworks") -> Res.drawable.logo_studio_sunrise
            normalized.contains("mbs") || normalized.contains("mainichi broadcasting") -> Res.drawable.logo_studio_mbs
            normalized.contains("tv tokyo") || normalized.contains("television tokyo") -> Res.drawable.logo_studio_tv_tokyo
            normalized.contains("tokyo mx") || normalized.contains("tokyo metropolitan television") -> Res.drawable.logo_studio_tokyo_mx
            normalized.contains("tbs") || normalized.contains("tokyo broadcasting system") -> Res.drawable.logo_studio_tbs
            normalized.contains("fuji tv") || normalized.contains("fuji television") -> Res.drawable.logo_studio_fuji_tv
            normalized.contains("netflix") -> Res.drawable.logo_studio_netflix
            normalized.contains("nhk") -> Res.drawable.logo_studio_nhk
            normalized.contains("at-x") || normalized == "atx" -> Res.drawable.logo_studio_at_x
            normalized.contains("bs11") -> Res.drawable.logo_studio_bs11
            normalized.contains("p.a. works") || normalized.contains("pa works") || normalized.contains("p.a.works") -> Res.drawable.logo_studio_pa_works
            normalized.contains("science saru") -> Res.drawable.logo_studio_science_saru
            normalized.contains("studio bind") -> Res.drawable.logo_studio_studio_bind
            normalized.contains("studio deen") || normalized == "deen" -> Res.drawable.logo_studio_studio_deen
            normalized.contains("kinema citrus") -> Res.drawable.logo_studio_kinema_citrus
            normalized.contains("silver link") -> Res.drawable.logo_studio_silver_link
            normalized.contains("liden") || normalized.contains("lidenfilms") -> Res.drawable.logo_studio_liden_films
            normalized.contains("doga kobo") || normalized.contains("dogakobo") -> Res.drawable.logo_studio_doga_kobo
            normalized.contains("passione") -> Res.drawable.logo_studio_passione
            normalized.contains("comix wave") -> Res.drawable.logo_studio_comix_wave
            normalized.contains("orange") -> Res.drawable.logo_studio_orange
            normalized.contains("crunchyroll") -> Res.drawable.logo_studio_crunchyroll
            else -> null
        }
    }.getOrNull()

    fun findLogo(rawName: String?): String? = null
}
