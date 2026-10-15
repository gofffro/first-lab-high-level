#!/usr/bin/env bash
set -euo pipefail

root_dir="$(cd "$(dirname "$0")" && pwd)"
"$root_dir/build.sh"
exec java -jar "$root_dir/app.jar" "$@"
