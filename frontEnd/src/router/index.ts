import { createRouter, createWebHistory } from 'vue-router'
import MatchingDemo from '../views/MatchingDemo.vue'
import AutoSimulate from '../views/AutoSimulate.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'MatchingDemo', component: MatchingDemo },
    { path: '/simulate', name: 'AutoSimulate', component: AutoSimulate }
  ]
})

export default router
