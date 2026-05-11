#!/usr/bin/env bash
# Downloads Temurin Windows x64 JRE 8 into the given destination dir.
# Used by build-dist.sh to bundle a JRE with the Windows installer EXE so
# end users don't need a separately installed JDK.

set -euo pipefail
DEST="${1:?usage: $0 <dest-dir>}"

if [ -d "$DEST" ] && [ -x "$DEST/bin/java.exe" ]; then
  echo "JRE already present at $DEST"
  exit 0
fi

API_URL="https://api.adoptium.net/v3/binary/latest/8/ga/windows/x64/jre/hotspot/normal/eclipse"
TMP_ZIP="$(mktemp -t windows-jre.XXXXXX).zip"
TMP_DIR="$(mktemp -d -t windows-jre.XXXXXX)"

echo "Downloading Temurin Windows x64 JRE 8..."
curl -fsSL "$API_URL" -o "$TMP_ZIP"

echo "Extracting..."
unzip -q "$TMP_ZIP" -d "$TMP_DIR"

# Temurin zip contains a single versioned folder (e.g., jdk8u482-b08-jre); flatten it.
INNER="$(ls "$TMP_DIR" | head -n 1)"
rm -rf "$DEST"
mkdir -p "$(dirname "$DEST")"
mv "$TMP_DIR/$INNER" "$DEST"
rm -rf "$TMP_DIR" "$TMP_ZIP"

echo "JRE installed at $DEST"
