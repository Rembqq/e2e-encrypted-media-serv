import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import JSZip from 'jszip'
import { getSnapshot } from '@/api/snapshots'
import type { SnapshotDetail, SnapshotFile } from '@/api/snapshots'
import { downloadBlob } from '@/api/blobs'
import { decryptFile, hashBuffer } from '@/crypto/encryption'
import { getMasterKey } from '@/crypto/keyStore'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { handleApiError } from '@/utils/errorHandler'
import toast from 'react-hot-toast'

interface FileStatus {
  path: string
  status: 'pending' | 'verified' | 'failed' | 'decrypting'
  hash?: string
}

export default function RestorePage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [snapshot, setSnapshot] = useState<SnapshotDetail | null>(null)
  const [selected, setSelected] = useState<Set<string>>(new Set())
  const [loading, setLoading] = useState(true)
  const [restoring, setRestoring] = useState(false)
  const [progress, setProgress] = useState(0)
  const [fileStatuses, setFileStatuses] = useState<FileStatus[]>([])

  useEffect(() => {
    if (!id) return
    getSnapshot(Number(id))
      .then(s => {
        setSnapshot(s)
        setSelected(new Set(s.files.map(f => f.blobId)))
      })
      .catch(() => toast.error('Failed to load evidence record'))
      .finally(() => setLoading(false))
  }, [id])

  const toggleFile = (blobId: string) => {
    setSelected(prev => {
      const next = new Set(prev)
      next.has(blobId) ? next.delete(blobId) : next.add(blobId)
      return next
    })
  }

  const updateStatus = (path: string, status: FileStatus['status'], hash?: string) => {
    setFileStatuses(prev => {
      const existing = prev.find(f => f.path === path)
      if (existing) {
        return prev.map(f => f.path === path ? { ...f, status, hash } : f)
      }
      return [...prev, { path, status, hash }]
    })
  }

  const getFileStatus = (path: string) =>
    fileStatuses.find(f => f.path === path)

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
    setFileStatuses([])

    const zip = new JSZip()
    const filesToRestore: SnapshotFile[] = snapshot.files.filter(f =>
      selected.has(f.blobId)
    )

    try {
      for (let i = 0; i < filesToRestore.length; i++) {
        const file: SnapshotFile = filesToRestore[i]
        updateStatus(file.path, 'decrypting')

        const { data, nonce, serverHash } = await downloadBlob(file.blobId)

        const decrypted = await decryptFile(data, key, nonce)

        const localHash = await hashBuffer(decrypted)
        if (serverHash && localHash !== serverHash) {
          updateStatus(file.path, 'failed', localHash)
          toast.error(`Integrity check failed for ${file.path}`)
          continue
        }

        updateStatus(file.path, 'verified', localHash)

        zip.file(file.path, decrypted)
        setProgress(i + 1)
      }

      const blob = await zip.generateAsync({ type: 'blob' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `${snapshot.name}-evidence.zip`
      a.click()
      URL.revokeObjectURL(url)

      toast.success('Evidence retrieved successfully!')
    } catch (error: unknown) {
      handleApiError(error, 'Restore failed — wrong master password or corrupted data')
    } finally {
      setRestoring(false)
    }
  }

  const progressPercent = selected.size > 0
    ? Math.round((progress / selected.size) * 100)
    : 0

  if (loading) {
    return (
      <div className="min-h-screen p-6 max-w-xl mx-auto">
        <div className="flex flex-col gap-3 mt-8">
          {[1, 2, 3].map(i => (
            <div key={i} className="h-8 bg-gray-100 rounded animate-pulse" />
          ))}
        </div>
      </div>
    )
  }

  if (!snapshot) {
    return (
      <div className="min-h-screen p-6 flex flex-col items-center justify-center">
        <p className="text-gray-500 mb-4">Evidence record not found</p>
        <Button variant="outline" onClick={() => navigate('/dashboard')}>
          Back to Dashboard
        </Button>
      </div>
    )
  }

  return (
    <div className="min-h-screen p-6">
      <div className="flex items-center justify-between mb-8">
        <h1 className="text-2xl font-bold">Retrieve Evidence</h1>
        <Button
          variant="outline"
          onClick={() => navigate('/dashboard')}
          disabled={restoring}
        >
          Back
        </Button>
      </div>

      <div className="flex flex-col gap-6 max-w-xl">
        <Card>
          <CardHeader>
            <CardTitle>{snapshot.name}</CardTitle>
          </CardHeader>
          <CardContent>
            {snapshot.description && (
              <p className="text-sm text-gray-500 mb-3">{snapshot.description}</p>
            )}
            <p className="text-sm text-gray-400 mb-4">
              {snapshot.files.length} files —{' '}
              {(snapshot.totalSize / 1024).toFixed(1)} KB —{' '}
              {new Date(snapshot.createdAt).toLocaleString()}
            </p>
            <ul className="flex flex-col gap-3">
              {snapshot.files.map((file: SnapshotFile) => {
                const status = getFileStatus(file.path)
                return (
                  <li key={file.blobId} className="flex flex-col gap-1">
                    <div className="flex items-center gap-3 text-sm">
                      <input
                        type="checkbox"
                        checked={selected.has(file.blobId)}
                        onChange={() => toggleFile(file.blobId)}
                        disabled={restoring}
                      />
                      <span className="truncate">{file.path}</span>
                      <span className="text-gray-400 shrink-0 ml-auto">
                        {(file.size / 1024).toFixed(1)} KB
                      </span>
                      {status?.status === 'decrypting' && (
                        <span className="text-blue-500 text-xs shrink-0">⏳</span>
                      )}
                      {status?.status === 'verified' && (
                        <span className="text-green-500 text-xs shrink-0">✅</span>
                      )}
                      {status?.status === 'failed' && (
                        <span className="text-red-500 text-xs shrink-0">❌</span>
                      )}
                    </div>
                    {status?.hash && status.status === 'verified' && (
                      <div className="pl-6 text-xs text-gray-400 font-mono truncate">
                        SHA-256: {status.hash}
                      </div>
                    )}
                    {status?.status === 'failed' && (
                      <div className="pl-6 text-xs text-red-400">
                        Integrity check failed — file may be corrupted
                      </div>
                    )}
                  </li>
                )
              })}
            </ul>
          </CardContent>
        </Card>

        {restoring && (
          <div className="flex flex-col gap-2">
            <div className="flex justify-between text-sm text-gray-500">
              <span>Verifying and decrypting...</span>
              <span>{progress} / {selected.size}</span>
            </div>
            <div className="w-full bg-gray-200 rounded-full h-2">
              <div
                className="bg-blue-500 h-2 rounded-full transition-all duration-300"
                style={{ width: `${progressPercent}%` }}
              />
            </div>
          </div>
        )}

        <Button
          onClick={handleRestore}
          disabled={restoring || selected.size === 0}
          className="w-full"
        >
          {restoring
            ? `Verifying... ${progressPercent}%`
            : `Retrieve ${selected.size} file(s)`}
        </Button>
      </div>
    </div>
  )
}