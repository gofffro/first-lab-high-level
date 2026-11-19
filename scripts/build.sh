#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

maven -Dmaven.test.skip=true package
cp "$root_dir/target/app.jar" "$root_dir/app.jar"
