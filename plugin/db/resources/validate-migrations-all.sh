#!/usr/bin/env bash
# Validate Flyway migrations for every db plugin / variant combination.
# Stops on the first failure. Run from any directory.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VALIDATE="$SCRIPT_DIR/validate-migrations.sh"

# flavour:variant pairs that have migrations on disk.
COMBINATIONS=(
  "postgres:default" "postgres:strict"
  "mariadb:default"
  "mssql:default"
  "oracle:default"  "oracle:strict"
  "hsqldb:default"  "hsqldb:strict"
  "h2:default"
  # db2 disabled by default — image is slow under emulation and requires
  # the shaded plugin jar to be built first.
  # "db2:default" "db2:strict"
)

FAILED=()
for combo in "${COMBINATIONS[@]}"; do
  flavour="${combo%%:*}"
  variant="${combo##*:}"
  echo "===================================================================="
  echo "Validating $flavour / $variant"
  echo "===================================================================="
  if ! "$VALIDATE" "$flavour" "$variant"; then
    FAILED+=("$combo")
  fi
done

echo
echo "===================================================================="
if [[ ${#FAILED[@]} -eq 0 ]]; then
  echo "All combinations passed."
  exit 0
else
  echo "FAILED combinations: ${FAILED[*]}"
  exit 1
fi
