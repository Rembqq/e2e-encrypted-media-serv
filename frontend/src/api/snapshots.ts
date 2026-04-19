import api from './axios'

export interface SnapshotFileRequest {
  path: string
  blobId: string    // сервер ожидает String
  size: number
  modifiedAt: string
}

export interface SnapshotCreateRequest {
  name: string
  description?: string
  files: SnapshotFileRequest[]
}

export interface Snapshot {
  id: number
  name: string
  description?: string
  createdAt: string
  totalSize: number
  fileCount: number
}

export const getSnapshots = async (): Promise<Snapshot[]> => {
  const response = await api.get<Snapshot[]>('/snapshots')
  return response.data
}

export const createSnapshot = async (
  request: SnapshotCreateRequest
): Promise<Snapshot> => {
  const response = await api.post<Snapshot>('/snapshots', request)
  return response.data
}