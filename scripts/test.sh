#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

bash "$root_dir/scripts/test-unit.sh"
bash "$root_dir/scripts/test-cli.sh"
