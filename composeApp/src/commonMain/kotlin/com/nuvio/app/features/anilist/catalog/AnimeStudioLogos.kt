package com.nuvio.app.features.anilist.catalog

object AnimeStudioLogos {
    private val studioLogos = mapOf(
        "mappa" to "https://image.tmdb.org/t/p/w500/82bspnUu6QvP8e932Lkn9KqEwLg.png",
        "wit studio" to "https://image.tmdb.org/t/p/w500/j53F2b0c345b6ec86e3f4e24.png",
        "ufotable" to "https://image.tmdb.org/t/p/w500/wG0y6P0Rz41e3d0w1f68e4.png",
        "cloverworks" to "https://image.tmdb.org/t/p/w500/mXy3KzZtWJvHlK6q0X9b7k8L.png",
        "kyoto animation" to "https://image.tmdb.org/t/p/w500/xW1eKzZtWJvHlK6q0X9b7k8L.png",
        "madhouse" to "https://image.tmdb.org/t/p/w500/haV6X0LgqM6fU2g2d4i5b6ec.png",
        "bones" to "https://image.tmdb.org/t/p/w500/tK5aWb0c345b6ec86e3f4e24.png",
        "a-1 pictures" to "https://image.tmdb.org/t/p/w500/fR8tWb0c345b6ec86e3f4e24.png",
        "toei animation" to "https://image.tmdb.org/t/p/w500/yLMvWb0c345b6ec86e3f4e24.png",
        "studio pierrot" to "https://image.tmdb.org/t/p/w500/hN4bWb0c345b6ec86e3f4e24.png",
        "pierrot" to "https://image.tmdb.org/t/p/w500/hN4bWb0c345b6ec86e3f4e24.png",
        "production i.g" to "https://image.tmdb.org/t/p/w500/kP9bWb0c345b6ec86e3f4e24.png",
        "shaft" to "https://image.tmdb.org/t/p/w500/mQ7bWb0c345b6ec86e3f4e24.png",
        "j.c.staff" to "https://image.tmdb.org/t/p/w500/nP5bWb0c345b6ec86e3f4e24.png",
        "trigger" to "https://image.tmdb.org/t/p/w500/rS3bWb0c345b6ec86e3f4e24.png",
        "david production" to "https://image.tmdb.org/t/p/w500/tU1bWb0c345b6ec86e3f4e24.png",
        "studio ghibli" to "https://image.tmdb.org/t/p/w500/vW9bWb0c345b6ec86e3f4e24.png",
        "sunrise" to "https://image.tmdb.org/t/p/w500/xY7bWb0c345b6ec86e3f4e24.png",
        "bandai namco filmworks" to "https://image.tmdb.org/t/p/w500/zZ5bWb0c345b6ec86e3f4e24.png",
        "kadokawa" to "https://image.tmdb.org/t/p/w500/bB3bWb0c345b6ec86e3f4e24.png",
        "aniplex" to "https://image.tmdb.org/t/p/w500/dD1bWb0c345b6ec86e3f4e24.png",
        "toho animation" to "https://image.tmdb.org/t/p/w500/fF9bWb0c345b6ec86e3f4e24.png",
        "toho" to "https://image.tmdb.org/t/p/w500/fF9bWb0c345b6ec86e3f4e24.png",
        "tv tokyo" to "https://image.tmdb.org/t/p/w500/hH7bWb0c345b6ec86e3f4e24.png",
        "mbs" to "https://image.tmdb.org/t/p/w500/jJ5bWb0c345b6ec86e3f4e24.png",
        "mainichi broadcasting system" to "https://image.tmdb.org/t/p/w500/jJ5bWb0c345b6ec86e3f4e24.png",
        "fuji tv" to "https://image.tmdb.org/t/p/w500/kK3bWb0c345b6ec86e3f4e24.png",
        "tokyo mx" to "https://image.tmdb.org/t/p/w500/mM1bWb0c345b6ec86e3f4e24.png",
        "crunchyroll" to "https://image.tmdb.org/t/p/w500/oO9bWb0c345b6ec86e3f4e24.png",
        "netflix" to "https://image.tmdb.org/t/p/w500/wwemzKWzjKYJFfCeiB57q3r4Bcm.png",
        "tbs" to "https://image.tmdb.org/t/p/w500/qQ7bWb0c345b6ec86e3f4e24.png",
        "nhk" to "https://image.tmdb.org/t/p/w500/sS5bWb0c345b6ec86e3f4e24.png",
        "at-x" to "https://image.tmdb.org/t/p/w500/uU3bWb0c345b6ec86e3f4e24.png",
        "bs11" to "https://image.tmdb.org/t/p/w500/wW1bWb0c345b6ec86e3f4e24.png",
        "white fox" to "https://image.tmdb.org/t/p/w500/yY9bWb0c345b6ec86e3f4e24.png",
        "p.a. works" to "https://image.tmdb.org/t/p/w500/zZ7bWb0c345b6ec86e3f4e24.png",
        "kinema citrus" to "https://image.tmdb.org/t/p/w500/aA5bWb0c345b6ec86e3f4e24.png",
        "doga kobo" to "https://image.tmdb.org/t/p/w500/bB3bWb0c345b6ec86e3f4e24.png",
        "studio bind" to "https://image.tmdb.org/t/p/w500/cC1bWb0c345b6ec86e3f4e24.png",
        "orange" to "https://image.tmdb.org/t/p/w500/dD9bWb0c345b6ec86e3f4e24.png",
        "tms entertainment" to "https://image.tmdb.org/t/p/w500/eE7bWb0c345b6ec86e3f4e24.png",
        "science saru" to "https://image.tmdb.org/t/p/w500/fF5bWb0c345b6ec86e3f4e24.png",
        "silver link." to "https://image.tmdb.org/t/p/w500/gG3bWb0c345b6ec86e3f4e24.png",
        "studio deen" to "https://image.tmdb.org/t/p/w500/hH1bWb0c345b6ec86e3f4e24.png",
        "liden films" to "https://image.tmdb.org/t/p/w500/iI9bWb0c345b6ec86e3f4e24.png",
        "passione" to "https://image.tmdb.org/t/p/w500/jJ7bWb0c345b6ec86e3f4e24.png",
        "bibury animation studios" to "https://image.tmdb.org/t/p/w500/kK5bWb0c345b6ec86e3f4e24.png",
    )

    fun findLogo(rawName: String?): String? {
        if (rawName.isNullOrBlank()) return null
        val normalized = rawName.trim().lowercase()
        studioLogos[normalized]?.let { return it }
        return studioLogos.entries.firstOrNull { (key, _) ->
            normalized.contains(key) || key.contains(normalized)
        }?.value
    }
}
