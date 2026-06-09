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
