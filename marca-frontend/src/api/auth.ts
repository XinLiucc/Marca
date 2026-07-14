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
  createdAt: string
}

export const authApi = {
  register(payload: RegisterPayload) {
    return http.post<UserResponse>('/api/auth/register', payload).then((r) => r.data)
  },
  login(payload: LoginPayload) {
    return http.post<LoginResponse>('/api/auth/login', payload).then((r) => r.data)
  },
  me() {
    return http.get<UserResponse>('/api/auth/me').then((r) => r.data)
  },
  updateProfile(nickname: string) {
    return http.patch<UserResponse>('/api/auth/me', { nickname }).then((r) => r.data)
  },
}
