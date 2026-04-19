import api from './axios'

export interface BlobResponse {
  blobId: number        // Long, не UUID
  storageKey: string
  deduped: boolean
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

  // поле называется "blob" — так требует сервер
  formData.append('blob', encryptedBlob, originalName)

  const metadata = {
    clientId: crypto.randomUUID(),
    originalFilename: originalName,
    size: encryptedBlob.size,   // размер зашифрованного файла
    modifiedAt: modifiedAt,
    cipherHash: cipherHash,
    nonce: btoa(String.fromCharCode(...nonce)),
  }

  formData.append('metadata', JSON.stringify(metadata))

  const response = await api.post<BlobResponse>('/blobs', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })

  return response.data
}