<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, register } from '@/api/authApi'
import { useAuthStore } from '@/stores/authStore'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const activeTab = ref<'login' | 'register'>('login')
const submitting = ref(false)

const loginForm = reactive({
  loginId: '',
  password: '',
})

const registerForm = reactive({
  loginId: '',
  password: '',
  confirmPassword: '',
})

async function submitLogin() {
  if (!loginForm.loginId.trim() || !loginForm.password.trim()) {
    ElMessage.warning('请输入账号和密码')
    return
  }

  submitting.value = true
  try {
    const session = await login(loginForm)
    authStore.setSession(session)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/trade'
    ElMessage.success('登录成功')
    router.push(redirect === '/login' ? '/trade' : redirect)
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '登录失败')
  } finally {
    submitting.value = false
  }
}

async function submitRegister() {
  if (!registerForm.loginId.trim() || !registerForm.password.trim()) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  if (registerForm.password.length < 6) {
    ElMessage.warning('密码长度至少 6 位')
    return
  }
  if (registerForm.password !== registerForm.confirmPassword) {
    ElMessage.warning('两次输入密码不一致')
    return
  }

  submitting.value = true
  try {
    const session = await register({
      loginId: registerForm.loginId,
      password: registerForm.password,
    })
    authStore.setSession(session)
    ElMessage.success('注册成功，已自动登录')
    router.push('/trade')
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '注册失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="backdrop-shape shape-a" />
    <div class="backdrop-shape shape-b" />

    <el-card class="auth-card" shadow="never">
      <template #header>
        <h2 class="auth-title">交易系统访问</h2>
      </template>

      <el-tabs v-model="activeTab" stretch>
        <el-tab-pane label="登录" name="login">
          <el-form label-position="top">
            <el-form-item label="登录名">
              <el-input v-model="loginForm.loginId" autocomplete="username" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="loginForm.password" type="password" show-password autocomplete="current-password" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" class="submit-btn" :loading="submitting" @click="submitLogin">
                登录
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form label-position="top">
            <el-form-item label="登录名">
              <el-input v-model="registerForm.loginId" autocomplete="username" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="registerForm.password" type="password" show-password autocomplete="new-password" />
            </el-form-item>
            <el-form-item label="确认密码">
              <el-input
                v-model="registerForm.confirmPassword"
                type="password"
                show-password
                autocomplete="new-password"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="success" class="submit-btn" :loading="submitting" @click="submitRegister">
                注册
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  position: relative;
  overflow: hidden;
}

.auth-card {
  width: min(460px, 100%);
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  z-index: 2;
}

.auth-title {
  margin: 0;
  font-size: 24px;
}

.submit-btn {
  width: 100%;
}

.backdrop-shape {
  position: absolute;
  border-radius: 999px;
  filter: blur(2px);
  opacity: 0.75;
}

.shape-a {
  width: 320px;
  height: 320px;
  background: radial-gradient(circle, rgba(15, 118, 110, 0.35), rgba(37, 99, 235, 0.08));
  top: -100px;
  left: -80px;
}

.shape-b {
  width: 380px;
  height: 380px;
  background: radial-gradient(circle, rgba(245, 158, 11, 0.3), rgba(244, 63, 94, 0.08));
  right: -120px;
  bottom: -110px;
}
</style>
