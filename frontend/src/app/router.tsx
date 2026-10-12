import { createBrowserRouter, type RouteObject } from 'react-router'
import { RootLayout } from '@/components/layout/RootLayout'
import { HomePage } from '@/features/catalog/pages/HomePage'
import { NotFoundPage } from '@/pages/NotFoundPage'

export const routes: RouteObject[] = [
  {
    element: <RootLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
  {
    path: 'admin',
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
]

export const router = createBrowserRouter(routes)
