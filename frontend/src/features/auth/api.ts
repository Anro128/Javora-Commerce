import type { ApiResponse } from '@/api/types'
import { apiClient, refreshSession } from '@/api/client'
import { useAuthStore } from '@/features/auth/store'
import type { AuthResponse } from '@/features/auth/types'

export interface LoginInput {
  email: string
  password: string
}

export interface RegisterInput {
  fullName: string
  email: string
  phone?: string
  password: string
}

export async function login(input: LoginInput): Promise<AuthResponse> {
  const { data } = await apiClient.post<ApiResponse<AuthResponse>>('/auth/login', input)
  return data.data
}

export async function register(input: RegisterInput): Promise<AuthResponse> {
  const { data } = await apiClient.post<ApiResponse<AuthResponse>>('/auth/register', input)
  return data.data
}

export async function logout(): Promise<void> {
  await apiClient.post('/auth/logout')
}

// Called once on app start to turn the refresh cookie back into an in-memory session.
export async function restoreSession(): Promise<void> {
  try {
    await refreshSession()
  } catch {
    useAuthStore.getState().clear()
  }
}

export async function forgotPassword(email: string): Promise<void> {
  await apiClient.post('/auth/forgot-password', { email })
}

export async function resetPassword(token: string, newPassword: string): Promise<void> {
  await apiClient.post('/auth/reset-password', { token, newPassword })
}

export async function verifyEmail(token: string): Promise<void> {
  await apiClient.post('/auth/verify-email', { token })
}

export async function resendVerification(): Promise<void> {
  await apiClient.post('/auth/resend-verification')
}

export async function changePassword(currentPassword: string, newPassword: string): Promise<void> {
  await apiClient.post('/auth/change-password', { currentPassword, newPassword })
}
