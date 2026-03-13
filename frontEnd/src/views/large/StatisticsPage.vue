<template>
  <div class="statistics-page">
    <div class="header card">
      <h3>{{ isOverview ? '统计总览' : '按股票统计' }}</h3>
      <button class="btn" @click="loadData">刷新</button>
    </div>

    <div v-if="isOverview">
      <div class="cards">
        <div class="card stat-card">
          <div class="label">待撮合挂单数量</div>
          <div class="value">{{ pendingCount }}</div>
        </div>
        <div class="card stat-card">
          <div class="label">成功成交订单量</div>
          <div class="value">{{ successCount }}</div>
        </div>
        <div class="card stat-card">
          <div class="label">撮合失败订单量</div>
          <div class="value">{{ illegalCount }}</div>
        </div>
        <div class="card stat-card">
          <div class="label">总成交量（股）</div>
          <div class="value">{{ totalQty }}</div>
        </div>
        <div class="card stat-card">
          <div class="label">总成交额（元）</div>
          <div class="value">{{ totalTurnover.toFixed(2) }}</div>
        </div>
        <div class="card stat-card">
          <div class="label">成交均价（元）</div>
          <div class="value">{{ averagePrice.toFixed(2) }}</div>
        </div>
      </div>

      <div class="card pie-card">
        <div class="pie-title">订单状态分布</div>
        <div class="pie-content">
          <template v-if="fanSlices.length">
            <svg viewBox="0 0 100 100" class="pie-chart">
              <circle
                v-if="fanSlices.length === 1"
                cx="50"
                cy="50"
                r="45"
                :fill="fanSlices[0].color"
              />
              <path
                v-for="slice in fanSlices"
                :key="slice.label"
                class="fan-slice"
                :d="slice.path"
                :fill="slice.color"
                v-else
              />
            </svg>
            <div class="pie-legend">
              <div
                v-for="slice in fanSlices"
                :key="slice.label"
                class="legend-item"
              >
                <span class="legend-dot" :style="{ backgroundColor: slice.color }" />
                <span class="legend-label">{{ slice.label }}</span>
                <span class="legend-value">{{ slice.value }}</span>
              </div>
            </div>
          </template>
          <template v-else>
            <div class="pie-empty">暂无订单数据</div>
          </template>
        </div>
      </div>

      <div class="card bar-card">
        <div class="bar-header">
          <div class="bar-title">成交信息</div>
          <div class="bar-subtitle">单位：万</div>
        </div>
        <div class="bar-content">
          <div class="bar-y-axis">
            <span v-for="tick in barTicks" :key="tick" class="bar-y-tick">
              {{ tick.toFixed(1) }}
            </span>
          </div>
          <div class="bar-chart-wrap">
            <div class="bar-grid">
              <div
                v-for="tick in barTicks"
                :key="tick"
                class="bar-grid-line"
              />
            </div>
            <div class="bar-bars">
              <div class="bar-item">
                <div
                  class="bar-rect volume"
                  :style="{ height: barHeight(volumeWan) }"
                  title="总成交量"
                />
              </div>
              <div class="bar-item">
                <div
                  class="bar-rect amount"
                  :style="{ height: barHeight(amountWan) }"
                  title="总成交额"
                />
              </div>
            </div>
          </div>
        </div>
        <div class="bar-x-labels">
          <div class="bar-x-item">
            <div class="bar-label">总成交量</div>
            <div class="bar-value">{{ volumeWan.toFixed(2) }}</div>
          </div>
          <div class="bar-x-item">
            <div class="bar-label">总成交额</div>
            <div class="bar-value">{{ amountWan.toFixed(2) }}</div>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="card table-section">
      <table class="data-table">
        <thead>
          <tr>
            <th>股票代码</th>
            <th>成交笔数</th>
            <th>成交总额</th>
            <th>成交均价</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in stockStats" :key="item.securityId">
            <td>{{ item.securityId }}</td>
            <td>{{ item.count }}</td>
            <td>{{ item.turnover.toFixed(2) }}</td>
            <td>{{ item.avgPrice.toFixed(2) }}</td>
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

const pendingCount = computed(() => {
  // 只统计仍在挂单中的订单：数量大于 0 且状态不是“已完全成交/非法”等终态
  const isPending = (o: Order) => o.qty > 0 && ![3, 4].includes(o.status)
  return (
    exchangeBuys.value.filter(isPending).length +
    exchangeSells.value.filter(isPending).length
  )
})
const successCount = computed(() => tradeSuccesses.value.length)
const illegalCount = computed(() => tradeIllegals.value.length)

const totalQty = computed(() => tradeSuccesses.value.reduce((sum, item) => sum + item.execQty, 0))
const totalTurnover = computed(() => tradeSuccesses.value.reduce((sum, item) => sum + item.execQty * item.execPrice, 0))

const averagePrice = computed(() => {
  if (totalQty.value === 0) return 0
  return totalTurnover.value / totalQty.value
})

const stockStats = computed(() => {
  const map: Record<
    string,
    { securityId: string; count: number; qty: number; turnover: number; avgPrice: number }
  > = {}

  for (const item of tradeSuccesses.value) {
    if (!map[item.securityId]) {
      map[item.securityId] = {
        securityId: item.securityId,
        count: 0,
        qty: 0,
        turnover: 0,
        avgPrice: 0
      }
    }
    map[item.securityId].count += 1
    map[item.securityId].qty += item.execQty
    map[item.securityId].turnover += item.execQty * item.execPrice
  }

  const list = Object.values(map)
  for (const item of list) {
    item.avgPrice = item.qty === 0 ? 0 : item.turnover / item.qty
  }

  return list.sort((a, b) => b.qty - a.qty)
})

const fanSlices = computed(() => {
  const base = [
    {
      label: '待撮合订单数量',
      value: pendingCount.value,
      color: '#fb7185'
    },
    {
      label: '成功成交订单数量',
      value: successCount.value,
      color: '#22c55e'
    },
    {
      label: '撮合失败订单数量',
      value: illegalCount.value,
      color: '#9ca3af'
    }
  ]

  const hasData = base.some(item => item.value > 0)

  // 没有任何订单时，画一个灰色扇形占位
  if (!hasData) {
    const radius = 45
    const center = 50
    const startAngle = -0.5 * Math.PI
    const endAngle = startAngle + Math.PI * 2

    const x1 = center + radius * Math.cos(startAngle)
    const y1 = center + radius * Math.sin(startAngle)
    const x2 = center + radius * Math.cos(endAngle)
    const y2 = center + radius * Math.sin(endAngle)

    const path = [
      `M ${center} ${center}`,
      `L ${x1.toFixed(3)} ${y1.toFixed(3)}`,
      `A ${radius} ${radius} 0 1 1 ${x2.toFixed(3)} ${y2.toFixed(3)}`,
      'Z'
    ].join(' ')

    return [
      {
        label: '暂无订单',
        value: 0,
        color: '#e5e7eb',
        path
      }
    ]
  }

  const raw = base.filter(item => item.value > 0)
  const total = raw.reduce((sum, item) => sum + item.value, 0)

  const radius = 45
  const center = 50
  let startAngle = -0.5 * Math.PI

  return raw.map(item => {
    const angle = (item.value / total) * Math.PI * 2
    const endAngle = startAngle + angle
    const largeArc = angle > Math.PI ? 1 : 0

    const x1 = center + radius * Math.cos(startAngle)
    const y1 = center + radius * Math.sin(startAngle)
    const x2 = center + radius * Math.cos(endAngle)
    const y2 = center + radius * Math.sin(endAngle)

    const path = [
      `M ${center} ${center}`,
      `L ${x1.toFixed(3)} ${y1.toFixed(3)}`,
      `A ${radius} ${radius} 0 ${largeArc} 1 ${x2.toFixed(3)} ${y2.toFixed(3)}`,
      'Z'
    ].join(' ')

    startAngle = endAngle

    return {
      ...item,
      path
    }
  })
})

const volumeWan = computed(() => totalQty.value / 10000)
const amountWan = computed(() => totalTurnover.value / 10000)

const barTicks = [0, 0.2, 0.4, 0.6, 0.8, 1]

const barHeight = (value: number) => {
  const max = 1
  if (value <= 0) return '0px'
  const ratio = Math.min(value / max, 1)
  const px = 20 + ratio * 80
  return `${px}px`
}

const loadData = async () => {
  // 统计分析页面走数据库汇总接口
  const res = await orderApi.getStats()
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

.pie-card {
  margin-top: 0.8rem;
}

.pie-title {
  font-size: 0.9rem;
  font-weight: 600;
  margin-bottom: 0.6rem;
}

.pie-content {
  display: flex;
  align-items: center;
  gap: 1.5rem;
}

.pie-chart {
  width: 150px;
  height: 150px;
}

.fan-slice {
  stroke: #ffffff;
  stroke-width: 0.5;
}

.pie-legend {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.82rem;
  color: #4b5563;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
}

.legend-label {
  min-width: 6rem;
}

.legend-value {
  font-weight: 600;
  color: #111827;
}

.pie-empty {
  padding: 1.5rem 0;
  font-size: 0.88rem;
  color: #9ca3af;
}

.bar-card {
  margin-top: 0.8rem;
}

.bar-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 0.6rem;
}

.bar-title {
  font-size: 0.9rem;
  font-weight: 600;
}

.bar-subtitle {
  font-size: 0.8rem;
  color: #9ca3af;
}

.bar-content {
  display: flex;
}

.bar-y-axis {
  display: flex;
  flex-direction: column-reverse;
  justify-content: space-between;
  margin-right: 0.5rem;
  font-size: 0.75rem;
  color: #9ca3af;
}

.bar-chart-wrap {
  flex: 1;
  height: 160px;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding: 0;
  border-bottom: 1px solid #9ca3af;
  position: relative;
}

.bar-grid {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.bar-bars {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  width: 600px; /* 与下方标签同宽：2 * 300px */
  margin: 0 auto;
  transform: translateX(-8px); /* 只移动柱形，文字不动 */
}

.bar-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.25rem;
  width: 50%;
}

.bar-rect {
  width: 34px;
  border-radius: 6px 6px 0 0;
  background: #3b82f6;
  box-shadow: 0 4px 8px rgba(37, 99, 235, 0.35);
  transition: height 0.2s ease;
  margin: 0 auto 0px; /* 居中并轻微压线，保证视觉对齐 */
}

.bar-rect.volume {
  background: #60a5fa;
}

.bar-rect.amount {
  background: #38bdf8;
}

.bar-label {
  font-size: 0.8rem;
  color: #6b7280;
}

.bar-value {
  font-size: 0.78rem;
  color: #111827;
  font-weight: 500;
}

.bar-x-labels {
  margin-top: 0.35rem;
  display: flex;
  justify-content: center;
  gap: 0;
  width: 600px; /* 与柱形区域同宽，保证左右居中对齐 */
  margin-left: auto;
  margin-right: auto;
}

.bar-x-item {
  text-align: center;
  width: 300px; /* 与柱形区域一半宽度一致，确保文字在柱子正下方，间距更大一点 */
}

.bar-grid-line {
  position: absolute;
  left: 0;
  right: 0;
  border-top: 1px dashed #e5e7eb;
}

.bar-grid-line:nth-child(1) { /* 0.0 基线已用 border-bottom 表达，这里略过 */
  display: none;
}

.bar-grid-line:nth-child(2) { top: 128px; } /* 0.2 */
.bar-grid-line:nth-child(3) { top: 96px; }  /* 0.4 */
.bar-grid-line:nth-child(4) { top: 64px; }  /* 0.6 */
.bar-grid-line:nth-child(5) { top: 32px; }  /* 0.8 */
.bar-grid-line:nth-child(6) { top: 0; }     /* 1.0 */

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

  .pie-content {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
