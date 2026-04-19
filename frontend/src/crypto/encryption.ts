export async function encryptFile(
    file: File,
    key: CryptoKey
  ): Promise<{ ciphertext: ArrayBuffer; nonce: Uint8Array<ArrayBuffer> }> {
    const nonce = crypto.getRandomValues(new Uint8Array(12))
    const fileBuffer = await file.arrayBuffer()
  
    const ciphertext = await crypto.subtle.encrypt(
      { name: 'AES-GCM', iv: nonce },
      key,
      fileBuffer
    )
  
    return { ciphertext, nonce }
  }
  
  export async function decryptFile(
    ciphertext: ArrayBuffer,
    key: CryptoKey,
    nonce: Uint8Array<ArrayBuffer>
  ): Promise<ArrayBuffer> {
    return crypto.subtle.decrypt(
      { name: 'AES-GCM', iv: nonce },
      key,
      ciphertext
    )
  }
  
  export async function hashBuffer(buffer: ArrayBuffer): Promise<string> {
    const hashBuffer = await crypto.subtle.digest('SHA-256', buffer)
    const hashArray = Array.from(new Uint8Array(hashBuffer))
    return hashArray.map(b => b.toString(16).padStart(2, '0')).join('')
  }