import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import JSZip from 'jszip'
import { getSnapshot } from '@/api/snapshots'
import type { SnapshotDetail, SnapshotFile } from '@/api/snapshots'
import { downloadBlob } from '@/api/blobs'
import { decryptFile } from '@/crypto/encryption'
import { getMasterKey } from '@/crypto/keyStore'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import toast from 'react-hot-toast'

export default function RestorePage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [snapshot, setSnapshot] = useState<SnapshotDetail | null>(null)
  const [selected, setSelected] = useState<Set<string>>(new Set())
  const [loading, setLoading] = useState(true)
  const [restoring, setRestoring] = useState(false)
  const [progress, setProgress] = useState(0)

  useEffect(() => {
    if (!id) return
    getSnapshot(Number(id))
      .then(s => {
        setSnapshot(s)
        setSelected(new Set(s.files.map(f => f.blobId)))
      })
      .catch(() => toast.error('Failed to load snapshot'))
      .finally(() => setLoading(false))
  }, [id])

  const toggleFile = (blobId: string) => {
    setSelected(prev => {
      const next = new Set(prev)
      next.has(blobId) ? next.delete(blobId) : next.add(blobId)
      return next
    })
  }

  const handleRestore = async () => {
    const key = getMasterKey()
    if (!key) {
      toast.error('Master key not found, please re-enter your password')
      return
    }
    if (!snapshot || selected.size === 0) {
      toast.error('Please select at least one file')
      return
    }

    setRestoring(true)
    setProgress(0)

    const zip = new JSZip()
    const filesToRestore: SnapshotFile[] = snapshot.files.filter(f => selected.has(f.blobId))

    try {
      for (let i = 0; i < filesToRestore.length; i++) {
        const file: SnapshotFile = filesToRestore[i]

        const { data, nonce } = await downloadBlob(file.blobId)
        const decrypted = await decryptFile(data, key, nonce)

        zip.file(file.path, decrypted)
        setProgress(i + 1)
      }

      const blob = await zip.generateAsync({ type: 'blob' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `${snapshot.name}-restore.zip`
      a.click()
      URL.revokeObjectURL(url)

      toast.success('Restore complete!')
    } catch {
      toast.error('Restore failed, please try again')
    } finally {
      setRestoring(false)
    }
  }

  if (loading) return <div className="p-6">Loading...</div>
  if (!snapshot) return <div className="p-6">Snapshot not found</div>

  return (
    <div className="min-h-screen p-6">
      <div className="flex items-center justify-between mb-8">
        <h1 className="text-2xl font-bold">Restore Snapshot</h1>
        <Button variant="outline" onClick={() => navigate('/dashboard')}>
          Back
        </Button>
      </div>

      <div className="flex flex-col gap-6 max-w-xl">
        <Card>
          <CardHeader>
            <CardTitle>{snapshot.name}</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-gray-500 mb-4">
              {snapshot.files.length} files —{' '}
              {(snapshot.totalSize / 1024).toFixed(1)} KB total
            </p>
            <ul className="flex flex-col gap-2">
              {snapshot.files.map((file: SnapshotFile) => (
                <li key={file.blobId} className="flex items-center gap-3 text-sm">
                  <input
                    type="checkbox"
                    checked={selected.has(file.blobId)}
                    onChange={() => toggleFile(file.blobId)}
                  />
                  <span className="truncate">{file.path}</span>
                  <span className="text-gray-400 shrink-0 ml-auto">
                    {(file.size / 1024).toFixed(1)} KB
                  </span>
                </li>
              ))}
            </ul>
          </CardContent>
        </Card>

        {restoring && (
          <p className="text-sm text-gray-500">
            Decrypting... {progress} / {selected.size}
          </p>
        )}

        <Button
          onClick={handleRestore}
          disabled={restoring || selected.size === 0}
        >
          {restoring ? 'Restoring...' : `Restore ${selected.size} file(s)`}
        </Button>
      </div>
    </div>
  )
}