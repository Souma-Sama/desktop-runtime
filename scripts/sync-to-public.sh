#!/bin/bash
set -e

SOURCE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TARGET_DIR="/Users/souma/Downloads/Nuvio-Kai Public"

echo "Syncing code from: $SOURCE_DIR"
echo "To public folder:  $TARGET_DIR"

if [ ! -d "$TARGET_DIR" ]; then
    mkdir -p "$TARGET_DIR"
fi

rsync -av --delete \
    --exclude='.git' \
    --exclude='.gradle' \
    --exclude='build' \
    --exclude='*/build' \
    --exclude='.idea' \
    --exclude='.kotlin' \
    --exclude='CHANGELOG_LOCAL.md' \
    --exclude='AGENTS.md' \
    --exclude='.DS_Store' \
    --exclude='scripts/sync-to-public.sh' \
    "$SOURCE_DIR/" "$TARGET_DIR/"

echo "Stripping comments and internal notes from custom public files..."
python3 - << 'PYEOF'
import os

PUBLIC_DIR = "/Users/souma/Downloads/Nuvio-Kai Public"

def strip_kotlin_comments(source: str) -> str:
    result = []
    i = 0
    n = len(source)
    
    in_single_quote = False
    in_double_quote = False
    in_triple_quote = False
    
    while i < n:
        if not in_single_quote and not in_double_quote:
            if source[i:i+3] == '"""':
                in_triple_quote = not in_triple_quote
                result.append('"""')
                i += 3
                continue
                
        if in_triple_quote:
            result.append(source[i])
            i += 1
            continue
            
        if not in_single_quote:
            if source[i] == '"' and (i == 0 or source[i-1] != '\\' or (i > 1 and source[i-2] == '\\')):
                in_double_quote = not in_double_quote
                result.append(source[i])
                i += 1
                continue
                
        if in_double_quote:
            result.append(source[i])
            i += 1
            continue
            
        if source[i] == "'" and (i == 0 or source[i-1] != '\\'):
            in_single_quote = not in_single_quote
            result.append(source[i])
            i += 1
            continue
            
        if in_single_quote:
            result.append(source[i])
            i += 1
            continue
            
        if source[i:i+2] == '//':
            while i < n and source[i] != '\n':
                i += 1
            continue
            
        if source[i:i+2] == '/*':
            i += 2
            nesting = 1
            while i < n and nesting > 0:
                if source[i:i+2] == '/*':
                    nesting += 1
                    i += 2
                elif source[i:i+2] == '*/':
                    nesting -= 1
                    i += 2
                else:
                    i += 1
            continue
            
        result.append(source[i])
        i += 1
        
    cleaned = "".join(result)
    lines = [line.rstrip() for line in cleaned.splitlines()]
    final_lines = []
    prev_blank = False
    for line in lines:
        is_blank = (len(line) == 0)
        if is_blank and prev_blank:
            continue
        final_lines.append(line)
        prev_blank = is_blank
        
    return "\n".join(final_lines) + "\n"

target_files = []
for root, _, files in os.walk(PUBLIC_DIR):
    for f in files:
        if f.endswith(".kt"):
            rel = os.path.relpath(os.path.join(root, f), PUBLIC_DIR)
            if "features/anilist" in rel or "AnimeTrackerMenu" in rel or "AnilistSettingsPage" in rel:
                target_files.append(os.path.join(root, f))

for fpath in target_files:
    with open(fpath, "r", encoding="utf-8") as f:
        content = f.read()
    cleaned = strip_kotlin_comments(content)
    with open(fpath, "w", encoding="utf-8") as f:
        f.write(cleaned)

print(f"Cleaned comments across {len(target_files)} custom files in public folder.")
PYEOF

echo "Sync and cleaning completed successfully!"
