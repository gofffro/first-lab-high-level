#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

app_jar="${APP_JAR:-$root_dir/target/app.jar}"
if [ ! -f "$app_jar" ]; then
    echo 'Build the application first: bash scripts/build.sh' >&2
    exit 1
fi
db_path="$(absolute_native_path "${APP_DB_PATH:-$root_dir/data/access}")"
# The path denotes a file name without .mv.db. H2 creates the directory as needed.
db_url="jdbc:h2:file:$db_path;TRACE_LEVEL_FILE=0"
for sql_file in init.sql fill.sql; do
    run_h2 RunScript \
        -url "$db_url" -user sa -password '' \
        -script "$(native_path "$root_dir/scripts/$sql_file")"
done
