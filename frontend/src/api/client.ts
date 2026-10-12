import axios, { AxiosError } from 'axios'
import type { ProblemDetail } from '@/api/types'

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

apiClient.interceptors.response.use(
  (response) => response,
  (error) => Promise.reject(toApiError(error)),
)
