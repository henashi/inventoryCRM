// frontend/src/api/auth.ts
import request from './request'
import type { 
  LoginRequest, 
  LoginResponse, 
  RegisterRequest, 
  User,
  RefreshTokenRequest
} from '@/types/auth'

export const authApi = {
  // 用户登录
  login: (data: LoginRequest) => 
    request.post<LoginResponse>('/auth/login', data),
  
  // 用户注册
  register: (data: RegisterRequest) => 
    request.post<User>('/auth/register', data),
  
  // 用户登出
  logout: () => 
    request.post<void>('/auth/logout'),
  
  // 获取当前用户信息
  getCurrentUser: () => 
    request.get<User>('/auth/me'),
  
  // 刷新访问令牌
  refreshToken: (data: RefreshTokenRequest) => 
    request.post<LoginResponse>('/auth/refresh-token', data),
  
  // 修改密码
  changePassword: (data: { oldPassword: string; newPassword: string }) =>
    request.post<void>('/auth/change-password', data),
  
  // 更新用户信息
  updateProfile: (data: Partial<User>) =>
    request.put<User>('/auth/profile', data)
}