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

`curl -X POST http://localhost:8080/api/v1/blobs \
  -F "blob=@testfile.bin" \
  -F 'metadata={
        "clientId":"alice",
        "originalFilename":"file.txt",
        "size":123,
        "modifiedAt":"2026-02-09T12:00:00Z",
        "cipherHash":"abc123"
      };type=application/json'`

`KEY=$(openssl rand -base64 32)`
```
mvnw compile exec:java \
-Dexec.mainClass="org.example.e2eencryptedmediaserv.client.BackupClient" \
-Dexec.args="photo.jpg --server http://localhost:8080 --key $KEY --client-id alice"

```