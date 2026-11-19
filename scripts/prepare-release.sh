#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

release="$root_dir/out/release"
mkdir -p "$release"
cp "$root_dir/app.jar" "$root_dir/scripts/init.sql" "$root_dir/scripts/fill.sql" "$release/"
git -C "$root_dir" rev-parse HEAD > "$release/commit.txt"

# Use a directory without lib/ to prove that the release JAR is self-contained.
cd "$release"
export APP_DB_PATH="$(absolute_native_path "$release/demo")"
for script in init.sql fill.sql; do
    java -cp app.jar org.h2.tools.RunScript -url "jdbc:h2:file:$APP_DB_PATH" -user sa -script "$script"
done
java -jar app.jar --login alice --password qwerty --action read --resource A.B.C --volume 10
sha256sum app.jar init.sql fill.sql commit.txt > SHA256SUMS
