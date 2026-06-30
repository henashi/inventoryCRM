import request from './request'
import type { PermissionDefDTO, RoleDTO, UserDTO } from '@/types'

export interface RoleCreateParams {
  name: string
  displayName?: string
  description?: string
  sortOrder?: number
}

export interface RoleUpdateParams {
  displayName?: string
  description?: string
  sortOrder?: number
  status?: string
}

export const adminApi = {
  // ===== 角色管理 =====

  /** 获取所有角色列表 */
  listRoles() {
    return request.get<RoleDTO[]>('/admin/roles')
  },

  /** 根据 ID 查询角色 */
  getRole(id: number) {
    return request.get<RoleDTO>(`/admin/roles/${id}`)
  },

  /** 创建角色 */
  createRole(params: RoleCreateParams) {
    return request.post<RoleDTO>('/admin/roles', null, { params })
  },

  /** 更新角色 */
  updateRole(id: number, params: RoleUpdateParams) {
    return request.put<RoleDTO>(`/admin/roles/${id}`, null, { params })
  },

  /** 删除角色 */
  deleteRole(id: number) {
    return request.delete<void>(`/admin/roles/${id}`)
  },

  // ===== 用户管理 =====

  /** 分页查询用户列表 */
  listUsers(keyword?: string, page = 0, size = 10) {
    return request.get<{ content: UserDTO[]; totalElements: number; totalPages: number }>(
      '/users',
      {
        params: { keyword, page, size },
      },
    )
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
  updateUser(
    id: number,
    data: { username: string; password?: string; role: string; remark?: string },
  ) {
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

  /** 查询指定角色的权限映射（合并默认值 + 角色覆盖） */
  getPermissions(role: string) {
    return request.get<Record<string, boolean>>(`/admin/permissions/${role}`)
  },

  /** 更新指定角色的权限映射 */
  updatePermissions(role: string, permissions: Record<string, boolean>) {
    return request.put<void>(`/admin/permissions/${role}`, permissions)
  },

  /** 获取所有权限定义 */
  getPermissionDefinitions() {
    return request.get<Array<{ key: string; name: string; module: string; moduleName: string; type: string; defaultRoles: string }>>(
      '/admin/permissions/list-definitions',
    )
  },

  // ===== 权限定义管理 =====

  /** 获取所有权限定义（管理用） */
  listPermissionDefs() {
    return request.get<PermissionDefDTO[]>('/admin/permission-defs')
  },

  /** 创建权限定义 */
  createPermissionDef(data: {
    key: string
    name: string
    module?: string
    moduleName?: string
    type?: string
    defaultRoles?: string
    description?: string
  }) {
    return request.post<PermissionDefDTO>('/admin/permission-defs', null, { params: data })
  },

  /** 更新权限定义 */
  updatePermissionDef(id: number, data: {
    name?: string
    moduleName?: string
    type?: string
    defaultRoles?: string
    description?: string
  }) {
    return request.put<PermissionDefDTO>(`/admin/permission-defs/${id}`, null, { params: data })
  },

  /** 删除权限定义 */
  deletePermissionDef(id: number) {
    return request.delete<void>(`/admin/permission-defs/${id}`)
  },

  /** 查询指定用户的权限覆盖 */
  getUserPermissions(userId: number) {
    return request.get<Record<string, boolean>>(`/admin/users/${userId}/permissions`)
  },

  /** 更新指定用户的权限覆盖 */
  updateUserPermissions(userId: number, permissions: Record<string, boolean>) {
    return request.put<void>(`/admin/users/${userId}/permissions`, permissions)
  },
}
