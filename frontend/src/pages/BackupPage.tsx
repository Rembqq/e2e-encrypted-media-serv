import { useState, useCallback } from 'react'
import { useDropzone } from 'react-dropzone'
import { useNavigate } from 'react-router-dom'
import { getMasterKey } from '@/crypto/keyStore'
import { encryptFile, hashBuffer } from '@/crypto/encryption'
import { uploadBlob } from '@/api/blobs'
import { createSnapshot } from '@/api/snapshots'
import type { SnapshotFile } from '@/api/snapshots'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { handleApiError } from '@/utils/errorHandler'
import toast from 'react-hot-toast'

export default function BackupPage() {
  const [files, setFiles] = useState<File[]>([])
  const [loading, setLoading] = useState(false)
  const [progress, setProgress] = useState(0)
  const [eventName, setEventName] = useState('')
  const [eventDescription, setEventDescription] = useState('')
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
    if (!eventName.trim()) {
      toast.error('Please enter event name')
      return
    }

    setLoading(true)
    setProgress(0)

    const snapshotFiles: SnapshotFile[] = []

    try {
      for (let i = 0; i < files.length; i++) {
        const file = files[i]

        const { ciphertext, nonce } = await encryptFile(file, key)
        const originalBuffer = await file.arrayBuffer()
        const hash = await hashBuffer(originalBuffer)

        const result = await uploadBlob(
          ciphertext,
          nonce,
          file.name,
          hash,
          new Date(file.lastModified).toISOString()
        )

        snapshotFiles.push({
          blobId: String(result.blobId),
          path: file.name,
          size: ciphertext.byteLength,
          modifiedAt: new Date(file.lastModified).toISOString(),
        })

        setProgress(i + 1)
      }

      await createSnapshot({
        name: eventName.trim(),
        description: eventDescription.trim() || undefined,
        files: snapshotFiles,
      })

      toast.success('Evidence documented successfully!')
      navigate('/dashboard')
    } catch (error: unknown) {
      handleApiError(error, 'Upload failed, please try again')
    } finally {
      setLoading(false)
    }
  }

  const progressPercent = files.length > 0
    ? Math.round((progress / files.length) * 100)
    : 0

  return (
    <div className="min-h-screen p-6">
      <div className="flex items-center justify-between mb-8">
        <h1 className="text-2xl font-bold">Document Evidence</h1>
        <Button variant="outline" onClick={() => navigate('/dashboard')} disabled={loading}>
          Back
        </Button>
      </div>

      <div className="flex flex-col gap-6 max-w-xl">

        {}
        <Card>
          <CardHeader>
            <CardTitle>Event Information</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-3">
            <div>
              <label className="text-sm font-medium mb-1 block">
                Event name <span className="text-red-500">*</span>
              </label>
              <Input
                placeholder="e.g. Bucha Atrocities 20.03.2022"
                value={eventName}
                onChange={e => setEventName(e.target.value)}
                disabled={loading}
              />
            </div>
            <div>
              <label className="text-sm font-medium mb-1 block">
                Description (optional)
              </label>
              <Input
                placeholder="Additional details about the event"
                value={eventDescription}
                onChange={e => setEventDescription(e.target.value)}
                disabled={loading}
              />
            </div>
          </CardContent>
        </Card>

        {}
        <Card>
          <CardHeader>
            <CardTitle>Evidence Files</CardTitle>
          </CardHeader>
          <CardContent>
            <div
              {...getRootProps()}
              className={`border-2 border-dashed rounded-lg p-10 text-center cursor-pointer transition-colors ${
                isDragActive
                  ? 'border-blue-500 bg-blue-50'
                  : 'border-gray-300 hover:border-gray-400'
              } ${loading ? 'pointer-events-none opacity-50' : ''}`}
            >
              <input {...getInputProps()} />
              {isDragActive ? (
                <p className="text-blue-500">Drop files here...</p>
              ) : (
                <p className="text-gray-500">
                  Drag & drop evidence files here, or click to select
                </p>
              )}
            </div>
          </CardContent>
        </Card>

        {}
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

        {}
        {loading && (
          <div className="flex flex-col gap-2">
            <div className="flex justify-between text-sm text-gray-500">
              <span>Encrypting and uploading evidence...</span>
              <span>{progress} / {files.length}</span>
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
          onClick={handleBackup}
          disabled={loading || files.length === 0 || !eventName.trim()}
          className="w-full"
        >
          {loading ? `Uploading... ${progressPercent}%` : 'Submit Evidence'}
        </Button>
      </div>
    </div>
  )
}