import { http } from '@/api/http'
import type {
  MyOrdersQuery,
  OrderExecutionDetail,
  OrderRequest,
  OrderSubmitResult,
  UserOrder,
} from '@/types'

export async function submitOrder(payload: OrderRequest): Promise<OrderSubmitResult> {
  const { data } = await http.post<OrderSubmitResult>('/orders', payload)
  return data
}

export async function getMyOrders(params?: MyOrdersQuery): Promise<UserOrder[]> {
  const { data } = await http.get<UserOrder[]>('/me/orders', { params })
  return data
}

export async function getOrderExecutions(clOrderId: string): Promise<OrderExecutionDetail> {
  const { data } = await http.get<OrderExecutionDetail>(`/me/orders/${clOrderId}/executions`)
  return data
}
