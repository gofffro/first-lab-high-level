#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

image="${1:?Pass the image tag}"
log="$(mktemp)"
trap 'rm -f "$log"' EXIT
export MSYS_NO_PATHCONV=1

check_case() {
    local name="$1" expected="$2" actual=0
    shift 2
    docker run --rm "$image" "$@" >"$log" 2>&1 || actual=$?
    if [ "$actual" -ne "$expected" ]; then
        printf 'FAIL %s: expected %s, got %s\n' "$name" "$expected" "$actual" >&2
        cat "$log" >&2
        return 1
    fi
    if [ "$expected" -eq 1 ]; then
        grep -q '^Usage: java -jar app.jar' "$log"
    else
        test ! -s "$log"
    fi
    printf 'OK %s: exit %s\n' "$name" "$actual"
}

check_case success 0 --login alice --password qwerty --action read --resource A.B.C --volume 10
check_case denial 5 --login bob --password secret --action execute --resource A.A8B.C --volume 10
check_case help 1 --help

expected="$(sha256sum "$root_dir/app.jar" | cut -d ' ' -f 1)"
actual="$(docker run --rm --entrypoint sha256sum "$image" /app/app.jar | cut -d ' ' -f 1)"
test "$expected" = "$actual"
echo 'OK: image and release JAR SHA-256 match'
