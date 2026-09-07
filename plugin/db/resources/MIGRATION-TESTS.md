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

# Everything (DB2 only runs on amd64 hosts, see below)
mvn -B verify
```

The ITs are picked up by the Maven Failsafe plugin (`*IT.java` pattern) and bound
to the `integration-test` / `verify` phases.

## Database images

| flavour  |                                             image / engine                                             |                                  URL                                  |
|----------|--------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------|
| postgres | `postgres:16-alpine`                                                                                   | `jdbc:tc:postgresql:16-alpine:///ebms`                                |
| mariadb  | `mariadb:11`                                                                                           | `jdbc:tc:mariadb:11:///ebms`                                          |
| mssql    | `mcr.microsoft.com/mssql/server:latest` (amd64) <br> `mcr.microsoft.com/azure-sql-edge:latest` (arm64) | typed `MSSQLServerContainer`, gated per architecture                  |
| oracle   | `container-registry.oracle.com/database/free:latest`                                                   | typed `OracleContainer` (official Oracle 23ai Free image, multi-arch) |
| db2      | `icr.io/db2_community/db2:11.5.0.0a`                                                                   | `jdbc:tc:db2:11.5.0.0a:///test` (amd64 only)                          |
| h2       | embedded                                                                                               | `jdbc:h2:mem:ebms;DB_CLOSE_DELAY=-1;MODE=LEGACY`                      |
| hsqldb   | embedded                                                                                               | `jdbc:hsqldb:mem:ebms;hsqldb.tx=mvcc`                                 |

### MSSQL

Two ITs live side-by-side in the `mssql` plugin and are mutually exclusive per host
architecture:

- `MsSqlServerFlywayMigrationIT` runs the official `mcr.microsoft.com/mssql/server:latest`
  image; gated to `os.arch` matching `amd64|x86_64`.
- `MsSqlFlywayMigrationIT` runs the multi-arch `mcr.microsoft.com/azure-sql-edge:latest`
  image (substituted via `DockerImageName.asCompatibleSubstituteFor("mcr.microsoft.com/mssql/server")`);
  gated to `os.arch` matching `aarch64|arm64` because Microsoft does not publish
  an arm64 build of `mssql/server`.

### Oracle

Uses the official `container-registry.oracle.com/database/free` image (multi-arch,
linux/amd64 + linux/arm64), as documented in
[oracle/docker-images SingleInstance](https://github.com/oracle/docker-images/blob/main/OracleDatabase/SingleInstance/README.md).
This image is a prebuilt database (datafiles ship in `/opt/oracle/oradata/FREE/`),
so the conventional `/opt/oracle/scripts/setup/` hook is not executed at start-up.
The IT therefore connects once as `system` (using `ORACLE_PWD`) to provision a
dedicated `ebms` application user, and then runs Flyway as that user.

### DB2

The DB2 image is amd64-only (~3 GB) and would otherwise run under QEMU emulation
on arm64 (several minutes to start). The IT is therefore gated on the host CPU
architecture (`@EnabledIfSystemProperty(named = "os.arch", matches = "amd64|x86_64")`)
and automatically skipped on non-amd64 hosts:

```sh
# Runs on amd64, skipped on arm64
mvn -pl ebms-core/plugin/db/db2 -am verify
```

To force a run on arm64 (slow, requires QEMU registered for amd64):

```sh
docker run --privileged --rm tonistiigi/binfmt --install all
mvn -pl ebms-core/plugin/db/db2 -am verify -Dos.arch=amd64
```

