import { Link, Navigate, Outlet, useLocation } from 'react-router'
import { PageSpinner } from '@/components/layout/PageSpinner'
import { Button } from '@/components/ui/button'
import { useAuthStore } from '@/features/auth/store'

function ForbiddenPage() {
  return (
    <section className="flex min-h-svh flex-col items-center justify-center gap-4 px-4 text-center">
      <p className="text-muted-foreground text-6xl font-bold">403</p>
      <h1 className="text-2xl font-semibold">Access denied</h1>
      <p className="text-muted-foreground">You do not have permission to view this page.</p>
      <Button variant="outline" asChild>
        <Link to="/">Back to home</Link>
      </Button>
    </section>
  )
}

export function AdminRoute() {
  const status = useAuthStore((state) => state.status)
  const role = useAuthStore((state) => state.user?.role)
  const location = useLocation()

  if (status === 'loading') {
    return <PageSpinner />
  }
  if (status === 'anonymous') {
    return <Navigate to="/login" replace state={{ from: location }} />
  }
  return role === 'ADMIN' ? <Outlet /> : <ForbiddenPage />
}
