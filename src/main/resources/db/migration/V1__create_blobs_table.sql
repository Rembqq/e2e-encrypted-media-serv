CREATE TABLE blobs (
   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   hash TEXT NOT NULL UNIQUE,
   storage_key TEXT NOT NULL,
   size BIGINT NOT NULL,
   refcount INTEGER DEFAULT 1,
   metadata JSONB,
   created_at TIMESTAMPTZ DEFAULT now()
);
