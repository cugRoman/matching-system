<template>
  <div class="query-container">
    <div class="header">
      <h2>成交订单查询 (数据库实况)</h2>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <div class="input-group">
        <label>股东号：</label>
        <input
          v-model="searchShareHolderId"
          placeholder="请输入精确股东号 (如 SH001)"
          @keyup.enter="handleSearch"
        />
      </div>
      <div class="action-btns">
        <button class="btn primary" @click="handleSearch">查 询</button>
        <button class="btn default" @click="handleReset">
          重 置 / 最近订单
        </button>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="table-wrapper">
      <table>
        <thead>
          <tr>
            <th>成交编号</th>
            <th>股票代码</th>
            <th>买方订单</th>
            <th>卖方订单</th>
            <th>买方股东</th>
            <th>卖方股东</th>
            <th>成交数量</th>
            <th>成交价格</th>
            <th>成交时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="trade in trades" :key="trade.execId">
            <td class="id-col">{{ trade.execId }}</td>
            <td class="bold">{{ trade.securityId }}</td>
            <td class="muted">{{ trade.buyOrderId }}</td>
            <td class="muted">{{ trade.sellOrderId }}</td>
            <td :class="{ highlight: isHighlight(trade.buyShareHolderId) }">
              {{ trade.buyShareHolderId }}
            </td>
            <td :class="{ highlight: isHighlight(trade.sellShareHolderId) }">
              {{ trade.sellShareHolderId }}
            </td>
            <td class="bold qty">{{ trade.execQty }}</td>
            <td class="bold price">￥{{ trade.execPrice.toFixed(2) }}</td>
            <td>{{ formatTime(trade.execTime) }}</td>
          </tr>
          <tr v-if="trades.length === 0">
            <td colspan="9" class="empty-data">暂无相关的成交记录</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { orderApi, TradeSuccess } from "../api/order";

const searchShareHolderId = ref("");
const trades = ref<TradeSuccess[]>([]);
const currentSearch = ref(""); // 记录当前高亮的股东号

// 加载数据
const loadData = async (holderId?: string) => {
  try {
    currentSearch.value = holderId || "";
    const res = await orderApi.queryTrades(holderId);
    trades.value = res.data;
  } catch (error) {
    console.error("获取成交记录失败:", error);
    alert("获取数据失败，请检查后端或数据库连接");
  }
};

// 按钮事件
const handleSearch = () => loadData(searchShareHolderId.value.trim());
const handleReset = () => {
  searchShareHolderId.value = "";
  loadData();
};

// 辅助方法：判断是否需要高亮显示的股东号
const isHighlight = (id: string) => {
  return (
    currentSearch.value &&
    id.toUpperCase() === currentSearch.value.toUpperCase()
  );
};

// 辅助方法：时间戳格式化
const formatTime = (timestamp: number) => {
  if (!timestamp) return "-";
  const d = new Date(timestamp);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")} ${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}:${String(d.getSeconds()).padStart(2, "0")}`;
};

// 页面加载时默认获取最近订单
onMounted(() => {
  loadData();
});
</script>

<style scoped>
.query-container {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
  font-family:
    -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue",
    Arial, sans-serif;
  color: #333;
}

.header h2 {
  margin-top: 0;
  color: #2c3e50;
  border-left: 5px solid #409eff;
  padding-left: 10px;
}

.search-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #f8f9fa;
  padding: 16px 20px;
  border-radius: 8px;
  margin-bottom: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.input-group {
  display: flex;
  align-items: center;
}

.input-group label {
  font-weight: 500;
  margin-right: 10px;
}

.input-group input {
  width: 260px;
  padding: 8px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  outline: none;
  transition: border-color 0.2s;
  font-size: 14px;
}

.input-group input:focus {
  border-color: #409eff;
}

.action-btns .btn {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  margin-left: 10px;
  transition: all 0.2s;
}

.btn.primary {
  background: #409eff;
  color: white;
}
.btn.primary:hover {
  background: #66b1ff;
}

.btn.default {
  background: #fff;
  border: 1px solid #dcdfe6;
  color: #606266;
}
.btn.default:hover {
  color: #409eff;
  border-color: #c6e2ff;
  background-color: #ecf5ff;
}

.table-wrapper {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

thead th {
  background-color: #f5f7fa;
  color: #909399;
  font-weight: 600;
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
}

tbody td {
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
  color: #606266;
  font-size: 14px;
}

tbody tr:hover {
  background-color: #f5f7fa;
}

.id-col {
  font-size: 12px;
  color: #909399;
}
.bold {
  font-weight: bold;
  color: #303133;
}
.muted {
  color: #c0c4cc;
  font-size: 12px;
}
.qty {
  color: #e6a23c;
}
.price {
  color: #f56c6c;
}

.highlight {
  background-color: #fff8e6;
  color: #e6a23c;
  font-weight: bold;
  padding: 2px 6px;
  border-radius: 4px;
}

.empty-data {
  text-align: center;
  padding: 40px;
  color: #909399;
}
</style>
