import { createBrowserRouter, type RouteObject } from 'react-router'
import { RootLayout } from '@/components/layout/RootLayout'
import { AccountLayout } from '@/features/account/pages/AccountLayout'
import { AddressesPage } from '@/features/account/pages/AddressesPage'
import { ProfilePage } from '@/features/account/pages/ProfilePage'
import { ForgotPasswordPage } from '@/features/auth/pages/ForgotPasswordPage'
import { LoginPage } from '@/features/auth/pages/LoginPage'
import { RegisterPage } from '@/features/auth/pages/RegisterPage'
import { ResetPasswordPage } from '@/features/auth/pages/ResetPasswordPage'
import { VerifyEmailPage } from '@/features/auth/pages/VerifyEmailPage'
import { HomePage } from '@/features/catalog/pages/HomePage'
import { NotFoundPage } from '@/pages/NotFoundPage'
import { AdminRoute } from '@/routes/AdminRoute'
import { ProtectedRoute } from '@/routes/ProtectedRoute'

export const routes: RouteObject[] = [
  {
    element: <RootLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'login', element: <LoginPage /> },
      { path: 'register', element: <RegisterPage /> },
      { path: 'forgot-password', element: <ForgotPasswordPage /> },
      { path: 'reset-password', element: <ResetPasswordPage /> },
      { path: 'verify-email', element: <VerifyEmailPage /> },
      {
        element: <ProtectedRoute />,
        children: [
          {
            path: 'account',
            element: <AccountLayout />,
            children: [
              { index: true, element: <ProfilePage /> },
              { path: 'addresses', element: <AddressesPage /> },
            ],
          },
        ],
      },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
  {
    path: 'admin',
    element: <AdminRoute />,
    children: [
      {
        // Admin pages are lazy loaded to keep them out of the shopper bundle.
        lazy: async () => ({
          Component: (await import('@/components/layout/AdminLayout')).AdminLayout,
        }),
        children: [
          {
            index: true,
            lazy: async () => ({
              Component: (await import('@/features/admin/pages/AdminDashboardPage')).AdminDashboardPage,
            }),
          },
        ],
      },
    ],
  },
]

export const router = createBrowserRouter(routes)
