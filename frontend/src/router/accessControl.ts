export type AppRole = 'ADMIN' | 'MANAGER' | 'USER'
export type FeatureKey =
  | 'dashboard'
  | 'customers'
  | 'products'
  | 'inventory'
  | 'gifts'
  | 'gift-logs'
  | 'data-dicts'
  | 'admin'
  | 'account'
  | 'ai'
  | 'operation-logs'
  | 'roles'
  | 'permission-defs'

const defaultHomePath = '/dashboard'
const giftCatalogManagers: AppRole[] = ['ADMIN', 'MANAGER']
const destructiveAdmins: AppRole[] = ['ADMIN']

const featureRoles: Record<FeatureKey, AppRole[]> = {
  dashboard: ['ADMIN', 'MANAGER', 'USER'],
  'roles': ['ADMIN'],
  'permission-defs': ['ADMIN'],
  customers: ['ADMIN', 'MANAGER', 'USER'],
  products: ['ADMIN', 'MANAGER', 'USER'],
  inventory: ['ADMIN', 'MANAGER', 'USER'],
  gifts: ['ADMIN', 'MANAGER', 'USER'],
  'gift-logs': ['ADMIN', 'MANAGER', 'USER'],
  'data-dicts': ['ADMIN'],
  admin: ['ADMIN'],
  account: ['ADMIN', 'MANAGER', 'USER'],
  ai: ['ADMIN', 'MANAGER'],
  'operation-logs': ['ADMIN', 'MANAGER', 'USER'],
}

const quickActionFeatures = {
  addCustomer: 'customers',
  addInventory: 'inventory',
  createOrder: null,
  distributeGift: 'gift-logs',
  exportReport: 'dashboard',
  systemSettings: 'data-dicts',
} as const

const statFeatureMap = {
  customers: 'customers',
  products: 'products',
  inventory: 'inventory',
  'gift-logs': 'gift-logs',
  gifts: 'gifts',
  dataDicts: 'data-dicts',
} as const

export const normalizeRole = (role?: string | null): AppRole => {
  if (role === 'ADMIN' || role === 'MANAGER' || role === 'USER') {
    return role
  }

  return 'USER'
}

export const canAccessFeature = (role: string | null | undefined, feature: FeatureKey) => {
  const normalizedRole = normalizeRole(role)
  return featureRoles[feature].includes(normalizedRole)
}

export const resolveHomePath = (role?: string | null) => {
  const normalizedRole = normalizeRole(role)

  if (canAccessFeature(normalizedRole, 'dashboard')) {
    return defaultHomePath
  }

  return defaultHomePath
}

export const getFeatureRoles = (feature: FeatureKey) => featureRoles[feature]

export const canManageGiftCatalog = (role?: string | null) =>
  giftCatalogManagers.includes(normalizeRole(role))
export const canDeleteGift = (role?: string | null) =>
  destructiveAdmins.includes(normalizeRole(role))
export const canDeleteGiftLog = (role?: string | null) =>
  destructiveAdmins.includes(normalizeRole(role))

export const filterQuickActions = <T extends { action: keyof typeof quickActionFeatures }>(
  items: readonly T[],
  role?: string | null,
) => {
  return items.filter((item) => {
    const feature = quickActionFeatures[item.action]

    if (!feature) {
      return false
    }

    return canAccessFeature(role, feature)
  })
}

export const filterDashboardStats = <T extends { type: keyof typeof statFeatureMap }>(
  items: readonly T[],
  role?: string | null,
) => {
  return items.filter((item) => canAccessFeature(role, statFeatureMap[item.type]))
}

export const shouldShowCustomerSection = (role?: string | null) =>
  canAccessFeature(role, 'customers')
export const shouldShowInventorySection = (role?: string | null) =>
  canAccessFeature(role, 'inventory')
