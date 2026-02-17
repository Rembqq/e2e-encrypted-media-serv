`./mvnw spring-boot:run`
`./mvnw flyway:migrate`

`docker compose ps -a`
`docker compose up minio`
`docker compose up postgres`

`docker compose down -v`

`docker compose up -d`

`docker compose logs postgres --tail=50`
`docker compose logs minio --tail=50`

`psql -h localhost -p 5433 -U media_user -d media_backup_db`

`docker compose exec postgres psql -U postgres -d media-backup-db`
`psql -h localhost -p 5433 -U media_user -d media_backup_db`
`\dt`
