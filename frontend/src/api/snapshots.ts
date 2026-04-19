import api from './axios'

export interface SnapshotFile {
  id: number
  path: string
  size: number
}

export interface Snapshot {
  id: number
  createdAt: string
  fileCount: number
  files?: SnapshotFile[]
}

export const getSnapshots = async (): Promise<Snapshot[]> => {
  const response = await api.get<Snapshot[]>('/snapshots')
  return response.data
}