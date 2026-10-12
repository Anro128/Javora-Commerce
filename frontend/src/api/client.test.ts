import {
  AxiosError,
  AxiosHeaders,
  type AxiosAdapter,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios'
import { afterEach, beforeEach } from 'vitest'
import { ApiError, apiClient, toApiError } from '@/api/client'
import { useAuthStore } from '@/features/auth/store'
import { customer } from '@/test/utils'

function axiosErrorWith(status: number, data: unknown) {
  const config = { headers: new AxiosHeaders() }
  return new AxiosError('Request failed', 'ERR_BAD_RESPONSE', config, null, {
    status,
    statusText: '',
    headers: {},
    config,
    data,
  })
}

describe('toApiError', () => {
  it('maps a problem detail response to an ApiError', () => {
    const error = toApiError(
      axiosErrorWith(400, {
        status: 400,
        detail: 'Validation failed',
        errors: { email: 'must be a well-formed email address' },
      }),
    )

    expect(error).toBeInstanceOf(ApiError)
    expect(error.status).toBe(400)
    expect(error.message).toBe('Validation failed')
    expect(error.fieldErrors.email).toBe('must be a well-formed email address')
  })

  it('reports network failures as unreachable server', () => {
    const error = toApiError(new AxiosError('Network Error', 'ERR_NETWORK'))

    expect(error.status).toBe(0)
    expect(error.message).toBe('Unable to reach the server')
  })
})

describe('auth interceptors', () => {
  const originalAdapter = apiClient.defaults.adapter
  let calls: { url: string; authorization: string | null }[]

  function respond(config: InternalAxiosRequestConfig, status: number, data: unknown): AxiosResponse {
    return { status, statusText: '', headers: {}, config, data }
  }

  function useServer(handler: (config: InternalAxiosRequestConfig) => AxiosResponse) {
    const adapter: AxiosAdapter = async (config) => {
      calls.push({
        url: config.url ?? '',
        authorization: (config.headers.get('Authorization') as string | undefined) ?? null,
      })
      const response = handler(config)
      if (response.status >= 400) {
        throw new AxiosError('Request failed', 'ERR_BAD_REQUEST', config, null, response)
      }
      return response
    }
    apiClient.defaults.adapter = adapter
  }

  function refreshedSession(config: InternalAxiosRequestConfig) {
    return respond(config, 200, {
      data: { accessToken: 'new-token', tokenType: 'Bearer', expiresIn: 900, user: customer },
    })
  }

  beforeEach(() => {
    calls = []
    useAuthStore.setState({ status: 'authenticated', accessToken: 'old-token', user: customer })
  })

  afterEach(() => {
    apiClient.defaults.adapter = originalAdapter
  })

  it('refreshes the session once and retries a request that was rejected with 401', async () => {
    useServer((config) => {
      if (config.url === '/auth/refresh') {
        return refreshedSession(config)
      }
      const authorized = config.headers.get('Authorization') === 'Bearer new-token'
      return respond(config, authorized ? 200 : 401, { data: 'secret' })
    })

    const response = await apiClient.get('/orders')

    expect(response.data.data).toBe('secret')
    expect(useAuthStore.getState().accessToken).toBe('new-token')
    expect(calls.map((call) => call.url)).toEqual(['/orders', '/auth/refresh', '/orders'])
  })

  it('shares a single refresh between concurrent requests', async () => {
    useServer((config) => {
      if (config.url === '/auth/refresh') {
        return refreshedSession(config)
      }
      const authorized = config.headers.get('Authorization') === 'Bearer new-token'
      return respond(config, authorized ? 200 : 401, {})
    })

    await Promise.all([apiClient.get('/orders'), apiClient.get('/cart'), apiClient.get('/users/me')])

    expect(calls.filter((call) => call.url === '/auth/refresh')).toHaveLength(1)
  })

  it('signs the user out when the session cannot be refreshed', async () => {
    useServer((config) => respond(config, 401, { detail: 'Session is invalid or has expired' }))

    await expect(apiClient.get('/orders')).rejects.toMatchObject({ status: 401 })

    expect(useAuthStore.getState().status).toBe('anonymous')
    expect(useAuthStore.getState().accessToken).toBeNull()
  })

  it('never sends the access token to public auth endpoints', async () => {
    useServer((config) => respond(config, 200, {}))

    await apiClient.post('/auth/login', { email: 'a@b.c', password: 'x' })
    await apiClient.post('/auth/change-password', {})

    expect(calls[0].authorization).toBeNull()
    expect(calls[1].authorization).toBe('Bearer old-token')
  })
})
