import { create } from 'zustand'
import type { User } from '@/features/auth/types'

export type AuthStatus = 'loading' | 'authenticated' | 'anonymous'

interface AuthState {
  status: AuthStatus
  // The access token lives only in memory so scripts cannot read it from storage.
  accessToken: string | null
  user: User | null
  setSession: (accessToken: string, user: User) => void
  setUser: (user: User) => void
  clear: () => void
}

export const useAuthStore = create<AuthState>((set) => ({
  status: 'loading',
  accessToken: null,
  user: null,
  setSession: (accessToken, user) => set({ status: 'authenticated', accessToken, user }),
  setUser: (user) => set({ user }),
  clear: () => set({ status: 'anonymous', accessToken: null, user: null }),
}))
