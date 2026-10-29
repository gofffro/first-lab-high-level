#!/usr/bin/env bash
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/common.sh"
bash "$root_dir/scripts/build.sh" >/dev/null
mkdir -p "$root_dir/out"
test_dir="$(mktemp -d "$root_dir/out/cli-db.XXXXXX")"
export APP_DB_PATH="$(native_path "$test_dir/access")"
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
    if [ "$expected" -ne 1 ] && [ "$expected" -ne 127 ]; then
        if [ -s "$test_dir/stdout" ] || [ -s "$test_dir/stderr" ]; then
            quiet=false
        fi
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

# kotlinx-cli terminates the JVM for syntax errors; test these only in child processes.
run_case "unknown option (library exit)" 127 --unknown value
run_case "option without value (library exit)" 127 --login
run_case "unexpected positional argument (library exit)" 127 unexpected

# A second initialization preserves the data and produces no duplicate rows.
bash "$root_dir/scripts/init-db.sh"
run_case "success after repeated initialization" 0 --login bob --password secret --action read --resource A.A8B.C.f_d --volume 10

initialized_db="$APP_DB_PATH"
export APP_DB_PATH="$(native_path "$test_dir/missing")"
run_case "missing database" 9 --login alice --password qwerty --action read --resource A --volume 1
run_case "help without database" 1 --help

# Break only a disposable test database. The user's data/access DB is never touched.
export APP_DB_PATH="$initialized_db"
java -cp "$(native_path "$root_dir/lib/h2-2.3.232.jar")" org.h2.tools.Shell \
    -url "jdbc:h2:file:$APP_DB_PATH;IFEXISTS=TRUE" -user sa -password '' \
    -sql 'ALTER TABLE resources RENAME COLUMN maximum_volume TO broken_volume' >"$test_dir/setup.log"
run_case "SQL query error" 10 --login alice --password qwerty --action read --resource A --volume 1

echo "$passed/$total"
[ "$passed" -eq "$total" ]
