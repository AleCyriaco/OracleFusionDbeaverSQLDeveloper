#!/bin/bash
# Builds all three artifacts and assembles the distribution package.
# Output: dist/fusion-query-jdbc-1.0.0.zip containing both install paths + README,
# plus dist/fusion-sqldev-installer-1.0.0-windows-bundle.zip with the EXE +
# bundled Windows JRE for users without Java installed.

set -e
cd "$(dirname "$0")"
ROOT="$(pwd)"
DIST="$ROOT/dist"
WIN_JRE="$ROOT/.build/windows-jre"

echo "[1/5] Building JDBC driver"
( cd "$ROOT/fusion-query-jdbc" && mvn -q install )

echo "[2/5] Building SQL Developer extension"
( cd "$ROOT/fusion-sqldev-extension" && mvn -q package )

echo "[3/5] Building cross-platform installer (JAR + Windows EXE)"
( cd "$ROOT/fusion-sqldev-installer" && mvn -q package )

echo "[4/5] Fetching Windows JRE for bundled-runtime installer"
"$ROOT/scripts/fetch-windows-jre.sh" "$WIN_JRE"

echo "[5/5] Assembling dist/"
mkdir -p "$DIST"
cp "$ROOT/fusion-query-jdbc/target/fusion-query-jdbc-1.0.0.jar"             "$DIST/"
cp "$ROOT/fusion-sqldev-installer/target/fusion-sqldev-installer-1.0.0.jar" "$DIST/"
cp "$ROOT/fusion-sqldev-installer/target/fusion-sqldev-installer-1.0.0.exe" "$DIST/"

# Windows bundle (EXE + JRE folder) — single ZIP, user extracts and double-clicks the EXE.
WIN_BUNDLE_DIR="$ROOT/.build/fusion-sqldev-installer-1.0.0-windows"
rm -rf "$WIN_BUNDLE_DIR"
mkdir -p "$WIN_BUNDLE_DIR"
cp "$ROOT/fusion-sqldev-installer/target/fusion-sqldev-installer-1.0.0.exe" "$WIN_BUNDLE_DIR/"
cp -R "$WIN_JRE" "$WIN_BUNDLE_DIR/jre"
( cd "$ROOT/.build" && rm -f "$DIST/fusion-sqldev-installer-1.0.0-windows-bundle.zip" \
  && zip -qr "$DIST/fusion-sqldev-installer-1.0.0-windows-bundle.zip" \
       "fusion-sqldev-installer-1.0.0-windows" )

# README.md is maintained in dist/ directly

# Main release ZIP
( cd "$DIST" && rm -f fusion-query-jdbc-1.0.0.zip \
  && zip -q fusion-query-jdbc-1.0.0.zip \
    fusion-query-jdbc-1.0.0.jar \
    fusion-sqldev-installer-1.0.0.jar \
    fusion-sqldev-installer-1.0.0.exe \
    README.md )

echo
echo "Done. Distribution package:"
ls -lh "$DIST"
