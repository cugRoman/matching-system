<template>
  <div class="recent-trades-page">
    <div class="header card">
      <h3>查找交易记录</h3>
      <div class="header-actions">
        <button class="btn" @click="loadData">刷新</button>
      </div>
    </div>

    <div class="search-form card">
      <div class="search-grid">
        <label>
          买家股东
          <input v-model="searchParams.buyHolder" placeholder="SH001" />
        </label>
        <label>
          卖家股东
          <input v-model="searchParams.sellHolder" placeholder="SH002" />
        </label>
        <label>
          股票代码
          <input v-model="searchParams.securityId" placeholder="600519" />
        </label>
        <label>
          开始时间
          <input v-model="searchParams.startTime" type="datetime-local" />
        </label>
        <label>
          结束时间
          <input v-model="searchParams.endTime" type="datetime-local" />
        </label>
        <label class="btn-label">
          <button class="btn btn-primary" @click="doSearch">查询</button>
          <button class="btn" @click="resetSearch">重置</button>
        </label>
      </div>
    </div>

    <div class="card table-section">
      <table class="data-table">
        <thead>
          <tr>
            <th>成交号</th>
            <th>股票</th>
            <th>买方股东</th>
            <th>卖方股东</th>
            <th>价格</th>
            <th>数量</th>
            <th>成交时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="trade in filteredTrades" :key="trade.execId">
            <td>{{ trade.execId }}</td>
            <td>{{ trade.securityId }}</td>
            <td>{{ trade.buyShareHolderId }}</td>
            <td>{{ trade.sellShareHolderId }}</td>
            <td class="price">{{ trade.execPrice.toFixed(2) }}</td>
            <td>{{ trade.execQty }}</td>
            <td>{{ formatTime(trade.execTime) }}</td>
          </tr>
          <tr v-if="filteredTrades.length === 0">
            <td colspan="7" class="empty">暂无成交数据</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { orderApi, type TradeIllegal, type TradeSuccess } from '../../api/order'

const props = withDefaults(defineProps<{ activeTab?: string }>(), {
  activeTab: 'recent'
})

const trades = ref<TradeSuccess[]>([])
const illegalOrders = ref<TradeIllegal[]>([])

const searchParams = ref({
  buyHolder: '',
  sellHolder: '',
  securityId: '',
  startTime: '',
  endTime: ''
})

const isRecent = computed(() => props.activeTab === 'recent')

const filteredTrades = computed(() => {
  let result = [...trades.value]
  
  if (searchParams.value.buyHolder) {
    const holder = searchParams.value.buyHolder.trim().toUpperCase()
    result = result.filter(t => t.buyShareHolderId.toUpperCase().includes(holder))
  }
  
  if (searchParams.value.sellHolder) {
    const holder = searchParams.value.sellHolder.trim().toUpperCase()
    result = result.filter(t => t.sellShareHolderId.toUpperCase().includes(holder))
  }
  
  if (searchParams.value.securityId) {
    const security = searchParams.value.securityId.trim().toUpperCase()
    result = result.filter(t => t.securityId.toUpperCase().includes(security))
  }
  
  if (searchParams.value.startTime) {
    const startMs = new Date(searchParams.value.startTime).getTime()
    result = result.filter(t => t.execTime >= startMs)
  }
  
  if (searchParams.value.endTime) {
    const endMs = new Date(searchParams.value.endTime).getTime()
    result = result.filter(t => t.execTime <= endMs)
  }
  
  return result.sort((a, b) => b.execTime - a.execTime)
})

const recentTrades = computed(() => {
  return [...trades.value].sort((a, b) => b.execTime - a.execTime)
})

const loadData = async () => {
  const res = await orderApi.getAll()
  trades.value = res.data.tradeSuccesses || []
  illegalOrders.value = res.data.tradeIllegals || []
}

const doSearch = () => {
  // 搜索逻辑在computed中自动处理
}

const resetSearch = () => {
  searchParams.value = {
    buyHolder: '',
    sellHolder: '',
    securityId: '',
    startTime: '',
    endTime: ''
  }
}

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

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.recent-trades-page {
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
}

.card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 0.9rem;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.btn {
  border: none;
  background: #2563eb;
  color: #fff;
  padding: 0.45rem 0.9rem;
  border-radius: 6px;
  cursor: pointer;
}

.btn-primary {
  background: #16a34a;
}

.header-actions {
  display: flex;
  gap: 0.5rem;
}

.search-form {
  padding: 0.9rem;
}

.search-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.8rem;
}

.search-grid label {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  font-size: 0.85rem;
  color: #475569;
}

.search-grid input {
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  padding: 0.4rem 0.5rem;
  font-size: 0.85rem;
}

.btn-label {
  flex-direction: row !important;
  align-items: flex-end;
  gap: 0.5rem !important;
}

.btn-label .btn {
  padding: 0.4rem 0.8rem;
  font-size: 0.85rem;
}

.table-section { overflow-x: auto; }

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  text-align: left;
  padding: 0.55rem;
  border-bottom: 1px solid #f1f5f9;
  font-size: 0.88rem;
}

.data-table th { background: #f8fafc; }
.price { font-family: monospace; }
.buy { color: #dc2626; }
.sell { color: #16a34a; }
.empty { text-align: center; color: #64748b; }
</style>
