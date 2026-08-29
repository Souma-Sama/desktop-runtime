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
            normalized.contains("kadokawa") -> Res.drawable.studio_kadokawa
            normalized.contains("mappa") -> Res.drawable.studio_mappa
            normalized.contains("wit studio") || (normalized.contains("wit") && !normalized.contains("with")) -> Res.drawable.studio_wit_studio
            normalized.contains("cloverworks") || normalized.contains("clover works") -> Res.drawable.studio_cloverworks
            normalized.contains("ufotable") -> Res.drawable.studio_ufotable
            normalized.contains("kyoto animation") || normalized == "kyoani" -> Res.drawable.studio_kyoto_animation
            normalized.contains("bones") -> Res.drawable.studio_bones
            normalized.contains("madhouse") -> Res.drawable.studio_madhouse
            normalized.contains("pierrot") -> Res.drawable.studio_pierrot
            normalized.contains("production i.g") || normalized.contains("production ig") -> Res.drawable.studio_production_ig
            normalized.contains("j.c.staff") || normalized.contains("jc staff") || normalized.contains("j.c. staff") || normalized.contains("jcstaff") -> Res.drawable.studio_jc_staff
            normalized.contains("trigger") -> Res.drawable.studio_trigger
            normalized.contains("white fox") || normalized.contains("whitefox") -> Res.drawable.studio_white_fox
            normalized.contains("a-1 pictures") || normalized.contains("a 1 pictures") || normalized.contains("a1 pictures") -> Res.drawable.studio_a_1_pictures
            normalized.contains("toei animation") || normalized.contains("toei") -> Res.drawable.studio_toei_animation
            normalized.contains("david production") -> Res.drawable.studio_david_production
            normalized.contains("studio ghibli") || normalized == "ghibli" -> Res.drawable.studio_studio_ghibli
            normalized.contains("sunrise") || normalized.contains("bandai namco filmworks") -> Res.drawable.studio_sunrise
            normalized.contains("mbs") || normalized.contains("mainichi broadcasting") -> Res.drawable.studio_mbs
            normalized.contains("tv tokyo") || normalized.contains("television tokyo") -> Res.drawable.studio_tv_tokyo
            normalized.contains("tokyo mx") || normalized.contains("tokyo metropolitan television") -> Res.drawable.studio_tokyo_mx
            normalized.contains("tbs") || normalized.contains("tokyo broadcasting system") -> Res.drawable.studio_tbs
            normalized.contains("fuji tv") || normalized.contains("fuji television") -> Res.drawable.studio_fuji_tv
            normalized.contains("netflix") -> Res.drawable.studio_netflix
            normalized.contains("nhk") -> Res.drawable.studio_nhk
            normalized.contains("at-x") || normalized == "atx" -> Res.drawable.studio_at_x
            normalized.contains("bs11") -> Res.drawable.studio_bs11
            normalized.contains("p.a. works") || normalized.contains("pa works") || normalized.contains("p.a.works") -> Res.drawable.studio_pa_works
            normalized.contains("science saru") -> Res.drawable.studio_science_saru
            normalized.contains("studio bind") -> Res.drawable.studio_studio_bind
            normalized.contains("studio deen") || normalized == "deen" -> Res.drawable.studio_studio_deen
            normalized.contains("kinema citrus") -> Res.drawable.studio_kinema_citrus
            normalized.contains("silver link") -> Res.drawable.studio_silver_link
            normalized.contains("liden") || normalized.contains("lidenfilms") -> Res.drawable.studio_liden_films
            normalized.contains("doga kobo") || normalized.contains("dogakobo") -> Res.drawable.studio_doga_kobo
            normalized.contains("passione") -> Res.drawable.studio_passione
            normalized.contains("comix wave") -> Res.drawable.studio_comix_wave_films
            normalized.contains("orange") -> Res.drawable.studio_orange
            normalized.contains("crunchyroll") -> Res.drawable.studio_crunchyroll
            else -> null
        }
    }

    fun findLogo(rawName: String?): String? = null
}
