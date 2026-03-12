import { createRouter, createWebHistory } from "vue-router";
import LargeDashboard from "../views/LargeDashboard.vue";
import TradeQuery from "../views/TradeQuery.vue";
const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", name: "LargeDashboard", component: LargeDashboard },
    { path: "/query", name: "TradeQuery", component: TradeQuery },
  ],
});

export default router;
