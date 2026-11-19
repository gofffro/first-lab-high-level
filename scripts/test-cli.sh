#!/usr/bin/env bash
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/common.sh"
if [ "${1:-}" = '--use-built-jar' ]; then
    test -f "$root_dir/app.jar"
else
    bash "$root_dir/scripts/build.sh" >/dev/null
fi
mkdir -p "$root_dir/out"
test_dir="$(mktemp -d "$root_dir/out/cli-db.XXXXXX")"
export APP_DB_PATH="$(native_path "$test_dir/access")"
export APP_LOG_FILE="$(native_path "$test_dir/access.log")"
export APP_JAR="$root_dir/app.jar"
app_jar="$APP_JAR"
bash "$root_dir/scripts/init-db.sh"

passed=0
total=0

run_case() {
    local name="$1"
    local expected="$2"
    shift 2
    total=$((total + 1))

    set +e
    java -jar "$root_dir/app.jar" "$@" >"$test_dir/stdout" 2>"$test_dir/stderr"
    local actual=$?
    set -e

    local quiet=true
    if [ -s "$test_dir/stderr" ]; then
        quiet=false
    elif [ "$expected" -eq 1 ]; then
        grep -q '^Usage: java -jar app.jar' "$test_dir/stdout" || quiet=false
    elif [ -s "$test_dir/stdout" ]; then
        quiet=false
    fi
    if [ "$actual" -eq "$expected" ] && [ "$quiet" = true ]; then
        echo "OK: $name"
        passed=$((passed + 1))
    else
        echo "FAIL: $name (expected $expected, got $actual, quiet=$quiet)"
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

run_case "unknown option" 127 --unknown value
run_case "option without value" 127 --login
run_case "unexpected positional argument" 127 unexpected

# A second initialization preserves the data and produces no duplicate rows.
bash "$root_dir/scripts/init-db.sh"
run_case "success after repeated initialization" 0 --login bob --password secret --action read --resource A.A8B.C.f_d --volume 10

initialized_db="$APP_DB_PATH"
export APP_DB_PATH="$(native_path "$test_dir/missing")"
run_case "missing database" 9 --login alice --password qwerty --action read --resource A --volume 1
run_case "help without database" 1 --help

# Break only a disposable test database. The user's data/access DB is never touched.
export APP_DB_PATH="$initialized_db"
run_h2 Shell \
    -url "jdbc:h2:file:$APP_DB_PATH;IFEXISTS=TRUE" -user sa -password '' \
    -sql 'ALTER TABLE resources RENAME COLUMN maximum_volume TO broken_volume' >"$test_dir/setup.log"
run_case "SQL query error" 10 --login alice --password qwerty --action read --resource A --volume 1

grep -q 'CLI completed with exit code 0' "$test_dir/access.log"
grep -q 'Database query failed' "$test_dir/access.log"
echo 'OK: application logs are written to file'
echo "$passed/$total"
[ "$passed" -eq "$total" ]
