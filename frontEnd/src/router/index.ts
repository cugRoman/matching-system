import { createRouter, createWebHistory } from 'vue-router'
import LargeDashboard from '../views/LargeDashboard.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'LargeDashboard', component: LargeDashboard }
  ]
})

export default router
