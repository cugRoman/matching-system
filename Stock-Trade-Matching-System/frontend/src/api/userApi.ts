import { http } from '@/api/http'
import type { UserStockActivity } from '@/types'

export async function getMyStockActivity(params?: {
  market?: string
  securityId?: string
}): Promise<UserStockActivity[]> {
  const { data } = await http.get<UserStockActivity[]>('/me/stocks/activity', { params })
  return data
}
