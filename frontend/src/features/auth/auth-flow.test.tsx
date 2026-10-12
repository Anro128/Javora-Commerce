import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach } from 'vitest'
import { ApiError } from '@/api/client'
import * as authApi from '@/features/auth/api'
import { useAuthStore } from '@/features/auth/store'
import type { AuthResponse } from '@/features/auth/types'
import { admin, customer, renderApp, signInAs, signOut } from '@/test/utils'

vi.mock('@/features/auth/api')

function sessionFor(user: AuthResponse['user']): AuthResponse {
  return { accessToken: 'access-token', tokenType: 'Bearer', expiresIn: 900, user }
}

describe('login', () => {
  beforeEach(() => {
    signOut()
    vi.resetAllMocks()
  })

  it('validates the form before calling the API', async () => {
    const user = userEvent.setup()
    renderApp('/login')

    await user.click(await screen.findByRole('button', { name: 'Log in' }))

    expect(await screen.findByText('Email is required')).toBeInTheDocument()
    expect(screen.getByText('Password is required')).toBeInTheDocument()
    expect(authApi.login).not.toHaveBeenCalled()
  })

  it('signs in and returns to the page the visitor originally asked for', async () => {
    vi.mocked(authApi.login).mockResolvedValue(sessionFor(customer))
    const user = userEvent.setup()
    const router = renderApp('/account')

    expect(await screen.findByRole('heading', { name: 'Log in' })).toBeInTheDocument()
    await user.type(screen.getByLabelText('Email'), '  jane@example.com ')
    await user.type(screen.getByLabelText('Password'), 'Secret123')
    await user.click(screen.getByRole('button', { name: 'Log in' }))

    expect(await screen.findByRole('heading', { name: 'My account' })).toBeInTheDocument()
    expect(authApi.login).toHaveBeenCalledWith({ email: 'jane@example.com', password: 'Secret123' })
    expect(useAuthStore.getState().user?.email).toBe('jane@example.com')
    expect(router.state.location.pathname).toBe('/account')
  })

  it('shows the message returned by the API when credentials are wrong', async () => {
    vi.mocked(authApi.login).mockRejectedValue(new ApiError(401, 'Invalid email or password'))
    const user = userEvent.setup()
    renderApp('/login')

    await user.type(await screen.findByLabelText('Email'), 'jane@example.com')
    await user.type(screen.getByLabelText('Password'), 'WrongPass1')
    await user.click(screen.getByRole('button', { name: 'Log in' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid email or password')
    expect(useAuthStore.getState().status).toBe('anonymous')
  })
})

describe('register', () => {
  beforeEach(() => {
    signOut()
    vi.resetAllMocks()
  })

  it('requires matching passwords that satisfy the password rules', async () => {
    const user = userEvent.setup()
    renderApp('/register')

    await user.type(await screen.findByLabelText('Full name'), 'Jane Doe')
    await user.type(screen.getByLabelText('Email'), 'jane@example.com')
    await user.type(screen.getByLabelText('Password'), 'onlyletters')
    await user.type(screen.getByLabelText('Confirm password'), 'different1')
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    expect(
      await screen.findByText('Password must contain at least one letter and one number'),
    ).toBeInTheDocument()
    expect(screen.getByText('Passwords do not match')).toBeInTheDocument()
    expect(authApi.register).not.toHaveBeenCalled()
  })

  it('creates the account and signs the new customer in', async () => {
    vi.mocked(authApi.register).mockResolvedValue(sessionFor(customer))
    const user = userEvent.setup()
    renderApp('/register')

    await user.type(await screen.findByLabelText('Full name'), ' Jane Doe ')
    await user.type(screen.getByLabelText('Email'), 'jane@example.com')
    await user.type(screen.getByLabelText('Password'), 'Secret123')
    await user.type(screen.getByLabelText('Confirm password'), 'Secret123')
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    await waitFor(() => expect(useAuthStore.getState().status).toBe('authenticated'))
    expect(authApi.register).toHaveBeenCalledWith({
      fullName: 'Jane Doe',
      email: 'jane@example.com',
      phone: undefined,
      password: 'Secret123',
    })
    expect(await screen.findByRole('heading', { name: 'Welcome to Javora' })).toBeInTheDocument()
  })
})

describe('route guards', () => {
  beforeEach(() => {
    signOut()
    vi.resetAllMocks()
  })

  it('sends anonymous visitors from the account area to the login page', async () => {
    renderApp('/account/addresses')

    expect(await screen.findByRole('heading', { name: 'Log in' })).toBeInTheDocument()
  })

  it('shows a loading state until the session has been restored', () => {
    useAuthStore.setState({ status: 'loading', accessToken: null, user: null })
    renderApp('/account')

    expect(screen.getByRole('status', { name: 'Loading' })).toBeInTheDocument()
  })

  it('denies customers access to the admin area', async () => {
    signInAs(customer)
    renderApp('/admin')

    expect(await screen.findByRole('heading', { name: 'Access denied' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Dashboard' })).not.toBeInTheDocument()
  })

  it('lets admins into the admin area', async () => {
    signInAs(admin)
    renderApp('/admin')

    expect(await screen.findByRole('heading', { name: 'Dashboard' })).toBeInTheDocument()
  })

  it('shows account actions in the header for signed in users', async () => {
    signInAs(customer)
    renderApp('/')

    expect(await screen.findByRole('link', { name: 'Account' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Log out' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Log in' })).not.toBeInTheDocument()
  })
})
