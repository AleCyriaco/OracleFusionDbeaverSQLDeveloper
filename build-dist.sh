#!/bin/bash
# Builds all three artifacts and assembles the distribution package.
# Output: dist/fusion-query-jdbc-1.0.0.zip containing both install paths + README.

set -e
cd "$(dirname "$0")"
ROOT="$(pwd)"
DIST="$ROOT/dist"

echo "[1/4] Building JDBC driver"
( cd "$ROOT/fusion-query-jdbc" && mvn -q install )

echo "[2/4] Building SQL Developer extension"
( cd "$ROOT/fusion-sqldev-extension" && mvn -q package )

echo "[3/4] Building cross-platform installer"
( cd "$ROOT/fusion-sqldev-installer" && mvn -q package )

echo "[4/4] Assembling dist/"
mkdir -p "$DIST"
cp "$ROOT/fusion-query-jdbc/target/fusion-query-jdbc-1.0.0.jar"             "$DIST/"
cp "$ROOT/fusion-sqldev-installer/target/fusion-sqldev-installer-1.0.0.jar" "$DIST/"
# README.md is maintained in dist/ directly

# ZIP for release
( cd "$DIST" && rm -f fusion-query-jdbc-1.0.0.zip \
  && zip -q fusion-query-jdbc-1.0.0.zip \
    fusion-query-jdbc-1.0.0.jar \
    fusion-sqldev-installer-1.0.0.jar \
    README.md )

echo
echo "Done. Distribution package:"
ls -lh "$DIST"
