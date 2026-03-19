#!/bin/bash
set -e

echo "1. Launch infrastructure..."
docker compose up -d

echo "2. Launch server (in background or separate terminal)..."

./mvnw spring-boot:run &
SERVER_PID=$!
echo $SERVER_PID
sleep 10

echo "3. Launching upload client..."
export TEST_KEY="AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGBkaGxwdHh8="

./mvnw compile exec:java \
  -Dexec.mainClass="org.example.e2eencryptedmediaserv.client.BackupClient" \
  -Dexec.args="upload testfile.bin --server http://localhost:8080 --key $TEST_KEY --token dummy-token"

echo "4. Checking the results:"
echo "=== Postgres ==="
docker compose exec postgres psql -U media_user -d media_backup_db -c "SELECT * FROM blobs;"

echo "=== Storage ==="
# Если FileSystem:
#ls -l ./data/blobs/

# Если MinIO:
docker compose exec minio mc ls data/blobs/

kill $SERVER_PID
docker compose down