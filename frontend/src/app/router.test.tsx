import { screen } from '@testing-library/react'
import { beforeEach } from 'vitest'
import { admin, renderApp, signInAs, signOut } from '@/test/utils'

vi.mock('@/features/auth/api')
vi.mock('@/features/catalog/api')

describe('app routes', () => {
  beforeEach(() => {
    signOut()
  })

  it('renders the home page inside the store layout', async () => {
    renderApp('/')

    expect(await screen.findByRole('heading', { name: 'Welcome to Javora' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Cart' })).toBeInTheDocument()
  })

  it('shows the not found page for unknown routes', async () => {
    renderApp('/does-not-exist')

    expect(await screen.findByRole('heading', { name: 'Page not found' })).toBeInTheDocument()
  })

  it('lazy loads the admin dashboard for admins', async () => {
    signInAs(admin)
    renderApp('/admin')

    expect(await screen.findByRole('heading', { name: 'Dashboard' })).toBeInTheDocument()
    expect(screen.getByText('Javora Admin')).toBeInTheDocument()
  })
})
