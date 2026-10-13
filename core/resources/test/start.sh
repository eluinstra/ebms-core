#!/bin/sh
# Starts the embedded server (ebms-server fat jar) for manual testing against
# the manual test resources in this directory (ebms.wsdl, ebms.http, ...).
#
# Build the jar first:  mvn -B -f ebms-core/pom.xml -pl server -am install
# The api.soap/health connectors and the in-process H2 are property-driven:
# the -soap/-health CLI args are no-ops in nl.clockwork.ebms.server.embedded.startup.StartEmbedded.
EBMS_SERVER_JAR="${EBMS_SERVER_JAR:-ebms-server-2.21.0-SNAPSHOT.jar}"
H2_PLUGIN_JAR="${H2_PLUGIN_JAR:-ebms-h2-db-plugin-2.21.0-SNAPSHOT.jar}"

java -Djavax.net.ssl.trustStore= \
  -Debms.jdbc.update=true \
  -Ddatabase.start=true \
  -Dapi.host=localhost \
  -Dapi.soap.enabled=true \
  -cp "$EBMS_SERVER_JAR:$H2_PLUGIN_JAR" \
  nl.clockwork.ebms.server.embedded.startup.StartEmbedded
