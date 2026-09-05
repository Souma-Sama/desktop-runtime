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

echo "Sync completed successfully!"
