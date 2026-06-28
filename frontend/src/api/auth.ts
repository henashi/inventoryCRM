import request from './request'
import type {
  ChangePasswordRequest,
  LoginRequest,
  LoginResponse,
  RefreshTokenRequest,
  RegisterRequest,
  UpdateProfileRequest,
  User,
} from '@/types/auth'

export const authApi = {
  login: (data: LoginRequest) => request.post<LoginResponse>('/auth/login', data),

  register: (data: RegisterRequest) => request.post<User>('/auth/register', data),

  logout: () => request.post<void>('/auth/logout'),

  getCurrentUser: () => request.get<User>('/auth/me'),

  refreshToken: (data: RefreshTokenRequest) =>
    request.post<LoginResponse>('/auth/refresh-token', data),

  changePassword: (data: ChangePasswordRequest) =>
    request.post<void>('/auth/change-password', data),

  updateProfile: (data: UpdateProfileRequest) => request.put<User>('/auth/profile', data),
}
