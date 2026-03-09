<template>
  <div class="position-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>我的股票持仓</span>
          <el-button
            type="primary"
            size="small"
            @click="fetchPositions"
            :loading="loading"
          >
            刷新最新持仓
          </el-button>
        </div>
      </template>

      <!-- Element Plus 的表格组件 -->
      <el-table
        :data="positions"
        v-loading="loading"
        style="width: 100%"
        border
      >
        <!-- 市场列，加了个颜色标签 -->
        <el-table-column prop="market" label="交易市场" width="120">
          <template #default="{ row }">
            <el-tag :type="row.market === 'XSHG' ? 'danger' : 'primary'">
              {{
                row.market === "XSHG"
                  ? "上交所"
                  : row.market === "XSHE"
                    ? "深交所"
                    : row.market
              }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="securityId" label="股票代码" width="150" />

        <el-table-column prop="totalQty" label="总持仓 (股)" />

        <!-- 可用数量，设为绿色 -->
        <el-table-column prop="availableQty" label="可用数量 (股)">
          <template #default="{ row }">
            <span style="color: #67c23a; font-weight: bold">{{
              row.availableQty
            }}</span>
          </template>
        </el-table-column>

        <!-- 冻结数量，设为红色 -->
        <el-table-column prop="frozenQty" label="冻结数量 (股)">
          <template #default="{ row }">
            <span style="color: #f56c6c; font-weight: bold">{{
              row.frozenQty
            }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { ElMessage } from "element-plus";
import { getMyPositions, type Position } from "@/api/positionApi";

// 存放表格数据的变量
const positions = ref<Position[]>([]);
// 控制转圈圈加载动画的变量
const loading = ref(false);

// 核心逻辑：调用 API 获取数据并赋给 positions 变量
const fetchPositions = async () => {
  loading.value = true;
  try {
    positions.value = await getMyPositions();
  } catch (error) {
    ElMessage.error("获取持仓数据失败，请检查是否已登录");
    console.error(error);
  } finally {
    loading.value = false;
  }
};

// 页面一打开，就自动执行一次获取数据
onMounted(() => {
  fetchPositions();
});
</script>

<style scoped>
.position-container {
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}
</style>
