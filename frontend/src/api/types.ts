export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
  timestamp: string
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}

export interface ProblemDetail {
  type?: string
  title?: string
  status: number
  detail?: string
  instance?: string
  timestamp?: string
  errors?: Record<string, string>
}
