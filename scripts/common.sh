#!/usr/bin/env bash
# Shared paths for scripts invoked from any working directory.
root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
classpath_separator=':'
case "$(uname -s)" in
    MINGW*|MSYS*|CYGWIN*) classpath_separator=';' ;;
esac

native_path() {
    if [ "$classpath_separator" = ';' ]; then
        cygpath -m "$1"
    else
        printf '%s' "$1"
    fi
}
