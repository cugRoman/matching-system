<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyOrders, submitOrder } from '@/api/orderApi'
import type { MarketCode, OrderRequest, OrderSide, UserOrder } from '@/types'
import { validateOrderRequest } from '@/utils/orderValidation'

const submitting = ref(false)
const loadingOrders = ref(false)
const orders = ref<UserOrder[]>([])

const form = reactive({
  market: 'XSHG' as MarketCode,
  securityId: '',
  side: 'B' as OrderSide,
  qty: 100,
  price: 10,
  tradeDate: '',
})

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

async function loadOrders() {
  loadingOrders.value = true
  try {
    orders.value = await getMyOrders()
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '加载订单失败')
  } finally {
    loadingOrders.value = false
  }
}

async function handleSubmit() {
  const payload: OrderRequest = {
    market: form.market,
    securityId: form.securityId,
    side: form.side,
    qty: form.qty,
    price: Number(form.price),
    tradeDate: form.tradeDate || undefined,
  }

  const validationMessage = validateOrderRequest(payload)
  if (validationMessage) {
    ElMessage.warning(validationMessage)
    return
  }

  submitting.value = true
  try {
    const result = await submitOrder(payload)

    if (result.resultType === 'ACCEPTED') {
      ElMessage.success(`订单已接收：${result.clOrderId}`)
    } else if (result.resultType === 'REJECTED_WASH') {
      ElMessage.error(`对敲拦截：${result.message}`)
    } else {
      ElMessage.error(`订单被拒绝：${result.message}`)
    }

    await loadOrders()
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

onMounted(loadOrders)
</script>

<template>
  <div class="panel-grid">
    <el-card shadow="never">
      <template #header>
        <h3>提交订单</h3>
      </template>

      <el-form label-position="top" class="order-form">
        <el-form-item label="市场">
          <el-select v-model="form.market">
            <el-option label="上交所 XSHG" value="XSHG" />
            <el-option label="深交所 XSHE" value="XSHE" />
            <el-option label="北交所 BJSE" value="BJSE" />
          </el-select>
        </el-form-item>

        <el-form-item label="证券代码">
          <el-input v-model="form.securityId" maxlength="6" placeholder="示例：600519" />
        </el-form-item>

        <el-form-item label="方向">
          <el-segmented
            v-model="form.side"
            :options="[
              { label: '买入 B', value: 'B' },
              { label: '卖出 S', value: 'S' },
            ]"
          />
        </el-form-item>

        <el-form-item label="数量">
          <el-input-number v-model="form.qty" :min="1" :step="1" :controls-position="'right'" />
        </el-form-item>

        <el-form-item label="价格">
          <el-input-number
            v-model="form.price"
            :min="0.01"
            :step="0.01"
            :precision="2"
            :controls-position="'right'"
          />
        </el-form-item>

        <el-form-item label="交易日（可选）">
          <el-date-picker
            v-model="form.tradeDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="默认使用今天"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">提交订单</el-button>
          <el-button @click="loadOrders">刷新我的订单</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="table-header">
          <h3>我的订单</h3>
          <el-tag type="info">{{ orders.length }} 笔</el-tag>
        </div>
      </template>

      <el-table :data="orders" v-loading="loadingOrders" stripe border>
        <el-table-column prop="clOrderId" label="订单号" min-width="160" />
        <el-table-column prop="market" label="市场" width="90" />
        <el-table-column prop="securityId" label="证券" width="100" />
        <el-table-column prop="side" label="方向" width="90">
          <template #default="{ row }">
            <el-tag :type="row.side === 'B' ? 'danger' : 'success'" size="small">
              {{ row.side === 'B' ? '买入' : '卖出' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="qty" label="委托量" width="92" />
        <el-table-column prop="qtyFilled" label="已成交" width="92" />
        <el-table-column prop="qtyRemaining" label="剩余" width="92" />
        <el-table-column prop="price" label="价格" width="92" />
        <el-table-column prop="status" label="状态" min-width="120">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
h3 {
  margin: 0;
}

.panel-grid {
  display: grid;
  gap: 16px;
}

.order-form {
  max-width: 460px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
