<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import * as echarts from 'echarts'
import type { MarketCode, OrderSide, OrderRequest } from '@/types'
import { Client, type IMessage } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

interface ExecPoint {
  time: number
  price: number
  securityId: string
}

interface WashAlert {
  clOrderId: string
  securityId?: string
  shareHolderId?: string
  rejectCode?: number
  rejectText?: string
  ts: number
}

interface SimOrder {
  id: string
  side: OrderSide
  price: number
  qty: number
  remaining: number
  securityId: string
  shareHolderId: string
  createdAt: number
  isWash: boolean
}

interface TradeRecord {
  time: number
  price: number
  qty: number
  securityId: string
  buyShareHolderId: string
  sellShareHolderId: string
}

interface WashPair {
  time: number
  securityId: string
  shareHolderId: string
  orderId1: string
  orderId2: string
}

const markets: MarketCode[] = ['XSHG', 'XSHE', 'BJSE']

const securityOptions = [
  { label: '600519 贵州茅台', value: '600519', meanPrice: 1800 },
  { label: '000001 平安银行', value: '000001', meanPrice: 15 },
  { label: '601318 中国平安', value: '601318', meanPrice: 50 },
]

const running = ref(false)
const sending = ref(false)
const selectedSecurity = ref(securityOptions[0].value)
const selectedMarket = ref<MarketCode>('XSHG')
const ordersPerSecond = ref(2)

const executions = ref<ExecPoint[]>([])
const washAlerts = ref<WashAlert[]>([])
const orders = ref<SimOrder[]>([]) // 仅保存参与撮合的合法订单
const trades = ref<TradeRecord[]>([])
const washPairs = ref<WashPair[]>([]) // 对敲风险记录，只保存成对的订单号
const orderSortMode = ref<'price' | 'time'>('price')

const stats = reactive({
  totalOrders: 0,
  totalExecQty: 0,
  lastExecPrice: 0,
})

const chartEl = ref<HTMLDivElement | null>(null)
let chart: echarts.ECharts | null = null
let timer: number | undefined

function randomSide(): OrderSide {
  return Math.random() < 0.5 ? 'B' : 'S'
}

function randomQty(): number {
  const multiples = [100, 200, 300, 400, 500, 600, 700, 800, 900]
  const idx = Math.floor(Math.random() * multiples.length)
  return multiples[idx]
}

function randomPrice(securityId: string): number {
  const found = securityOptions.find((s) => s.value === securityId) || securityOptions[0]
  const mean = found.meanPrice
  const std = mean * 0.01 * 2
  const u1 = Math.random() || 1e-6
  const u2 = Math.random()
  const z0 = Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2.0 * Math.PI * u2)
  let price = mean + z0 * std
  const min = mean * 0.9
  const max = mean * 1.1
  if (price < min) price = min
  if (price > max) price = max
  return Number(price.toFixed(2))
}

function generateShareHolderId(): string {
  const n = Math.floor(Math.random() * 99) + 1
  return `SH${String(n).padStart(3, '0')}`
}

function runMatching(securityId: string) {
  const activeBuys = orders.value
    .filter((o) => !o.isWash && o.securityId === securityId && o.remaining > 0 && o.side === 'B')
    .sort((a, b) => (b.price === a.price ? a.createdAt - b.createdAt : b.price - a.price))

  const activeSells = orders.value
    .filter((o) => !o.isWash && o.securityId === securityId && o.remaining > 0 && o.side === 'S')
    .sort((a, b) => (a.price === b.price ? a.createdAt - b.createdAt : a.price - b.price))

  while (activeBuys.length > 0 && activeSells.length > 0) {
    const buy = activeBuys[0]
    const sell = activeSells[0]

    if (buy.price < sell.price) {
      break
    }

    const qty = Math.min(buy.remaining, sell.remaining)
    if (qty <= 0) break

    const execPrice = sell.price
    const ts = Date.now()

    trades.value.unshift({
      time: ts,
      price: execPrice,
      qty,
      securityId,
      buyShareHolderId: buy.shareHolderId,
      sellShareHolderId: sell.shareHolderId,
    })
    if (trades.value.length > 200) {
      trades.value = trades.value.slice(0, 200)
    }

    executions.value.push({
      time: ts,
      price: execPrice,
      securityId,
    })
    if (executions.value.length > 500) {
      executions.value = executions.value.slice(-500)
    }

    stats.totalExecQty += qty
    stats.lastExecPrice = execPrice

    buy.remaining -= qty
    sell.remaining -= qty

    if (buy.remaining <= 0) {
      activeBuys.shift()
    }
    if (sell.remaining <= 0) {
      activeSells.shift()
    }
  }

  // 已经完全成交（remaining <= 0）的订单，从挂单列表中撤出
  orders.value = orders.value.filter((o) => o.remaining > 0)
}

const sortedPendingOrders = computed(() => {
  const list = orders.value.filter(
    (o) => o.securityId === selectedSecurity.value && o.remaining > 0,
  )

  if (orderSortMode.value === 'time') {
    return list.sort((a, b) => a.createdAt - b.createdAt)
  }

  // 价格优先，同价时间优先；买单按价高优先，卖单按价低优先
  return list.sort((a, b) => {
    if (a.side !== b.side) {
      return a.side === 'B' ? -1 : 1
    }
    if (a.price === b.price) {
      return a.createdAt - b.createdAt
    }
    if (a.side === 'B') {
      return b.price - a.price
    }
    return a.price - b.price
  })
})

async function sendRandomOrder() {
  const payload: OrderRequest = {
    market: selectedMarket.value,
    securityId: selectedSecurity.value,
    side: randomSide(),
    qty: randomQty(),
    price: randomPrice(selectedSecurity.value),
  }

  const shareHolderId = generateShareHolderId()

  const counter = orders.value.find(
    (o) =>
      o.securityId === payload.securityId &&
      o.shareHolderId === shareHolderId &&
      o.remaining > 0 &&
      o.side !== payload.side,
  )

  const orderId = `SIM_${Date.now()}_${Math.floor(Math.random() * 10000)}`
  const createdAt = Date.now()
  const simOrder: SimOrder = {
    id: orderId,
    side: payload.side,
    price: payload.price,
    qty: payload.qty,
    remaining: payload.qty,
    securityId: payload.securityId,
    shareHolderId,
    createdAt,
    isWash: !!counter,
  }
  stats.totalOrders += 1

  if (counter) {
    // 视为对敲：新旧两笔订单都不再参与撮合，只记录一条对敲风险
    washPairs.value.unshift({
      time: createdAt,
      securityId: payload.securityId,
      shareHolderId,
      orderId1: counter.id,
      orderId2: simOrder.id,
    })
    if (washPairs.value.length > 300) {
      washPairs.value = washPairs.value.slice(0, 300)
    }
    orders.value = orders.value.filter((o) => o.id !== counter.id)
  } else {
    orders.value.unshift(simOrder)
    if (orders.value.length > 300) {
      orders.value = orders.value.slice(0, 300)
    }
    runMatching(payload.securityId)
  }
}

function startSimulation() {
  if (running.value) return
  running.value = true
  const interval = Math.max(200, Math.floor(1000 / Math.max(1, ordersPerSecond.value)))
  timer = window.setInterval(() => {
    void sendRandomOrder()
  }, interval)
}

function stopSimulation() {
  running.value = false
  if (timer) {
    window.clearInterval(timer)
    timer = undefined
  }
}

watch(ordersPerSecond, () => {
  if (running.value) {
    stopSimulation()
    startSimulation()
  }
})

function initChart() {
  if (!chartEl.value) return
  chart = echarts.init(chartEl.value)
  updateChart()
}

function updateChart() {
  if (!chart) return
  const now = Date.now()
  const points = executions.value
    .filter((p) => p.securityId === selectedSecurity.value && now - p.time <= 5 * 60 * 1000)
    .sort((a, b) => a.time - b.time)

  const xData = points.map((p) => new Date(p.time).toLocaleTimeString())
  const yData = points.map((p) => p.price)

  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 16, top: 30, bottom: 40 },
    xAxis: {
      type: 'category',
      data: xData,
      boundaryGap: false,
      axisLabel: { color: '#6b7280' },
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#6b7280' },
      scale: true,
    },
    series: [
      {
        type: 'line',
        data: yData,
        smooth: true,
        symbolSize: 6,
        areaStyle: {
          opacity: 0.15,
        },
        lineStyle: {
          width: 2,
        },
      },
    ],
  })
}

watch(executions, updateChart, { deep: true })
watch(selectedSecurity, updateChart)

let stompClient: Client | null = null

function handleExecutionMessage(message: IMessage) {
  try {
    const body = JSON.parse(message.body) as {
      execPrice: number
      execQty: number
      execId: string
      securityId: string
    }
    const ts = Date.now()
    executions.value.push({
      time: ts,
      price: body.execPrice,
      securityId: body.securityId,
    })
    if (executions.value.length > 500) {
      executions.value = executions.value.slice(-500)
    }
    stats.totalExecQty += body.execQty
    stats.lastExecPrice = body.execPrice
  } catch {
    // ignore parse error
  }
}

function handleWashTradeMessage(message: IMessage) {
  try {
    const body = JSON.parse(message.body) as {
      clOrderId: string
      securityId?: string
      shareHolderId?: string
      rejectCode?: number
      rejectText?: string
    }
    washAlerts.value.unshift({
      clOrderId: body.clOrderId,
      securityId: body.securityId,
      shareHolderId: body.shareHolderId,
      rejectCode: body.rejectCode,
      rejectText: body.rejectText,
      ts: Date.now(),
    })
    if (washAlerts.value.length > 100) {
      washAlerts.value = washAlerts.value.slice(0, 100)
    }
  } catch {
    // ignore
  }
}

function setupWebSocket() {
  stompClient = new Client({
    webSocketFactory: () => new SockJS('/ws'),
    reconnectDelay: 5000,
  })

  stompClient.onConnect = () => {
    stompClient?.subscribe('/topic/reports/execution', handleExecutionMessage)
    stompClient?.subscribe('/topic/alerts/washtrade', handleWashTradeMessage)
  }

  stompClient.onStompError = (frame) => {
    console.error('STOMP error', frame)
  }

  stompClient.activate()
}

onMounted(() => {
  initChart()
  setupWebSocket()
})

onBeforeUnmount(() => {
  stopSimulation()
  if (stompClient) {
    stompClient.deactivate()
    stompClient = null
  }
  if (chart) {
    chart.dispose()
    chart = null
  }
})
</script>

<template>
  <div class="page">
    <el-card shadow="never" class="top-card">
      <template #header>
        <div class="header-row">
          <div>
            <h3>内部撮合演示 & 实时价格曲线</h3>
            <p class="hint">
              随机生成订单，通过后台撮合与 WebSocket 推送，实时观察成交价格曲线和对敲拦截。
            </p>
          </div>
          <div class="actions">
            <el-button
              type="primary"
              :loading="sending"
              @click="running ? stopSimulation() : startSimulation()"
            >
              {{ running ? '停止模拟' : '开始模拟' }}
            </el-button>
          </div>
        </div>
      </template>

      <div class="controls">
        <el-form inline>
          <el-form-item label="市场">
            <el-select v-model="selectedMarket" style="width: 140px">
              <el-option v-for="m in markets" :key="m" :label="m" :value="m" />
            </el-select>
          </el-form-item>

          <el-form-item label="股票">
            <el-select v-model="selectedSecurity" style="width: 200px">
              <el-option
                v-for="s in securityOptions"
                :key="s.value"
                :label="s.label"
                :value="s.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="订单频率(每秒)">
            <el-input-number
              v-model="ordersPerSecond"
              :min="1"
              :max="10"
              :step="1"
              controls-position="right"
            />
          </el-form-item>
        </el-form>

        <div class="stats">
          <el-statistic title="已发送订单" :value="stats.totalOrders" />
          <el-statistic title="累计成交数量" :value="stats.totalExecQty" />
          <el-statistic
            title="最新成交价"
            :value="stats.lastExecPrice ? stats.lastExecPrice.toFixed(2) : '-'"
          />
        </div>
      </div>

      <div class="chart-wrapper">
        <div ref="chartEl" class="chart"></div>
      </div>
    </el-card>

    <div class="lists-grid">
      <el-card shadow="never" class="list-card">
        <template #header>
          <div class="side-header">
            <h3>撮合成交记录（本地模拟）</h3>
            <el-tag type="info">{{ trades.length }} 笔</el-tag>
          </div>
        </template>
        <el-table :data="trades.slice(0, 50)" height="260" border stripe>
          <el-table-column label="时间" width="140">
            <template #default="{ row }">
              {{ new Date(row.time).toLocaleTimeString() }}
            </template>
          </el-table-column>
          <el-table-column prop="securityId" label="股票" width="90" />
          <el-table-column prop="qty" label="数量" width="90" />
          <el-table-column prop="price" label="成交价" width="90" />
          <el-table-column label="买方 / 卖方" min-width="170">
            <template #default="{ row }">
              买: {{ row.buyShareHolderId }} / 卖: {{ row.sellShareHolderId }}
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card shadow="never" class="list-card">
        <template #header>
          <div class="side-header">
            <div>
              <h3>当前挂单（合法订单）</h3>
              <p class="subhint">可切换按价格优先 / 时间优先查看</p>
            </div>
            <div class="side-actions">
              <el-segmented
                v-model="orderSortMode"
                :options="[
                  { label: '价格优先', value: 'price' },
                  { label: '时间优先', value: 'time' },
                ]"
              />
              <el-tag type="info">{{ orders.length }} 笔</el-tag>
            </div>
          </div>
        </template>
        <div class="order-lists">
          <div class="order-list">
            <div class="order-list-header">
              <span>买单列表 (B)</span>
            </div>
            <el-table
              :data="sortedPendingOrders.filter((o) => o.side === 'B').slice(0, 40)"
              height="240"
              border
              stripe
            >
              <el-table-column prop="id" label="订单号" min-width="140" />
              <el-table-column prop="price" label="买价" width="80" />
              <el-table-column prop="remaining" label="数量" width="80" />
              <el-table-column prop="shareHolderId" label="股东号" width="100" />
              <el-table-column label="对敲" width="60">
                <template #default="{ row }">
                  <el-tag v-if="row.isWash" type="danger" size="small">是</el-tag>
                  <span v-else>否</span>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div class="order-list">
            <div class="order-list-header">
              <span>卖单列表 (S)</span>
            </div>
            <el-table
              :data="sortedPendingOrders.filter((o) => o.side === 'S').slice(0, 40)"
              height="240"
              border
              stripe
            >
              <el-table-column prop="id" label="订单号" min-width="140" />
              <el-table-column prop="price" label="卖价" width="80" />
              <el-table-column prop="remaining" label="数量" width="80" />
              <el-table-column prop="shareHolderId" label="股东号" width="100" />
              <el-table-column label="对敲" width="60">
                <template #default="{ row }">
                  <el-tag v-if="row.isWash" type="danger" size="small">是</el-tag>
                  <span v-else>否</span>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </el-card>

      <el-card shadow="never" class="list-card">
        <template #header>
          <div class="side-header">
            <h3>对敲风险记录（非法，不参与撮合）</h3>
            <el-tag type="danger">{{ washPairs.length }} 组</el-tag>
          </div>
        </template>
        <el-table
          :data="washPairs.filter((p) => p.securityId === selectedSecurity).slice(0, 80)"
          height="260"
          border
          stripe
        >
          <el-table-column label="时间" width="140">
            <template #default="{ row }">
              {{ new Date(row.time).toLocaleTimeString() }}
            </template>
          </el-table-column>
          <el-table-column prop="securityId" label="股票" width="80" />
          <el-table-column prop="shareHolderId" label="股东号" width="100" />
          <el-table-column prop="orderId1" label="订单号1" min-width="150" />
          <el-table-column prop="orderId2" label="订单号2" min-width="150" />
          <el-table-column label="标记" width="90">
            <template #default>
              <el-tag type="danger" size="small">对敲风险</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
h3 {
  margin: 0;
}

.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.top-card {
  width: 100%;
}

.lists-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.list-card {
  width: 100%;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.hint {
  margin-top: 6px;
  color: #6b7280;
  max-width: 520px;
}

.actions {
  display: flex;
  align-items: center;
}

.controls {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 10px;
}

.stats {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.chart-wrapper {
  margin-top: 4px;
}

.chart {
  width: 100%;
  height: 360px;
}

.side-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.side-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.subhint {
  margin: 4px 0 0;
  font-size: 12px;
  color: #9ca3af;
}

.order-list-header {
  font-size: 13px;
  color: #4b5563;
  margin-bottom: 6px;
}

@media (max-width: 1100px) {
}
</style>

