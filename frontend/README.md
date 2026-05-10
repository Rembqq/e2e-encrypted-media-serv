# E2E Backup — Frontend

A web application for encrypted file backup. All encryption happens in the browser — the server never sees file contents.

## Stack

- React 18 + TypeScript
- Vite
- Tailwind CSS v4
- shadcn/ui (Radix)
- Axios
- React Router v6
- React Hot Toast
- JSZip
- Web Crypto API (AES-GCM, PBKDF2)

## Setup

cd frontend
npm install
npm run dev

Opens at http://localhost:5173
Backend must be running at http://localhost:8080

## How it works

1. User registers and logs in — receives JWT token
2. User enters master password — browser derives AES-256 key via PBKDF2
3. User selects files — browser encrypts each file with AES-GCM
4. Encrypted blobs are uploaded to the server with metadata
5. A snapshot (version) is created referencing all uploaded blobs
6. To restore — encrypted blobs are downloaded, decrypted in browser, packed into zip

## Security

- Master password never leaves the browser
- Encryption key is stored only in memory (lost on tab close)
- Server stores only ciphertext + metadata
- AES-GCM provides authenticated encryption — tampered data will fail to decrypt

## Structure

src/
├── api/           # axios instance + API calls
│   ├── axios.ts
│   ├── auth.ts
│   ├── blobs.ts
│   └── snapshots.ts
├── components/    # shared components
│   ├── ui/        # shadcn components
│   ├── PageLayout.tsx
│   └── PrivateRoute.tsx
├── crypto/        # encryption logic
│   ├── encryption.ts
│   ├── keyStore.ts
│   └── masterKey.ts
├── hooks/
│   └── useAuth.ts
├── pages/
│   ├── LoginPage.tsx
│   ├── RegisterPage.tsx
│   ├── MasterKeyPage.tsx
│   ├── DashboardPage.tsx
│   ├── BackupPage.tsx
│   └── RestorePage.tsx
└── utils/
    └── errorHandler.ts