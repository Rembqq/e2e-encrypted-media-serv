import api from './axios'

export interface SnapshotFile {
  blobId: string
  path: string
  size: number
  modifiedAt: string
}

export interface SnapshotCreateRequest {
  name: string
  description?: string
  files: SnapshotFile[]
}

export interface Snapshot {
  id: number
  name: string
  description?: string
  createdAt: string
  totalSize: number
  fileCount: number
}

export interface SnapshotDetail {
  id: number
  name: string
  description?: string
  createdAt: string
  totalSize: number
  fileCount: number
  files: SnapshotFile[]
}

export const getSnapshots = async (): Promise<Snapshot[]> => {
  const response = await api.get<Snapshot[]>('/snapshots')
  return response.data
}

export const getSnapshot = async (id: number): Promise<SnapshotDetail> => {
  const response = await api.get<SnapshotDetail>(`/snapshots/${id}`)
  return response.data
}

export const createSnapshot = async (
  request: SnapshotCreateRequest
): Promise<Snapshot> => {
  const response = await api.post<Snapshot>('/snapshots', request)
  return response.data
}

export const deleteSnapshot = async (id: number): Promise<void> => {
  await api.delete(`/snapshots/${id}`)
}
