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
          <h3>价格走势 - {{ selectedStock }}</h3>
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
                stroke="#1890ff"
                stroke-width="2"
              />
            </svg>
            <div class="chart-x-axis">
              <span>{{ timeLabels[0] }}</span>
              <span>{{ timeLabels[1] }}</span>
              <span>{{ timeLabels[2] }}</span>
            </div>
          </div>
          <div class="price-info">
            <span>当前价: {{ currentPrice.toFixed(2) }}</span>
            <span>最高: {{ maxPrice.toFixed(2) }}</span>
            <span>最低: {{ minPrice.toFixed(2) }}</span>
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
                    <th>时间</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="trade in tradeSuccesses" :key="trade.execId">
                    <td>{{ trade.execId }}</td>
                    <td>{{ trade.buyShareHolderId }}</td>
                    <td>{{ trade.sellShareHolderId }}</td>
                    <td class="price">{{ trade.execPrice.toFixed(2) }}</td>
                    <td class="qty">{{ trade.execQty }}</td>
                    <td>{{ formatTime(trade.execTime) }}</td>
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
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { simulateApi, type TradeSuccess, type TradeIllegal } from '../api/order'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

const autoSimulate = ref(false)
const currentTime = ref('')
const selectedStock = ref('600519')
const stocks = ref<string[]>(['600519', '000001', '601318'])
const priceHistory = ref<number[]>([])
const timeHistory = ref<number[]>([])
const tradeSuccesses = ref<TradeSuccess[]>([])
const tradeIllegals = ref<TradeIllegal[]>([])

let stompClient: Client | null = null
let timeInterval: number | null = null

const currentPrice = computed(() => {
  if (priceHistory.value.length === 0) return 0
  return priceHistory.value[priceHistory.value.length - 1]
})

const minPrice = computed(() => {
  if (priceHistory.value.length === 0) return 0
  return Math.min(...priceHistory.value) * 0.95
})

const maxPrice = computed(() => {
  if (priceHistory.value.length === 0) return 100
  return Math.max(...priceHistory.value) * 1.05
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
    const res = await simulateApi.getAll()
    tradeSuccesses.value = res.data.tradeSuccesses || []
    tradeIllegals.value = res.data.tradeIllegals || []
    selectedStock.value = res.data.currentSecurityId || '600519'

    tradeSuccesses.value.forEach(t => {
      if (!priceHistory.value.includes(t.execPrice)) {
        priceHistory.value.push(t.execPrice)
        timeHistory.value.push(t.execTime)
        if (priceHistory.value.length > 50) {
          priceHistory.value.shift()
          timeHistory.value.shift()
        }
      }
    })
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

        tradeSuccesses.value.forEach(t => {
          if (!priceHistory.value.includes(t.execPrice)) {
            priceHistory.value.push(t.execPrice)
            timeHistory.value.push(t.execTime)
            if (priceHistory.value.length > 50) {
              priceHistory.value.shift()
              timeHistory.value.shift()
            }
          }
        })
      })
    }
  })
  stompClient.activate()
}

const toggleAutoSimulate = async () => {
  autoSimulate.value = !autoSimulate.value
  await simulateApi.setAutoSimulate(autoSimulate.value)
}

const changeStock = async (stock: string) => {
  selectedStock.value = stock
  await simulateApi.setSecurityId(stock)
  priceHistory.value = []
  timeHistory.value = []
}

const clearData = async () => {
  await simulateApi.clear()
  priceHistory.value = []
  timeHistory.value = []
  tradeSuccesses.value = []
  tradeIllegals.value = []
}

defineExpose({
  toggleAutoSimulate,
  clearData,
  changeStock
})

const formatTime = (timestamp: number) => {
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', { hour12: false })
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

.price-chart h3 {
  margin-bottom: 1rem;
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

.price-info {
  display: flex;
  gap: 2rem;
  color: #666;
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
