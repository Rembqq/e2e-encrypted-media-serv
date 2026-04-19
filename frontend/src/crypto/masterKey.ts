export async function deriveMasterKey(password: string, username: string): Promise<CryptoKey> {
    const enc = new TextEncoder()
  
    const keyMaterial = await crypto.subtle.importKey(
      'raw',
      enc.encode(password),
      { name: 'PBKDF2' },
      false,
      ['deriveKey']
    )
  
    return crypto.subtle.deriveKey(
      {
        name: 'PBKDF2',
        salt: enc.encode(username),
        iterations: 250000,
        hash: 'SHA-256',
      },
      keyMaterial,
      { name: 'AES-GCM', length: 256 },
      false,
      ['encrypt', 'decrypt']
    )
  }