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
      <button @click="submitManualOrder" class="btn btn-primary">手动添加订单</button>
      <button @click="executeMatch" class="btn btn-success">开始撮合</button>
      <button @click="clearData" class="btn btn-danger">清空数据</button>
      <span class="tip">提示: 点击导入请求选择txt文件，或使用内置示例</span>
    </div>

    <div class="manual-form">
      <h3>手动输入订单</h3>
      <div class="manual-grid">
        <label>
          订单号(可选)
          <input v-model.trim="manualOrder.clOrderId" placeholder="留空自动生成" />
        </label>
        <label>
          市场
          <input v-model.trim="manualOrder.market" placeholder="XSHG" />
        </label>
        <label>
          股票代码
          <input v-model.trim="manualOrder.securityId" placeholder="600519" />
        </label>
        <label>
          方向
          <select v-model="manualOrder.side">
            <option value="B">买入(B)</option>
            <option value="S">卖出(S)</option>
          </select>
        </label>
        <label>
          数量
          <input v-model.number="manualOrder.qty" type="number" min="1" step="1" />
        </label>
        <label>
          价格
          <input v-model.number="manualOrder.price" type="number" min="0.01" step="0.01" />
        </label>
        <label>
          股东号
          <input v-model.trim="manualOrder.shareHolderId" placeholder="SH001" />
        </label>
        <label>
          账户号
          <input v-model.trim="manualOrder.accountId" placeholder="ACC001" />
        </label>
      </div>
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

const manualOrder = ref<OrderRequest>({
  clOrderId: '',
  market: 'XSHG',
  securityId: '',
  side: 'B',
  qty: 100,
  price: 10,
  shareHolderId: '',
  accountId: 'ACC001'
})

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

const buildAutoOrderId = () => {
  const now = Date.now().toString().slice(-8)
  const rand = Math.floor(Math.random() * 1000).toString().padStart(3, '0')
  return `MAN${now}${rand}`
}

const submitManualOrder = async () => {
  const payload: OrderRequest = {
    clOrderId: manualOrder.value.clOrderId || buildAutoOrderId(),
    market: (manualOrder.value.market || 'XSHG').trim(),
    securityId: (manualOrder.value.securityId || '').trim(),
    side: manualOrder.value.side,
    qty: Number(manualOrder.value.qty),
    price: Number(manualOrder.value.price),
    shareHolderId: (manualOrder.value.shareHolderId || '').trim(),
    accountId: (manualOrder.value.accountId || 'ACC001').trim()
  }

  if (!payload.securityId || !payload.shareHolderId) {
    alert('请至少填写股票代码和股东号')
    return
  }
  if (!Number.isInteger(payload.qty) || payload.qty <= 0 || payload.price <= 0) {
    alert('数量需为正整数，价格需大于0')
    return
  }

  try {
    await orderApi.addOrder(payload)
    manualOrder.value = {
      ...manualOrder.value,
      clOrderId: '',
      securityId: '',
      qty: 100,
      price: 10,
      shareHolderId: ''
    }
  } catch (e) {
    console.error('Failed to submit order:', e)
    alert('手动添加订单失败')
  }
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

const executeMatch = async () => {
  await orderApi.match()
}

const clearData = async () => {
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

.manual-form {
  background: white;
  padding: 1rem;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.manual-form h3 {
  margin-bottom: 0.75rem;
  color: #333;
  font-size: 1rem;
}

.manual-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(160px, 1fr));
  gap: 0.75rem;
}

.manual-grid label {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  font-size: 0.82rem;
  color: #555;
}

.manual-grid input,
.manual-grid select {
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  padding: 0.45rem 0.55rem;
  font-size: 0.9rem;
}

@media (max-width: 1200px) {
  .manual-grid {
    grid-template-columns: repeat(2, minmax(160px, 1fr));
  }
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
