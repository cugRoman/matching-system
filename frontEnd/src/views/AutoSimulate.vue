<template>
  <div class="auto-simulate">
    <div class="toolbar">
      <div class="status">
        <span>状态: {{ autoSimulate ? '运行中' : '已停止' }}</span>
        <span>当前时间: {{ currentTime }}</span>
      </div>
    </div>

    <div class="content">
      <div class="sidebar">
        <h3>选择股票</h3>
        <div class="stock-list">
          <div
            v-for="stock in stocks"
            :key="stock"
            class="stock-item"
            :class="{ active: selectedStock === stock }"
            @click="changeStock(stock)"
          >
            {{ stock }}
          </div>
        </div>
      </div>

      <div class="main-content">
        <div class="price-chart">
          <div class="stock-header">
            <div class="stock-title">
              <span class="stock-code">{{ selectedStock }}</span>
              <span class="stock-name">{{ getStockName(selectedStock) }}</span>
            </div>
            <div class="stock-price" :class="priceChangeClass">
              <span class="price-value">{{ currentPrice.toFixed(2) }}</span>
              <span class="price-change" v-if="priceHistory.length > 1">
                {{ priceChange >= 0 ? '+' : '' }}{{ priceChange.toFixed(2) }}
                ({{ priceChangePercent >= 0 ? '+' : '' }}{{ priceChangePercent.toFixed(2) }}%)
              </span>
            </div>
            <div class="price-stats">
              <div class="stat-item">
                <span class="label">今开</span>
                <span class="value">{{ openPrice.toFixed(2) }}</span>
              </div>
              <div class="stat-item">
                <span class="label">最高</span>
                <span class="value high">{{ actualMaxPrice.toFixed(2) }}</span>
              </div>
              <div class="stat-item">
                <span class="label">最低</span>
                <span class="value low">{{ actualMinPrice.toFixed(2) }}</span>
              </div>
              <div class="stat-item">
                <span class="label">均价</span>
                <span class="value">{{ meanPrice.toFixed(2) }}</span>
              </div>
              <div class="stat-item">
                <span class="label">成交量</span>
                <span class="value">{{ totalVolume }}</span>
              </div>
            </div>
          </div>
          
          <h3>分时走势</h3>
          <div class="chart-container">
            <div class="chart-y-axis">
              <span>{{ maxPrice.toFixed(2) }}</span>
              <span>{{ ((maxPrice + minPrice) / 2).toFixed(2) }}</span>
              <span>{{ minPrice.toFixed(2) }}</span>
            </div>
            <svg class="chart-svg" viewBox="0 0 800 200" preserveAspectRatio="none">
              <polyline
                :points="chartPoints"
                fill="none"
                :stroke="priceChange >= 0 ? '#f5222d' : '#52c41a'"
                stroke-width="2"
              />
            </svg>
            <div class="chart-x-axis">
              <span>{{ timeLabels[0] }}</span>
              <span>{{ timeLabels[1] }}</span>
              <span>{{ timeLabels[2] }}</span>
            </div>
          </div>
        </div>

        <div class="tables-row">
          <div class="table-section">
            <h3>撮合成功列表 (共{{ tradeSuccesses.length }} 条)</h3>
            <div class="table-wrapper">
              <table class="data-table">
                <thead>
                  <tr>
                    <th>成交号</th>
                    <th>买方</th>
                    <th>卖方</th>
                    <th>价格</th>
                    <th>数量</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="trade in tradeSuccesses" :key="trade.execId">
                    <td>{{ trade.execId }}</td>
                    <td>{{ trade.buyShareHolderId }}</td>
                    <td>{{ trade.sellShareHolderId }}</td>
                    <td class="price">{{ trade.execPrice.toFixed(2) }}</td>
                    <td class="qty">{{ trade.execQty }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <div class="table-section">
            <h3>撮合失败列表 (共{{ tradeIllegals.length }} 条)</h3>
            <div class="table-wrapper">
              <table class="data-table">
                <thead>
                  <tr>
                    <th>订单号</th>
                    <th>股东</th>
                    <th>方向</th>
                    <th>价格</th>
                    <th>数量</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="illegal in tradeIllegals" :key="illegal.clOrderId">
                    <td>{{ illegal.clOrderId }}</td>
                    <td>{{ illegal.shareHolderId }}</td>
                    <td :class="illegal.side === 'B' ? 'buy' : 'sell'">{{ illegal.side === 'B' ? '买' : '卖' }}</td>
                    <td class="price">{{ illegal.price.toFixed(2) }}</td>
                    <td class="qty">{{ illegal.qty }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <div class="table-section">
            <h3>交易所挂单 (买入)</h3>
            <div class="table-wrapper">
              <table class="data-table">
                <thead>
                  <tr>
                    <th>订单号</th>
                    <th>股东</th>
                    <th>价格</th>
                    <th>数量</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="order in exchangeBuys.filter(o => o.qty > 0)" :key="order.clOrderId">
                    <td>{{ order.clOrderId }}</td>
                    <td>{{ order.shareHolderId }}</td>
                    <td class="price">{{ order.price.toFixed(2) }}</td>
                    <td class="qty">{{ order.qty }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <div class="table-section">
            <h3>交易所挂单 (卖出)</h3>
            <div class="table-wrapper">
              <table class="data-table">
                <thead>
                  <tr>
                    <th>订单号</th>
                    <th>股东</th>
                    <th>价格</th>
                    <th>数量</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="order in exchangeSells.filter(o => o.qty > 0)" :key="order.clOrderId">
                    <td>{{ order.clOrderId }}</td>
                    <td>{{ order.shareHolderId }}</td>
                    <td class="price">{{ order.price.toFixed(2) }}</td>
                    <td class="qty">{{ order.qty }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { simulateApi, type TradeSuccess, type TradeIllegal, type Order } from '../api/order'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

const autoSimulate = ref(false)
const currentTime = ref('')
const selectedStock = ref('600519')
const stocks = ref<string[]>(['600519', '000001', '601318'])
const stockNames: Record<string, string> = {
  '600519': '贵州茅台',
  '000001': '平安银行',
  '601318': '中国平安'
}
const priceHistory = ref<number[]>([])
const timeHistory = ref<number[]>([])
const tradeSuccesses = ref<TradeSuccess[]>([])
const tradeIllegals = ref<TradeIllegal[]>([])
const exchangeBuys = ref<Order[]>([])
const exchangeSells = ref<Order[]>([])
const openPrice = ref(0)
const lastPrice = ref(0)
const meanPrice = ref(100)

let stompClient: Client | null = null
let timeInterval: number | null = null

const getStockName = (code: string) => stockNames[code] || code

const priceChange = computed(() => {
  if (openPrice.value === 0 || priceHistory.value.length === 0) return 0
  return currentPrice.value - openPrice.value
})

const priceChangePercent = computed(() => {
  if (openPrice.value === 0) return 0
  return (priceChange.value / openPrice.value) * 100
})

const priceChangeClass = computed(() => {
  if (priceChange.value > 0) return 'up'
  if (priceChange.value < 0) return 'down'
  return ''
})

const totalVolume = computed(() => {
  return tradeSuccesses.value.reduce((sum, t) => sum + t.execQty, 0).toLocaleString()
})

const currentPrice = computed(() => {
  if (priceHistory.value.length === 0) return 0
  return priceHistory.value[priceHistory.value.length - 1]
})

const minPrice = computed(() => {
  if (priceHistory.value.length === 0) return 0
  return 0
})

const maxPrice = computed(() => {
  return 120
})

const actualMaxPrice = computed(() => {
  if (priceHistory.value.length === 0) return 0
  return Math.max(...priceHistory.value)
})

const actualMinPrice = computed(() => {
  if (priceHistory.value.length === 0) return 0
  return Math.min(...priceHistory.value)
})

const chartPoints = computed(() => {
  if (priceHistory.value.length < 2) return ''

  const width = 800
  const height = 200
  const padding = 10

  const min = minPrice.value
  const max = maxPrice.value
  const range = max - min || 1

  return priceHistory.value.map((price, index) => {
    const x = (index / (priceHistory.value.length - 1)) * (width - padding * 2) + padding
    const y = height - ((price - min) / range) * (height - padding * 2) - padding
    return `${x},${y}`
  }).join(' ')
})

const timeLabels = computed(() => {
  if (timeHistory.value.length < 2) return ['', '', '']

  const getLabel = (timestamp: number) => {
    const date = new Date(timestamp)
    return date.toLocaleTimeString('zh-CN', { hour12: false })
  }

  return [
    getLabel(timeHistory.value[0]),
    getLabel(timeHistory.value[Math.floor(timeHistory.value.length / 2)]),
    getLabel(timeHistory.value[timeHistory.value.length - 1])
  ]
})

const loadData = async () => {
  try {
    const [allRes, securityRes] = await Promise.all([
      simulateApi.getAll(),
      simulateApi.getSecurityId()
    ])
    
    tradeSuccesses.value = allRes.data.tradeSuccesses || []
    tradeIllegals.value = allRes.data.tradeIllegals || []
    exchangeBuys.value = allRes.data.exchangeBuys || []
    exchangeSells.value = allRes.data.exchangeSells || []
    selectedStock.value = securityRes.data.securityId || '600519'
    meanPrice.value = securityRes.data.meanPrice || 100
    
    if (openPrice.value === 0 && tradeSuccesses.value.length > 0) {
      openPrice.value = tradeSuccesses.value[0].execPrice
    }
    
    const prices = new Set<number>()
    tradeSuccesses.value.forEach(t => {
      if (!prices.has(t.execPrice)) {
        prices.add(t.execPrice)
        priceHistory.value.push(t.execPrice)
        timeHistory.value.push(t.execTime)
      }
    })
    
    while (priceHistory.value.length > 360) {
      priceHistory.value.shift()
      timeHistory.value.shift()
    }
  } catch (e) {
    console.error('Failed to load data:', e)
  }
}

const connectWebSocket = () => {
  stompClient = new Client({
    webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
    onConnect: () => {
      stompClient?.subscribe('/topic/simulate', (message) => {
        const data = JSON.parse(message.body)
        tradeSuccesses.value = data.tradeSuccesses || []
        tradeIllegals.value = data.tradeIllegals || []
        exchangeBuys.value = data.exchangeBuys || []
        exchangeSells.value = data.exchangeSells || []

        if (tradeSuccesses.value.length > 0 && openPrice.value === 0) {
          openPrice.value = tradeSuccesses.value[0].execPrice
        }
        
        const prices = new Set<number>()
        tradeSuccesses.value.forEach(t => {
          if (!prices.has(t.execPrice)) {
            prices.add(t.execPrice)
            priceHistory.value.push(t.execPrice)
            timeHistory.value.push(t.execTime)
          }
        })
        
        while (priceHistory.value.length > 360) {
          priceHistory.value.shift()
          timeHistory.value.shift()
        }
      })
    }
  })
  stompClient.activate()
}

const toggleAutoSimulate = async () => {
  autoSimulate.value = !autoSimulate.value
  await simulateApi.setAutoSimulate(autoSimulate.value)
  if (autoSimulate.value) {
    alert('自动模拟已启动')
  } else {
    alert('自动模拟已停止')
  }
}

const changeStock = async (stock: string) => {
  selectedStock.value = stock
  await simulateApi.setSecurityId(stock)
  const securityRes = await simulateApi.getSecurityId()
  meanPrice.value = securityRes.data.meanPrice || 100
  priceHistory.value = []
  timeHistory.value = []
  openPrice.value = 0
  tradeSuccesses.value = []
  tradeIllegals.value = []
  exchangeBuys.value = []
  exchangeSells.value = []
  alert(`已切换到股票 ${stock}`)
}

const clearData = async () => {
  await simulateApi.clear()
  priceHistory.value = []
  timeHistory.value = []
  tradeSuccesses.value = []
  tradeIllegals.value = []
  exchangeBuys.value = []
  exchangeSells.value = []
  openPrice.value = 0
  alert('模拟数据已清空')
}

defineExpose({
  toggleAutoSimulate,
  clearData,
  changeStock
})

const formatTime = (timestamp: number) => {
  const date = new Date(timestamp)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  const second = String(date.getSeconds()).padStart(2, '0')
  return `${year}年${month}月${day}日 ${hour}:${minute}:${second}`
}

const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', { hour12: false })
}

onMounted(() => {
  loadData()
  connectWebSocket()
  updateTime()
  timeInterval = window.setInterval(updateTime, 1000)
})

onUnmounted(() => {
  stompClient?.deactivate()
  if (timeInterval) clearInterval(timeInterval)
})
</script>

<style scoped>
.auto-simulate {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.toolbar {
  background: white;
  padding: 1rem;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.status {
  display: flex;
  gap: 2rem;
  color: #666;
}

.content {
  display: flex;
  gap: 1rem;
  min-width: 0;
}

.sidebar {
  width: 200px;
  background: white;
  padding: 1rem;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.sidebar h3 {
  margin-bottom: 1rem;
}

.stock-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.stock-item {
  padding: 0.75rem;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.3s;
}

.stock-item:hover {
  background: #f5f5f5;
}

.stock-item.active {
  background: #1890ff;
  color: white;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  min-width: 0;
}

.price-chart {
  background: white;
  padding: 1rem;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.stock-header {
  display: flex;
  align-items: center;
  gap: 2rem;
  padding: 1rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  margin-bottom: 1rem;
}

.stock-title {
  display: flex;
  flex-direction: column;
}

.stock-code {
  font-size: 1.5rem;
  font-weight: bold;
  color: white;
}

.stock-name {
  font-size: 0.9rem;
  color: rgba(255, 255, 255, 0.8);
}

.stock-price {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.stock-price.up .price-value,
.stock-price.up .price-change {
  color: #f5222d;
}

.stock-price.down .price-value,
.stock-price.down .price-change {
  color: #52c41a;
}

.price-value {
  font-size: 2rem;
  font-weight: bold;
  color: white;
  font-family: monospace;
}

.price-change {
  font-size: 0.9rem;
  font-weight: 600;
  color: white;
}

.price-stats {
  display: flex;
  gap: 1.5rem;
  margin-left: auto;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-item .label {
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.7);
}

.stat-item .value {
  font-size: 1rem;
  font-weight: 600;
  color: white;
}

.stat-item .value.high {
  color: #f5222d;
}

.stat-item .value.low {
  color: #52c41a;
}

.chart-container {
  display: flex;
  height: 200px;
  position: relative;
  background: #fafafa;
  border: 1px solid #e8e8e8;
  margin-bottom: 1rem;
}

.chart-y-axis {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 10px 5px;
  font-size: 0.75rem;
  color: #999;
  min-width: 50px;
  text-align: right;
}

.chart-svg {
  flex: 1;
  height: 100%;
}

.chart-x-axis {
  position: absolute;
  bottom: 5px;
  left: 55px;
  right: 10px;
  display: flex;
  justify-content: space-between;
  font-size: 0.75rem;
  color: #999;
}

.tables-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
  min-width: 0;
}

.table-section {
  background: white;
  padding: 1rem;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  min-width: 0;
}

.table-section h3 {
  margin-bottom: 0.75rem;
}

.table-wrapper {
  max-height: 300px;
  overflow-y: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.85rem;
}

.data-table th,
.data-table td {
  padding: 0.5rem;
  text-align: left;
  border-bottom: 1px solid #f0f0f0;
}

.data-table th {
  background: #fafafa;
  font-weight: 600;
  color: #666;
  position: sticky;
  top: 0;
}

.data-table .price,
.data-table .qty {
  text-align: center;
  font-family: monospace;
}

.data-table .buy {
  color: #f5222d;
}

.data-table .sell {
  color: #52c41a;
}

@media (max-width: 1200px) {
  .tables-row {
    grid-template-columns: 1fr;
  }
}
</style>
