#!/usr/bin/env bash
# Validate Flyway migration scripts of the ebms db plugins against a real DB.
#
# Usage:
#   ./validate-migrations.sh <flavour> <variant>
#     flavour: db2 | h2 | hsqldb | mariadb | mssql | oracle | postgres
#     variant: default | strict
#
# Requirements:
# - docker (with permission to run containers)
# - the docker network 'flyway-validate' (auto-created if missing)
# - for db2: the shaded plugin jar must exist; build it with
#     mvn -pl ebms-core/plugin/db/db2 -am package
#   The script extracts the JCC driver from that shaded jar (the IBM driver is
#   not on a public Maven repo without an EULA, but it IS bundled inside the
#   plugin jar).
# - aarch64 hosts running db2 also need:
#     docker run --privileged --rm tonistiigi/binfmt --install all
#
# Notes per flavour:
# - mssql: uses mcr.microsoft.com/azure-sql-edge (works on amd64 and arm64).
#   The ARM64 image ships no in-container sqlcmd, so Flyway connects directly
#   to the always-present 'master' database for syntax validation.
# - db2  : icr.io/db2_community is amd64 only; on arm64 the image runs under
#   QEMU emulation and takes several minutes to become ready.
#   Database name is limited to 8 characters.
# - h2 / hsqldb: embedded; the script wipes /tmp/flyway-{h2,hsqldb} between
#   runs (busybox is used because the flyway container writes files as root).
#
set -uo pipefail

FLAVOUR="${1:?flavour required: db2|h2|hsqldb|mariadb|mssql|oracle|postgres}"
VARIANT="${2:?variant required: default|strict}"

# Resolve plugin root relative to this script so the harness is portable.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DB_PLUGINS="$SCRIPT_DIR"

SQL_DIR="$DB_PLUGINS/$FLAVOUR/src/main/resources/db/migration/$VARIANT"
PLUGIN_JAR="$(ls "$DB_PLUGINS/$FLAVOUR/target/ebms-${FLAVOUR}-db-plugin"-*-SNAPSHOT.jar 2>/dev/null | grep -v sources | grep -v javadoc | head -1)"

if [[ ! -d "$SQL_DIR" ]]; then
  echo "SKIP: $FLAVOUR/$VARIANT has no migrations"
  exit 0
fi

NET=flyway-validate
docker network inspect "$NET" >/dev/null 2>&1 || docker network create "$NET" >/dev/null

PASS="EbMS-Validate-PW-123!"
DB_NAME=ebms_validate
# DB2 has an 8-char database name limit.
[[ "$FLAVOUR" == "db2" ]] && DB_NAME=EBMSVAL
CID="ebms-${FLAVOUR}-validate"

WORK_DIR="${TMPDIR:-/tmp}/flyway-validate"
JARS_DIR="$WORK_DIR/jars-${FLAVOUR}"
H2_DIR="$WORK_DIR/h2"
HSQLDB_DIR="$WORK_DIR/hsqldb"
mkdir -p "$JARS_DIR"

cleanup() {
  docker rm -fv "$CID" >/dev/null 2>&1 || true
}
trap cleanup EXIT

# For DB2 we need the JCC driver extracted from the shaded plugin jar.
if [[ "$FLAVOUR" == "db2" ]]; then
  if [[ -z "$PLUGIN_JAR" ]]; then
    echo "FAIL: plugin jar not found for db2; build it with 'mvn -pl ebms-core/plugin/db/db2 -am package'"
    exit 1
  fi
  if [[ ! -f "$JARS_DIR/db2jcc.jar" ]]; then
    # Build a thin driver jar that excludes org.flywaydb/* and project classes
    # to avoid clashing with the flyway image's bundled flyway-core.
    cp "$PLUGIN_JAR" "$JARS_DIR/db2jcc.jar"
    ( cd "$JARS_DIR" && zip -q -d db2jcc.jar 'org/flywaydb/*' 'META-INF/services/org.flywaydb*' 'META-INF/maven/org.flywaydb/*' 'db/migration/*' 'nl/clockwork/*' 'org/springframework/*' || true ) >/dev/null
  fi
fi

case "$FLAVOUR" in
  postgres)
    IMG=postgres:16-alpine
    docker run -d --rm --name "$CID" --network "$NET" \
      -e POSTGRES_PASSWORD="$PASS" -e POSTGRES_DB="$DB_NAME" "$IMG" >/dev/null
    URL="jdbc:postgresql://$CID:5432/$DB_NAME"
    USR=postgres
    READY="docker exec $CID pg_isready -U postgres"
    ;;
  mariadb)
    IMG=mariadb:11
    docker run -d --rm --name "$CID" --network "$NET" \
      -e MARIADB_ROOT_PASSWORD="$PASS" -e MARIADB_DATABASE="$DB_NAME" "$IMG" >/dev/null
    URL="jdbc:mariadb://$CID:3306/$DB_NAME"
    USR=root
    READY="docker exec $CID mariadb -uroot -p$PASS -e 'SELECT 1'"
    ;;
  mssql)
    IMG=mcr.microsoft.com/azure-sql-edge:latest
    # Per docker hub docs: needs SYS_PTRACE; ACCEPT_EULA=1; SA password must be strong.
    # ARM64 image has no in-container sqlcmd, so we connect Flyway directly to
    # the always-present 'master' database for syntax validation.
    docker run -d --rm --name "$CID" --network "$NET" --cap-add SYS_PTRACE \
      -e 'ACCEPT_EULA=1' -e "MSSQL_SA_PASSWORD=$PASS" "$IMG" >/dev/null
    URL="jdbc:sqlserver://$CID:1433;databaseName=master;encrypt=false;trustServerCertificate=true"
    USR=sa
    READY="docker run --rm --network $NET busybox sh -c 'nc -z $CID 1433'"
    ;;
  oracle)
    IMG=gvenzl/oracle-free:23-slim-faststart
    docker run -d --rm --name "$CID" --network "$NET" \
      -e ORACLE_PASSWORD="$PASS" -e APP_USER=ebms -e APP_USER_PASSWORD="$PASS" "$IMG" >/dev/null
    URL="jdbc:oracle:thin:@//$CID:1521/FREEPDB1"
    USR=ebms
    READY="docker exec $CID healthcheck.sh"
    ;;
  db2)
    IMG=icr.io/db2_community/db2:11.5.9.0
    docker run -d --rm --privileged=true --platform linux/amd64 --name "$CID" --network "$NET" \
      -e LICENSE=accept -e DB2INST1_PASSWORD="$PASS" -e DBNAME="$DB_NAME" "$IMG" >/dev/null
    URL="jdbc:db2://$CID:50000/$DB_NAME"
    USR=db2inst1
    READY="docker logs $CID 2>&1 | grep -q 'Setup has completed.'"
    ;;
  h2)
    # Embedded — use the flyway image directly with a file URL.
    URL="jdbc:h2:/tmp/flyway-h2/ebms;DB_CLOSE_DELAY=-1"
    USR=sa
    PASS=
    docker run --rm -v "$H2_DIR":/wipe busybox sh -c 'rm -rf /wipe/* /wipe/.[!.]* 2>/dev/null || true'
    mkdir -p "$H2_DIR"
    ;;
  hsqldb)
    URL="jdbc:hsqldb:file:/tmp/flyway-hsqldb/ebms;hsqldb.tx=mvcc"
    USR=SA
    PASS=
    docker run --rm -v "$HSQLDB_DIR":/wipe busybox sh -c 'rm -rf /wipe/* /wipe/.[!.]* 2>/dev/null || true'
    mkdir -p "$HSQLDB_DIR"
    ;;
  *)
    echo "Unknown flavour $FLAVOUR"; exit 2;;
esac

# Wait for DB readiness (skip for embedded)
if [[ -n "${READY:-}" ]]; then
  # DB2 on QEMU emulation takes several minutes; others are fast.
  READY_TIMEOUT=${READY_TIMEOUT:-90}
  [[ "$FLAVOUR" == "db2" ]] && READY_TIMEOUT=600
  echo "[$FLAVOUR/$VARIANT] waiting for $CID to be ready (timeout ${READY_TIMEOUT}s) ..."
  for i in $(seq 1 $READY_TIMEOUT); do
    if eval "$READY" >/dev/null 2>&1; then
      echo "[$FLAVOUR/$VARIANT] DB ready after ${i}s"
      break
    fi
    sleep 1
    if [[ $i -eq $READY_TIMEOUT ]]; then
      echo "FAIL: $FLAVOUR did not become ready"
      docker logs "$CID" 2>&1 | tail -40
      exit 1
    fi
  done
fi

# Per-flavour post-readiness setup.
case "$FLAVOUR" in
  mssql)
    # azure-sql-edge ARM64 has no in-container sqlcmd. We use 'master' DB above,
    # so no separate CREATE DATABASE step is needed. Just wait an extra moment
    # for the engine to finish initialising master.
    sleep 5
    ;;
esac

echo "[$FLAVOUR/$VARIANT] running flyway migrate"
JARS_MOUNT=""
[[ -d "$JARS_DIR" ]] && [[ -n "$(ls "$JARS_DIR" 2>/dev/null)" ]] && JARS_MOUNT="-v $JARS_DIR:/flyway/jars"

docker run --rm --network "$NET" \
  -v "$SQL_DIR:/flyway/sql:ro" \
  $JARS_MOUNT \
  -v "$H2_DIR":/tmp/flyway-h2 \
  -v "$HSQLDB_DIR":/tmp/flyway-hsqldb \
  flyway/flyway:11 \
  -url="$URL" -user="$USR" -password="$PASS" \
  -locations=filesystem:/flyway/sql \
  -connectRetries=5 \
  -baselineOnMigrate=true \
  -X migrate
RC=$?

if [[ $RC -eq 0 ]]; then
  echo "[$FLAVOUR/$VARIANT] running flyway info + validate"
  docker run --rm --network "$NET" \
    -v "$SQL_DIR:/flyway/sql:ro" \
    $JARS_MOUNT \
    -v "$H2_DIR":/tmp/flyway-h2 \
    -v "$HSQLDB_DIR":/tmp/flyway-hsqldb \
    flyway/flyway:11 \
    -url="$URL" -user="$USR" -password="$PASS" \
    -locations=filesystem:/flyway/sql \
    info validate
  echo "[$FLAVOUR/$VARIANT] OK"
else
  echo "[$FLAVOUR/$VARIANT] FAIL (rc=$RC)"
fi
exit $RC
