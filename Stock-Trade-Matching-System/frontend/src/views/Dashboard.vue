<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getRecentExecutions } from '@/api/marketApi'
import type { RecentExecution } from '@/types'

const loading = ref(false)
const rows = ref<RecentExecution[]>([])
const lastUpdated = ref('')
let timer: number | undefined

const filters = reactive({
  market: '',
  securityId: '',
})

const filteredRows = computed(() => {
  return rows.value.filter((row) => {
    if (filters.market && row.market !== filters.market) {
      return false
    }
    if (filters.securityId && row.securityId !== filters.securityId.trim()) {
      return false
    }
    return true
  })
})

async function refresh() {
  loading.value = true
  try {
    rows.value = await getRecentExecutions()
    lastUpdated.value = new Date().toLocaleString()
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '加载市场快照失败')
  } finally {
    loading.value = false
  }
}

function clearFilters() {
  filters.market = ''
  filters.securityId = ''
}

onMounted(() => {
  refresh()
  timer = window.setInterval(refresh, 5000)
})

onUnmounted(() => {
  if (timer) {
    window.clearInterval(timer)
  }
})
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div class="head">
        <div>
          <h3>每个市场每支股票最近成交</h3>
          <p class="hint">自动每 5 秒刷新一次</p>
        </div>
        <div class="actions">
          <el-tag type="info">{{ lastUpdated || '未刷新' }}</el-tag>
          <el-button @click="refresh">手动刷新</el-button>
        </div>
      </div>
    </template>

    <div class="filters">
      <el-select v-model="filters.market" clearable placeholder="按市场筛选">
        <el-option label="XSHG" value="XSHG" />
        <el-option label="XSHE" value="XSHE" />
        <el-option label="BJSE" value="BJSE" />
      </el-select>
      <el-input v-model="filters.securityId" clearable maxlength="6" placeholder="按证券代码筛选" />
      <el-button @click="clearFilters">清空筛选</el-button>
    </div>

    <el-table :data="filteredRows" v-loading="loading" stripe border>
      <el-table-column prop="market" label="市场" width="100" />
      <el-table-column prop="securityId" label="证券代码" width="120" />
      <el-table-column prop="execId" label="最近成交编号" min-width="160" />
      <el-table-column prop="execQty" label="最近成交数量" width="130" />
      <el-table-column prop="execPrice" label="最近成交价格" width="130" />
      <el-table-column prop="executionCount" label="累计成交记录数" width="130" />
      <el-table-column label="最近成交时间" min-width="180">
        <template #default="{ row }">
          {{ row.executedAt ? new Date(row.executedAt).toLocaleString() : '-' }}
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && filteredRows.length === 0" description="暂无成交数据" />
  </el-card>
</template>

<style scoped>
h3 {
  margin: 0;
}

.head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
}

.hint {
  margin: 6px 0 0;
  color: #6b7280;
}

.actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filters {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
</style>
