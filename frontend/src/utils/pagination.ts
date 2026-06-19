type TablePaginationLike = {
  current?: number
  pageSize?: number
}

const DEFAULT_PAGE_SIZE = 10

export const toUiPage = (serverPage = 0) => serverPage + 1

export const toServerPage = (uiPage = 1) => Math.max(uiPage - 1, 0)

export const buildServerPageParams = (
  pagination?: TablePaginationLike,
  defaultPageSize = DEFAULT_PAGE_SIZE,
) => ({
  page: toServerPage(pagination?.current ?? 1),
  size: pagination?.pageSize ?? defaultPageSize,
})
