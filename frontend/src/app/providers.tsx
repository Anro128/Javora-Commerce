import { QueryClientProvider } from '@tanstack/react-query'
import { useEffect, useState, type ReactNode } from 'react'
import { createQueryClient } from '@/app/queryClient'
import { Toaster } from '@/components/ui/sonner'
import { restoreSession } from '@/features/auth/api'

function SessionBootstrap() {
  useEffect(() => {
    void restoreSession()
  }, [])
  return null
}

export function AppProviders({ children }: { children: ReactNode }) {
  const [queryClient] = useState(createQueryClient)

  return (
    <QueryClientProvider client={queryClient}>
      <SessionBootstrap />
      {children}
      <Toaster richColors position="top-right" />
    </QueryClientProvider>
  )
}
