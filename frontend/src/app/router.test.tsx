import { render, screen } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { AppProviders } from '@/app/providers'
import { routes } from '@/app/router'

function renderAt(path: string) {
  const router = createMemoryRouter(routes, { initialEntries: [path] })
  render(
    <AppProviders>
      <RouterProvider router={router} />
    </AppProviders>,
  )
}

describe('app routes', () => {
  it('renders the home page inside the store layout', async () => {
    renderAt('/')

    expect(await screen.findByRole('heading', { name: 'Welcome to Javora' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Cart' })).toBeInTheDocument()
  })

  it('shows the not found page for unknown routes', async () => {
    renderAt('/does-not-exist')

    expect(await screen.findByRole('heading', { name: 'Page not found' })).toBeInTheDocument()
  })

  it('lazy loads the admin dashboard', async () => {
    renderAt('/admin')

    expect(await screen.findByRole('heading', { name: 'Dashboard' })).toBeInTheDocument()
    expect(screen.getByText('Javora Admin')).toBeInTheDocument()
  })
})
