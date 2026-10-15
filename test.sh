#!/usr/bin/env bash
set -euo pipefail

root_dir="$(cd "$(dirname "$0")" && pwd)"
"$root_dir/build.sh" >/dev/null

passed=0
total=0

run_case() {
    local name="$1"
    local expected="$2"
    shift 2
    total=$((total + 1))

    set +e
    java -jar "$root_dir/app.jar" "$@" >/dev/null 2>&1
    local actual=$?
    set -e

    if [ "$actual" -eq "$expected" ]; then
        echo "OK: $name"
        passed=$((passed + 1))
    else
        echo "FAIL: $name (expected $expected, got $actual)"
    fi
}

run_case "success" 0 --login alice --password qwerty --action read --resource A.B.C --volume 10
run_case "help" 1 --help
run_case "invalid password" 2 --login alice --password wrong --action read --resource A.B.C --volume 10
run_case "invalid login" 3 --login charlie --password qwerty --action read --resource A.B.C --volume 10
run_case "unknown action" 4 --login alice --password qwerty --action remove --resource A.B.C --volume 10
run_case "access denied" 5 --login bob --password secret --action execute --resource A.A8B.C --volume 10
run_case "resource not found" 6 --login alice --password qwerty --action read --resource A.B.D --volume 10
run_case "invalid resource" 7 --login alice --password qwerty --action read --resource A..B --volume 10
run_case "invalid volume" 7 --login alice --password qwerty --action read --resource A.B.C --volume none
run_case "volume limit exceeded" 8 --login alice --password qwerty --action read --resource A.B.C --volume 21

echo "$passed/$total"
[ "$passed" -eq "$total" ]
