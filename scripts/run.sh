#!/usr/bin/env bash
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/common.sh"
export APP_DB_PATH="${APP_DB_PATH:-$(native_path "$root_dir/data/access")}"
bash "$root_dir/scripts/build.sh"
exec java -jar "$root_dir/app.jar" "$@"
