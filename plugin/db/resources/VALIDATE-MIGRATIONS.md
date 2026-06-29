# Flyway migration validation harness

Helper scripts to validate the SQL migrations under each `plugin/db/<flavour>/src/main/resources/db/migration/` against a real database container, using the official `flyway/flyway:11` CLI image.

## Files

- `validate-migrations.sh` — validates one `<flavour>/<variant>` combination.
- `validate-migrations-all.sh` — runs every supported combination, collects failures.

## Usage

```sh
# Single combination
./validate-migrations.sh postgres default
./validate-migrations.sh mssql default

# Everything (except DB2)
./validate-migrations-all.sh
```

Output of each run ends with `[<flavour>/<variant>] OK` or `[<flavour>/<variant>] FAIL`.

## How it works

For each flavour the script:

1. Starts a docker container with the matching database engine on the `flyway-validate` docker network (auto-created on first run).
2. Waits for the database to become ready.
3. Mounts the migrations directory into the `flyway/flyway:11` container and runs `flyway migrate` followed by `flyway info validate`.
4. Tears down the database container on exit (via `trap`).

H2 and HSQLDB are file-based and need no container.

## Database images

| flavour  | image                                     | notes |
|----------|-------------------------------------------|-------|
| postgres | `postgres:16-alpine`                      | |
| mariadb  | `mariadb:11`                              | |
| mssql    | `mcr.microsoft.com/azure-sql-edge:latest` | needs `--cap-add SYS_PTRACE` (set by the script); on arm64 there is no in-container `sqlcmd`, so Flyway uses the always-present `master` database |
| oracle   | `gvenzl/oracle-free:23-slim-faststart`    | |
| db2      | `icr.io/db2_community/db2:11.5.9.0`       | amd64 only; runs under QEMU on arm64 and takes several minutes to start. Database name limited to 8 chars (script uses `EBMSVAL`). |
| h2       | embedded                                  | persists to `${TMPDIR:-/tmp}/flyway-validate/h2` |
| hsqldb   | embedded                                  | persists to `${TMPDIR:-/tmp}/flyway-validate/hsqldb` |

## JDBC drivers

The `flyway/flyway:11` image ships drivers for Postgres, MariaDB, MSSQL, Oracle, H2 and HSQLDB. For DB2 the IBM JCC driver is not freely redistributable, so the script extracts it from the shaded plugin jar at `plugin/db/db2/target/ebms-db2-db-plugin-<version>-SNAPSHOT.jar`. Build it first:

```sh
mvn -pl ebms-core/plugin/db/db2 -am package
```

On arm64 hosts you also need QEMU registered to run the amd64 DB2 image:

```sh
docker run --privileged --rm tonistiigi/binfmt --install all
```

DB2 is excluded from `validate-migrations-all.sh` by default; uncomment the entry there to include it.
