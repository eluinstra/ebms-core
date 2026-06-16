EbMS Core

Main multi-module parent for common, core and plugin modules.

Build from repository root:

	mvn -f ebms-core/pom.xml -B clean install

Run all tests in ebms-core:

	mvn -f ebms-core/pom.xml -B verify

Build only one module with dependencies:

	mvn -f ebms-core/pom.xml -pl <module-path> -am -B package

Examples:

	mvn -f ebms-core/pom.xml -pl core -am -B test
	mvn -f ebms-core/pom.xml -pl plugin/db -am -B verify

Notes:

- API changes in core can impact ebms-admin and plugin modules.
- DB schema and SQL changes should be reflected in plugin/db and test resources
	under core/resources/test.
- For broader project docs, see https://eluinstra.github.io/ebms-admin/

Dependency Vulnerability Check:

Run dependency check to scan for known vulnerabilities in dependencies:

	mvn -f ebms-core/pom.xml -B dependency-check:aggregate

The check fails if any vulnerability with CVSS score >= 7 (HIGH) is found.

Optional: Add NVD API key for faster updates (required for production CI/CD):

	mvn -f ebms-core/pom.xml -B dependency-check:aggregate -Dnvd.apikey=YOUR_API_KEY

Configure API key in ~/.m2/settings.xml:

	<settings>
	  <properties>
	    <nvd.apikey>YOUR_API_KEY</nvd.apikey>
	  </properties>
	</settings>
