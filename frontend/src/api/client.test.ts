import { AxiosError, AxiosHeaders } from 'axios'
import { ApiError, toApiError } from '@/api/client'

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
