package com.nuvio.app.features.anilist.catalog

import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource

object AnimeStudioLogos {
    fun findLogoResource(rawName: String?): DrawableResource? = runCatching {
        if (rawName.isNullOrBlank()) return null
        val normalized = rawName.trim().lowercase()
        when {
            // Major Animation Studios & Brands
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
            normalized.contains("shaft") -> Res.drawable.logo_studio_shaft
            normalized.contains("tms entertainment") || normalized.contains("tms") -> Res.drawable.logo_studio_tms
            normalized.contains("olm") -> Res.drawable.logo_studio_olm
            normalized.contains("nippon animation") -> Res.drawable.logo_studio_nippon_animation
            normalized.contains("gonzo") -> Res.drawable.logo_studio_gonzo
            normalized.contains("8bit") || normalized.contains("eight bit") -> Res.drawable.logo_studio_eight_bit
            normalized.contains("feel.") || normalized == "feel" -> Res.drawable.logo_studio_feel
            normalized.contains("tatsunoko") -> Res.drawable.logo_studio_tatsunoko
            normalized.contains("gainax") -> Res.drawable.logo_studio_gainax
            normalized.contains("bibury") -> Res.drawable.logo_studio_bibury
            normalized.contains("studio kai") -> Res.drawable.logo_studio_kai
            normalized.contains("brain's base") || normalized.contains("brains base") -> Res.drawable.logo_studio_brains_base
            normalized.contains("c-station") || normalized.contains("c station") -> Res.drawable.logo_studio_c_station
            normalized.contains("colorido") -> Res.drawable.logo_studio_colorido
            normalized.contains("gohands") || normalized.contains("go hands") -> Res.drawable.logo_studio_gohands
            normalized.contains("project no.9") || normalized.contains("project no 9") -> Res.drawable.logo_studio_project_no9
            normalized.contains("nexus") -> Res.drawable.logo_studio_nexus
            normalized.contains("engi") -> Res.drawable.logo_studio_engi
            normalized.contains("cygames") -> Res.drawable.logo_studio_cygames
            normalized.contains("diomedéa") || normalized.contains("diomedea") -> Res.drawable.logo_studio_diomedea
            normalized.contains("satelight") -> Res.drawable.logo_studio_satelight
            normalized.contains("manglobe") -> Res.drawable.logo_studio_manglobe
            normalized.contains("actas") -> Res.drawable.logo_studio_actas
            normalized.contains("millepensee") -> Res.drawable.logo_studio_millepensee
            normalized.contains("studio nut") || normalized.contains(" nut") -> Res.drawable.logo_studio_nut
            normalized.contains("studio 3hz") || normalized.contains("3hz") -> Res.drawable.logo_studio_3hz
            normalized.contains("lay-duce") || normalized.contains("layduce") -> Res.drawable.logo_studio_layduce
            normalized.contains("studio gokumi") || normalized.contains("gokumi") -> Res.drawable.logo_studio_gokumi
            normalized.contains("seven arcs") -> Res.drawable.logo_studio_seven_arcs
            normalized.contains("graphinica") -> Res.drawable.logo_studio_graphinica
            normalized.contains("ajia-do") || normalized.contains("ajiado") -> Res.drawable.logo_studio_ajiado
            normalized.contains("troyca") -> Res.drawable.logo_studio_troyca
            normalized.contains("pine jam") -> Res.drawable.logo_studio_pine_jam
            normalized.contains("zero-g") || normalized.contains("zero g") -> Res.drawable.logo_studio_zero_g
            normalized.contains("yokohama animation") -> Res.drawable.logo_studio_yokohama
            normalized.contains("pontdarc") || normalized.contains("atelierpontdarc") -> Res.drawable.logo_studio_pontdarc
            normalized.contains("felix film") || normalized.contains("felixfilm") -> Res.drawable.logo_studio_felix
            normalized.contains("tezuka") -> Res.drawable.logo_studio_tezuka

            // Publishers, Producers & Music
            normalized.contains("pony canyon") -> Res.drawable.logo_studio_pony_canyon
            normalized.contains("bandai namco") || normalized.contains("bandai visual") -> Res.drawable.logo_studio_bandai_namco
            normalized.contains("square enix") -> Res.drawable.logo_studio_square_enix
            normalized.contains("shueisha") -> Res.drawable.logo_studio_shueisha
            normalized.contains("kodansha") -> Res.drawable.logo_studio_kodansha
            normalized.contains("shogakukan") -> Res.drawable.logo_studio_shogakukan
            normalized.contains("bushiroad") -> Res.drawable.logo_studio_bushiroad
            normalized.contains("lantis") -> Res.drawable.logo_studio_lantis
            normalized.contains("sentai filmworks") || normalized.contains("sentai") -> Res.drawable.logo_studio_sentai

            // TV Broadcast Networks & Streaming
            normalized.contains("mbs") || normalized.contains("mainichi broadcasting") -> Res.drawable.logo_studio_mbs
            normalized.contains("tv tokyo") || normalized.contains("television tokyo") -> Res.drawable.logo_studio_tv_tokyo
            normalized.contains("tokyo mx") || normalized.contains("tokyo metropolitan television") -> Res.drawable.logo_studio_tokyo_mx
            normalized.contains("tbs") || normalized.contains("tokyo broadcasting system") -> Res.drawable.logo_studio_tbs
            normalized.contains("fuji tv") || normalized.contains("fuji television") -> Res.drawable.logo_studio_fuji_tv
            normalized.contains("nippon television") || normalized.contains("nippon tv") || normalized == "ntv" -> Res.drawable.logo_studio_ntv
            normalized.contains("netflix") -> Res.drawable.logo_studio_netflix
            normalized.contains("nhk") -> Res.drawable.logo_studio_nhk
            normalized.contains("at-x") || normalized == "atx" -> Res.drawable.logo_studio_at_x
            normalized.contains("bs11") -> Res.drawable.logo_studio_bs11
            normalized.contains("crunchyroll") -> Res.drawable.logo_studio_crunchyroll
            normalized.contains("hidive") -> Res.drawable.logo_studio_hidive
            normalized.contains("disney+") || normalized.contains("disney plus") -> Res.drawable.logo_studio_disney_plus
            else -> null
        }
    }.getOrNull()

    fun findLogo(rawName: String?): String? = null
}
