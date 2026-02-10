`docker compose ps -a`
`docker compose up minio`
`docker compose up postgres`

`docker compose logs postgres --tail=50`
`docker compose logs minio --tail=50`

`docker compose exec postgres psql -U postgres -d media-backup-db`

`\dt`
