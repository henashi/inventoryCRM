// frontend/src/types/auth.ts
export interface User {
  id?: number
  username: string
  realName: string
  email: string
  role: 'USER' | 'MANAGER' | 'ADMIN'
  status: 0 | 1
  lastLoginAt?: string
  createdAt?: string
}

export interface LoginRequest {
  username: string
  password: string
  rememberMe?: boolean
}

export interface LoginResponse {
  token: string
  refreshToken?: string
  user: User
  expiresIn: number
  tokenType: string
}

export interface RegisterRequest {
  username: string
  password: string
  realName: string
  email: string
  confirmPassword?: string
}

export interface RefreshTokenRequest {
  refreshToken: string
}