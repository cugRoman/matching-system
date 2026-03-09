import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import router from './router'
import App from './App.vue'
import './style.css'

// 兼容 sockjs-client 等依赖在浏览器中访问 global 变量
// eslint-disable-next-line @typescript-eslint/no-explicit-any
;(window as any).global = window

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

app.mount('#app')
