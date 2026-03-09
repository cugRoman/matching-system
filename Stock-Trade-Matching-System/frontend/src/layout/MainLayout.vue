<script setup lang="ts">
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "@/stores/authStore";

const authStore = useAuthStore();
const route = useRoute();
const router = useRouter();

const menuItems = [
  { index: "/trade", label: "提交订单" },
  { index: "/matching", label: "撮合演示" },
  { index: "/market", label: "市场最近成交" },
  { index: "/orders", label: "我的订单" },
  { index: "/positions", label: "我的持仓" },
];

const activeMenu = computed(() => route.path);
const pageTitle = computed(() => String(route.meta.title || "交易系统"));

function handleMenuSelect(path: string) {
  router.push(path);
}

function logout() {
  authStore.logout();
  router.push("/login");
}
</script>

<template>
  <el-container class="app-shell">
    <el-aside width="240px" class="app-aside">
      <div class="brand">
        <div class="brand-badge">TSMS</div>
        <div class="brand-text">Trade Matching</div>
      </div>

      <el-menu
        class="menu"
        :default-active="activeMenu"
        @select="handleMenuSelect"
      >
        <el-menu-item
          v-for="item in menuItems"
          :key="item.index"
          :index="item.index"
        >
          {{ item.label }}
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="app-header">
        <div>
          <h1 class="title">{{ pageTitle }}</h1>
          <p class="subtitle">股票对敲检测与内部撮合系统</p>
        </div>
        <div class="user-panel">
          <div class="user-id">{{ authStore.session?.loginId }}</div>
          <div class="user-meta">{{ authStore.session?.shareHolderId }}</div>
          <el-button text type="danger" @click="logout">退出</el-button>
        </div>
      </el-header>

      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  background: transparent;
}

.app-aside {
  border-right: 1px solid rgba(17, 24, 39, 0.08);
  background: linear-gradient(
    180deg,
    rgba(255, 255, 255, 0.9),
    rgba(244, 251, 246, 0.9)
  );
  backdrop-filter: blur(10px);
}

.brand {
  padding: 24px 18px 14px;
}

.brand-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 12px;
  font-weight: 700;
  color: #ffffff;
  background: linear-gradient(135deg, #0f766e, #2563eb);
}

.brand-text {
  margin-top: 10px;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.02em;
  color: #1f2937;
}

.menu {
  border-right: none;
  background: transparent;
}

.app-header {
  height: auto;
  min-height: 86px;
  padding: 20px 24px;
  border-bottom: 1px solid rgba(17, 24, 39, 0.08);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  margin: 0;
  font-size: 24px;
  line-height: 1.2;
}

.subtitle {
  margin: 6px 0 0;
  color: #4b5563;
}

.user-panel {
  text-align: right;
}

.user-id {
  font-weight: 600;
}

.user-meta {
  color: #6b7280;
  margin-bottom: 6px;
  font-size: 13px;
}

.app-main {
  padding: 20px 24px 28px;
}

@media (max-width: 960px) {
  .app-shell {
    flex-direction: column;
  }

  .app-aside {
    width: 100% !important;
  }

  .app-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .user-panel {
    text-align: left;
  }
}
</style>
