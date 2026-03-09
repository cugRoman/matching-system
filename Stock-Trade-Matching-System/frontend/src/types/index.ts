export type MarketCode = 'XSHG' | 'XSHE' | 'BJSE'
export type OrderSide = 'B' | 'S'

export interface AuthSession {
  token: string
  loginId: string
  shareHolderId: string
  role: string
  expiresAtEpochMs: number
}

export interface LoginRequest {
  loginId: string
  password: string
}

export interface RegisterRequest {
  loginId: string
  password: string
}

export interface OrderRequest {
  market: MarketCode
  securityId: string
  side: OrderSide
  qty: number
  price: number
  tradeDate?: string
}

export interface OrderSubmitResult {
  clOrderId: string
  resultType: 'ACCEPTED' | 'REJECTED_INVALID' | 'REJECTED_WASH'
  message: string
}

export interface UserOrder {
  id: number
  clOrderId: string
  market: MarketCode
  securityId: string
  side: OrderSide
  qty: number
  price: number
  shareHolderId: string
  tradeDate: string
  status: string
  sentToExchange: boolean
  qtyFilled: number
  qtyRemaining: number
  washTrade: boolean
  recvTime: string
  updatedAt: string
}

export interface RecentExecution {
  market: MarketCode
  securityId: string
  execId: string
  execQty: number
  execPrice: number
  executedAt: string
  executionCount: number
}

export interface UserOrderSummary {
  clOrderId: string
  market: MarketCode
  securityId: string
  side: OrderSide
  qty: number
  price: number
  status: string
  qtyFilled: number
  qtyRemaining: number
  recvTime: string
  updatedAt: string
}

export interface UserReportSummary {
  reportId: string
  type: string
  refClOrdId: string
  ts: string
  qty: number | null
  price: number | null
  rejectCode: number | null
  rejectText: string | null
  execId: string | null
  execQty: number | null
  execPrice: number | null
  execSource: string | null
  cancelText: string | null
}

export interface UserStockActivity {
  market: MarketCode
  securityId: string
  totalOrderQty: number
  totalExecQty: number
  orders: UserOrderSummary[]
  reports: UserReportSummary[]
}

export interface MyOrdersQuery {
  market?: string
  securityId?: string
  status?: string
}

export interface OrderExecutionItem {
  execId: string
  execQty: number
  execPrice: number
  execType: string
  counterOrderId: string | null
  executedAt: string
}

export interface OrderExecutionDetail {
  clOrderId: string
  market: MarketCode
  securityId: string
  side: OrderSide
  qty: number
  qtyFilled: number
  qtyRemaining: number
  status: string
  executionCount: number
  totalExecutedQty: number
  avgExecutedPrice: number
  lastExecutedAt: string | null
  executions: OrderExecutionItem[]
}
