## Як запустити локально

### Вимоги
- Java 21+
- Docker + docker-compose
- Maven

### Кроки
1. docker compose up -d
2. Запуск сервера:
   `./mvnw spring-boot:run`
3. Запуск клієнта (приклад):
   `export TEST_KEY="AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGBkaGxwdHh8="`
   ```
   mvn compile exec:java \
   -Dexec.mainClass="org.example.e2eencryptedmediaserv.client.BackupClient" \
   -Dexec.args="upload testfile.bin --server http://localhost:8080 --key $TEST_KEY --token dummy-token"
    ```
### Зберігання
- Метадані: таблиця `blobs` в Postgres
- Блоби: `./blob-storage/` (FileSystem) або MinIO bucket `blobs`