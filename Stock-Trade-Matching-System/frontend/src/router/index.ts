import {
  createRouter,
  createWebHistory,
  type RouteRecordRaw,
} from "vue-router";
import { hasValidSession } from "@/stores/authStore";

const routes: RouteRecordRaw[] = [
  {
    path: "/login",
    name: "Login",
    component: () => import("@/views/AuthView.vue"),
    meta: { public: true, title: "登录 / 注册" },
  },
  {
    path: "/",
    component: () => import("@/layout/MainLayout.vue"),
    redirect: "/trade",
    meta: { requiresAuth: true },
    children: [
      {
        path: "trade",
        name: "Trade",
        component: () => import("@/views/TradePanel.vue"),
        meta: { requiresAuth: true, title: "提交订单" },
      },
      {
        path: "matching",
        name: "MatchingSimulation",
        component: () => import("@/views/MatchingSimulation.vue"),
        meta: { requiresAuth: true, title: "内部撮合演示" },
      },
      {
        path: "market",
        name: "Market",
        component: () => import("@/views/Dashboard.vue"),
        meta: { requiresAuth: true, title: "市场最近成交" },
      },
      {
        path: "orders",
        name: "Orders",
        component: () => import("@/views/OrderList.vue"),
        meta: { requiresAuth: true, title: "我的订单" },
      },
      {
        path: "positions",
        name: "Positions",
        component: () => import("@/views/PositionList.vue"),
        meta: { requiresAuth: true, title: "我的持仓" },
      },
      {
        path: "activity",
        redirect: "/orders",
      },
    ],
  },
  {
    path: "/:pathMatch(.*)*",
    redirect: "/trade",
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to) => {
  const authenticated = hasValidSession();

  if (to.meta.requiresAuth && !authenticated) {
    return {
      path: "/login",
      query: { redirect: to.fullPath },
    };
  }

  if (to.path === "/login" && authenticated) {
    const redirect =
      typeof to.query.redirect === "string" ? to.query.redirect : "/trade";
    return redirect === "/login" ? "/trade" : redirect;
  }

  return true;
});

export default router;
