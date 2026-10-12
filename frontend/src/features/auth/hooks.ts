import { useMutation, useQueryClient } from '@tanstack/react-query'
import * as authApi from '@/features/auth/api'
import { useAuthStore } from '@/features/auth/store'

export function useLogin() {
  const setSession = useAuthStore((state) => state.setSession)
  return useMutation({
    mutationFn: (input: authApi.LoginInput) => authApi.login(input),
    onSuccess: (session) => setSession(session.accessToken, session.user),
  })
}

export function useRegister() {
  const setSession = useAuthStore((state) => state.setSession)
  return useMutation({
    mutationFn: (input: authApi.RegisterInput) => authApi.register(input),
    onSuccess: (session) => setSession(session.accessToken, session.user),
  })
}

export function useLogout() {
  const clear = useAuthStore((state) => state.clear)
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => authApi.logout(),
    // The local session is dropped even if the server call fails.
    onSettled: () => {
      clear()
      queryClient.clear()
    },
  })
}
