#!/usr/bin/env bash
#
# release-version.sh — parse and validate an EbMS release version.
#
# Single source of truth for the version logic used by the release pipeline
# (.github/workflows/release.yml and .github/workflows/docker-deploy.yml), so
# the two workflows cannot drift apart on what a version means.
#
# Accepted versions
# -----------------
#   stable        MAJOR.MINOR.PATCH                     2.20.11
#   pre-release   MAJOR.MINOR.PATCH-<qualifier>         2.21.0-M1
#                                               2.21.0-RC1, 2.21.0-beta.1
#
# A pre-release is a milestone/RC/alpha/beta *of* MAJOR.MINOR.PATCH, so its
# base version (the version it will eventually become) drives everything the
# pipeline derives: the development branch, the next-iteration <revision>, and
# which Docker tags may be moved. `-SNAPSHOT` versions are never releasable.
#
# Usage
# -----
#   scripts/release-version.sh parse <version> [--branch <branch>] [--prefix <p>]
#       Prints key=value lines (for GITHUB_OUTPUT). Fails on an invalid version.
#       --branch  overrides the branch derived from the version
#       --prefix  release tag prefix (default: ebms-core)
#
#   scripts/release-version.sh prev-tag --dir <repo> [--prefix <p>]
#                                  [--exclude <tag>]
#       Prints the release tag that the changelog should start from: the newest
#       tag <p>-* that is an ancestor of HEAD and is not superseded by another
#       candidate. Empty output + exit 0 when there is no previous release.
#       --exclude drops a tag from the candidates (the release workflow calls
#       this after creating its own tag, which then sits at HEAD).
#
#   scripts/release-version.sh --self-test
#       Run the built-in checks.
#
# Keys printed by `parse`
# -----------------------
#   version         the input version, e.g. 2.21.0-M1
#   base_version    MAJOR.MINOR.PATCH it is based on, e.g. 2.21.0
#   major_minor     MAJOR.MINOR, e.g. 2.21  (Docker minor tag)
#   prerelease      true for a pre-release, false for a stable release
#   snapshot        <revision> for the next development iteration
#   branch          development branch to release from, e.g. dev-2.21.x
#   tag             release tag, e.g. ebms-core-2.21.0-M1

set -u

usage() {
  # print the leading comment block of this file (keeps itself in sync)
  awk 'NR==1 {next} /^#/ {sub(/^# ?/, ""); print; next} {exit}' "${BASH_SOURCE[0]}"
}

# Maven version grammar: <numeric core>[-<qualifier>][.<numeric>...]. We accept
# a dotted alphanumeric qualifier and reject anything else (no '..', no trailing
# '-', no whitespace, no slashes that would break a git tag name).
VERSION_RE='^[0-9]+\.[0-9]+\.[0-9]+(-[0-9A-Za-z]+([._-][0-9A-Za-z]+)*)?$'
SNAPSHOT_RE='-SNAPSHOT$'
BRANCH_RE='^[A-Za-z0-9][A-Za-z0-9._/-]*$'

# parse <version> — echoes the validated version, exits 1 with a message on a
# version we refuse to release.
validate_version() {
  local version=$1
  if [ -z "$version" ]; then
    echo "::error::version input is empty" >&2
    exit 1
  fi
  if [[ "$version" =~ $SNAPSHOT_RE ]]; then
    echo "::error::version $version is a SNAPSHOT; pre-releases are plain versions with a qualifier, e.g. 2.21.0-M1" >&2
    exit 1
  fi
  if ! [[ "$version" =~ $VERSION_RE ]]; then
    echo "::error::version must be MAJOR.MINOR.PATCH or MAJOR.MINOR.PATCH-QUALIFIER (e.g. 2.20.9 or 2.21.0-M1), got: $version" >&2
    exit 1
  fi
}

# parse <version> [--branch <b>] [--prefix <p>] — print key=value lines.
cmd_parse() {
  local version=${1:-}
  shift || true
  local branch='' prefix='ebms-core'
  while [ $# -gt 0 ]; do
    case "$1" in
      --branch) branch=${2:-}; shift 2 ;;
      --prefix) prefix=${2:-}; shift 2 ;;
      *) echo "unknown argument: $1" >&2; return 2 ;;
    esac
  done

  validate_version "$version"

  local base prerelease major_minor
  base=${version%%-*}
  if [ "$base" = "$version" ]; then
    prerelease=false
  else
    prerelease=true
  fi
  major_minor=${base%.*}

  # next development iteration: a pre-release returns to the -SNAPSHOT of the
  # version it is a pre-release of (the base is still in development); a stable
  # release moves on to the next patch.
  local snapshot
  if [ "$prerelease" = true ]; then
    snapshot="${base}-SNAPSHOT"
  else
    snapshot="${base%.*}.$(( ${base##*.} + 1 ))-SNAPSHOT"
  fi

  # the development branch follows the minor line of the version being released
  if [ -z "$branch" ]; then
    branch="dev-${major_minor}.x"
  elif ! [[ "$branch" =~ $BRANCH_RE ]]; then
    echo "::error::branch must be a simple branch name, got: $branch" >&2
    exit 1
  fi

  cat <<EOF
version=${version}
base_version=${base}
major_minor=${major_minor}
prerelease=${prerelease}
snapshot=${snapshot}
branch=${branch}
tag=${prefix}-${version}
EOF
}

# prev-tag --dir <repo> [--prefix <p>] — newest ancestor release tag.
#
# `sort -V | head -n -1 | tail -n 1` (the previous approach) picks the second
# highest version string, which is wrong around pre-releases: for the release
# after GA 2.21.0 it would pick 2.21.0-M2 (still the highest string) rather
# than 2.21.0. Selecting the maximal element of the tag DAG instead: a tag is a
# candidate when it is an ancestor of HEAD, and the answer is the candidate no
# other candidate descends from.
cmd_prev_tag() {
  local dir='' prefix='ebms-core' exclude=''
  while [ $# -gt 0 ]; do
    case "$1" in
      --dir) dir=${2:-}; shift 2 ;;
      --prefix) prefix=${2:-}; shift 2 ;;
      --exclude) exclude=${2:-}; shift 2 ;;
      *) echo "unknown argument: $1" >&2; return 2 ;;
    esac
  done
  [ -n "$dir" ] || { echo "prev-tag requires --dir" >&2; return 2; }
  git -C "$dir" rev-parse --git-dir >/dev/null 2>&1 || { echo "not a git repo: $dir" >&2; return 2; }

  local candidates=() tag kept=()
  while IFS= read -r tag; do
    [ -n "$tag" ] || continue
    [ "$tag" = "$exclude" ] && continue
    # only tags already contained in this branch: a tag from a side branch that
    # happens to sort higher must not become the changelog start
    git -C "$dir" merge-base --is-ancestor "$tag" HEAD 2>/dev/null || continue
    candidates+=("$tag")
  done < <(git -C "$dir" tag --list "${prefix}-*")

  [ ${#candidates[@]} -gt 0 ] || return 0

  for tag in "${candidates[@]}"; do
    local superseded=false other
    for other in "${candidates[@]}"; do
      [ "$other" = "$tag" ] && continue
      if git -C "$dir" merge-base --is-ancestor "$tag" "$other" 2>/dev/null; then
        superseded=true
        break
      fi
    done
    [ "$superseded" = false ] && kept+=("$tag")
  done

  [ ${#kept[@]} -gt 0 ] || return 0
  # a tie (e.g. a pre-release and its base on parallel branches) -> highest
  # version string wins
  printf '%s\n' "${kept[@]}" | sort -V | tail -n 1
}

# --- self test ---------------------------------------------------------------
failures=0
check() {
  local what=$1 expected=$2 actual=$3
  if [ "$expected" != "$actual" ]; then
    echo "FAIL ${what}: expected [${expected}] got [${actual}]" >&2
    failures=$((failures + 1))
  else
    echo "ok   ${what}"
  fi
}

field() { printf '%s\n' "$1" | grep "^$2=" | head -n 1 | cut -d= -f2-; }

self_test() {
  local out

  out=$(cmd_parse 2.20.9)
  check "stable prerelease" false "$(field "$out" prerelease)"
  check "stable snapshot" 2.20.10-SNAPSHOT "$(field "$out" snapshot)"
  check "stable base" 2.20.9 "$(field "$out" base_version)"
  check "stable major_minor" 2.20 "$(field "$out" major_minor)"
  check "stable branch" dev-2.20.x "$(field "$out" branch)"
  check "stable tag" ebms-core-2.20.9 "$(field "$out" tag)"

  out=$(cmd_parse 2.21.0-M1)
  check "milestone prerelease" true "$(field "$out" prerelease)"
  check "milestone snapshot" 2.21.0-SNAPSHOT "$(field "$out" snapshot)"
  check "milestone base" 2.21.0 "$(field "$out" base_version)"
  check "milestone branch" dev-2.21.x "$(field "$out" branch)"
  check "milestone tag" ebms-core-2.21.0-M1 "$(field "$out" tag)"

  out=$(cmd_parse 2.21.0-RC1)
  check "rc prerelease" true "$(field "$out" prerelease)"
  check "rc snapshot" 2.21.0-SNAPSHOT "$(field "$out" snapshot)"

  out=$(cmd_parse 2.21.0-beta.1)
  check "dotted qualifier prerelease" true "$(field "$out" prerelease)"
  check "dotted qualifier snapshot" 2.21.0-SNAPSHOT "$(field "$out" snapshot)"

  out=$(cmd_parse 2.21.0 --branch release/2.21)
  check "explicit branch override" release/2.21 "$(field "$out" branch)"

  # rejected inputs
  local bad
  # subshells: validate_version exits, which must not kill the self test
  for bad in 2.21 2.21.0-SNAPSHOT 2.21.x '' '2.21.0-' '2.21.0-M1..2' 'v2.21.0-M1' '2.21.0 M1' '2.21.0-M1/2'; do
    if ( cmd_parse "$bad" ) >/dev/null 2>&1; then
      echo "FAIL should reject [${bad}]" >&2
      failures=$((failures + 1))
    else
      echo "ok   rejects [${bad}]"
    fi
  done
  if ( cmd_parse 2.21.0 --branch 'bad branch' ) >/dev/null 2>&1; then
    echo "FAIL should reject a branch with a space" >&2
    failures=$((failures + 1))
  else
    echo "ok   rejects a branch with a space"
  fi

  # prev-tag: milestone -> GA -> patch, plus an unrelated side-branch tag
  local repo
  repo=$(mktemp -d)
  git -C "$repo" init -q .
  git -C "$repo" config user.name t
  git -C "$repo" config user.email t@example.com
  local commit_tag
  commit_tag() {
    echo "$1" >> "$repo/f"
    git -C "$repo" add f
    git -C "$repo" commit -qm "$1"
    git -C "$repo" tag "$1"
  }
  echo init > "$repo/f"
  git -C "$repo" add f
  git -C "$repo" commit -qm init
  commit_tag ebms-core-2.20.10
  commit_tag ebms-core-2.21.0-M1
  commit_tag ebms-core-2.21.0-M2
  commit_tag ebms-core-2.21.0
  commit_tag ebms-core-2.21.1
  # the workflow creates its own tag before asking for the changelog start, so
  # it excludes that tag (it sits at HEAD)
  check "prev-tag after patch release" ebms-core-2.21.0 \
    "$(cd "$repo" && cmd_prev_tag --dir . --exclude ebms-core-2.21.1)"
  check "prev-tag at a milestone release" ebms-core-2.21.0-M1 \
    "$(git -C "$repo" checkout -q ebms-core-2.21.0-M2 && cd "$repo" && cmd_prev_tag --dir . --exclude ebms-core-2.21.0-M2)"
  check "prev-tag at a GA release" ebms-core-2.21.0-M2 \
    "$(git -C "$repo" checkout -q ebms-core-2.21.0 && cd "$repo" && cmd_prev_tag --dir . --exclude ebms-core-2.21.0)"
  # side-branch tag that is not an ancestor of HEAD must be ignored
  git -C "$repo" checkout -q -b side ebms-core-2.20.10
  commit_tag ebms-core-2.99.0
  git -C "$repo" checkout -q ebms-core-2.21.1
  check "prev-tag ignores non-ancestor tag" ebms-core-2.21.0 \
    "$(cd "$repo" && cmd_prev_tag --dir . --exclude ebms-core-2.21.1)"
  # no previous release on the branch at all -> empty output, exit 0
  git -C "$repo" checkout -q --detach "$(git -C "$repo" rev-list --max-parents=0 HEAD)"
  check "prev-tag with no ancestor tags" "" "$(cd "$repo" && cmd_prev_tag --dir . )"
  rm -rf "$repo"

  if [ "$failures" -gt 0 ]; then
    echo "${failures} check(s) failed" >&2
    return 1
  fi
  echo "all checks passed"
}

main() {
  case "${1:-}" in
    parse) shift; cmd_parse "$@" ;;
    prev-tag) shift; cmd_prev_tag "$@" ;;
    --self-test|self-test) self_test ;;
    -h|--help|'') usage ;;
    *) echo "unknown command: $1" >&2; usage >&2; exit 2 ;;
  esac
}

main "$@"
