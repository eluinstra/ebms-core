# Flyway migration integration tests

Each database plugin under `plugin/db/<flavour>/` has a `*FlywayMigrationIT.java` integration
test under `src/test/java`. The tests apply the SQL migrations from
`src/main/resources/db/migration/<variant>` against a real database and assert that
`flyway.migrate()` succeeds and applies at least one migration.

For container-backed flavours the test starts the database on demand via
Testcontainers, using the `jdbc:tc:` JDBC URL scheme where possible.

## Running

```sh
# A single plugin (recommended during development)
mvn -pl ebms-core/plugin/db/postgres -am verify
mvn -pl ebms-core/plugin/db/mariadb  -am verify
mvn -pl ebms-core/plugin/db/mssql    -am verify
mvn -pl ebms-core/plugin/db/oracle   -am verify
mvn -pl ebms-core/plugin/db/h2       -am verify
mvn -pl ebms-core/plugin/db/hsqldb   -am verify

# Everything (DB2 stays gated, see below)
mvn -B verify
```

The ITs are picked up by the Maven Failsafe plugin (`*IT.java` pattern) and bound
to the `integration-test` / `verify` phases.

## Database images

| flavour  | image / engine                                | URL                                           |
|----------|-----------------------------------------------|-----------------------------------------------|
| postgres | `postgres:16-alpine`                          | `jdbc:tc:postgresql:16-alpine:///ebms`        |
| mariadb  | `mariadb:11`                                  | `jdbc:tc:mariadb:11:///ebms`                  |
| mssql    | `mcr.microsoft.com/azure-sql-edge:latest`     | typed `MSSQLServerContainer` (arm64-friendly) |
| oracle   | `gvenzl/oracle-free:23-slim-faststart`        | `jdbc:tc:oracle:23-slim-faststart:///freepdb1`|
| db2      | `icr.io/db2_community/db2:11.5.0.0a`          | `jdbc:tc:db2:11.5.0.0a:///test` (gated)       |
| h2       | embedded                                      | `jdbc:h2:mem:ebms;DB_CLOSE_DELAY=-1;MODE=LEGACY` |
| hsqldb   | embedded                                      | `jdbc:hsqldb:mem:ebms;hsqldb.tx=mvcc`         |

### MSSQL on arm64

Microsoft does not publish an arm64 image of `mssql/server`. The IT uses
`azure-sql-edge` as a compatible substitute via
`DockerImageName.asCompatibleSubstituteFor("mcr.microsoft.com/mssql/server")`.

### DB2

The DB2 image is amd64-only (~3 GB) and is slow under emulation on arm64. The
IT is therefore gated on a system property and skipped by default:

```sh
mvn -pl ebms-core/plugin/db/db2 -am verify -Debms.it.db2=true
```

On arm64 hosts QEMU must be registered first:

```sh
docker run --privileged --rm tonistiigi/binfmt --install all
```
