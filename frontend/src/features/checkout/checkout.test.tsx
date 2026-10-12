import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach } from 'vitest'
import { ApiError } from '@/api/client'
import * as accountApi from '@/features/account/api'
import type { Address } from '@/features/account/types'
import * as cartApi from '@/features/cart/api'
import * as checkoutApi from '@/features/checkout/api'
import type { CheckoutPreview, Order, ShippingOption } from '@/features/checkout/types'
import * as catalogApi from '@/features/catalog/api'
import { cartLine, cartOf, categories } from '@/test/fixtures'
import { customer, renderApp, signInAs, signOut } from '@/test/utils'

vi.mock('@/features/auth/api')
vi.mock('@/features/catalog/api')
vi.mock('@/features/account/api')
vi.mock('@/features/checkout/api')

const home: Address = {
  id: 1,
  label: 'Home',
  recipientName: 'Jane Doe',
  phone: '081234567890',
  province: 'DKI Jakarta',
  city: 'Jakarta Selatan',
  district: 'Kebayoran',
  postalCode: '12190',
  street: 'Jl. Merdeka No. 1',
  defaultAddress: true,
}

const office: Address = {
  ...home,
  id: 2,
  label: 'Office',
  street: 'Jl. Sudirman No. 5',
  defaultAddress: false,
}

const options: ShippingOption[] = [
  {
    courier: 'jnt',
    courierName: 'J&T Express',
    service: 'ez',
    serviceName: 'EZ',
    cost: 8000,
    etdMinDays: 2,
    etdMaxDays: 4,
  },
  {
    courier: 'jne',
    courierName: 'JNE',
    service: 'reg',
    serviceName: 'Regular',
    cost: 9000,
    etdMinDays: 2,
    etdMaxDays: 3,
  },
]

function previewFor(request: { courier: string; service: string }): CheckoutPreview {
  const option = options.find(
    (candidate) => candidate.courier === request.courier && candidate.service === request.service,
  )!
  return {
    items: [cartLine()],
    subtotal: 549000,
    shippingCost: option.cost,
    discount: 0,
    total: 549000 + option.cost,
    totalWeightGram: 800,
    shipping: option,
    address: home,
  }
}

const createdOrder: Order = {
  orderNumber: 'JVR-20261012-K7M2QX',
  status: 'PENDING_PAYMENT',
  subtotal: 549000,
  shippingCost: 8000,
  discount: 0,
  total: 557000,
  courier: 'jnt',
  courierService: 'ez',
  address: home,
  items: [
    {
      variantId: 101,
      productId: 10,
      productName: 'Urban Runner Sneakers',
      variantName: 'Size 41',
      sku: 'URB-41',
      unitPrice: 549000,
      quantity: 1,
      lineTotal: 549000,
    },
  ],
  createdAt: '2026-10-12T10:00:00Z',
  expiresAt: '2026-10-13T10:00:00Z',
  cancelledAt: null,
}

beforeEach(() => {
  signOut()
  signInAs(customer)
  vi.resetAllMocks()
  vi.mocked(catalogApi.fetchCategories).mockResolvedValue(categories)
  vi.mocked(cartApi.fetchCart).mockResolvedValue(cartOf(cartLine()))
  vi.mocked(accountApi.fetchAddresses).mockResolvedValue([home, office])
  vi.mocked(checkoutApi.fetchShippingOptions).mockResolvedValue(options)
  vi.mocked(checkoutApi.previewCheckout).mockImplementation(async (request) => previewFor(request))
})

describe('checkout', () => {
  it('uses the default address and cheapest shipping, then places the order', async () => {
    vi.mocked(checkoutApi.placeOrder).mockResolvedValue(createdOrder)
    vi.mocked(checkoutApi.fetchOrder).mockResolvedValue(createdOrder)
    const user = userEvent.setup()
    const router = renderApp('/checkout')

    expect(await screen.findByRole('heading', { name: 'Checkout' })).toBeInTheDocument()
    expect(await screen.findByRole('radio', { name: /Jane Doe · Home/ })).toBeChecked()
    expect(await screen.findByRole('radio', { name: /J&T Express EZ/ })).toBeChecked()
    await waitFor(() => expect(screen.getByText(/557\.000/)).toBeInTheDocument())

    await user.click(screen.getByRole('button', { name: 'Place order' }))

    await waitFor(() =>
      expect(checkoutApi.placeOrder).toHaveBeenCalledWith({ addressId: 1, courier: 'jnt', service: 'ez' }),
    )
    await waitFor(() => expect(router.state.location.pathname).toBe('/account/orders/JVR-20261012-K7M2QX'))
    expect(await screen.findByRole('heading', { name: 'Order JVR-20261012-K7M2QX' })).toBeInTheDocument()
  })

  it('recalculates the total when another shipping method is chosen', async () => {
    const user = userEvent.setup()
    renderApp('/checkout')

    await user.click(await screen.findByRole('radio', { name: /JNE Regular/ }))

    await waitFor(() =>
      expect(checkoutApi.previewCheckout).toHaveBeenLastCalledWith({
        addressId: 1,
        courier: 'jne',
        service: 'reg',
      }),
    )
    expect(await screen.findByText(/558\.000/)).toBeInTheDocument()
  })

  it('reloads shipping options when another address is picked', async () => {
    const user = userEvent.setup()
    renderApp('/checkout')

    await user.click(await screen.findByRole('radio', { name: /Jane Doe · Office/ }))

    await waitFor(() => expect(checkoutApi.fetchShippingOptions).toHaveBeenCalledWith(2))
  })

  it('asks for an address first when none is saved', async () => {
    vi.mocked(accountApi.fetchAddresses).mockResolvedValue([])
    renderApp('/checkout')

    expect(await screen.findByLabelText('Recipient name')).toBeInTheDocument()
    expect(screen.getByText('Choose a delivery address first.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Place order' })).toBeDisabled()
  })

  it('keeps the shopper on the page and refreshes the cart when stock ran out', async () => {
    vi.mocked(checkoutApi.placeOrder).mockRejectedValue(
      new ApiError(409, 'Not enough stock for Urban Runner Sneakers (Size 41)'),
    )
    const user = userEvent.setup()
    const router = renderApp('/checkout')

    await user.click(await screen.findByRole('button', { name: 'Place order' }))

    expect(
      await screen.findByText('Not enough stock for Urban Runner Sneakers (Size 41)'),
    ).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/checkout')
    await waitFor(() => expect(vi.mocked(cartApi.fetchCart).mock.calls.length).toBeGreaterThan(1))
  })

  it('shows an empty state when there is nothing to buy', async () => {
    vi.mocked(cartApi.fetchCart).mockResolvedValue(cartOf())
    renderApp('/checkout')

    expect(await screen.findByRole('heading', { name: 'Your cart is empty' })).toBeInTheDocument()
  })

  it('requires login', async () => {
    signOut()
    renderApp('/checkout')

    expect(await screen.findByRole('heading', { name: 'Log in' })).toBeInTheDocument()
  })
})

describe('order detail', () => {
  it('shows the order and cancels it after confirmation', async () => {
    vi.mocked(checkoutApi.fetchOrder).mockResolvedValue(createdOrder)
    vi.mocked(checkoutApi.cancelOrder).mockResolvedValue({
      ...createdOrder,
      status: 'CANCELLED',
      cancelledAt: '2026-10-12T11:00:00Z',
    })
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    const user = userEvent.setup()
    renderApp('/account/orders/JVR-20261012-K7M2QX')

    expect(await screen.findByText('Awaiting payment')).toBeInTheDocument()
    expect(screen.getByText('Urban Runner Sneakers')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Cancel order' }))

    await waitFor(() => expect(checkoutApi.cancelOrder).toHaveBeenCalledWith('JVR-20261012-K7M2QX'))
    expect(await screen.findByText('Cancelled')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Cancel order' })).not.toBeInTheDocument()
  })

  it('shows a friendly page for orders that are not found', async () => {
    vi.mocked(checkoutApi.fetchOrder).mockRejectedValue(new ApiError(404, 'Order not found'))
    renderApp('/account/orders/JVR-NOPE')

    expect(await screen.findByRole('heading', { name: 'Order not found' })).toBeInTheDocument()
  })
})
