docker run -d -v ./postgres.conf:/etc/postgresql/postgresql.conf -p 5432:5432 -e POSTGRES_PASSWORD=ebms -e POSTGRES_USER=ebms -e POSTGRES_DB=ebms postgres:12
