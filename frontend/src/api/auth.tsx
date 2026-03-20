import api from './axios'

export interface AuthRequest {
  username: string
  password: string
}

export interface AuthResponse {
  token: string
}

export const register = async (data: AuthRequest): Promise<AuthResponse> => {
  const response = await api.post<AuthResponse>('/auth/register', data)
  return response.data
}

export const login = async (data: AuthRequest): Promise<AuthResponse> => {
  const response = await api.post<AuthResponse>('/auth/login', data)
  return response.data
}