import { http } from '@/api/http'
import type { AuthSession, LoginRequest, RegisterRequest } from '@/types'

export async function login(request: LoginRequest): Promise<AuthSession> {
  const { data } = await http.post<AuthSession>('/auth/login', request)
  return data
}

export async function register(request: RegisterRequest): Promise<AuthSession> {
  const { data } = await http.post<AuthSession>('/auth/register', request)
  return data
}
