import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { deriveMasterKey } from '@/crypto/masterKey'
import { setMasterKey } from '@/crypto/keyStore'
import { generatePassword } from '@/crypto/generatePassword'
import { useAuth } from '@/hooks/useAuth'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import toast from 'react-hot-toast'

export default function MasterKeyPage() {
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [showSchema, setShowSchema] = useState(false)
  const navigate = useNavigate()
  const { getUsername } = useAuth()

  const handleSubmit = async () => {
    if (!password) {
      toast.error('Please enter your master password')
      return
    }
    setLoading(true)
    try {
      const username = getUsername() ?? ''
      const key = await deriveMasterKey(password, username)
      setMasterKey(key)
      navigate('/dashboard')
    } catch {
      toast.error('Failed to derive key')
    } finally {
      setLoading(false)
    }
  }

  const handleGenerate = () => {
    const pwd = generatePassword(24)
    setPassword(pwd)
    toast.success('Strong password generated — save it somewhere safe!')
  }

  return (
    <div className="min-h-screen flex flex-col items-center justify-center gap-6 px-4">
      <div className="text-center">
        <h1 className="text-3xl font-bold mb-2">Secure Evidence Storage</h1>
        <p className="text-gray-500 text-sm">
          Enter your master password to access the system
        </p>
      </div>

      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>Master Password</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          <div className="bg-amber-50 border border-amber-200 rounded p-3 text-xs text-amber-800">
            ⚠️ This password is never sent to the server. If you forget it, your files cannot be recovered.
          </div>

          <div className="flex gap-2">
            <Input
              type="text"
              placeholder="Master password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && handleSubmit()}
              disabled={loading}
              className="font-mono text-sm"
            />
            <Button
              variant="outline"
              onClick={handleGenerate}
              disabled={loading}
              className="shrink-0"
            >
              Generate
            </Button>
          </div>

          <Button onClick={handleSubmit} disabled={loading || !password}>
            {loading ? 'Deriving key...' : 'Unlock'}
          </Button>

          <button
            className="text-xs text-gray-400 underline text-left"
            onClick={() => setShowSchema(!showSchema)}
          >
            {showSchema ? 'Hide' : 'How does key derivation work?'}
          </button>

          {showSchema && (
            <div className="bg-gray-50 border rounded p-3 text-xs font-mono flex flex-col gap-1 text-gray-700">
              <p>Your password</p>
              <p className="text-gray-400 pl-4">↓ PBKDF2 (SHA-256, 250 000 iterations)</p>
              <p className="text-gray-400 pl-4">↓ salt = your username</p>
              <p>AES-256-GCM encryption key (256 bits)</p>
              <p className="text-gray-400 pl-4">↓ stored only in browser memory</p>
              <p className="text-gray-400 pl-4">↓ lost on tab close</p>
              <p>Files encrypted before upload</p>
              <p className="text-gray-400 pl-4">↓ server stores only ciphertext</p>
              <p>Server never sees plaintext ✓</p>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}