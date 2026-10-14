#!/usr/bin/env bash
#
# release-admin-companion.sh — create the companion ebms-admin GitHub release
# for an ebms-core release, tolerantly.
#
# The ebms-admin console (eluinstra/ebms-admin) is the legacy predecessor of the
# ebms-server module; its GitHub release is a downstream companion of the
# PRIMARY ebms-core release and must NEVER block or fail it. Every failure
# path in this script therefore ends in "warn + exit 0": the primary release
# is complete by the time this runs, and a broken or missing companion is
# reported as a workflow warning instead of a red step.
#
# What it does (mirrors the parent release.yml admin steps):
#   clone the admin release branch -> bump its pom to <version> ->
#   mvn clean verify (tests) -> collect the admin jar + plugin jars ->
#   commit + tag (ebms-admin-<version>) + push -> create the GitHub release
#   (prerelease flag, changelog, pointer to the primary release) + upload the
#   assets -> bump back to <snapshot> + push (next development iteration).
#
# Prerequisites:
#   * the ebms-core reactor artifacts for <version> must be resolvable by
#     Maven — the release workflows run `mvn clean install` (local repo) or
#     the version is on Maven Central — before this script builds the admin
#   * scripts/release-version.sh next to this script (version rules)
#   * the token in the env var named by --token-env (default
#     EBMS_ADMIN_TOKEN) needs Contents read+write on the admin repo
#
# Usage:
#   scripts/release-admin-companion.sh --version <v> --branch <branch> \
#       [--token-env EBMS_ADMIN_TOKEN] [--admin-repo eluinstra/ebms-admin] \
#       [--admin-jar-glob 'target/ebms-admin-<version>.jar'] \
#       [--plugins-glob 'plugin/db/*/target/ebms-*-<version>.jar' ...] \
#       [--work-dir <dir>]
#
#   <version> is validated and its SNAPSHOT derived via release-version.sh, so
#   the companion can never drift from the primary release's version rules.
#   <version> and the derived <snapshot> are pushed to <branch> (e.g.
#   dev-2.21.x).
#   Repeat --plugins-glob for each asset pattern, relative to the current
#   directory (e.g. plugin/db/*/target/ebms-*-<version>.jar); <version> in a
#   pattern is substituted. No --plugins-glob: the admin jar alone is
#   released. sources/original variants of a jar are never collected.

# No `set -e` on purpose: each step checks its own result and exits 0 with a
# warning, so no failure can escalate into failing the primary release.
set -u

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
VERSION=""
BRANCH=""
TOKEN_ENV="EBMS_ADMIN_TOKEN"
ADMIN_REPO="eluinstra/ebms-admin"
ADMIN_URL_OVERRIDE=""
ADMIN_JAR_GLOB="target/ebms-admin-<version>.jar"
PLUGIN_GLOBS=()
WORK_DIR=""

while [ $# -gt 0 ]; do
  case "$1" in
    --version) VERSION=${2:-}; shift 2 ;;
    --branch) BRANCH=${2:-}; shift 2 ;;
    --token-env) TOKEN_ENV=${2:-}; shift 2 ;;
    --admin-repo) ADMIN_REPO=${2:-}; shift 2 ;;
    --admin-url) ADMIN_URL_OVERRIDE=${2:-}; shift 2 ;;
    --admin-jar-glob) ADMIN_JAR_GLOB=${2:-}; shift 2 ;;
    --plugins-glob) PLUGIN_GLOBS+=("${2:-}"); shift 2 ;;
    --work-dir) WORK_DIR=${2:-}; shift 2 ;;
    *)
      echo "release-admin-companion: unknown argument: $1" >&2
      exit 2 ;;
  esac
done

[ -n "$VERSION" ] && [ -n "$BRANCH" ] || {
  echo "release-admin-companion: --version and --branch are required" >&2
  exit 2
}

skip() { echo "::warning::companion ebms-admin release skipped: $*"; exit 0; }

TOKEN=${!TOKEN_ENV:-}
[ -n "$TOKEN" ] || skip "\$$TOKEN_ENV is not set (no token with write access to ${ADMIN_REPO})"

# validate the version + derive the next-iteration SNAPSHOT with the shared
# rules, so the companion cannot drift from the primary release
SNAPSHOT=$("$SCRIPT_DIR/release-version.sh" parse "$VERSION" 2>/dev/null | sed -n 's/^snapshot=//p')
if [ -z "${SNAPSHOT:-}" ]; then
  skip "invalid version $VERSION (see scripts/release-version.sh)"
fi

ADMIN_URL=${ADMIN_URL_OVERRIDE:-https://x-access-token:${TOKEN}@github.com/${ADMIN_REPO}.git}
TAG="ebms-admin-${VERSION}"

# idempotency: a re-run after a successful release is a no-op (a crashed run
# that pushed the tag but not the release needs the tag deleted first, then
# this script recreates both)
if curl -sf -H "Authorization: Bearer ${TOKEN}" \
    "https://api.github.com/repos/${ADMIN_REPO}/releases/tags/${TAG}" | jq -e '.id' >/dev/null 2>&1; then
  echo "${ADMIN_REPO}: companion release ${TAG} already exists; nothing to do"
  exit 0
fi

# the companion releases from its own release branch; a missing branch (e.g.
# the admin repo has no 2.22.x line) is not an error for the primary release
if ! git ls-remote --exit-code --heads "$ADMIN_URL" "$BRANCH" >/dev/null 2>&1; then
  skip "branch $BRANCH does not exist on ${ADMIN_REPO}"
fi

WORK_DIR=${WORK_DIR:-$(mktemp -d "${TMPDIR:-/tmp}/ebms-admin-companion.XXXXXX")}
rm -rf "$WORK_DIR"
if ! git clone --quiet --branch "$BRANCH" "$ADMIN_URL" "$WORK_DIR"; then
  skip "could not clone ${ADMIN_REPO} ($BRANCH)"
fi
if ! git -C "$WORK_DIR" fetch --quiet --tags origin "$BRANCH"; then
  skip "could not fetch tags of ${ADMIN_REPO} ($BRANCH)"
fi

# bump the admin pom (project version is the line after
# <artifactId>ebms-admin</artifactId>)
sed -i "/<artifactId>ebms-admin<\/artifactId>/{n;s/<version>.*<\/version>/<version>${VERSION}<\/version>/}" "$WORK_DIR/pom.xml" \
  && grep -q "<version>${VERSION}</version>" "$WORK_DIR/pom.xml" \
  || skip "admin pom has no <version>${VERSION}</version> after the bump; pom layout changed?"

# build + test the admin project; a failing legacy build must not block the
# primary release (the parent pipeline gated this behind a warning too)
if ! mvn -B -T 1C -f "$WORK_DIR/pom.xml" clean verify -Ddependency-check.skip=true; then
  skip "ebms-admin build/tests failed for $VERSION"
fi

# collect assets: the admin jar + the plugin jars (legacy companion set)
DIST="$WORK_DIR/dist"
mkdir -p "$DIST"
admin_jar_rel=${ADMIN_JAR_GLOB//<version>/$VERSION}
if ! cp "$WORK_DIR/$admin_jar_rel" "$DIST/" 2>/dev/null; then
  skip "admin jar $admin_jar_rel not found after the build"
fi
for glob in ${PLUGIN_GLOBS[@]+"${PLUGIN_GLOBS[@]}"}; do
  glob=${glob//<version>/$VERSION}
  found=0
  # unquoted on purpose: the glob must be expanded by the shell
  for j in $glob; do
    [ -f "$j" ] || continue
    case $(basename "$j") in
      *sources*|*original*) continue ;;
    esac
    cp "$j" "$DIST/" && found=1
  done
  [ "$found" = 1 ] || echo "release-admin-companion: note: no jars matched $glob" >&2
done
asset_count=$(ls "$DIST" | wc -l)

# commit + tag + push the release commit BEFORE creating the release: posting
# a release for a tag that does not exist yet would make GitHub create the
# tag at the default branch, i.e. the wrong commit
git -C "$WORK_DIR" -c user.name="ebms-release" \
  -c user.email="ebms-release@users.noreply.github.com" \
  commit -aqm "release $VERSION" || true
sha=$(git -C "$WORK_DIR" rev-parse HEAD)
if ! git -C "$WORK_DIR" tag -f "$TAG" "$sha" \
  || ! git -C "$WORK_DIR" push --quiet "$ADMIN_URL" "$sha:refs/heads/$BRANCH" \
  || ! git -C "$WORK_DIR" push --quiet "$ADMIN_URL" "refs/tags/$TAG"; then
  skip "could not push release commit/tag $TAG to ${ADMIN_REPO}"
fi

# changelog body: commit subjects since the previous admin release tag on this
# branch (shared rules; the tag just created at HEAD must not be its own
# predecessor). The companion is a legacy distribution, so point at the
# primary ebms-core release first.
prev_tag=$("$SCRIPT_DIR/release-version.sh" prev-tag --dir "$WORK_DIR" \
  --prefix ebms-admin --exclude "$TAG")
body=""
if [ -n "$prev_tag" ]; then
  body=$(git -C "$WORK_DIR" log --no-merges --pretty='- %s' "${prev_tag}..HEAD" | grep -v '^- release ' || true)
fi
[ -n "$body" ] || body="- (no changelog available)"
body=$(printf 'Primary release: https://github.com/eluinstra/ebms-core/releases/tag/ebms-core-%s\n\n%s' "$VERSION" "$body")

# prerelease flag from the shared version rules
prerelease=$("$SCRIPT_DIR/release-version.sh" parse "$VERSION" | sed -n 's/^prerelease=//p')
{ [ "$prerelease" = true ] || [ "$prerelease" = false ]; } || prerelease=false

payload=$(jq -n --arg tag "$TAG" --arg body "$body" --arg target "$BRANCH" --argjson pre "$prerelease" \
  '{tag_name: $tag, target_commitish: $target, body: $body, draft: false, prerelease: $pre}')
release_id=$(curl -sf -X POST \
    -H "Authorization: Bearer ${TOKEN}" \
    -H "Accept: application/vnd.github+json" \
    -d "$payload" \
    "https://api.github.com/repos/${ADMIN_REPO}/releases" | jq .id) \
  || release_id=$(curl -sf \
    -H "Authorization: Bearer ${TOKEN}" \
    "https://api.github.com/repos/${ADMIN_REPO}/releases/tags/${TAG}" | jq .id)
if [ -z "$release_id" ] || [ "$release_id" = "null" ]; then
  skip "could not create or fetch release $TAG on ${ADMIN_REPO}"
fi
for f in "$DIST"/*.jar; do
  if ! curl -fs --retry 3 --retry-delay 5 --retry-all-errors -X POST \
    -H "Authorization: Bearer ${TOKEN}" \
    -H "Accept: application/vnd.github+json" \
    -H "Content-Type: application/java-archive" \
    --data-binary @"$f" \
    "https://uploads.github.com/repos/${ADMIN_REPO}/releases/${release_id}/assets?name=$(basename "$f")" \
    > /dev/null; then
    skip "asset upload failed after retries: $(basename "$f") (release $TAG is partial)"
  fi
done
echo "${ADMIN_REPO}: created companion release ${TAG} (id ${release_id}, prerelease ${prerelease}) with ${asset_count} assets"

# next development iteration: back to SNAPSHOT (stable: next patch,
# pre-release: the base version stays in development), like the primary repo
sed -i "/<artifactId>ebms-admin<\/artifactId>/{n;s/<version>.*<\/version>/<version>${SNAPSHOT}<\/version>/}" "$WORK_DIR/pom.xml"
git -C "$WORK_DIR" -c user.name="ebms-release" \
  -c user.email="ebms-release@users.noreply.github.com" \
  commit -aqm "prepare for next development iteration"
if ! git -C "$WORK_DIR" push --quiet "$ADMIN_URL" "HEAD:$BRANCH"; then
  # the release itself is out; only the snapshot bump failed
  echo "::warning::companion release ${TAG} created, but the next development iteration bump ($SNAPSHOT) could not be pushed to ${ADMIN_REPO}"
  exit 0
fi
echo "${ADMIN_REPO}: prepared next development iteration $SNAPSHOT on $BRANCH"
exit 0
