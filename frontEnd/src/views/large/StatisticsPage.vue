<template>
  <div class="statistics-page">
    <div class="header card">
      <h3>{{ isOverview ? '统计总览' : '按股票统计' }}</h3>
      <button class="btn" @click="loadData">刷新</button>
    </div>

    <div v-if="isOverview" class="cards">
      <div class="card stat-card">
        <div class="label">待撮合挂单</div>
        <div class="value">{{ pendingCount }}</div>
      </div>
      <div class="card stat-card">
        <div class="label">成功成交笔数</div>
        <div class="value">{{ successCount }}</div>
      </div>
      <div class="card stat-card">
        <div class="label">失败单数</div>
        <div class="value">{{ illegalCount }}</div>
      </div>
      <div class="card stat-card">
        <div class="label">累计成交量</div>
        <div class="value">{{ totalQty }}</div>
      </div>
      <div class="card stat-card">
        <div class="label">累计成交额</div>
        <div class="value">{{ totalTurnover.toFixed(2) }}</div>
      </div>
      <div class="card stat-card">
        <div class="label">成交均价</div>
        <div class="value">{{ averagePrice.toFixed(2) }}</div>
      </div>
    </div>

    <div v-else class="card table-section">
      <table class="data-table">
        <thead>
          <tr>
            <th>股票代码</th>
            <th>成交笔数</th>
            <th>成交总量</th>
            <th>成交总额</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in stockStats" :key="item.securityId">
            <td>{{ item.securityId }}</td>
            <td>{{ item.count }}</td>
            <td>{{ item.qty }}</td>
            <td>{{ item.turnover.toFixed(2) }}</td>
          </tr>
          <tr v-if="stockStats.length === 0">
            <td colspan="4" class="empty">暂无统计数据</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { orderApi, type Order, type TradeIllegal, type TradeSuccess } from '../../api/order'

const props = withDefaults(defineProps<{ activeTab?: string }>(), {
  activeTab: 'overview'
})

const exchangeBuys = ref<Order[]>([])
const exchangeSells = ref<Order[]>([])
const tradeSuccesses = ref<TradeSuccess[]>([])
const tradeIllegals = ref<TradeIllegal[]>([])

const isOverview = computed(() => props.activeTab === 'overview')

const pendingCount = computed(() => exchangeBuys.value.length + exchangeSells.value.length)
const successCount = computed(() => tradeSuccesses.value.length)
const illegalCount = computed(() => tradeIllegals.value.length)

const totalQty = computed(() => tradeSuccesses.value.reduce((sum, item) => sum + item.execQty, 0))
const totalTurnover = computed(() => tradeSuccesses.value.reduce((sum, item) => sum + item.execQty * item.execPrice, 0))

const averagePrice = computed(() => {
  if (totalQty.value === 0) return 0
  return totalTurnover.value / totalQty.value
})

const stockStats = computed(() => {
  const map: Record<string, { securityId: string; count: number; qty: number; turnover: number }> = {}

  for (const item of tradeSuccesses.value) {
    if (!map[item.securityId]) {
      map[item.securityId] = {
        securityId: item.securityId,
        count: 0,
        qty: 0,
        turnover: 0
      }
    }
    map[item.securityId].count += 1
    map[item.securityId].qty += item.execQty
    map[item.securityId].turnover += item.execQty * item.execPrice
  }

  return Object.values(map).sort((a, b) => b.qty - a.qty)
})

const loadData = async () => {
  const res = await orderApi.getAll()
  exchangeBuys.value = res.data.exchangeBuys || []
  exchangeSells.value = res.data.exchangeSells || []
  tradeSuccesses.value = res.data.tradeSuccesses || []
  tradeIllegals.value = res.data.tradeIllegals || []
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.statistics-page {
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

.cards {
  display: grid;
  grid-template-columns: repeat(3, minmax(140px, 1fr));
  gap: 0.75rem;
}

.stat-card {
  box-shadow: inset 0 0 0 1px #eff6ff;
}

.label {
  font-size: 0.82rem;
  color: #64748b;
}

.value {
  margin-top: 0.45rem;
  font-size: 1.25rem;
  font-weight: 600;
  color: #0f172a;
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
.empty { text-align: center; color: #64748b; }

@media (max-width: 980px) {
  .cards { grid-template-columns: repeat(2, minmax(140px, 1fr)); }
}
</style>
