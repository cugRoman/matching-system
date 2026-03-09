import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { SESSION_STORAGE_KEY } from '@/api/http'
import type { AuthSession } from '@/types'

function parseStoredSession(): AuthSession | null {
  const raw = localStorage.getItem(SESSION_STORAGE_KEY)
  if (!raw) {
    return null
  }
  try {
    const session = JSON.parse(raw) as AuthSession
    if (!session.token || session.expiresAtEpochMs <= Date.now()) {
      localStorage.removeItem(SESSION_STORAGE_KEY)
      return null
    }
    return session
  } catch {
    localStorage.removeItem(SESSION_STORAGE_KEY)
    return null
  }
}

export function hasValidSession(): boolean {
  return parseStoredSession() !== null
}

export const useAuthStore = defineStore('auth', () => {
  const session = ref<AuthSession | null>(parseStoredSession())

  const isAuthenticated = computed(() => {
    if (!session.value) {
      return false
    }
    if (session.value.expiresAtEpochMs <= Date.now()) {
      logout()
      return false
    }
    return true
  })

  function setSession(next: AuthSession) {
    session.value = next
    localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(next))
  }

  function logout() {
    session.value = null
    localStorage.removeItem(SESSION_STORAGE_KEY)
  }

  return {
    session,
    isAuthenticated,
    setSession,
    logout,
  }
})
