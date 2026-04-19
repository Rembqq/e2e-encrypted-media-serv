import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { deriveMasterKey } from '@/crypto/masterKey'
import { setMasterKey } from '@/crypto/keyStore'
import { useAuth } from '@/hooks/useAuth'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import toast from 'react-hot-toast'

export default function MasterKeyPage() {
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
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

  return (
    <div className="min-h-screen flex items-center justify-center">
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle>Enter Master Password</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          <p className="text-sm text-gray-500">
            This password encrypts your files. It never leaves your device.
          </p>
          <Input
            type="password"
            placeholder="Master password"
            value={password}
            onChange={e => setPassword(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && handleSubmit()}
          />
          <Button onClick={handleSubmit} disabled={loading}>
            {loading ? 'Deriving key...' : 'Unlock'}
          </Button>
        </CardContent>
      </Card>
    </div>
  )
}