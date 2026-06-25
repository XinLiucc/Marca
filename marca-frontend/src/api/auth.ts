import { http } from './index'

export interface RegisterPayload {
  email: string
  password: string
  nickname?: string
}

export interface LoginPayload {
  email: string
  password: string
}

export interface LoginResponse {
  token: string
  nickname: string
}

export interface UserResponse {
  id: number
  email: string
  nickname: string
}

export const authApi = {
  register(payload: RegisterPayload) {
    return http.post<UserResponse>('/api/auth/register', payload).then((r) => r.data)
  },
  login(payload: LoginPayload) {
    return http.post<LoginResponse>('/api/auth/login', payload).then((r) => r.data)
  },
}
