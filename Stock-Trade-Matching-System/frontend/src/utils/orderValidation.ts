import type { OrderRequest } from '@/types'

export function isValidSecurityId(securityId: string): boolean {
  return /^\d{6}$/.test(securityId)
}

export function validateOrderRequest(payload: OrderRequest): string | null {
  if (!isValidSecurityId(payload.securityId)) {
    return '证券代码必须是 6 位数字'
  }
  if (payload.qty <= 0) {
    return '数量必须大于 0'
  }
  if (payload.price <= 0) {
    return '价格必须大于 0'
  }
  return null
}
