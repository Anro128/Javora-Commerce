import { act, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach } from 'vitest'
import { ApiError } from '@/api/client'
import * as cartApi from '@/features/cart/api'
import { useGuestCart } from '@/features/cart/guestCart'
import * as catalogApi from '@/features/catalog/api'
import { cartLine, cartOf, categories, sneakersDetail } from '@/test/fixtures'
import { customer, renderApp, signInAs, signOut } from '@/test/utils'

vi.mock('@/features/auth/api')
vi.mock('@/features/catalog/api')

beforeEach(() => {
  signOut()
  useGuestCart.setState({ items: [] })
  vi.mocked(catalogApi.fetchCategories).mockResolvedValue(categories)
  vi.mocked(catalogApi.fetchProduct).mockResolvedValue(sneakersDetail)
})

describe('guest cart store', () => {
  it('merges repeated adds and keeps quantities within limits', () => {
    const { add, setQuantity, remove } = useGuestCart.getState()

    add(101, 2)
    add(101, 3)
    add(102, 500)
    expect(useGuestCart.getState().items).toEqual([
      { variantId: 101, quantity: 5 },
      { variantId: 102, quantity: 99 },
    ])

    setQuantity(101, 0)
    expect(useGuestCart.getState().items[0].quantity).toBe(1)

    remove(102)
    expect(useGuestCart.getState().items).toEqual([{ variantId: 101, quantity: 1 }])
  })

  it('survives a reload through local storage', () => {
    useGuestCart.getState().add(101, 2)

    expect(JSON.parse(localStorage.getItem('javora.guest-cart') ?? '{}').state.items).toEqual([
      { variantId: 101, quantity: 2 },
    ])
  })
})

describe('cart page as a guest', () => {
  it('prices the browser cart through the public preview and edits quantities locally', async () => {
    useGuestCart.setState({ items: [{ variantId: 101, quantity: 1 }] })
    vi.mocked(cartApi.previewGuestCart).mockImplementation(async (items) =>
      cartOf(cartLine({ quantity: items[0].quantity, lineTotal: 549000 * items[0].quantity })),
    )
    const user = userEvent.setup()
    renderApp('/cart')

    expect(await screen.findByRole('heading', { name: 'Shopping cart' })).toBeInTheDocument()
    expect(screen.getByText('Urban Runner Sneakers')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Increase quantity of Urban Runner Sneakers' }))

    await waitFor(() => expect(useGuestCart.getState().items[0].quantity).toBe(2))
    await waitFor(() =>
      expect(cartApi.previewGuestCart).toHaveBeenLastCalledWith([{ variantId: 101, quantity: 2 }]),
    )
    expect(cartApi.updateCartItem).not.toHaveBeenCalled()
  })

  it('asks guests to log in before checkout and remembers the destination', async () => {
    useGuestCart.setState({ items: [{ variantId: 101, quantity: 1 }] })
    vi.mocked(cartApi.previewGuestCart).mockResolvedValue(cartOf(cartLine()))
    const user = userEvent.setup()
    const router = renderApp('/cart')

    await user.click(await screen.findByRole('button', { name: 'Log in to checkout' }))

    expect(router.state.location.pathname).toBe('/login')
    expect(router.state.location.state).toEqual({ from: { pathname: '/checkout' } })
  })

  it('shows an empty state without calling the API', async () => {
    renderApp('/cart')

    expect(await screen.findByRole('heading', { name: 'Your cart is empty' })).toBeInTheDocument()
    expect(cartApi.previewGuestCart).not.toHaveBeenCalled()
  })

  it('adds from the product page into the browser cart', async () => {
    const user = userEvent.setup()
    renderApp('/products/urban-runner-sneakers')

    await user.click(await screen.findByRole('button', { name: 'Increase quantity' }))
    await user.click(screen.getByRole('button', { name: 'Add to cart' }))

    await waitFor(() => expect(useGuestCart.getState().items).toEqual([{ variantId: 101, quantity: 2 }]))
    expect(cartApi.addCartItem).not.toHaveBeenCalled()
    expect(await screen.findByRole('link', { name: 'Cart, 2 items' })).toBeInTheDocument()
  })
})

describe('cart page when logged in', () => {
  beforeEach(() => {
    signInAs(customer)
  })

  it('updates and removes lines on the server', async () => {
    vi.mocked(cartApi.fetchCart).mockResolvedValue(cartOf(cartLine({ quantity: 2, lineTotal: 1098000 })))
    const user = userEvent.setup()
    renderApp('/cart')

    await user.click(
      await screen.findByRole('button', { name: 'Increase quantity of Urban Runner Sneakers' }),
    )
    await waitFor(() => expect(cartApi.updateCartItem).toHaveBeenCalledWith(101, 3))

    await user.click(screen.getByRole('button', { name: 'Remove Urban Runner Sneakers from cart' }))
    await waitFor(() => expect(cartApi.removeCartItem).toHaveBeenCalledWith(101))
  })

  it('stops the quantity at the available stock', async () => {
    vi.mocked(cartApi.fetchCart).mockResolvedValue(
      cartOf(cartLine({ quantity: 3, available: 3, lineTotal: 1647000 })),
    )
    renderApp('/cart')

    expect(
      await screen.findByRole('button', { name: 'Increase quantity of Urban Runner Sneakers' }),
    ).toBeDisabled()
  })

  it('blocks checkout while an item has a stock problem', async () => {
    vi.mocked(cartApi.fetchCart).mockResolvedValue(
      cartOf(cartLine({ quantity: 4, available: 2, issue: 'LOW_STOCK' })),
    )
    renderApp('/cart')

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Not enough stock for this quantity. Only 2 left.',
    )
    expect(screen.getByRole('button', { name: 'Proceed to checkout' })).toBeDisabled()
  })

  it('goes to checkout from a healthy cart', async () => {
    vi.mocked(cartApi.fetchCart).mockResolvedValue(cartOf(cartLine()))
    const user = userEvent.setup()
    const router = renderApp('/cart')

    await user.click(await screen.findByRole('button', { name: 'Proceed to checkout' }))

    expect(router.state.location.pathname).toBe('/checkout')
  })

  it('adds from the product page through the API and reports stock errors', async () => {
    vi.mocked(cartApi.addCartItem).mockRejectedValue(new ApiError(409, 'Only 3 available'))
    const user = userEvent.setup()
    renderApp('/products/urban-runner-sneakers')

    await user.click(await screen.findByRole('button', { name: 'Add to cart' }))

    await waitFor(() => expect(cartApi.addCartItem).toHaveBeenCalledWith({ variantId: 101, quantity: 1 }))
    expect(await screen.findByText('Only 3 available')).toBeInTheDocument()
    expect(useGuestCart.getState().items).toEqual([])
  })
})

describe('cart sync at login', () => {
  it('moves the browser cart into the account and clears it', async () => {
    useGuestCart.setState({
      items: [
        { variantId: 101, quantity: 2 },
        { variantId: 102, quantity: 1 },
      ],
    })
    vi.mocked(cartApi.previewGuestCart).mockResolvedValue(cartOf(cartLine()))
    vi.mocked(cartApi.mergeCart).mockResolvedValue(cartOf(cartLine({ quantity: 2 })))
    renderApp('/')
    await screen.findByRole('heading', { name: 'Welcome to Javora' })

    act(() => signInAs(customer))

    await waitFor(() =>
      expect(cartApi.mergeCart).toHaveBeenCalledWith([
        { variantId: 101, quantity: 2 },
        { variantId: 102, quantity: 1 },
      ]),
    )
    await waitFor(() => expect(useGuestCart.getState().items).toEqual([]))
  })

  it('keeps the browser cart when merging fails so nothing is lost', async () => {
    useGuestCart.setState({ items: [{ variantId: 101, quantity: 1 }] })
    vi.mocked(cartApi.mergeCart).mockRejectedValue(new ApiError(500, 'boom'))
    renderApp('/')
    await screen.findByRole('heading', { name: 'Welcome to Javora' })

    act(() => signInAs(customer))

    await waitFor(() => expect(cartApi.mergeCart).toHaveBeenCalled())
    expect(useGuestCart.getState().items).toEqual([{ variantId: 101, quantity: 1 }])
  })
})
