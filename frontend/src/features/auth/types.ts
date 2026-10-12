export type Role = 'CUSTOMER' | 'ADMIN'

export interface User {
  id: number
  email: string
  fullName: string
  phone: string | null
  role: Role
  emailVerified: boolean
  createdAt: string
}

export interface AuthResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  user: User
}
