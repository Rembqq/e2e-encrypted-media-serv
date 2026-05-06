import api from './axios'

export interface BlobResponse {
  blobId: string
  storageKey: string
  deduped: boolean
}

export interface BlobDownloadResult {
  data: ArrayBuffer
  nonce: Uint8Array<ArrayBuffer>
  filename: string
}

export async function uploadBlob(
  encryptedData: ArrayBuffer,
  nonce: Uint8Array<ArrayBuffer>,
  originalName: string,
  cipherHash: string,
  modifiedAt: string
): Promise<BlobResponse> {
  const formData = new FormData()

  const encryptedBlob = new Blob([encryptedData], { type: 'application/octet-stream' })
  formData.append('blob', encryptedBlob, originalName)

  const metadata = {
    clientId: crypto.randomUUID(),
    originalFilename: originalName,
    size: encryptedBlob.size,
    modifiedAt,
    cipherHash,
    nonce: btoa(String.fromCharCode(...nonce)),
  }

  formData.append('metadata', JSON.stringify(metadata))

  const response = await api.post<BlobResponse>('/blobs', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })

  return response.data
}

export async function downloadBlob(blobId: string): Promise<BlobDownloadResult> {
  const response = await api.get(`/blobs/${blobId}`, {
    responseType: 'arraybuffer',
  })

  const nonce = Uint8Array.from(
    atob(response.headers['x-nonce']),
    c => c.charCodeAt(0)
  ) as Uint8Array<ArrayBuffer>

  const filename = response.headers['x-filename'] ?? 'file'

  return {
    data: response.data as ArrayBuffer,
    nonce,
    filename,
  }
}