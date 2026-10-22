#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

for command_name in kotlinc java; do
    if ! command -v "$command_name" >/dev/null 2>&1; then
        echo "$command_name was not found in PATH" >&2
        exit 1
    fi
done
junit_jar="$root_dir/lib/junit-platform-console-standalone-1.12.2.jar"
cli_jar="$root_dir/lib/kotlinx-cli-jvm-0.3.6.jar"
if [ ! -f "$junit_jar" ] || [ ! -f "$cli_jar" ]; then
    echo 'Missing libraries in lib/. See docs/README.md and scripts/setup-libs.sh.' >&2
    exit 1
fi
mapfile -d '' kotlin_sources < <(find "$root_dir/src" "$root_dir/tests" -type f -name '*.kt' -print0)
test_classpath="$(native_path "$junit_jar")$classpath_separator$(native_path "$cli_jar")"
mkdir -p "$root_dir/out"
kotlinc "${kotlin_sources[@]}" -classpath "$test_classpath" -include-runtime -d "$root_dir/out/tests.jar"
