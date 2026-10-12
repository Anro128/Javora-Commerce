import { useQuery } from '@tanstack/react-query'
import { CheckCircle2Icon, Loader2Icon, XCircleIcon } from 'lucide-react'
import { useEffect } from 'react'
import { Link, useSearchParams } from 'react-router'
import { AuthCard } from '@/components/layout/AuthCard'
import { Button } from '@/components/ui/button'
import { verifyEmail } from '@/features/auth/api'
import { useAuthStore } from '@/features/auth/store'

export function VerifyEmailPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')
  const user = useAuthStore((state) => state.user)
  const setUser = useAuthStore((state) => state.setUser)

  // A query dedupes the call, so React StrictMode cannot spend the one time token twice.
  const verification = useQuery({
    queryKey: ['verify-email', token],
    queryFn: async () => {
      await verifyEmail(token ?? '')
      return true
    },
    enabled: !!token,
    retry: false,
    staleTime: Infinity,
  })

  useEffect(() => {
    if (verification.isSuccess && user && !user.emailVerified) {
      setUser({ ...user, emailVerified: true })
    }
  }, [verification.isSuccess, user, setUser])

  if (!token || verification.isError) {
    return (
      <AuthCard title="Verification failed" description="This verification link is invalid or has expired.">
        <div className="flex flex-col items-center gap-4">
          <XCircleIcon className="text-destructive size-10" />
          <Button asChild variant="outline">
            <Link to="/account">Go to my account to request a new link</Link>
          </Button>
        </div>
      </AuthCard>
    )
  }

  if (verification.isSuccess) {
    return (
      <AuthCard title="Email verified" description="Thanks, your email address is now confirmed.">
        <div className="flex flex-col items-center gap-4">
          <CheckCircle2Icon className="size-10 text-green-600" />
          <Button asChild>
            <Link to="/">Continue shopping</Link>
          </Button>
        </div>
      </AuthCard>
    )
  }

  return (
    <AuthCard title="Verifying your email">
      <div className="flex justify-center">
        <Loader2Icon className="text-muted-foreground size-8 animate-spin" />
      </div>
    </AuthCard>
  )
}
