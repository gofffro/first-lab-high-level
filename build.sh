#!/usr/bin/env bash
set -euo pipefail

root_dir="$(cd "$(dirname "$0")" && pwd)"
cli_jar="$root_dir/lib/kotlinx-cli-jvm-0.3.6.jar"
app_jar="$root_dir/app.jar"

if ! command -v kotlinc >/dev/null 2>&1; then
    echo "kotlinc was not found in PATH" >&2
    exit 1
fi

if [ ! -f "$cli_jar" ]; then
    mkdir -p "$root_dir/lib"
    curl -fL "https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-cli-jvm/0.3.6/kotlinx-cli-jvm-0.3.6.jar" -o "$cli_jar"
fi

kotlinc "$root_dir/src/Main.kt" -classpath "$cli_jar" -include-runtime -d "$app_jar"

temp_dir="$(mktemp -d)"
trap 'rm -rf "$temp_dir"' EXIT
unzip -q "$cli_jar" -d "$temp_dir"
rm -rf "$temp_dir/META-INF"
jar uf "$app_jar" -C "$temp_dir" .
