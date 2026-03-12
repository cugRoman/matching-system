<template>
  <div class="submit-order-page">
    <div v-if="isForm" class="manual-form card">
      <h3>手动下单</h3>
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
      <div class="actions">
        <button class="btn btn-primary" @click="submitManualOrder">提交订单</button>
        <button class="btn btn-success" @click="executeMatch">立即撮合</button>
      </div>
    </div>

    <div v-if="isImport" class="card import-card">
      <h3>批量导入</h3>
      <p>支持 `.txt` / `.json`，会按现有撮合逻辑入库。</p>
      <div class="actions">
        <button @click="triggerFileInput" class="btn btn-primary">选择并导入文件</button>
        <button class="btn btn-muted" @click="loadData">刷新状态</button>
      </div>
      <input
        ref="fileInput"
        type="file"
        style="display: none"
        accept=".txt,.json"
        @change="handleFileImport"
      />
    </div>

    <div v-if="isQueue" class="card">
      <div class="queue-header">
        <h3>交易所挂单(最近20条)</h3>
        <div class="actions">
          <button class="btn btn-success" @click="executeMatch">开始撮合</button>
          <button class="btn btn-danger" @click="clearData">清空数据</button>
          <button class="btn btn-muted" @click="loadData">刷新</button>
        </div>
      </div>
      <table class="data-table">
        <thead>
          <tr>
            <th>订单号</th>
            <th>方向</th>
            <th>股票</th>
            <th>股东</th>
            <th>价格</th>
            <th>数量</th>
            <th>状态</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="order in exchangeOrders" :key="order.clOrderId + '-' + order.timestamp">
            <td>{{ order.clOrderId }}</td>
            <td :class="order.side === 'B' ? 'buy' : 'sell'">{{ order.side === 'B' ? '买入' : '卖出' }}</td>
            <td>{{ order.securityId }}</td>
            <td>{{ order.shareHolderId }}</td>
            <td class="price">{{ order.price.toFixed(2) }}</td>
            <td>{{ order.qty }}</td>
            <td>{{ getStatusText(order.status) }}</td>
          </tr>
          <tr v-if="exchangeOrders.length === 0">
            <td colspan="7" class="empty">暂无数据</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { orderApi, type Order, type OrderRequest } from '../../api/order'

const props = withDefaults(defineProps<{ activeTab?: string }>(), {
  activeTab: 'form'
})

const exchangeBuys = ref<Order[]>([])
const exchangeSells = ref<Order[]>([])
const fileInput = ref<HTMLInputElement | null>(null)

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

const isForm = computed(() => props.activeTab === 'form')
const isImport = computed(() => props.activeTab === 'import')
const isQueue = computed(() => props.activeTab === 'queue')

const exchangeOrders = computed(() => {
  return [...exchangeBuys.value, ...exchangeSells.value]
    .sort((a, b) => b.timestamp - a.timestamp)
    .slice(0, 20)
})

const loadData = async () => {
  const res = await orderApi.getAll()
  exchangeBuys.value = res.data.exchangeBuys || []
  exchangeSells.value = res.data.exchangeSells || []
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
    alert('数量需要为正整数，价格需要大于0')
    return
  }

  await orderApi.addOrder(payload)
  manualOrder.value = {
    ...manualOrder.value,
    clOrderId: '',
    securityId: '',
    qty: 100,
    price: 10,
    shareHolderId: ''
  }
  await loadData()
}

const handleFileImport = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  const text = await file.text()
  const orders = file.name.endsWith('.json') ? JSON.parse(text) : parseTxtFile(text)
  await orderApi.addOrders(orders)
  target.value = ''
  await loadData()
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
      side: parts[3] === 'S' ? 'S' : 'B',
      qty: parseInt(parts[4], 10) || 0,
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
  await loadData()
}

const clearData = async () => {
  await orderApi.clear()
  await loadData()
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
})
</script>

<style scoped>
.submit-order-page {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 1rem;
}

.card h3 {
  margin-bottom: 0.75rem;
}

.manual-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(150px, 1fr));
  gap: 0.75rem;
}

.manual-grid label {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  font-size: 0.85rem;
}

.manual-grid input,
.manual-grid select {
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  padding: 0.5rem;
}

.actions {
  display: flex;
  gap: 0.6rem;
  margin-top: 0.8rem;
}

.btn {
  border: none;
  border-radius: 6px;
  color: #fff;
  padding: 0.5rem 0.85rem;
  cursor: pointer;
}

.btn-primary { background: #2563eb; }
.btn-success { background: #16a34a; }
.btn-danger { background: #dc2626; }
.btn-muted { background: #475569; }

.import-card p {
  color: #475569;
}

.queue-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.86rem;
}

.data-table th,
.data-table td {
  padding: 0.52rem;
  border-bottom: 1px solid #f1f5f9;
  text-align: left;
}

.data-table th { background: #f8fafc; }
.price { font-family: monospace; }
.buy { color: #dc2626; }
.sell { color: #16a34a; }
.empty { text-align: center; color: #64748b; }

@media (max-width: 1200px) {
  .manual-grid { grid-template-columns: repeat(2, minmax(160px, 1fr)); }
}
</style>
