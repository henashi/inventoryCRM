export interface User {
  id?: number
  username: string
  realName: string
  email: string
  role: 'USER' | 'MANAGER' | 'ADMIN'
  status: 0 | 1
  lastLoginAt?: string
  createdAt?: string
  remark?: string
}

export interface LoginRequest {
  username: string
  password: string
  rememberMe?: boolean
}

export interface LoginResponse {
  token: string
  refreshToken?: string
  access_token?: string
  accessToken?: string
  refresh_token?: string
  user: User
  expiresIn: number
  expires_in?: number
  tokenType: string
  token_type?: string
  data?: {
    user?: User
  }
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

export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
}

export interface UpdateProfileRequest {
  username?: string
  realName?: string
  email?: string
}
