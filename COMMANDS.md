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
./mvnw compile exec:java \
  -Dexec.mainClass="org.example.e2eencryptedmediaserv.client.BackupClient" \
  -Dexec.args="upload testfile.bin --server http://localhost:8080 --key $KEY --token dummy-token"
```

`openssl rand -base64 48`

{
    "username": "testuser",
    "password": "password123"
}

{"clientId":"test-client-123","originalFilename":"my-test-file.jpg","size":5242880,"modifiedAt":"2026-03-10T14:30:00Z","cipherHash":"e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"}

{"clientId":"","originalFilename":"","size":,"modifiedAt":"","cipherHash":""}