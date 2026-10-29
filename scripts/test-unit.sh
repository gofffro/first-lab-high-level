#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

bash "$root_dir/scripts/build-tests.sh"
test_classpath="$(native_path "$root_dir/out/tests.jar")$classpath_separator$(native_path "$root_dir/lib/kotlinx-cli-jvm-0.3.6.jar")$classpath_separator$(native_path "$root_dir/lib/h2-2.3.232.jar")"
exec java -Dproject.root="$(native_path "$root_dir")" -jar "$root_dir/lib/junit-platform-console-standalone-1.12.2.jar" execute \
    --class-path "$test_classpath" --scan-class-path --include-engine junit-jupiter \
    --fail-if-no-tests --reports-dir "$(native_path "$root_dir/out/test-results")" \
    --disable-ansi-colors --details summary "$@"
