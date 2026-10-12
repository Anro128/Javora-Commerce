import { Navigate, Outlet, useLocation } from 'react-router'
import { PageSpinner } from '@/components/layout/PageSpinner'
import { useAuthStore } from '@/features/auth/store'

// Route guards only improve the experience; the backend enforces the real access rules.
export function ProtectedRoute() {
  const status = useAuthStore((state) => state.status)
  const location = useLocation()

  if (status === 'loading') {
    return <PageSpinner />
  }
  if (status === 'anonymous') {
    return <Navigate to="/login" replace state={{ from: location }} />
  }
  return <Outlet />
}
