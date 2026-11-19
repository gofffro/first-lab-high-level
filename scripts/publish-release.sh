#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

commit="${GITHUB_SHA:?}"
repo="${GITHUB_REPOSITORY:?}"
namespace="${DOCKERHUB_USERNAME:?}"
image="$namespace/first-lab-high-level:sha-$commit"
tag="build-$commit"
cd "$root_dir/out/release"
test "$(cat commit.txt)" = "$commit"
sha256sum -c SHA256SUMS

if release_draft="$(gh release view "$tag" --repo "$repo" --json isDraft --jq .isDraft 2>/dev/null)"; then
    if [ "$release_draft" = 'false' ]; then
        echo "Release $tag is already public."
        exit 0
    fi
else
    release_draft=''
fi

docker tag first-lab-high-level:checked "$image"
docker push "$image"
if [ -z "$release_draft" ]; then
    notes="$(printf 'Commit: `%s`\n\nDocker image: `%s`\n\nhttps://hub.docker.com/r/%s/first-lab-high-level/tags?name=sha-%s\n\nThe fat JAR includes Kotlin, H2 and kotlinx-cli. See the README for database initialization.\n' \
        "$commit" "$image" "$namespace" "$commit")"
    gh release create "$tag" --repo "$repo" --target "$commit" --draft \
        --title "$tag" --notes "$notes"
fi
gh release upload "$tag" app.jar init.sql fill.sql commit.txt SHA256SUMS --repo "$repo" --clobber
gh release edit "$tag" --repo "$repo" --draft=false
