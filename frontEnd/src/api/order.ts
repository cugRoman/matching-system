import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

export interface OrderRequest {
  clOrderId: string
  market: string
  securityId: string
  side: 'B' | 'S'
  qty: number
  price: number
  shareHolderId: string
  timestamp?: number
}

export interface Order {
  clOrderId: string
  market: string
  securityId: string
  side: string
  qty: number
  price: number
  shareHolderId: string
  timestamp: number
  status: number
}

export interface TradeSuccess {
  execId: string
  securityId: string
  buyOrderId: string
  sellOrderId: string
  buyShareHolderId: string
  sellShareHolderId: string
  execQty: number
  execPrice: number
  execTime: number
}

export interface TradeIllegal {
  clOrderId: string
  shareHolderId: string
  securityId: string
  side: string
  price: number
  qty: number
  rejectCode: number
  rejectTime: number
  status: number
}

export interface AllData {
  buyRequests: Order[]
  sellRequests: Order[]
  exchangeBuys: Order[]
  exchangeSells: Order[]
  tradeSuccesses: TradeSuccess[]
  tradeIllegals: TradeIllegal[]
}

export interface SimulateData extends AllData {
  meanPrice: number
  currentSecurityId: string
}

export const orderApi = {
  // 撮合演示列表（内存数据）
  getAll: () => api.get<AllData>('/orders/all'),
  // 统计分析页面（数据库数据）
  getStats: () => api.get<AllData>('/orders/stats'),
  addOrder: (order: OrderRequest) => api.post('/orders', order),
  addOrderToMemory: (order: OrderRequest) => api.post('/orders/memory', order),
  addOrders: (orders: OrderRequest[]) => api.post('/orders/batch', orders),
  addOrdersToMemory: (orders: OrderRequest[]) => api.post('/orders/batch-memory', orders),
  match: () => api.post('/orders/match'),
  clear: () => api.post('/orders/clear'),
  clearMemory: () => api.post('/orders/clear-memory'),
  clearDatabase: () => api.post('/orders/clear-database'),
  saveMemoryToDatabase: () => api.post('/orders/save-memory'),
  setAutoSimulate: (enabled: boolean) => api.post('/orders/auto-simulate?enabled=' + enabled),
  getAutoSimulate: () => api.get<{ autoSimulate: boolean }>('/orders/auto-simulate')
}

export const simulateApi = {
  getAll: () => api.get<SimulateData>('/simulate/all'),
  match: () => api.post('/simulate/match'),
  clear: () => api.post('/simulate/clear'),
  setAutoSimulate: (enabled: boolean) => api.post('/simulate/auto-simulate?enabled=' + enabled),
  getAutoSimulate: () => api.get<{ autoSimulate: boolean }>('/simulate/auto-simulate'),
  setSecurityId: (securityId: string) => api.post('/simulate/security/' + securityId),
  getSecurityId: () => api.get<{ securityId: string; meanPrice: number }>('/simulate/security')
}

export default api
