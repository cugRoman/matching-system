<template>
  <div class="large-dashboard">
    <aside class="primary-nav panel">
      <div class="panel-title">主页面</div>
      <button
        v-for="item in pages"
        :key="item.key"
        class="nav-btn"
        :class="{ active: activePage === item.key }"
        @click="switchPage(item.key)"
      >
        {{ item.label }}
      </button>
    </aside>

    <aside class="secondary-nav panel">
      <div class="panel-title">功能Tab</div>
      <button
        v-for="item in currentFeatureTabs"
        :key="item.key"
        class="nav-btn sub"
        :class="{ active: activeFeature === item.key }"
        @click="setFeature(item.key)"
      >
        {{ item.label }}
      </button>
    </aside>

    <main class="content panel">
      <component
        :is="activeComponent"
        ref="activeViewRef"
        :active-tab="activeFeature"
      />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, type Component } from "vue";
import MatchingDemo from "./MatchingDemo.vue";
import AutoSimulate from "./AutoSimulate.vue";
import RecentTradesPage from "./large/RecentTradesPage.vue";
import StatisticsPage from "./large/StatisticsPage.vue";
// 【新增】引入我们刚刚写的成交订单查询页面
import TradeQuery from "./TradeQuery.vue";

// 【修改】增加了一个 'query' 类型
type PageKey = "recent" | "stats" | "demo" | "auto" | "query";

type TabItem = {
  key: string;
  label: string;
};

// 【修改】在左侧主菜单加入“成交查询”
const pages: Array<{ key: PageKey; label: string }> = [
  { key: "recent", label: "最近成交" },
  { key: "stats", label: "统计分析" },
  { key: "demo", label: "撮合演示" },
  { key: "auto", label: "自动模拟" },
  { key: "query", label: "成交查询" }, // <-- 这里是新加的
];

// 【修改】为成交查询页面配置一个右侧的副导航Tab
const featureTabs: Record<PageKey, TabItem[]> = {
  recent: [
    { key: "recent", label: "最近成交" },
    { key: "illegal", label: "失败订单" },
  ],
  stats: [
    { key: "overview", label: "总览指标" },
    { key: "security", label: "按股票统计" },
  ],
  demo: [
    { key: "import", label: "导入请求" },
    { key: "manual", label: "手动添加订单" },
    { key: "match", label: "开始撮合" },
    { key: "clear", label: "清空数据" },
  ],
  auto: [
    { key: "toggle", label: "启动/停止模拟" },
    { key: "clear", label: "清空模拟数据" },
  ],
  query: [
    { key: "search", label: "数据库检索" }, // <-- 这里是新加的
  ],
};

// 【修改】把 'query' 和刚引入的 TradeQuery 组件绑定起来
const componentMap: Record<PageKey, Component> = {
  recent: RecentTradesPage,
  stats: StatisticsPage,
  demo: MatchingDemo,
  auto: AutoSimulate,
  query: TradeQuery, // <-- 这里是新加的
};

const activePage = ref<PageKey>("recent");

// 【修改】设置 query 页面的默认副导航Tab
const activeFeatureByPage = ref<Record<PageKey, string>>({
  recent: "recent",
  stats: "overview",
  demo: "import",
  auto: "toggle",
  query: "search", // <-- 这里是新加的
});

const activeViewRef = ref<{
  triggerFileInput?: () => void;
  submitManualOrder?: () => Promise<void>;
  executeMatch?: () => Promise<void>;
  clearData?: () => Promise<void>;
  toggleAutoSimulate?: () => Promise<void>;
} | null>(null);

const currentFeatureTabs = computed(() => featureTabs[activePage.value]);
const activeFeature = computed(
  () => activeFeatureByPage.value[activePage.value],
);
const activeComponent = computed(() => componentMap[activePage.value]);

const switchPage = (page: PageKey) => {
  activePage.value = page;
};

const setFeature = (feature: string) => {
  if (activePage.value === "demo") {
    if (feature === "import") activeViewRef.value?.triggerFileInput?.();
    if (feature === "manual") void activeViewRef.value?.submitManualOrder?.();
    if (feature === "match") void activeViewRef.value?.executeMatch?.();
    if (feature === "clear") void activeViewRef.value?.clearData?.();
  }
  if (activePage.value === "auto") {
    if (feature === "toggle") void activeViewRef.value?.toggleAutoSimulate?.();
    if (feature === "clear") void activeViewRef.value?.clearData?.();
  }
  activeFeatureByPage.value = {
    ...activeFeatureByPage.value,
    [activePage.value]: feature,
  };
};
</script>

<style scoped>
.large-dashboard {
  min-height: 100vh;
  padding: 1rem;
  display: grid;
  grid-template-columns: 200px 1fr 180px;
  grid-template-areas: "primary content secondary";
  gap: 0.85rem;
}

.panel {
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(226, 232, 240, 0.85);
  border-radius: 14px;
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.05);
}

.primary-nav,
.secondary-nav {
  padding: 1rem;
  height: calc(100vh - 2rem);
  min-height: calc(100vh - 2rem);
  position: sticky;
  top: 1rem;
  overflow-y: auto;
}

.primary-nav {
  grid-area: primary;
}

.secondary-nav {
  grid-area: secondary;
}

.panel-title {
  font-size: 0.78rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #64748b;
  margin-bottom: 0.65rem;
}

.nav-btn {
  width: 100%;
  border: none;
  border-radius: 12px;
  padding: 0.9rem 0.85rem;
  margin-bottom: 0.6rem;
  text-align: left;
  background: transparent;
  color: #334155;
  font-weight: 600;
  font-size: 0.98rem;
  line-height: 1.15;
  cursor: pointer;
  transition: all 0.2s ease;
}

.nav-btn:hover {
  background: #eef2ff;
}

.nav-btn.active {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #fff;
  box-shadow: 0 8px 16px rgba(37, 99, 235, 0.22);
}

.nav-btn.sub.active {
  background: linear-gradient(135deg, #0f766e, #115e59);
  box-shadow: 0 8px 16px rgba(15, 118, 110, 0.25);
}

.content {
  grid-area: content;
  padding: 1rem;
  min-width: 0;
}

@media (max-width: 1100px) {
  .large-dashboard {
    grid-template-columns: 1fr;
  }

  .primary-nav,
  .secondary-nav {
    position: static;
    height: auto;
    min-height: 0;
    overflow: visible;
  }
}
</style>
