import request from './request'
import type { UserDTO } from '@/types'

export const adminApi = {
  /** 分页查询用户列表 */
  listUsers(keyword?: string, page = 0, size = 10) {
    return request.get<{ content: UserDTO[]; totalElements: number; totalPages: number }>('/users', {
      params: { keyword, page, size },
    })
  },

  /** 根据 ID 查询用户 */
  getUser(id: number) {
    return request.get<UserDTO>(`/users/${id}`)
  },

  /** 创建用户 */
  createUser(data: { username: string; password: string; role: string; remark?: string }) {
    return request.post<UserDTO>('/users', data)
  },

  /** 更新用户 */
  updateUser(id: number, data: { username: string; password?: string; role: string; remark?: string }) {
    return request.put<UserDTO>(`/users/${id}`, data)
  },

  /** 删除用户 */
  deleteUser(id: number) {
    return request.delete<void>(`/users/${id}`)
  },

  /** 重置密码为 123456 */
  resetPassword(id: number) {
    return request.put<void>(`/users/${id}/reset-password`)
  },

  /** 切换启用/禁用 */
  toggleStatus(id: number) {
    return request.put<void>(`/users/${id}/status`)
  },
}
