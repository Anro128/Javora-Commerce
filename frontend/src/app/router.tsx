import type { ComponentType } from 'react'
import { createBrowserRouter, type RouteObject } from 'react-router'
import { RootLayout } from '@/components/layout/RootLayout'
import { AccountLayout } from '@/features/account/pages/AccountLayout'
import { AddressesPage } from '@/features/account/pages/AddressesPage'
import { ProfilePage } from '@/features/account/pages/ProfilePage'
import { CartPage } from '@/features/cart/pages/CartPage'
import { CheckoutPage } from '@/features/checkout/pages/CheckoutPage'
import { OrderDetailPage } from '@/features/checkout/pages/OrderDetailPage'
import { ForgotPasswordPage } from '@/features/auth/pages/ForgotPasswordPage'
import { LoginPage } from '@/features/auth/pages/LoginPage'
import { RegisterPage } from '@/features/auth/pages/RegisterPage'
import { ResetPasswordPage } from '@/features/auth/pages/ResetPasswordPage'
import { VerifyEmailPage } from '@/features/auth/pages/VerifyEmailPage'
import { CategoryRedirectPage } from '@/features/catalog/pages/CategoryRedirectPage'
import { HomePage } from '@/features/catalog/pages/HomePage'
import { ProductDetailPage } from '@/features/catalog/pages/ProductDetailPage'
import { ProductListPage } from '@/features/catalog/pages/ProductListPage'
import { NotFoundPage } from '@/pages/NotFoundPage'
import { AdminRoute } from '@/routes/AdminRoute'
import { ProtectedRoute } from '@/routes/ProtectedRoute'

// Admin pages are lazy loaded to keep them out of the shopper bundle.
function lazyPage(load: () => Promise<ComponentType>): Pick<RouteObject, 'lazy'> {
  return { lazy: async () => ({ Component: await load() }) }
}

const adminRoutes: RouteObject[] = [
  {
    index: true,
    ...lazyPage(async () => (await import('@/features/admin/pages/AdminDashboardPage')).AdminDashboardPage),
  },
  {
    path: 'products',
    ...lazyPage(async () => (await import('@/features/admin/pages/AdminProductsPage')).AdminProductsPage),
  },
  {
    path: 'products/new',
    ...lazyPage(
      async () => (await import('@/features/admin/pages/AdminProductFormPage')).AdminProductFormPage,
    ),
  },
  {
    path: 'products/:id',
    ...lazyPage(
      async () => (await import('@/features/admin/pages/AdminProductFormPage')).AdminProductFormPage,
    ),
  },
  {
    path: 'categories',
    ...lazyPage(async () => (await import('@/features/admin/pages/AdminCategoriesPage')).AdminCategoriesPage),
  },
  {
    path: 'inventory',
    ...lazyPage(async () => (await import('@/features/admin/pages/AdminInventoryPage')).AdminInventoryPage),
  },
  {
    path: '*',
    ...lazyPage(async () => (await import('@/features/admin/pages/AdminComingSoonPage')).AdminComingSoonPage),
  },
]

export const routes: RouteObject[] = [
  {
    element: <RootLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'products', element: <ProductListPage /> },
      { path: 'products/:slug', element: <ProductDetailPage /> },
      { path: 'categories/:slug', element: <CategoryRedirectPage /> },
      { path: 'cart', element: <CartPage /> },
      { path: 'login', element: <LoginPage /> },
      { path: 'register', element: <RegisterPage /> },
      { path: 'forgot-password', element: <ForgotPasswordPage /> },
      { path: 'reset-password', element: <ResetPasswordPage /> },
      { path: 'verify-email', element: <VerifyEmailPage /> },
      {
        element: <ProtectedRoute />,
        children: [
          { path: 'checkout', element: <CheckoutPage /> },
          {
            path: 'account',
            element: <AccountLayout />,
            children: [
              { index: true, element: <ProfilePage /> },
              { path: 'addresses', element: <AddressesPage /> },
            ],
          },
          { path: 'account/orders/:orderNumber', element: <OrderDetailPage /> },
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
        ...lazyPage(async () => (await import('@/components/layout/AdminLayout')).AdminLayout),
        children: adminRoutes,
      },
    ],
  },
]

export const router = createBrowserRouter(routes)
