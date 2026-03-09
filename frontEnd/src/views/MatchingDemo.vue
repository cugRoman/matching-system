<template>
  <div class="matching-demo">
    <div class="toolbar">
      <button @click="triggerFileInput" class="btn btn-primary">导入请求</button>
      <input 
        type="file" 
        ref="fileInput" 
        @change="handleFileImport" 
        accept=".txt,.json" 
        style="display: none"
      />
      <button @click="executeMatch" class="btn btn-success">开始撮合</button>
      <button @click="clearData" class="btn btn-danger">清空数据</button>
      <span class="tip">提示: 点击导入请求选择txt文件，或使用内置示例</span>
    </div>

    <div class="tables-container">
      <div class="table-section">
        <h3>买入请求列表</h3>
        <table class="data-table">
          <thead>
            <tr>
              <th>订单号</th>
              <th>股东号</th>
              <th>股票</th>
              <th>价格</th>
              <th>数量</th>
              <th>时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in buyRequests" :key="order.clOrderId">
              <td>{{ order.clOrderId }}</td>
              <td>{{ order.shareHolderId }}</td>
              <td>{{ order.securityId }}</td>
              <td class="price">{{ order.price.toFixed(2) }}</td>
              <td>{{ order.qty }}</td>
              <td>{{ formatTime(order.timestamp) }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="table-section">
        <h3>卖出请求列表</h3>
        <table class="data-table">
          <thead>
            <tr>
              <th>订单号</th>
              <th>股东号</th>
              <th>股票</th>
              <th>价格</th>
              <th>数量</th>
              <th>时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in sellRequests" :key="order.clOrderId">
              <td>{{ order.clOrderId }}</td>
              <td>{{ order.shareHolderId }}</td>
              <td>{{ order.securityId }}</td>
              <td class="price">{{ order.price.toFixed(2) }}</td>
              <td>{{ order.qty }}</td>
              <td>{{ formatTime(order.timestamp) }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="table-section">
        <h3>挂载在交易所列表</h3>
        <table class="data-table">
          <thead>
            <tr>
              <th>订单号</th>
              <th>方向</th>
              <th>股东号</th>
              <th>股票</th>
              <th>价格</th>
              <th>数量</th>
              <th>状态</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in [...exchangeBuys, ...exchangeSells]" :key="order.clOrderId">
              <td>{{ order.clOrderId }}</td>
              <td :class="order.side === 'B' ? 'buy' : 'sell'">{{ order.side === 'B' ? '买入' : '卖出' }}</td>
              <td>{{ order.shareHolderId }}</td>
              <td>{{ order.securityId }}</td>
              <td class="price">{{ order.price.toFixed(2) }}</td>
              <td>{{ order.qty }}</td>
              <td>{{ getStatusText(order.status) }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="table-section">
        <h3>撮合成功列表</h3>
        <table class="data-table">
          <thead>
            <tr>
              <th>成交号</th>
              <th>股票</th>
              <th>买方订单</th>
              <th>卖方订单</th>
              <th>买方股东</th>
              <th>卖方股东</th>
              <th>成交价</th>
              <th>成交数量</th>
              <th>成交时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="trade in tradeSuccesses" :key="trade.execId">
              <td>{{ trade.execId }}</td>
              <td>{{ trade.securityId }}</td>
              <td>{{ trade.buyOrderId }}</td>
              <td>{{ trade.sellOrderId }}</td>
              <td>{{ trade.buyShareHolderId }}</td>
              <td>{{ trade.sellShareHolderId }}</td>
              <td class="price">{{ trade.execPrice.toFixed(2) }}</td>
              <td>{{ trade.execQty }}</td>
              <td>{{ formatTime(trade.execTime) }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="table-section">
        <h3>撮合失败列表（对敲）</h3>
        <table class="data-table">
          <thead>
            <tr>
              <th>订单号</th>
              <th>股东号</th>
              <th>股票</th>
              <th>方向</th>
              <th>价格</th>
              <th>数量</th>
              <th>拒绝码</th>
              <th>拒绝时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="illegal in tradeIllegals" :key="illegal.clOrderId">
              <td>{{ illegal.clOrderId }}</td>
              <td>{{ illegal.shareHolderId }}</td>
              <td>{{ illegal.securityId }}</td>
              <td :class="illegal.side === 'B' ? 'buy' : 'sell'">{{ illegal.side === 'B' ? '买入' : '卖出' }}</td>
              <td class="price">{{ illegal.price.toFixed(2) }}</td>
              <td>{{ illegal.qty }}</td>
              <td>{{ illegal.rejectCode }}</td>
              <td>{{ formatTime(illegal.rejectTime) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { orderApi, type Order, type TradeSuccess, type TradeIllegal, type OrderRequest } from '../api/order'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

const buyRequests = ref<Order[]>([])
const sellRequests = ref<Order[]>([])
const exchangeBuys = ref<Order[]>([])
const exchangeSells = ref<Order[]>([])
const tradeSuccesses = ref<TradeSuccess[]>([])
const tradeIllegals = ref<TradeIllegal[]>([])

const fileInput = ref<HTMLInputElement | null>(null)
let stompClient: Client | null = null
let importBatch = 0

const loadData = async () => {
  try {
    const res = await orderApi.getAll()
    buyRequests.value = res.data.buyRequests || []
    sellRequests.value = res.data.sellRequests || []
    exchangeBuys.value = res.data.exchangeBuys || []
    exchangeSells.value = res.data.exchangeSells || []
    tradeSuccesses.value = res.data.tradeSuccesses || []
    tradeIllegals.value = res.data.tradeIllegals || []
  } catch (e) {
    console.error('Failed to load data:', e)
  }
}

const connectWebSocket = () => {
  stompClient = new Client({
    webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
    onConnect: () => {
      stompClient?.subscribe('/topic/orders', (message) => {
        const data = JSON.parse(message.body)
        buyRequests.value = data.buyRequests || []
        sellRequests.value = data.sellRequests || []
        exchangeBuys.value = data.exchangeBuys || []
        exchangeSells.value = data.exchangeSells || []
        tradeSuccesses.value = data.tradeSuccesses || []
        tradeIllegals.value = data.tradeIllegals || []
      })
    }
  })
  stompClient.activate()
}

const triggerFileInput = () => {
  fileInput.value?.click()
}

const handleFileImport = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  try {
    const text = await file.text()
    let orders: OrderRequest[]
    
    if (file.name.endsWith('.json')) {
      orders = JSON.parse(text)
    } else {
      orders = parseTxtFile(text)
    }
    
    await orderApi.addOrders(orders)
    alert(`成功导入 ${orders.length} 条请求`)
  } catch (e) {
    console.error('Failed to import file:', e)
    alert('导入失败，请检查文件格式')
  }
  
  target.value = ''
}

const parseTxtFile = (content: string): OrderRequest[] => {
  const lines = content.trim().split('\n')
  const orders: OrderRequest[] = []
  
  for (const line of lines) {
    const parts = line.trim().split(/[\s,;]+/)
    if (parts.length < 7) continue
    
    const order: OrderRequest = {
      clOrderId: parts[0] || '',
      market: parts[1] || 'XSHG',
      securityId: parts[2] || '',
      side: (parts[3] === 'B' || parts[3] === 'S') ? parts[3] as 'B' | 'S' : 'B',
      qty: parseInt(parts[4]) || 0,
      price: parseFloat(parts[5]) || 0,
      shareHolderId: parts[6] || '',
      accountId: parts[7] || 'ACC001'
    }
    
    if (order.clOrderId && order.securityId && order.qty > 0) {
      orders.push(order)
    }
  }
  
  return orders
}

const importRequests = async () => {
  importBatch++
  const orders = getDemoOrders(importBatch)
  await orderApi.addOrders(orders)
}

const getDemoOrders = (batch: number) => {
  if (batch === 1) {
    return [
      { clOrderId: 'B001', market: 'XSHG', securityId: '600519', side: 'B', qty: 100, price: 1800.00, shareHolderId: 'SH001', accountId: 'ACC001' },
      { clOrderId: 'B002', market: 'XSHG', securityId: '600519', side: 'B', qty: 200, price: 1795.00, shareHolderId: 'SH002', accountId: 'ACC001' },
      { clOrderId: 'B003', market: 'XSHG', securityId: '600519', side: 'B', qty: 150, price: 1800.00, shareHolderId: 'SH003', accountId: 'ACC001' },
      { clOrderId: 'S001', market: 'XSHG', securityId: '600519', side: 'S', qty: 80, price: 1790.00, shareHolderId: 'SH004', accountId: 'ACC001' },
      { clOrderId: 'S002', market: 'XSHG', securityId: '600519', side: 'S', qty: 100, price: 1800.00, shareHolderId: 'SH005', accountId: 'ACC001' },
      { clOrderId: 'S003', market: 'XSHG', securityId: '600519', side: 'S', qty: 120, price: 1800.00, shareHolderId: 'SH006', accountId: 'ACC001' },
    ]
  } else {
    return [
      { clOrderId: 'B004', market: 'XSHG', securityId: '600519', side: 'B', qty: 50, price: 1810.00, shareHolderId: 'SH007', accountId: 'ACC001' },
      { clOrderId: 'S004', market: 'XSHG', securityId: '600519', side: 'S', qty: 50, price: 1805.00, shareHolderId: 'SH007', accountId: 'ACC001' },
      { clOrderId: 'B005', market: 'XSHG', securityId: '600519', side: 'B', qty: 100, price: 1800.00, shareHolderId: 'SH007', accountId: 'ACC001' },
    ]
  }
}

const executeMatch = async () => {
  await orderApi.match()
}

const clearData = async () => {
  importBatch = 0
  await orderApi.clear()
}

const formatTime = (timestamp: number) => {
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', { hour12: false })
}

const getStatusText = (status: number) => {
  const statusMap: Record<number, string> = {
    0: '未挂载',
    1: '已挂载',
    2: '部分成交',
    3: '已成交',
    4: '非法'
  }
  return statusMap[status] || '未知'
}

onMounted(() => {
  loadData()
  connectWebSocket()
})

onUnmounted(() => {
  stompClient?.deactivate()
})
</script>

<style scoped>
.matching-demo {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.toolbar {
  display: flex;
  gap: 1rem;
  align-items: center;
  background: white;
  padding: 1rem;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.btn {
  padding: 0.5rem 1.5rem;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: opacity 0.3s;
}

.btn:hover {
  opacity: 0.8;
}

.btn-primary {
  background: #1890ff;
  color: white;
}

.btn-success {
  background: #52c41a;
  color: white;
}

.btn-danger {
  background: #ff4d4f;
  color: white;
}

.tip {
  color: #666;
  font-size: 0.85rem;
  margin-left: auto;
}

.tables-container {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.table-section {
  background: white;
  padding: 1rem;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.table-section h3 {
  margin-bottom: 0.75rem;
  color: #333;
  font-size: 1rem;
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
</style>
