#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

revision="$(git -C "$root_dir" rev-parse HEAD)"
docker build --build-arg "VCS_REF=$revision" \
    --tag "${1:-first-lab-high-level:local}" "$(native_path "$root_dir")"
