<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyOrders, getOrderExecutions } from '@/api/orderApi'
import type { OrderExecutionDetail, UserOrder } from '@/types'

const loading = ref(false)
const detailLoading = ref(false)
const rows = ref<UserOrder[]>([])
const detailVisible = ref(false)
const detail = ref<OrderExecutionDetail | null>(null)

const filters = reactive({
  market: '',
  securityId: '',
  status: '',
})

const statusOptions = [
  'NEW',
  'REJECTED_INVALID',
  'REJECTED_WASH',
  'ACTIVE_IN_POOL',
  'EXCH_WORKING',
  'PARTIALLY_FILLED',
  'FILLED',
  'CANCEL_REQUESTED',
  'CANCELED',
  'CANCEL_REJECTED',
]

function statusTag(status: string): 'success' | 'warning' | 'danger' | 'info' {
  if (status === 'FILLED') {
    return 'success'
  }
  if (status === 'EXCH_WORKING' || status === 'PARTIALLY_FILLED') {
    return 'warning'
  }
  if (status.startsWith('REJECTED')) {
    return 'danger'
  }
  return 'info'
}

async function refresh() {
  loading.value = true
  try {
    rows.value = await getMyOrders({
      market: filters.market || undefined,
      securityId: filters.securityId.trim() || undefined,
      status: filters.status || undefined,
    })
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '加载订单列表失败')
  } finally {
    loading.value = false
  }
}

function clearFilters() {
  filters.market = ''
  filters.securityId = ''
  filters.status = ''
  refresh()
}

async function openDetail(clOrderId: string) {
  detailLoading.value = true
  detailVisible.value = true
  try {
    detail.value = await getOrderExecutions(clOrderId)
  } catch (error: any) {
    detail.value = null
    ElMessage.error(error?.response?.data?.message || '加载订单成交明细失败')
  } finally {
    detailLoading.value = false
  }
}

onMounted(refresh)
</script>

<template>
  <div class="orders-grid">
    <el-card shadow="never">
      <template #header>
        <h3>筛选条件</h3>
      </template>

      <div class="filters">
        <el-select v-model="filters.market" clearable placeholder="市场">
          <el-option label="XSHG" value="XSHG" />
          <el-option label="XSHE" value="XSHE" />
          <el-option label="BJSE" value="BJSE" />
        </el-select>
        <el-input v-model="filters.securityId" clearable maxlength="6" placeholder="证券代码（6 位）" />
        <el-select v-model="filters.status" clearable placeholder="订单状态">
          <el-option v-for="status in statusOptions" :key="status" :label="status" :value="status" />
        </el-select>
        <el-button type="primary" @click="refresh">查询</el-button>
        <el-button @click="clearFilters">重置</el-button>
      </div>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="header-row">
          <h3>我的订单列表</h3>
          <el-tag type="info">{{ rows.length }} 笔</el-tag>
        </div>
      </template>

      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="clOrderId" label="订单号" min-width="170" />
        <el-table-column prop="market" label="市场" width="96" />
        <el-table-column prop="securityId" label="证券" width="100" />
        <el-table-column prop="side" label="方向" width="90">
          <template #default="{ row }">
            <el-tag :type="row.side === 'B' ? 'danger' : 'success'" size="small">
              {{ row.side === 'B' ? '买入' : '卖出' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="qty" label="委托量" width="90" />
        <el-table-column prop="qtyFilled" label="已成交" width="90" />
        <el-table-column prop="qtyRemaining" label="剩余" width="90" />
        <el-table-column prop="price" label="价格" width="90" />
        <el-table-column prop="status" label="状态" min-width="140">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="180">
          <template #default="{ row }">
            {{ row.updatedAt ? new Date(row.updatedAt).toLocaleString() : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="124" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openDetail(row.clOrderId)">成交明细</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && rows.length === 0" description="暂无订单数据" />
    </el-card>

    <el-drawer v-model="detailVisible" title="订单成交明细" size="50%">
      <el-skeleton v-if="detailLoading" animated :rows="6" />
      <template v-else-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ detail.clOrderId }}</el-descriptions-item>
          <el-descriptions-item label="证券">{{ detail.market }} / {{ detail.securityId }}</el-descriptions-item>
          <el-descriptions-item label="方向">{{ detail.side === 'B' ? '买入(B)' : '卖出(S)' }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ detail.status }}</el-descriptions-item>
          <el-descriptions-item label="委托量">{{ detail.qty }}</el-descriptions-item>
          <el-descriptions-item label="已成交">{{ detail.qtyFilled }}</el-descriptions-item>
          <el-descriptions-item label="剩余">{{ detail.qtyRemaining }}</el-descriptions-item>
          <el-descriptions-item label="成交笔数">{{ detail.executionCount }}</el-descriptions-item>
          <el-descriptions-item label="累计成交量">{{ detail.totalExecutedQty }}</el-descriptions-item>
          <el-descriptions-item label="成交均价">{{ detail.avgExecutedPrice }}</el-descriptions-item>
          <el-descriptions-item label="最后成交时间" :span="2">
            {{ detail.lastExecutedAt ? new Date(detail.lastExecutedAt).toLocaleString() : '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">逐笔成交</el-divider>
        <el-table :data="detail.executions" border stripe>
          <el-table-column prop="execId" label="成交ID" min-width="150" />
          <el-table-column prop="execQty" label="成交量" width="90" />
          <el-table-column prop="execPrice" label="成交价" width="100" />
          <el-table-column prop="execType" label="成交类型" width="120" />
          <el-table-column prop="counterOrderId" label="对手方订单号" min-width="160" />
          <el-table-column label="成交时间" min-width="180">
            <template #default="{ row }">
              {{ row.executedAt ? new Date(row.executedAt).toLocaleString() : '-' }}
            </template>
          </el-table-column>
        </el-table>
      </template>
      <el-empty v-else description="未获取到订单详情" />
    </el-drawer>
  </div>
</template>

<style scoped>
h3 {
  margin: 0;
}

.orders-grid {
  display: grid;
  gap: 16px;
}

.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}
</style>

