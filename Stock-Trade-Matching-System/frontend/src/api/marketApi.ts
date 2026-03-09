import { http } from '@/api/http'
import type { RecentExecution } from '@/types'

export async function getRecentExecutions(): Promise<RecentExecution[]> {
  const { data } = await http.get<RecentExecution[]>('/market/recent-executions')
  return data
}
