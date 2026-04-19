import { useState, useCallback } from 'react'
import { useDropzone } from 'react-dropzone'
import { useNavigate } from 'react-router-dom'
import { getMasterKey } from '@/crypto/keyStore'
import { encryptFile, hashBuffer } from '@/crypto/encryption'
import { uploadBlob } from '@/api/blobs'
import { createSnapshot } from '@/api/snapshots'
import type { SnapshotFileRequest } from '@/api/snapshots'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import toast from 'react-hot-toast'

export default function BackupPage() {
  const [files, setFiles] = useState<File[]>([])
  const [loading, setLoading] = useState(false)
  const [progress, setProgress] = useState(0)
  const navigate = useNavigate()

  const onDrop = useCallback((accepted: File[]) => {
    setFiles(accepted)
    setProgress(0)
  }, [])

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    multiple: true,
  })

  const handleBackup = async () => {
    const key = getMasterKey()
    if (!key) {
      toast.error('Master key not found, please re-enter your password')
      return
    }
    if (files.length === 0) {
      toast.error('Please select files first')
      return
    }

    setLoading(true)
    setProgress(0)

    const snapshotFiles: SnapshotFileRequest[] = []

    try {
      for (let i = 0; i < files.length; i++) {
        const file = files[i]

        const { ciphertext, nonce } = await encryptFile(file, key)
        const hash = await hashBuffer(ciphertext)

        const result = await uploadBlob(
            ciphertext,
            nonce,
            file.name,
            hash,
            new Date(file.lastModified).toISOString()
          )
          
          snapshotFiles.push({
            blobId: String(result.blobId),  // конвертируем number → string
            path: file.name,
            size: ciphertext.byteLength,
            modifiedAt: new Date(file.lastModified).toISOString(),
          })

        setProgress(i + 1)
      }

      const snapshotName = `backup-${new Date().toISOString().slice(0, 10)}`
      await createSnapshot({ name: snapshotName, files: snapshotFiles })

      toast.success('Backup created successfully!')
      navigate('/dashboard')
    } catch {
      toast.error('Backup failed, please try again')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen p-6">
      <div className="flex items-center justify-between mb-8">
        <h1 className="text-2xl font-bold">Create Backup</h1>
        <Button variant="outline" onClick={() => navigate('/dashboard')}>
          Back
        </Button>
      </div>

      <div className="flex flex-col gap-6 max-w-xl">
        <Card>
          <CardContent className="pt-6">
            <div
              {...getRootProps()}
              className={`border-2 border-dashed rounded-lg p-10 text-center cursor-pointer transition-colors ${
                isDragActive
                  ? 'border-blue-500 bg-blue-50'
                  : 'border-gray-300 hover:border-gray-400'
              }`}
            >
              <input {...getInputProps()} />
              {isDragActive ? (
                <p className="text-blue-500">Drop files here...</p>
              ) : (
                <p className="text-gray-500">
                  Drag & drop files here, or click to select
                </p>
              )}
            </div>
          </CardContent>
        </Card>

        {files.length > 0 && (
          <Card>
            <CardHeader>
              <CardTitle>Selected files ({files.length})</CardTitle>
            </CardHeader>
            <CardContent>
              <ul className="text-sm flex flex-col gap-1">
                {files.map((file, i) => (
                  <li key={i} className="flex justify-between">
                    <span className="truncate">{file.name}</span>
                    <span className="text-gray-400 ml-4 shrink-0">
                      {(file.size / 1024).toFixed(1)} KB
                    </span>
                  </li>
                ))}
              </ul>
            </CardContent>
          </Card>
        )}

        {loading && (
          <p className="text-sm text-gray-500">
            Encrypting and uploading... {progress} / {files.length}
          </p>
        )}

        <Button
          onClick={handleBackup}
          disabled={loading || files.length === 0}
        >
          {loading ? 'Uploading...' : 'Start Backup'}
        </Button>
      </div>
    </div>
  )
}