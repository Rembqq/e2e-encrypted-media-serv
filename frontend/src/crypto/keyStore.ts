let masterKey: CryptoKey | null = null

export function setMasterKey(key: CryptoKey): void {
  masterKey = key
}

export function getMasterKey(): CryptoKey | null {
  return masterKey
}

export function clearMasterKey(): void {
  masterKey = null
}

export function hasMasterKey(): boolean {
  return masterKey !== null
}