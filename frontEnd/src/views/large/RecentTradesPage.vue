<template>
  <div class="recent-trades-page">
    <div class="header card">
      <h3>{{ isRecent ? '最近成交' : '失败订单' }}</h3>
      <button class="btn" @click="loadData">刷新</button>
    </div>

    <div v-if="isRecent" class="card table-section">
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
          <tr v-for="trade in recentTrades" :key="trade.execId">
            <td>{{ trade.execId }}</td>
            <td>{{ trade.securityId }}</td>
            <td>{{ trade.buyShareHolderId }}</td>
            <td>{{ trade.sellShareHolderId }}</td>
            <td class="price">{{ trade.execPrice.toFixed(2) }}</td>
            <td>{{ trade.execQty }}</td>
            <td>{{ formatTime(trade.execTime) }}</td>
          </tr>
          <tr v-if="recentTrades.length === 0">
            <td colspan="7" class="empty">暂无成交数据</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-else class="card table-section">
      <table class="data-table">
        <thead>
          <tr>
            <th>订单号</th>
            <th>股票</th>
            <th>股东</th>
            <th>方向</th>
            <th>价格</th>
            <th>数量</th>
            <th>拒绝码</th>
            <th>拒绝时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="illegal in illegalOrders" :key="illegal.clOrderId + '-' + illegal.rejectTime">
            <td>{{ illegal.clOrderId }}</td>
            <td>{{ illegal.securityId }}</td>
            <td>{{ illegal.shareHolderId }}</td>
            <td :class="illegal.side === 'B' ? 'buy' : 'sell'">{{ illegal.side === 'B' ? '买入' : '卖出' }}</td>
            <td class="price">{{ illegal.price.toFixed(2) }}</td>
            <td>{{ illegal.qty }}</td>
            <td>{{ illegal.rejectCode }}</td>
            <td>{{ formatTime(illegal.rejectTime) }}</td>
          </tr>
          <tr v-if="illegalOrders.length === 0">
            <td colspan="8" class="empty">暂无失败数据</td>
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

const isRecent = computed(() => props.activeTab === 'recent')

const recentTrades = computed(() => {
  return [...trades.value].sort((a, b) => b.execTime - a.execTime)
})

const loadData = async () => {
  const res = await orderApi.getAll()
  trades.value = res.data.tradeSuccesses || []
  illegalOrders.value = res.data.tradeIllegals || []
}

const formatTime = (timestamp: number) => {
  return new Date(timestamp).toLocaleString('zh-CN', { hour12: false })
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
