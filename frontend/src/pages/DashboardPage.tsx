import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getSnapshots } from '@/api/snapshots'
import type { Snapshot } from '@/api/snapshots'
import { useAuth } from '@/hooks/useAuth'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import toast from 'react-hot-toast'

export default function DashboardPage() {
  const { getUsername, logout } = useAuth()
  const [snapshots, setSnapshots] = useState<Snapshot[]>([])
  const [loading, setLoading] = useState(true)
  const navigate = useNavigate()

  useEffect(() => {
    getSnapshots()
      .then(setSnapshots)
      .catch(() => toast.error('Failed to load snapshots'))
      .finally(() => setLoading(false))
  }, [])

  return (
    <div className="min-h-screen p-6">
      <div className="flex items-center justify-between mb-8">
        <h1 className="text-2xl font-bold">E2E Backup</h1>
        <div className="flex items-center gap-4">
          <span className="text-sm text-gray-500">{getUsername()}</span>
          <Button variant="outline" onClick={logout}>
            Sign Out
          </Button>
        </div>
      </div>

      <div className="mb-6">
        <Button onClick={() => navigate('/backup')}>
          + Create Backup
        </Button>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Snapshots</CardTitle>
        </CardHeader>
        <CardContent>
          {loading && <p className="text-sm text-gray-500">Loading...</p>}

          {!loading && snapshots.length === 0 && (
            <p className="text-sm text-gray-500">No backups yet.</p>
          )}

          {!loading && snapshots.length > 0 && (
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left border-b">
                  <th className="pb-2">Name</th>
                  <th className="pb-2">Date</th>
                  <th className="pb-2">Files</th>
                  <th className="pb-2">Size</th>
                  <th className="pb-2"></th>
                </tr>
              </thead>
              <tbody>
                {snapshots.map(snapshot => (
                  <tr key={snapshot.id} className="border-b last:border-0">
                    <td className="py-2">{snapshot.name}</td>
                    <td className="py-2">
                      {new Date(snapshot.createdAt).toLocaleString()}
                    </td>
                    <td className="py-2">{snapshot.fileCount}</td>
                    <td className="py-2">
                      {(snapshot.totalSize / 1024).toFixed(1)} KB
                    </td>
                    <td className="py-2">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => navigate(`/restore/${snapshot.id}`)}
                      >
                        Restore
                      </Button>
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