#!/usr/bin/env python3
"""
update_arm_index.py
Fetches the open-source Fribb anime-lists dataset and generates
the high-performance, offline ArmStaticIndex.kt file for Nuvio Kai.
"""

import urllib.request
import json
import os
import sys

OUTPUT_FILE = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "composeApp/src/commonMain/kotlin/com/nuvio/app/features/anilist/streams/ArmStaticIndex.kt"
)

URL = "https://raw.githubusercontent.com/Fribb/anime-lists/master/anime-list-mini.json"

def fetch_data():
    print(f"Fetching mapping database from {URL}...")
    req = urllib.request.Request(URL, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req, timeout=15) as resp:
        return json.loads(resp.read().decode("utf-8"))

def build_index(data, limit=8500):
    entries = []
    # Prioritize items with IMDb ID, then items with Kitsu ID
    items_with_imdb = []
    items_with_kitsu_only = []
    
    for item in data:
        al_id = item.get("anilist_id")
        if not al_id:
            continue
        
        imdb = item.get("imdb_id")
        imdb_str = imdb[0] if isinstance(imdb, list) and imdb else (imdb if isinstance(imdb, str) else "")
        kitsu = item.get("kitsu_id")
        kitsu_str = str(kitsu) if kitsu else ""
        
        if imdb_str:
            items_with_imdb.append((al_id, imdb_str, kitsu_str, item))
        elif kitsu_str:
            items_with_kitsu_only.append((al_id, "", kitsu_str, item))
            
    combined = items_with_imdb + items_with_kitsu_only
    selected_items = combined[:limit]
    
    for al_id, imdb_str, kitsu_str, item in selected_items:
        tmdb = item.get("themoviedb_id")
        tmdb_val = ""
        if isinstance(tmdb, dict):
            tv = tmdb.get("tv")
            movie = tmdb.get("movie")
            if tv: tmdb_val = str(tv)
            elif movie and isinstance(movie, list) and movie: tmdb_val = str(movie[0])
            elif movie: tmdb_val = str(movie)
        elif tmdb:
            tmdb_val = str(tmdb)
            
        tvdb = item.get("tvdb_id")
        tvdb_str = str(tvdb) if tvdb else ""
        mal = item.get("mal_id")
        mal_str = str(mal) if mal else ""
        
        season_val = "1"
        season = item.get("season")
        if isinstance(season, dict):
            s = season.get("tvdb") or season.get("tmdb")
            if s is not None: season_val = str(s)
            
        # Packed entry format: anilist:imdb:kitsu:tmdb:tvdb:mal:season
        packed = f"{al_id}:{imdb_str}:{kitsu_str}:{tmdb_val}:{tvdb_str}:{mal_str}:{season_val}"
        entries.append(packed)
        
    print(f"Packed {len(entries)} anime mappings (including {len(items_with_imdb)} with IMDb IDs).")
    return entries

def chunk_entries(entries, max_chars_per_chunk=30000):
    chunks = []
    current_chunk = []
    current_len = 0
    for entry in entries:
        entry_len = len(entry) + 1 # for delimiter '|'
        if current_len + entry_len > max_chars_per_chunk:
            chunks.append("|".join(current_chunk))
            current_chunk = [entry]
            current_len = entry_len
        else:
            current_chunk.append(entry)
            current_len += entry_len
    if current_chunk:
        chunks.append("|".join(current_chunk))
    return chunks

def generate_kotlin_file(chunks, total_entries):
    lines = [
        "package com.nuvio.app.features.anilist.streams",
        "",
        "import com.nuvio.app.features.anilist.catalog.AnilistMetaDetailsResolver.ArmMapping",
        "",
        "/**",
        " * Pre-compiled, offline static index of anime mappings (AniList ID -> IMDb / Kitsu / TMDb / TVDb).",
        f" * Contains {total_entries} verified mappings for instant 0ms Frame-0 resolution with zero network traffic.",
        " * Generated automatically by scripts/update_arm_index.py.",
        " */",
        "object ArmStaticIndex {",
        "    private val indexMap: Map<Int, ArmMapping> by lazy {",
        f"        val map = HashMap<Int, ArmMapping>({int(total_entries * 1.3)})",
        "        for (chunk in CHUNKS) {",
        "            val entries = chunk.split('|')",
        "            for (entry in entries) {",
        "                if (entry.isEmpty()) continue",
        "                val parts = entry.split(':')",
        "                if (parts.size >= 7) {",
        "                    val anilistId = parts[0].toIntOrNull() ?: continue",
        "                    val imdbId = parts[1].ifEmpty { null }",
        "                    val kitsuId = parts[2].ifEmpty { null }",
        "                    val tmdbId = parts[3].toIntOrNull()",
        "                    val tvdbId = parts[4].ifEmpty { null }",
        "                    val malId = parts[5].toIntOrNull()",
        "                    val season = parts[6].toIntOrNull() ?: 1",
        "                    map[anilistId] = ArmMapping(",
        "                        imdbId = imdbId,",
        "                        kitsuId = kitsuId,",
        "                        tmdbId = tmdbId,",
        "                        tvdbId = tvdbId,",
        "                        malId = malId,",
        "                        season = season,",
        "                    )",
        "                }",
        "            }",
        "        }",
        "        map",
        "    }",
        "",
        "    fun find(anilistId: Int): ArmMapping? = indexMap[anilistId]",
        "",
        "    val count: Int",
        "        get() = indexMap.size",
        "",
        "    private val CHUNKS: Array<String> = arrayOf(",
    ]
    
    for i, chunk in enumerate(chunks):
        lines.append(f'        // Chunk {i + 1} ({len(chunk)} chars)')
        lines.append(f'        "{chunk}",')
        
    lines.append("    )")
    lines.append("}")
    lines.append("")
    return "\n".join(lines)

def main():
    try:
        data = fetch_data()
    except Exception as e:
        print(f"Error fetching data: {e}", file=sys.stderr)
        sys.exit(1)
        
    entries = build_index(data, limit=8500)
    chunks = chunk_entries(entries)
    print(f"Split {len(entries)} entries into {len(chunks)} bytecode-safe chunks.")
    
    kotlin_code = generate_kotlin_file(chunks, len(entries))
    os.makedirs(os.path.dirname(OUTPUT_FILE), exist_ok=True)
    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        f.write(kotlin_code)
        
    print(f"Successfully generated {OUTPUT_FILE} ({len(kotlin_code)} bytes).")

if __name__ == "__main__":
    main()
