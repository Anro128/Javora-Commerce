import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'
import type { ApiResponse, ProblemDetail } from '@/api/types'
import { useAuthStore } from '@/features/auth/store'
import type { AuthResponse } from '@/features/auth/types'

export class ApiError extends Error {
  readonly status: number
  readonly fieldErrors: Record<string, string>

  constructor(status: number, message: string, fieldErrors: Record<string, string> = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

export const apiClient = axios.create({
  baseURL: '/api/v1',
  // Sends the httpOnly refresh token cookie with every request.
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
})

export function toApiError(error: unknown): ApiError {
  if (error instanceof ApiError) {
    return error
  }
  if (error instanceof AxiosError) {
    const problem = error.response?.data as ProblemDetail | undefined
    const status = error.response?.status ?? 0
    const message = problem?.detail ?? (status === 0 ? 'Unable to reach the server' : error.message)
    return new ApiError(status, message, problem?.errors ?? {})
  }
  return new ApiError(0, 'An unexpected error occurred')
}

const AUTHENTICATED_AUTH_URLS = ['/auth/resend-verification', '/auth/change-password']

// Public auth calls never carry a bearer token and are never retried after a refresh.
function isPublicAuthCall(url?: string) {
  return !!url && url.startsWith('/auth/') && !AUTHENTICATED_AUTH_URLS.includes(url)
}

let refreshInFlight: Promise<AuthResponse> | null = null

// Shares one request between concurrent callers because refresh tokens are single use.
export function refreshSession(): Promise<AuthResponse> {
  refreshInFlight ??= apiClient
    .post<ApiResponse<AuthResponse>>('/auth/refresh')
    .then(({ data }) => {
      useAuthStore.getState().setSession(data.data.accessToken, data.data.user)
      return data.data
    })
    .finally(() => {
      refreshInFlight = null
    })
  return refreshInFlight
}

type RetriableRequest = InternalAxiosRequestConfig & { _retried?: boolean }

apiClient.interceptors.request.use((config) => {
  const { accessToken } = useAuthStore.getState()
  if (accessToken && !isPublicAuthCall(config.url)) {
    config.headers.set('Authorization', `Bearer ${accessToken}`)
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  async (error: unknown) => {
    const request = error instanceof AxiosError ? (error.config as RetriableRequest | undefined) : undefined
    const unauthorized = error instanceof AxiosError && error.response?.status === 401

    if (unauthorized && request && !request._retried && !isPublicAuthCall(request.url)) {
      request._retried = true
      try {
        const session = await refreshSession()
        request.headers.set('Authorization', `Bearer ${session.accessToken}`)
        return await apiClient(request)
      } catch (retryError) {
        if (retryError instanceof ApiError && retryError.status === 401) {
          useAuthStore.getState().clear()
        }
        throw toApiError(retryError)
      }
    }
    throw toApiError(error)
  },
)
