#!/usr/bin/env bash
set -euo pipefail

version=2.4.20
checksum=59e9ca74c7904ef2c122b12114937673ccce68de820a663f0ed66ccf8799e0b7
destination="${RUNNER_TOOL_CACHE:?}/kotlin-$version"
if [ ! -x "$destination/kotlinc/bin/kotlinc" ]; then
    mkdir -p "$destination"
    archive="$destination/compiler.zip"
    curl --fail --silent --show-error --location --retry 3 \
        "https://github.com/JetBrains/kotlin/releases/download/v$version/kotlin-compiler-$version.zip" \
        -o "$archive"
    echo "$checksum  $archive" | sha256sum -c -
    unzip -qo "$archive" -d "$destination"
    rm -f "$archive"
fi
printf '%s\n' "$destination/kotlinc/bin" >> "${GITHUB_PATH:?}"
