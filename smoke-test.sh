#!/usr/bin/env bash
#===============================================================================
# ebms-core/smoke-test.sh — two-adapter EbMS smoke test
#
# Brings up two EbMS adapters that can message each other and verifies the
# setup with the same REST calls as ebms-core/core/resources/test/ebms.rest:
#
#  Adapter 1 ("digipoort", local)
#    Launched exactly like the "Launch StartEmbedded SSL Server"
#    configuration in the parent repo's .vscode/launch.json (same mainClass and
#    vmArgs), from the shaded ebms-server jar plus the H2 plugin jar (the
#    plugin is "provided" scope, which is why the VSCode classpath contains
#    it but the jar doesn't). The core server is property-driven (no
#    -ssl/-soap/-health CLI flags): the web/REST+SOAP/health connectors and
#    their SSL are enabled with -D api.* flags; the EbMS connector
#    (https://localhost:8888/ebms) comes from the server's default.properties.
#
#      java -Djavax.net.ssl.trustStore= -Dlog4j.configurationFile=log4j2.xml \
#           -Debms.jdbc.update=true -Debms.verifyHostnames=false \
#           -Dapi.ssl.enabled=true -Dapi.ssl.keyStorePassword=my-secret-password \
#           -Dapi.health.enabled=true \
#           -cp <ebms-server jar>:<ebms-h2-db-plugin jar> \
#           nl.clockwork.ebms.server.embedded.startup.StartEmbedded
#
#      REST API : https://localhost:8443/service/rest/v19   (self-signed TLS)
#      EbMS     : https://localhost:8888/ebms
#      Health   : http://localhost:8008/health
#      DB       : embedded H2 in a throw-away working directory
#                 (-Ddatabase.start=true starts the in-process H2 on 9092)
#
#  Adapter 2 ("overheid", docker)
#    Started from docker-compose.yml (published image
#    eluinstra/ebms-adapter-bin:2.20.4; its Dockerfile lives in the ebms-core
#    project at ebms-core/docker/ebms-adapter-bin/Dockerfile). Its one-shot
#    init container loads the CPA and the URL mapping
#    (https://localhost:8888/ebms -> https://host.docker.internal:8888/ebms)
#    that lets it reach adapter 1.
#
#      REST API : http://localhost:8000/service/rest/v19
#      EbMS     : https://localhost:8088/ebms
#
#  Both adapters use the same CPA "cpaStubEBF.rm.https.signed":
#    - adapter 1: loaded from resources/CPAs/cpaStubEBF.rm.https.signed.xml (this project)
#    - adapter 2: loaded by the compose init container (same file)
#
#  Test steps
#    1. health checks (adapter 1 /health, adapter 2 REST + container health)
#    2. insertCPA + getCPAIds on adapter 1; getCPAIds + getURLMappings on 2
#    3. ping adapter 1 -> adapter 2 and adapter 2 -> adapter 1
#    4. sendMessage (REST + attachment) adapter 1 -> adapter 2
#    5. sendMessage (REST + attachment) adapter 2 -> adapter 1
#    6. sendMessageMTOM (multipart) adapter 1 -> adapter 2
#    7. each sent message is picked up on the other adapter via
#       /ebms/messages/unprocessed, fetched (processed) via
#       /ebms/messages/{id} and the payload is verified
#
#  Usage
#    ebms-core/smoke-test.sh [--rebuild] [--keep] [--help]
#      --rebuild   force rebuild of the ebms-server jar (mvn package)
#      --keep      leave both adapters running after the test (no teardown)
#
#  Environment
#    SMOKE_LOG_DIR  optional directory; when the test fails, the workdir
#                   (adapter1.log, curl.err, props, ...) and the overheid
#                   container log are copied there (used by the release
#                   workflow to upload the logs as an artifact)
#
#  Requirements: JDK 17, Maven, Docker (daemon + compose), curl, jq
#===============================================================================

set -u

#--- configuration -------------------------------------------------------------
# This script lives at the root of the ebms-core project; everything it needs
# (compose file, CPA, server jar, H2 plugin jar, pom.xml, log4j2.xml) is relative
# to that directory.
CORE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$CORE_DIR/docker-compose.yml"
COMPOSE_PROJECT="ebms-smoke-test"
CPA_FILE="$CORE_DIR/resources/CPAs/cpaStubEBF.rm.https.signed.xml"
LOG4J2_CONFIG="$CORE_DIR/log4j2.xml"

# endpoints (see ebms-core/core/resources/test/ebms.rest)
REST1="https://localhost:8443/service/rest/v19"   # adapter 1, self-signed TLS
HEALTH1="http://localhost:8008/health"
REST2="http://localhost:8000/service/rest/v19"    # adapter 2

CPA_ID="cpaStubEBF.rm.https.signed"
DIGIPOORT_PARTY='urn:osb:oin:00000000000000000000'
DIGIPOORT_ROLE="DIGIPOORT"
OVERHEID_PARTY='urn:osb:oin:00000000000000000001'
OVERHEID_ROLE="OVERHEID"
AFLEVEREN_SERVICE='urn:osb:services:osb:afleveren:1.1$1.0'
AANLEVEREN_SERVICE='urn:osb:services:osb:aanleveren:1.1$1.0'
TEST_PAYLOAD_B64="U2FtcGxlIG1lc3NhZ2Uu"            # base64("Sample message.")

# ports that must be free before starting
NEEDED_PORTS="8000 8008 8443 8888 8088 9092"

REBUILD=0
KEEP=0
for arg in "$@"; do
  case "$arg" in
    --rebuild) REBUILD=1 ;;
    --keep)    KEEP=1 ;;
    --help|-h) grep '^#' "$0" | sed 's/^#//;s/^ //' ; exit 0 ;;
    *) echo "Unknown option: $arg (use --help)" >&2; exit 2 ;;
  esac
done

PASS=0
FAIL=0
WORK_DIR=""
ADAPTER1_PID=""
DC=()

#--- helpers -------------------------------------------------------------------
info()  { echo -e "\033[1;34m==> $*\033[0m"; }
ok()    { echo -e "  \033[1;32m[PASS]\033[0m $1"; PASS=$((PASS+1)); }
bad()   { echo -e "  \033[1;31m[FAIL]\033[0m $1"; FAIL=$((FAIL+1)); }
warn()  { echo -e "  \033[1;33m[WARN]\033[0m $1"; }

die() {
  echo -e "\033[1;31mERROR: $*\033[0m" >&2
  exit 1
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || die "required command '$1' not found"
}

port_in_use() {
  # no bare "exec" redirects here: they persist in the main shell and would
  # silently move its stderr to /dev/null for the rest of the script
  local rc=1
  (exec 3<>"/dev/tcp/127.0.0.1/$1") 2>/dev/null && rc=0
  return $rc
}

# wait_for <description> <timeout-seconds> <command...>
wait_for() {
  local desc="$1" timeout="$2"; shift 2
  local deadline=$(( $(date +%s) + timeout ))
  while true; do
    if "$@" >/dev/null 2>&1; then
      return 0
    fi
    if (( $(date +%s) >= deadline )); then
      echo "  (timed out after ${timeout}s waiting: $desc)" >&2
      return 1
    fi
    sleep 2
  done
}

# http <method> <url> [extra curl args...] -> sets HTTP_CODE and HTTP_BODY
http() {
  local method="$1" url="$2"; shift 2
  local body_file; body_file=$(mktemp)
  HTTP_CODE=$(curl -sS -o "$body_file" -w '%{http_code}' -X "$method" "$@" "$url" 2>>"$WORK_DIR/curl.err") || HTTP_CODE=000
  HTTP_BODY=$(cat "$body_file")
  rm -f "$body_file"
}

# expect_http <description> <method> <url> [curl args...]  (asserts 2xx)
expect_http() {
  local desc="$1" method="$2" url="$3"; shift 3
  http "$method" "$url" "$@"
  case "$HTTP_CODE" in
    2*) ok "$desc (HTTP $HTTP_CODE)" ;;
    *)  bad "$desc (HTTP $HTTP_CODE) body: ${HTTP_BODY:0:300}";;
  esac
}

cleanup() {
  [[ -n "$ADAPTER1_PID" ]] && kill "$ADAPTER1_PID" 2>/dev/null
  if [[ -n "${DC[@]:-}" ]]; then
    "${DC[@]}" down --remove-orphans >/dev/null 2>&1 || true
  fi
  if [[ -n "$WORK_DIR" ]] && [[ $KEEP -eq 0 ]]; then
    rm -rf "$WORK_DIR"
  fi
}
trap 'rc=$?; if (( rc != 0 )) && [[ -n "$WORK_DIR" && -f "$WORK_DIR/adapter1.log" ]]; then
  echo; echo "--- adapter1.log (last 40 lines) ---"; tail -n 40 "$WORK_DIR/adapter1.log"
  echo "--- docker logs: overheid (last 20 lines) ---"; docker logs --tail 20 overheid 2>&1 || true
  if [[ -n "${SMOKE_LOG_DIR:-}" ]]; then
    mkdir -p "$SMOKE_LOG_DIR" 2>/dev/null || true
    cp -a "$WORK_DIR/." "$SMOKE_LOG_DIR/" 2>/dev/null || true
    docker logs overheid > "$SMOKE_LOG_DIR/overheid-docker.log" 2>&1 || true
    echo "smoke test logs saved to $SMOKE_LOG_DIR"
  fi
fi; cleanup' EXIT

#--- pre-flight ----------------------------------------------------------------
info "Pre-flight checks"
for c in java mvn docker curl jq; do require_cmd "$c"; done
docker info >/dev/null 2>&1 || die "docker daemon is not reachable"
if docker compose version >/dev/null 2>&1; then
  DC=(docker compose -p "$COMPOSE_PROJECT" -f "$COMPOSE_FILE")
else
  DC=(docker-compose -p "$COMPOSE_PROJECT" -f "$COMPOSE_FILE")
fi
java_major=$(java -version 2>&1 | head -1 | sed -E 's/.*"([0-9]+).*/\1/')
(( java_major >= 17 )) || die "JDK 17+ required, found $java_major"
for p in $NEEDED_PORTS; do
  port_in_use "$p" && die "port $p is already in use; stop the other process and retry"
done
[[ -f "$COMPOSE_FILE" ]] || die "compose file not found: $COMPOSE_FILE"
[[ -f "$CPA_FILE" ]] || die "CPA file not found: $CPA_FILE"

#--- working directory (throw-away, keeps repo clean) --------------------------
# adapter 1 (the ebms-core server) starts its in-process H2 via -Ddatabase.start=true,
# so no properties file needs to be copied here
WORK_DIR=$(mktemp -d "${TMPDIR:-/tmp}/ebms-smoke.XXXXXX")
# the server is started with -Dlog4j.configurationFile=log4j2.xml (a bare name,
# resolved against its working directory); stage this project's log4j2.xml there
cp "$LOG4J2_CONFIG" "$WORK_DIR/log4j2.xml"
info "Working directory: $WORK_DIR"

#--- adapter 1 artifacts (ebms-core server jar + H2 plugin jar) --------------------
SERVER_JAR=$(ls "$CORE_DIR"/server/target/ebms-server-*.jar 2>/dev/null | grep -v original- | grep -v sources | grep -v javadoc | head -1 || true)
H2_PLUGIN_JAR=$(ls "$CORE_DIR"/plugin/db/h2/target/ebms-h2-db-plugin-*.jar 2>/dev/null | grep -v original- | grep -v sources | grep -v javadoc | head -1 || true)
if [[ -z "$H2_PLUGIN_JAR" ]]; then
  H2_PLUGIN_JAR=$(ls "$HOME"/.m2/repository/nl/clockwork/ebms/plugin/db/ebms-h2-db-plugin/*/*.jar 2>/dev/null | grep -v sources | grep -v javadoc | head -1 || true)
fi
if [[ $REBUILD -eq 1 || -z "$SERVER_JAR" || -z "$H2_PLUGIN_JAR" ]]; then
  info "Building the ebms-server jar (ebms-core reactor; this can take a few minutes)..."
  mvn -B -q -f "$CORE_DIR/pom.xml" -pl server -am package -DskipTests -Ddependency-check.skip=true -Dspotless.apply.skip=true -Dspotless.check.skip=true -Dlicense.skip=true -Dlicense.skipAddThirdParty=true || die "ebms-server build failed"
  SERVER_JAR=$(ls "$CORE_DIR"/server/target/ebms-server-*.jar | grep -v original- | grep -v sources | grep -v javadoc | head -1)
  H2_PLUGIN_JAR=$(ls "$CORE_DIR"/plugin/db/h2/target/ebms-h2-db-plugin-*.jar | grep -v original- | grep -v sources | grep -v javadoc | head -1)
fi
[[ -n "$SERVER_JAR" && -n "$H2_PLUGIN_JAR" ]] || die "could not locate ebms-server jar / H2 plugin jar"
info "Using: $SERVER_JAR"
info "Using: $H2_PLUGIN_JAR"

# adapter 1 classpath: the shaded jar should carry Spring's JCL bridge, but this
# environment's custom spring-core-7.0.8 depends on commons-logging, which the
# shade config strips (ebms-core/pom.xml). When the jar has no org.apache.commons.logging
# bridge, add the JCL jar from the local .m2 so the server can start. In a normal
# build (real spring-core -> spring-jcl, not excluded) this is a no-op.
EXTRA_CP=""
if command -v jar >/dev/null 2>&1 \
   && ! jar tf "$SERVER_JAR" 2>/dev/null | grep -qE 'org/(apache/commons/logging/LogFactory|springframework/jcl)'; then
  # prefer the canonical spring-jcl bridge; otherwise the newest local commons-logging
  JCL_JAR=$(find "$HOME"/.m2/repository -path '*springframework/spring-jcl/*/*.jar' 2>/dev/null | grep -v -e sources -e javadoc | sort -rV | head -1 || true)
  [[ -z "$JCL_JAR" ]] && JCL_JAR=$(find "$HOME"/.m2/repository -path '*commons-logging/commons-logging/*/*.jar' 2>/dev/null | grep -v -e sources -e javadoc | sort -rV | head -1 || true)
  if [[ -n "$JCL_JAR" ]]; then
    EXTRA_CP=":$JCL_JAR"
    info "shaded server jar has no JCL bridge; adding $JCL_JAR to adapter 1 classpath"
  else
    warn "shaded server jar has no JCL bridge (org.apache.commons.logging) and no bridge jar was found in ~/.m2; adapter 1 will likely fail to start (see adapter1.log)"
  fi
fi
ADAPTER1_CP="$SERVER_JAR:$H2_PLUGIN_JAR$EXTRA_CP"

#--- adapter 2: docker compose ---------------------------------------------------
info "Starting adapter 2 (docker compose: $COMPOSE_FILE)"
"${DC[@]}" up -d || die "docker compose up failed"

info "Waiting for overheid container to become healthy..."
overheid_healthy() {
  [[ "$(docker inspect --format '{{.State.Health.Status}}' overheid 2>/dev/null)" == "healthy" ]]
}
wait_for "overheid healthy" 240 overheid_healthy || die "overheid container did not become healthy (see 'docker logs overheid')"

info "Waiting for overheid_init (CPA + URL mapping) to finish..."
init_done() {
  local init_id
  init_id=$("${DC[@]}" ps -q overheid_init 2>/dev/null)
  [[ -n "$init_id" ]] && [[ "$(docker inspect --format '{{.State.ExitCode}}' "$init_id" 2>/dev/null)" == "0" ]]
}
wait_for "overheid_init exited 0" 180 init_done || die "overheid_init did not finish successfully (see 'docker compose logs overheid_init')"
ok "adapter 2 (overheid) is up and initialized"

#--- adapter 1: local StartEmbedded (ebms-core, "Launch StartEmbedded SSL Server") --
# the core server is property-driven: -ssl/-soap/-health CLI args are no-ops, so
# the web(REST/SOAP)/health connectors are enabled with -D api.* flags and the
# in-process H2 with -Ddatabase.start=true (see the parent repo's .vscode/launch.json)
info "Starting adapter 1 (local ebms-core server, property-driven)..."
(
  cd "$WORK_DIR" || exit 1
  exec java \
    -Djavax.net.ssl.trustStore= \
    -Dlog4j.configurationFile=log4j2.xml \
    -Debms.jdbc.update=true \
    -Debms.verifyHostnames=false \
    -Ddatabase.start=true \
    -Dapi.ssl.enabled=true \
    -Dapi.ssl.keyStorePassword=my-secret-password \
    -Dapi.health.enabled=true \
    -cp "$ADAPTER1_CP" \
    nl.clockwork.ebms.server.embedded.startup.StartEmbedded \
    > "$WORK_DIR/adapter1.log" 2>&1
) &
ADAPTER1_PID=$!

# fail fast: if the JVM crashes during startup (e.g. a missing class), the
# health endpoint will never come up and the wait below would look like a hang
adapter1_alive() { kill -0 "$ADAPTER1_PID" 2>/dev/null; }
adapter1_health() { curl -fsS "$HEALTH1" >/dev/null; }
adapter1_rest()   { curl -fk -fsS "$REST1/cpas" >/dev/null; }
if ! wait_for "adapter 1 to keep running" 20 adapter1_alive; then
  die "adapter 1 (the ebms-core server JVM) exited during startup; last log lines:
$(tail -n 15 "$WORK_DIR/adapter1.log")"
fi
wait_for "adapter 1 health endpoint ($HEALTH1)" 240 adapter1_health || die "adapter 1 did not start (see $WORK_DIR/adapter1.log)"
wait_for "adapter 1 REST API ($REST1)" 120 adapter1_rest || die "adapter 1 REST API not reachable (see $WORK_DIR/adapter1.log)"
ok "adapter 1 (digipoort) is up on https://localhost:8443 + https://localhost:8888/ebms"

#--- health checks -----------------------------------------------------------------
expect_http "adapter 1 health check" GET "$HEALTH1"
expect_http "adapter 2 REST API reachable" GET "$REST2/cpas"

#--- CPA setup ----------------------------------------------------------------------
info "Loading CPA into adapter 1 ($CPA_FILE)"
expect_http "insertCPA adapter 1" POST "$REST1/cpas?overwrite=true" -k \
  -H 'Content-Type: text/plain' --data-binary "@$CPA_FILE"
http GET "$REST1/cpas" -k
echo "$HTTP_BODY" | jq -e --arg id "$CPA_ID" 'index($id)' >/dev/null \
  && ok "getCPAIds adapter 1 contains $CPA_ID" \
  || bad "getCPAIds adapter 1 missing $CPA_ID (body: $HTTP_BODY)"

http GET "$REST2/cpas"
echo "$HTTP_BODY" | jq -e --arg id "$CPA_ID" 'index($id)' >/dev/null \
  && ok "getCPAIds adapter 2 contains $CPA_ID" \
  || bad "getCPAIds adapter 2 missing $CPA_ID (body: $HTTP_BODY)"

expect_http "getURLMappings adapter 2" GET "$REST2/urlMappings"
echo "$HTTP_BODY" | grep -q 'host.docker.internal:8888' \
  && ok "adapter 2 URL mapping to adapter 1 (host.docker.internal:8888) is in place" \
  || bad "adapter 2 URL mapping missing (body: $HTTP_BODY)"

#--- ping (both directions) -----------------------------------------------------------
info "Ping tests"
expect_http "ping adapter 1 -> adapter 2" POST \
  "$REST1/ebms/ping/$CPA_ID/from/$DIGIPOORT_PARTY/to/$OVERHEID_PARTY" -k
expect_http "ping adapter 2 -> adapter 1" POST \
  "$REST2/ebms/ping/$CPA_ID/from/$OVERHEID_PARTY/to/$DIGIPOORT_PARTY"

#--- sendMessage helper ----------------------------------------------------------------
# send_message <adapter:1|2> <json-properties-file> -> sets SENT_MESSAGE_ID
send_message() {
  local adapter="$1" props_file="$2" rest
  if [[ "$adapter" == "1" ]]; then rest="$REST1"; else rest="$REST2"; fi
  local curl_k=()
  [[ "$adapter" == "1" ]] && curl_k=(-k)
  http POST "$rest/ebms/messages" "${curl_k[@]}" \
    -H 'Content-Type: application/json' --data-binary "@$props_file"
  case "$HTTP_CODE" in
    2*) SENT_MESSAGE_ID=$(echo "$HTTP_BODY" | tr -d '[:space:]');;
    *)  SENT_MESSAGE_ID="";;
  esac
}

# wait_received <adapter:1|2> <message-id> -> asserts unprocessed on that adapter
wait_received() {
  local adapter="$1" msg_id="$2" rest
  [[ -z "$msg_id" ]] && return 1
  if [[ "$adapter" == "1" ]]; then rest="$REST1"; else rest="$REST2"; fi
  local curl_k=()
  [[ "$adapter" == "1" ]] && curl_k=(-k)
  wait_for "message $msg_id to arrive on adapter $adapter" 120 sh -c \
    "curl -fsS ${curl_k[*]} '$rest/ebms/messages/unprocessed?messageId=$msg_id' | grep -q '$msg_id'" \
    || return 1
}

# fetch_and_verify <adapter:1|2> <message-id> <expected-to-party-id> [mtom]
fetch_and_verify() {
  local adapter="$1" msg_id="$2" expected_to="$3" mtom="${4:-}" rest
  if [[ "$adapter" == "1" ]]; then rest="$REST1"; else rest="$REST2"; fi
  local curl_k=()
  [[ "$adapter" == "1" ]] && curl_k=(-k)
  local path="messages/$msg_id?process=true"
  [[ -n "$mtom" ]] && path="messages/mtom/$msg_id?process=true"
  http GET "$rest/ebms/$path" "${curl_k[@]}"
  case "$HTTP_CODE" in
    2*) : ;;
    *)  bad "getMessage adapter $adapter for $msg_id (HTTP $HTTP_CODE) body: ${HTTP_BODY:0:300}"; return 1;;
  esac
  if [[ -n "$mtom" ]]; then
    if echo "$HTTP_BODY" | grep -q "$TEST_PAYLOAD_B64"; then
      ok "received MTOM message $msg_id on adapter $adapter (payload verified)"
    else
      bad "received MTOM message $msg_id on adapter $adapter but payload missing (body: ${HTTP_BODY:0:300})"
    fi
    return 0
  fi
  local content to_party
  content=$(echo "$HTTP_BODY" | jq -r '.dataSources[0].content' 2>/dev/null)
  to_party=$(echo "$HTTP_BODY" | jq -r '.properties.toParty.partyId' 2>/dev/null)
  [[ "$content" == "$TEST_PAYLOAD_B64" ]] \
    && ok "received message $msg_id on adapter $adapter (payload verified)" \
    || bad "message $msg_id payload mismatch (content: ${content:0:60})"
  [[ "$to_party" == "$expected_to" ]] \
    && ok "message $msg_id addressed to $to_party" \
    || bad "message $msg_id unexpected toParty (got: $to_party, want: $expected_to)"
}

#--- sendMessage adapter 1 -> adapter 2 (REST) -----------------------------------------
info "sendMessage adapter 1 -> adapter 2 (afleveren, REST + attachment)"
cat > "$WORK_DIR/props12.json" <<'EOF'
{
  "properties": {
    "cpaId": "cpaStubEBF.rm.https.signed",
    "fromPartyId": "urn:osb:oin:00000000000000000000",
    "fromRole": "DIGIPOORT",
    "toPartyId": "urn:osb:oin:00000000000000000001",
    "toRole": "OVERHEID",
    "service": "urn:osb:services:osb:afleveren:1.1$1.0",
    "action": "afleveren"
  },
  "dataSources": [{
    "name": "test.txt",
    "contentType": "text/plain",
    "content": "U2FtcGxlIG1lc3NhZ2Uu"
  }]
}
EOF
send_message 1 "$WORK_DIR/props12.json"
if [[ -n "$SENT_MESSAGE_ID" ]]; then
  ok "sendMessage accepted (messageId: $SENT_MESSAGE_ID)"
  if wait_received 2 "$SENT_MESSAGE_ID"; then
    ok "message $SENT_MESSAGE_ID picked up by adapter 2 (unprocessed)"
    fetch_and_verify 2 "$SENT_MESSAGE_ID" "$OVERHEID_PARTY"
  else
    bad "message $SENT_MESSAGE_ID never arrived on adapter 2"
  fi
else
  bad "sendMessage rejected (HTTP $HTTP_CODE) body: ${HTTP_BODY:0:300}"
fi

#--- sendMessage adapter 2 -> adapter 1 (REST) -----------------------------------------
info "sendMessage adapter 2 -> adapter 1 (aanleveren, REST + attachment)"
cat > "$WORK_DIR/props21.json" <<'EOF'
{
  "properties": {
    "cpaId": "cpaStubEBF.rm.https.signed",
    "fromPartyId": "urn:osb:oin:00000000000000000001",
    "fromRole": "OVERHEID",
    "toPartyId": "urn:osb:oin:00000000000000000000",
    "toRole": "DIGIPOORT",
    "service": "urn:osb:services:osb:aanleveren:1.1$1.0",
    "action": "aanleveren"
  },
  "dataSources": [{
    "name": "test.txt",
    "contentType": "text/plain",
    "content": "U2FtcGxlIG1lc3NhZ2Uu"
  }]
}
EOF
send_message 2 "$WORK_DIR/props21.json"
if [[ -n "$SENT_MESSAGE_ID" ]]; then
  ok "sendMessage accepted (messageId: $SENT_MESSAGE_ID)"
  if wait_received 1 "$SENT_MESSAGE_ID"; then
    ok "message $SENT_MESSAGE_ID picked up by adapter 1 (unprocessed)"
    fetch_and_verify 1 "$SENT_MESSAGE_ID" "$DIGIPOORT_PARTY"
  else
    bad "message $SENT_MESSAGE_ID never arrived on adapter 1"
  fi
else
  bad "sendMessage rejected (HTTP $HTTP_CODE) body: ${HTTP_BODY:0:300}"
fi

#--- sendMessageMTOM adapter 1 -> adapter 2 ---------------------------------------------
info "sendMessageMTOM adapter 1 -> adapter 2"
cat > "$WORK_DIR/props12-mtom.json" <<'EOF'
{
  "cpaId": "cpaStubEBF.rm.https.signed",
  "fromPartyId": "urn:osb:oin:00000000000000000000",
  "fromRole": "DIGIPOORT",
  "toPartyId": "urn:osb:oin:00000000000000000001",
  "toRole": "OVERHEID",
  "service": "urn:osb:services:osb:afleveren:1.1$1.0",
  "action": "afleveren"
}
EOF
printf '%s' "$TEST_PAYLOAD_B64" > "$WORK_DIR/attachment.b64"
http POST "$REST1/ebms/messages/mtom" -k \
  -F "requestProperties=@$WORK_DIR/props12-mtom.json;type=application/json" \
  -F "attachment=@$WORK_DIR/attachment.b64;filename=test.txt;type=text/plain;content-transfer-encoding=base64"
case "$HTTP_CODE" in
  2*) SENT_MESSAGE_ID=$(echo "$HTTP_BODY" | tr -d '[:space:]')
      ok "sendMessageMTOM accepted (messageId: $SENT_MESSAGE_ID)" ;;
  *)  SENT_MESSAGE_ID=""; bad "sendMessageMTOM rejected (HTTP $HTTP_CODE) body: ${HTTP_BODY:0:300}" ;;
esac
if [[ -n "$SENT_MESSAGE_ID" ]] && wait_received 2 "$SENT_MESSAGE_ID"; then
  ok "MTOM message $SENT_MESSAGE_ID picked up by adapter 2 (unprocessed)"
  fetch_and_verify 2 "$SENT_MESSAGE_ID" "$OVERHEID_PARTY" mtom
else
  bad "MTOM message $SENT_MESSAGE_ID never arrived on adapter 2"
fi

#--- summary ---------------------------------------------------------------------------
echo
echo "============================================================"
echo " Smoke test finished: $PASS passed, $FAIL failed"
echo "============================================================"
if [[ $KEEP -eq 1 ]]; then
  echo " --keep: leaving everything running"
  echo "   adapter 1 REST: $REST1  (stop: kill $ADAPTER1_PID)"
  echo "   adapter 2 REST: $REST2  (stop: ${DC[*]} down)"
  ADAPTER1_PID=""
  DC=()
  WORK_DIR=""
else
  info "Tearing down"
fi
exit $(( FAIL > 0 ? 1 : 0 ))
