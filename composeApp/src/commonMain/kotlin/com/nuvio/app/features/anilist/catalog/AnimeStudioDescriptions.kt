package com.nuvio.app.features.anilist.catalog

object AnimeStudioDescriptions {
    fun findDescription(rawName: String?): String? {
        if (rawName.isNullOrBlank()) return null
        val n = rawName.trim().lowercase()
        return when {
            // Major Animation Studios
            n.contains("mappa") ->
                "MAPPA Co., Ltd. is a Japanese animation studio founded on June 14, 2011, by Masao Maruyama, the co-founder of Madhouse. The studio has produced critically acclaimed series including Attack on Titan: The Final Season, Jujutsu Kaisen, Chainsaw Man, and Vinland Saga Season 2."
            n.contains("wit studio") || (n.contains("wit") && !n.contains("with")) ->
                "Wit Studio, Inc. is a Japanese animation studio founded on June 1, 2012, by producers at Production I.G as a subsidiary of IG Port. The studio gained worldwide acclaim for animating the first three seasons of Attack on Titan, Vinland Saga, Spy x Family, and Ranking of Kings."
            n.contains("cloverworks") || n.contains("clover works") ->
                "CloverWorks Inc. is a Japanese animation studio rebranded from A-1 Pictures' Kōenji Studio in 2018 as a subsidiary of Aniplex. Notable works include The Promised Neverland, Bocchi the Rock!, My Dress-Up Darling, Spy x Family, and Horimiya."
            n.contains("ufotable") ->
                "Ufotable, Inc. is a Japanese animation studio founded in October 2000 by former Telecom Animation Film producer Hikaru Kondo. The studio is celebrated internationally for its groundbreaking digital animation and visual effects in the Demon Slayer: Kimetsu no Yaiba and Fate/stay night series."
            n.contains("kyoto animation") || n == "kyoani" ->
                "Kyoto Animation Co., Ltd., often abbreviated as KyoAni, is a renowned Japanese animation studio and light novel publisher located in Uji, Kyoto Prefecture. Established in 1981, the studio is celebrated for its exceptional production quality, wholesome storytelling, and in-house salaried animators."
            n.contains("bones") ->
                "Bones Inc. is a Japanese animation studio founded in October 1998 by former Sunrise staff members Masahiko Minami, Hiroshi Ōsaka, and Toshihiro Kawamoto. Notable productions include Fullmetal Alchemist: Brotherhood, My Hero Academia, Mob Psycho 100, and Bungo Stray Dogs."
            n.contains("madhouse") ->
                "Madhouse Inc. is a legendary Japanese animation studio founded in 1972 by former Mushi Production animators. The studio has produced iconic global masterpieces including Death Note, One Punch Man Season 1, Hunter x Hunter (2011), Frieren: Beyond Journey's End, and Summer Wars."
            n.contains("pierrot") ->
                "Studio Pierrot Co., Ltd. is a Japanese animation studio founded in May 1979 by former Tatsunoko Production and Mushi Production employees. The studio is internationally famous for long-running shonen anime franchises such as Naruto, Bleach, Yu Yu Hakusho, Tokyo Ghoul, and Black Clover."
            n.contains("production i.g") || n.contains("production ig") ->
                "Production I.G, Inc. is a Japanese animation studio and production enterprise founded in December 1987 by Mitsuhisa Ishikawa and Takayuki Goto. Renowned for cutting-edge sci-fi and sports animation, its works include Ghost in the Shell, Psycho-Pass, Haikyu!!, and Kuroko's Basketball."
            n.contains("j.c.staff") || n.contains("jc staff") || n.contains("jcstaff") ->
                "J.C.Staff Co., Ltd. is a Japanese animation studio founded in January 1986 by Tomoyuki Miyata. The studio has animated over 300 anime titles, including Toradora!, Food Wars!: Shokugeki no Soma, A Certain Magical Index, The Familiar of Zero, and DanMachi."
            n.contains("trigger") ->
                "Studio Trigger Inc. is an acclaimed Japanese animation studio founded in August 2011 by former Gainax animators Hiroyuki Imaishi and Masahiko Otsuka. Famous for its hyper-kinetic animation style and bold visuals, works include Kill la Kill, Cyberpunk: Edgerunners, Little Witch Academia, and Delicious in Dungeon."
            n.contains("white fox") || n.contains("whitefox") ->
                "White Fox Co., Ltd. is a Japanese animation studio founded in April 2007 by Gaku Iwasa. The studio is renowned for producing masterclass fantasy and sci-fi adaptations such as Steins;Gate, Re:Zero - Starting Life in Another World, Katanagatari, and Goblin Slayer."
            n.contains("a-1 pictures") || n.contains("a 1 pictures") || n.contains("a1 pictures") ->
                "A-1 Pictures Inc. is a leading Japanese animation studio established on May 9, 2005, by Sony Music Entertainment Japan's anime production division Aniplex. Notable works include Sword Art Online, Kaguya-sama: Love Is War, Your Lie in April, 86, and Solo Leveling."
            n.contains("toei animation") || n == "toei" ->
                "Toei Animation Co., Ltd. is one of the oldest and largest animation studios in Japan, founded in 1948. The pioneer of Japanese television animation, Toei has created global cultural phenomenons including Dragon Ball, One Piece, Sailor Moon, Digimon, and Slam Dunk."
            n.contains("david production") ->
                "David Production Inc. is a Japanese animation studio founded in September 2007 by former Gonzo president Kōji Kajita. Acquired by Fuji Television in 2014, the studio is best known for its faithful adaptation of JoJo's Bizarre Adventure and Fire Force."
            n.contains("studio ghibli") || n == "ghibli" ->
                "Studio Ghibli Inc. is an internationally acclaimed Japanese animation film studio founded in June 1985 by directors Hayao Miyazaki and Isao Takahata and producer Toshio Suzuki. Ghibli has produced Academy Award-winning cinematic masterpieces such as Spirited Away, Princess Mononoke, and My Neighbor Totoro."
            n.contains("sunrise") || n.contains("bandai namco filmworks") ->
                "Bandai Namco Filmworks Inc., historically known as Sunrise, is a premier Japanese animation studio founded in September 1972 by former Mushi Production staff. Celebrated as the pioneer of the mecha genre, notable works include Mobile Suit Gundam, Code Geass, Cowboy Bebop, and Love Live!."
            n.contains("p.a. works") || n.contains("pa works") || n.contains("p.a.works") ->
                "P.A. Works Inc. (Progressive Animation Works) is a Japanese animation studio founded in November 2000 by Kenji Horikawa and located in Nanto, Toyama. Celebrated for its scenic realism and heartfelt coming-of-age dramas, works include Angel Beats!, Shirobako, Charlotte, and Ya Boy Kongming!."
            n.contains("science saru") ->
                "Science SARU Inc. is an acclaimed Japanese animation studio established in February 2013 by director Masaaki Yuasa and producer Eunyoung Choi. Recognized for innovative, hybrid hand-drawn and digital animation techniques, works include Devilman Crybaby, Keep Your Hands Off Eizouken!, and Dan Da Dan."
            n.contains("studio bind") ->
                "Studio Bind Inc. is a Japanese animation studio founded in November 2018 as a joint venture between animation studio White Fox and production company Egg Firm. It was established primarily to produce the flagship television adaptation of Mushoku Tensei: Jobless Reincarnation."
            n.contains("studio deen") || n == "deen" ->
                "Studio Deen Co., Ltd. is a veteran Japanese animation studio founded in March 1975 by former Sunrise employees. The studio has animated legendary anime series such as KonoSuba, Fate/stay night (2006), Rurouni Kenshin, Higurashi: When They Cry, and Fruits Basket (2001)."
            n.contains("kinema citrus") ->
                "Kinema Citrus Co., Ltd. is a Japanese animation studio founded in March 2008 by former Production I.G and Bones staff. Renowned for rich background art and emotional storytelling, notable works include Made in Abyss, The Rising of the Shield Hero, and Revue Starlight."
            n.contains("silver link") ->
                "Silver Link, Inc. is a Japanese animation studio founded in December 2007 by animation producer Hayato Kaneko. The studio is known for popular light novel and comedy adaptations such as Fate/kaleid liner Prisma Illya, Non Non Biyori, Baka and Test, and The Misfit of Demon King Academy."
            n.contains("liden") || n.contains("lidenfilms") ->
                "Liden Films, Inc. is a Japanese animation studio established in February 2012 as part of the Ultra Super Pictures holding company. Notable works include Tokyo Revengers, Call of the Night, Bastard!!, and the 2023 remake of Rurouni Kenshin."
            n.contains("doga kobo") || n.contains("dogakobo") ->
                "Doga Kobo, Inc. is a Japanese animation studio founded in July 1973 by former Toei Animation animators. Renowned for its vibrant character animation and charming slice-of-life and drama adaptations, notable works include Oshi no Ko, Plastic Memories, Monthly Girls' Nozaki-kun, and New Game!."
            n.contains("passione") ->
                "Passione Co., Ltd. is a Japanese animation studio founded in January 2011. The studio has produced notable anime adaptations including Higurashi: When They Cry - Gou/Sotsu, Mieruko-chan, The Demon Sword Master of Excalibur Academy, and High School DxD Hero."
            n.contains("comix wave") ->
                "Comix Wave Films Inc. is a Japanese animation film studio and distribution company established in March 2007. The studio is celebrated worldwide for producing and distributing director Makoto Shinkai's masterpiece feature films, including Your Name, Weathering with You, and Suzume."
            n.contains("orange") ->
                "Orange Co., Ltd. is a Japanese animation studio established in May 2004 by CG animator Eiji Inomoto. The studio is world-renowned for pioneering expressive 3D CGI animation with cinematic hand-drawn direction in series such as Beastars, Land of the Lustrous, and Trigun Stampede."
            n.contains("shaft") ->
                "Shaft Inc. is a Japanese animation studio founded in September 1975 by Hiroshi Wakao. Under the creative direction of Akiyuki Shinbo, Shaft became world-famous for its avant-garde visual style, distinct head tilts, and surreal cinematography in the Monogatari Series and Puella Magi Madoka Magica."
            n.contains("tms") ->
                "TMS Entertainment, Ltd. is one of the oldest and most prolific anime studios in Japan, founded in October 1946. A subsidiary of Sega Sammy Holdings, TMS has produced legendary classics and modern hits such as Detective Conan, Lupin the Third, Dr. Stone, Fruits Basket (2019), and Akira."
            n.contains("olm") ->
                "OLM, Inc. (Oriental Light and Magic) is a Japanese animation and film studio founded in June 1990. The studio is universally famous as the animation house behind the entire Pokémon television and movie franchise, as well as The Apothecary Diaries, Komi Can't Communicate, and Inazuma Eleven."
            n.contains("feel.") || n == "feel" ->
                "Feel. (Studio Feel) is a Japanese animation studio established in December 2002 by former Studio Pierrot staff. Notable works include My Teen Romantic Comedy SNAFU (Seasons 2 & 3), Hinamatsuri, Dagashi Kashi, and Spy Classroom."
            n.contains("8bit") || n.contains("eight bit") ->
                "Eight Bit Inc. (8bit) is a Japanese animation studio founded in September 2008 by former Satelight members. The studio is best known for animating That Time I Got Reincarnated as a Slime, Blue Lock, The Irregular at Magic High School (Season 2), and Infinite Stratos."
            n.contains("bibury") ->
                "Bibury Animation Studios G.K. is a Japanese animation studio founded in May 2017 by director Tensho (Motoki Tanaka). Notable works include The Quintessential Quintuplets ∬, Azur Lane, Black★★Rock Shooter: Dawn Fall, and The 100 Girlfriends Who Really, Really, Really, Really, REALLY Love You."
            n.contains("diomedéa") || n.contains("diomedea") ->
                "Diomedéa Inc. is a Japanese animation studio founded in October 2005. Notable productions include Domestic Girlfriend, Fuuka, The Magical Revolution of the Reincarnated Princess and the Genius Young Lady, and Squid Girl."
            n.contains("troyca") ->
                "Troyca Inc. is a Japanese animation studio co-founded in May 2013 by former AIC Classic staff including director Ei Aoki. Notable productions include Aldnoah.Zero, Re:CREATORS, Lord El-Melloi II's Case Files, and Overtake!."

            // Major Broadcasters & Networks
            n.contains("tokyo mx") || n.contains("tokyo metropolitan television") ->
                "Tokyo MX (JOMX-DTV) is an independent commercial television broadcasting station in Tokyo, Japan, owned by the Tokyo Metropolitan Television Broadcasting Corporation. It serves as the primary nighttime broadcast home for the vast majority of late-night anime series in the greater Tokyo metropolitan area."
            n.contains("tv tokyo") || n.contains("television tokyo") ->
                "TV Tokyo (JOTX-DTV) is a major Japanese television station and the flagship of the TX Network. Renowned for broadcasting flagship anime series across Japan, its historic lineup includes Naruto, Bleach, Pokémon, Yu-Gi-Oh!, Gintama, Neon Genesis Evangelion, and Spy x Family."
            n.contains("mbs") || n.contains("mainichi broadcasting") ->
                "Mainichi Broadcasting System, Inc. (MBS) is a major commercial radio and television broadcaster based in Osaka, Japan, affiliated with the Japan News Network (JNN). MBS is renowned for its iconic Animeism and Super Animeism broadcast programming blocks, premiering Attack on Titan, Jujutsu Kaisen, and Mobile Suit Gundam: Iron-Blooded Orphans."
            n.contains("fuji tv") || n.contains("fuji television") ->
                "Fuji Television Network, Inc. is a major Japanese television network and the flagship station of the Fuji News Network (FNN). It is famous for its groundbreaking Noitamina and +Ultra anime programming blocks, as well as broadcasting One Piece and Dragon Ball."
            n.contains("tbs") || n.contains("tokyo broadcasting system") ->
                "Tokyo Broadcasting System Television, Inc. (TBS) is the flagship television station of the Japan News Network (JNN). TBS has produced and broadcast numerous iconic anime series, including K-On!, Clannad, The Quintessential Quintuplets, and Hanako-kun."
            n.contains("nippon television") || n.contains("nippon tv") || n == "ntv" ->
                "Nippon Television Network Corporation (NTV) is a major commercial television network in Japan. NTV is famous for broadcasting iconic anime classics including Death Note, Hunter x Hunter, Detective Conan, and Studio Ghibli television premieres."
            n.contains("at-x") || n == "atx" ->
                "Anime Theater X (AT-X) is a specialized Japanese satellite broadcasting television network owned by AT-X, Inc., a subsidiary of TV Tokyo MediaNet. Dedicated exclusively to anime, AT-X is celebrated for broadcasting uncut, premium, and exclusive television premieres of anime series across Japan."
            n.contains("bs11") ->
                "Nippon BS Broadcasting Corporation (BS11) is a free-to-air satellite television broadcaster in Japan, owned by Bic Camera. BS11 is widely famous for its daily prime-time and late-night Anime+ programming block, broadcasting anime nationwide across Japan."
            n.contains("nhk") ->
                "NHK (Japan Broadcasting Corporation) is Japan's national public broadcaster. NHK has produced and broadcast critically acclaimed educational and narrative anime series including Attack on Titan (Final Season), Vinland Saga (Season 1), Welcome to Demon School! Iruma-kun, and Love Live! Superstar!!."
            n.contains("crunchyroll") ->
                "Crunchyroll is a global anime brand and streaming service offering the world's largest dedicated anime library, simulcasting series directly from Japan in multiple dubbed and subtitled languages."
            n.contains("hidive") ->
                "HIDIVE is an anime streaming service owned by AMC Networks' Sentai Filmworks, specializing in simulcasts, exclusive dubs, and classic uncut anime series."
            n.contains("netflix") ->
                "Netflix is a global streaming entertainment service that co-produces and distributes exclusive anime series, films, and international adaptations worldwide."
            else -> null
        }
    }
}
