Copilot instructions - ebms-core

Scope

- Applies to files under ebms-core/.
- This module is the parent for common, core, and plugin components.

Build and test

- Full build: mvn -f ebms-core/pom.xml -B clean install
- Full validation: mvn -f ebms-core/pom.xml -B verify
- Single module: mvn -f ebms-core/pom.xml -pl <module-path> -am -B package

High-impact change rules

- If changing core APIs, verify callers in ebms-admin and plugin modules.
- If changing SQL/schema in plugin/db, update matching test resources under
  ebms-core/core/resources/test and document migration implications.
- Keep tests deterministic and prefer targeted tests before full verify.

Review checklist

- Add or update unit tests for behavior changes.
- Run module-level tests and report exact command(s) in PR notes.
- Avoid unrelated formatting/refactor churn.

