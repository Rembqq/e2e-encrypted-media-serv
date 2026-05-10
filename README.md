# E2E Encrypted Backup Service

A web application for end-to-end encrypted file backup. Files are encrypted in the browser before upload — the server stores only ciphertext and never has access to file contents.

## Architecture

- **Frontend** — React + TypeScript + Vite + Web Crypto API
- **Backend** — Java 21 + Spring Boot 3
- **Database** — PostgreSQL (metadata)
- **Storage** — MinIO (encrypted blobs)
- **Auth** — JWT tokens

## How E2EE works

plaintext file
→ AES-GCM encrypt (browser, master key)
→ encrypted blob
→ upload to server
→ stored in MinIO

restore:
download encrypted blob
→ AES-GCM decrypt (browser, master key)
→ original file

The server never sees the decryption key or plaintext.

## Quick start

### Prerequisites
- Docker + Docker Compose
- Node.js 18+
- Java 21

### Run

# Start backend + database + MinIO
docker-compose up

# Start frontend
cd frontend
npm install
npm run dev

Open http://localhost:5173

## API Endpoints

### Auth
POST /api/v1/auth/register   — create account
POST /api/v1/auth/login      — get JWT token

### Blobs
POST /api/v1/blobs           — upload encrypted blob
GET  /api/v1/blobs           — list user blobs
GET  /api/v1/blobs/{id}      — download encrypted blob

### Snapshots
POST   /api/v1/snapshots        — create snapshot
GET    /api/v1/snapshots        — list snapshots
GET    /api/v1/snapshots/{id}   — get snapshot with files
DELETE /api/v1/snapshots/{id}   — delete snapshot

## Environment variables

See .env.example:

POSTGRES_DB=e2e_backup
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=minioadmin
JWT_SECRET=your-secret-key