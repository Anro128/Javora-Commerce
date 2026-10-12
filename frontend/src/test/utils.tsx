import { render } from '@testing-library/react'
import { createMemoryRouter, RouterProvider, type RouteObject } from 'react-router'
import { AppProviders } from '@/app/providers'
import { routes } from '@/app/router'
import { useAuthStore } from '@/features/auth/store'
import type { User } from '@/features/auth/types'

export const customer: User = {
  id: 1,
  email: 'jane@example.com',
  fullName: 'Jane Doe',
  phone: null,
  role: 'CUSTOMER',
  emailVerified: true,
  createdAt: '2026-01-01T00:00:00Z',
}

export const admin: User = {
  ...customer,
  id: 2,
  email: 'admin@example.com',
  fullName: 'Admin',
  role: 'ADMIN',
}

export function signInAs(user: User) {
  useAuthStore.setState({ status: 'authenticated', accessToken: 'test-token', user })
}

export function signOut() {
  useAuthStore.setState({ status: 'anonymous', accessToken: null, user: null })
}

export function renderApp(path = '/', appRoutes: RouteObject[] = routes) {
  const router = createMemoryRouter(appRoutes, { initialEntries: [path] })
  render(
    <AppProviders>
      <RouterProvider router={router} />
    </AppProviders>,
  )
  return router
}
