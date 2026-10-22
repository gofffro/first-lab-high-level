#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

# Explicit downloads only: no dependency resolver or build system.
base_url='https://repo.maven.apache.org/maven2'
artifacts=(
    'org/jetbrains/kotlinx/kotlinx-cli-jvm/0.3.6/kotlinx-cli-jvm-0.3.6.jar'
    'org/junit/platform/junit-platform-console-standalone/1.12.2/junit-platform-console-standalone-1.12.2.jar'
    'org/junit/jupiter/junit-jupiter-api/5.12.2/junit-jupiter-api-5.12.2.jar'
    'org/junit/jupiter/junit-jupiter-engine/5.12.2/junit-jupiter-engine-5.12.2.jar'
    'org/junit/jupiter/junit-jupiter-params/5.12.2/junit-jupiter-params-5.12.2.jar'
    'org/junit/platform/junit-platform-commons/1.12.2/junit-platform-commons-1.12.2.jar'
    'org/junit/platform/junit-platform-engine/1.12.2/junit-platform-engine-1.12.2.jar'
    'org/opentest4j/opentest4j/1.3.0/opentest4j-1.3.0.jar'
    'org/apiguardian/apiguardian-api/1.1.2/apiguardian-api-1.1.2.jar'
)
mkdir -p "$root_dir/lib"
for artifact in "${artifacts[@]}"; do
    destination="$root_dir/lib/${artifact##*/}"
    if [ ! -f "$destination" ]; then
        curl --fail --location --retry 2 "$base_url/$artifact" --output "$destination"
    fi
done
(cd "$root_dir/lib" && sha256sum --check SHA256SUMS)
