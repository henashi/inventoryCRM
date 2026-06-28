type LogoutStoreLike = {
  logout: () => Promise<unknown> | unknown
}

type RouterLike = {
  push: (path: string) => Promise<unknown> | unknown
}

export const logoutAndNavigateToLogin = async (authStore: LogoutStoreLike, router: RouterLike) => {
  await authStore.logout()
  await router.push('/login')
}
