#!/bin/bash

BASE_URL="http://localhost:8080/api/v1"
USERNAME="testuser_$(date +%s)"
PASSWORD="password123"

echo "=== 1. Register ==="
REGISTER_RESP=$(curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\"}")
echo "Register response: $REGISTER_RESP"

echo ""
echo "=== 2. Login ==="
TOKEN=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\"}" | jq -r '.token')
echo "Token: $TOKEN"

echo ""
echo "=== 3. Upload blob ==="
FILENAME="test-blob.bin"
echo "fake-encrypted-bytes" > /tmp/$FILENAME
FILESIZE=$(wc -c < /tmp/$FILENAME)
CIPHER_HASH="a3f1e2d4b5c6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2"

UPLOAD_RESP=$(curl -s -X POST "$BASE_URL/blobs" \
  -H "Authorization: Bearer $TOKEN" \
  -F "blob=@/tmp/$FILENAME;filename=$FILENAME" \
  -F "metadata={
    \"clientId\": \"client-001\",
    \"originalFilename\": \"$FILENAME\",
    \"size\": $FILESIZE,
    \"modifiedAt\": \"2026-03-13T10:00:00Z\",
    \"cipherHash\": \"$CIPHER_HASH\"
  };type=application/json")

echo $UPLOAD_RESP | jq .
BLOB_ID=$(echo $UPLOAD_RESP | jq -r '.blobId')
echo "Blob ID: $BLOB_ID"

echo ""
echo "=== 4. Create snapshot ==="
SNAPSHOT_RESP=$(curl -s -X POST "$BASE_URL/snapshots" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"name\": \"test-snapshot-$(date +%s)\",
    \"description\": \"integration test snapshot\",
    \"files\": [
      {
        \"path\": \"test/$FILENAME\",
        \"blobId\": \"$BLOB_ID\",
        \"size\": $FILESIZE,
        \"modifiedAt\": \"2026-03-13T10:00:00Z\"
      }
    ]
  }")
echo $SNAPSHOT_RESP | jq .
SNAPSHOT_ID=$(echo $SNAPSHOT_RESP | jq -r '.id')

echo ""
echo "=== 5. Get snapshots ==="
curl -s "$BASE_URL/snapshots" \
  -H "Authorization: Bearer $TOKEN" | jq .

echo ""
echo "=== 6. Get snapshot by ID ==="
curl -s "$BASE_URL/snapshots/$SNAPSHOT_ID" \
  -H "Authorization: Bearer $TOKEN" | jq .

echo ""
echo "=== 7. No token → expect 401/403 ==="
curl -s -o /dev/null -w "HTTP status: %{http_code}\n" \
  "$BASE_URL/snapshots"

echo ""
echo "=== Done ==="