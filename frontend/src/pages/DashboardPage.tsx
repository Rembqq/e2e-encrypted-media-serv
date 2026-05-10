import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getSnapshots, deleteSnapshot } from '@/api/snapshots'
import type { Snapshot } from '@/api/snapshots'
import { useAuth } from '@/hooks/useAuth'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import toast from 'react-hot-toast'

export default function DashboardPage() {
  const { getUsername, logout } = useAuth()
  const [snapshots, setSnapshots] = useState<Snapshot[]>([])
  const [loading, setLoading] = useState(true)
  const [deletingId, setDeletingId] = useState<number | null>(null)
  const navigate = useNavigate()

  useEffect(() => {
    getSnapshots()
      .then(setSnapshots)
      .catch(() => toast.error('Failed to load evidence records'))
      .finally(() => setLoading(false))
  }, [])

  const handleDelete = async (id: number) => {
    if (!confirm('Are you sure you want to delete this evidence record?')) return
    setDeletingId(id)
    try {
      await deleteSnapshot(id)
      setSnapshots(prev => prev.filter(s => s.id !== id))
      toast.success('Evidence record deleted')
    } catch {
      toast.error('Failed to delete evidence record')
    } finally {
      setDeletingId(null)
    }
  }

  return (
    <div className="min-h-screen p-6 max-w-4xl mx-auto">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold">Evidence Storage</h1>
          <p className="text-sm text-gray-500">End-to-end encrypted document archive</p>
        </div>
        <div className="flex items-center gap-4">
          <span className="text-sm text-gray-500">{getUsername()}</span>
          <Button variant="outline" onClick={logout}>
            Sign Out
          </Button>
        </div>
      </div>

      <div className="mb-6">
        <Button onClick={() => navigate('/backup')}>
          + Add Evidence
        </Button>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Documented Events</CardTitle>
        </CardHeader>
        <CardContent>
          {loading && (
            <div className="flex flex-col gap-3">
              {[1, 2, 3].map(i => (
                <div key={i} className="h-8 bg-gray-100 rounded animate-pulse" />
              ))}
            </div>
          )}

          {!loading && snapshots.length === 0 && (
            <div className="text-center py-12">
              <p className="text-gray-400 mb-4">No evidence records yet</p>
              <Button onClick={() => navigate('/backup')}>
                Add first evidence
              </Button>
            </div>
          )}

          {!loading && snapshots.length > 0 && (
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left border-b">
                  <th className="pb-2">Event</th>
                  <th className="pb-2">Date</th>
                  <th className="pb-2">Files</th>
                  <th className="pb-2">Size</th>
                  <th className="pb-2"></th>
                </tr>
              </thead>
              <tbody>
                {snapshots.map(snapshot => (
                  <tr key={snapshot.id} className="border-b last:border-0">
                    <td className="py-2">
                      <div>{snapshot.name}</div>
                      {snapshot.description && (
                        <div className="text-xs text-gray-400">{snapshot.description}</div>
                      )}
                    </td>
                    <td className="py-2">
                      {new Date(snapshot.createdAt).toLocaleString()}
                    </td>
                    <td className="py-2">{snapshot.fileCount}</td>
                    <td className="py-2">
                      {(snapshot.totalSize / 1024).toFixed(1)} KB
                    </td>
                    <td className="py-2">
                      <div className="flex gap-2">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => navigate(`/restore/${snapshot.id}`)}
                          disabled={deletingId === snapshot.id}
                        >
                          Retrieve
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => handleDelete(snapshot.id)}
                          disabled={deletingId === snapshot.id}
                          className="text-red-500 hover:text-red-700 hover:border-red-300"
                        >
                          {deletingId === snapshot.id ? 'Deleting...' : 'Delete'}
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </CardContent>
      </Card>
    </div>
  )
}