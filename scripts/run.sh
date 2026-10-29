#!/usr/bin/env bash
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/common.sh"
bash "$root_dir/scripts/build.sh"
exec java -jar "$root_dir/app.jar" "$@"
