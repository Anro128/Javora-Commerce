import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach } from 'vitest'
import { ApiError } from '@/api/client'
import * as catalogApi from '@/features/catalog/api'
import { candle, categories, pageOf, sneakers, sneakersDetail } from '@/test/fixtures'
import { renderApp, signOut } from '@/test/utils'

vi.mock('@/features/auth/api')
vi.mock('@/features/catalog/api')

describe('shop catalog', () => {
  beforeEach(() => {
    signOut()
    vi.resetAllMocks()
    vi.mocked(catalogApi.fetchCategories).mockResolvedValue(categories)
    vi.mocked(catalogApi.fetchProducts).mockResolvedValue(pageOf([sneakers, candle]))
    vi.mocked(catalogApi.fetchProduct).mockResolvedValue(sneakersDetail)
  })

  it('shows categories and new arrivals on the home page', async () => {
    renderApp('/')

    expect(await screen.findByRole('heading', { name: 'Shop by category' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Fashion' })).toHaveAttribute(
      'href',
      '/products?category=fashion',
    )
    expect(await screen.findByRole('link', { name: /Urban Runner Sneakers/ })).toHaveAttribute(
      'href',
      '/products/urban-runner-sneakers',
    )
  })

  it('lists products with prices and marks sold out items', async () => {
    renderApp('/products')

    expect(await screen.findByText('Urban Runner Sneakers')).toBeInTheDocument()
    expect(screen.getByText('2 products')).toBeInTheDocument()
    const candleLink = screen.getByRole('link', { name: /Scented Soy Candle/ })
    expect(within(candleLink).getByText('Out of stock')).toBeInTheDocument()
    expect(within(candleLink).getByText(/89\.000/)).toBeInTheDocument()
  })

  it('reads filters from the URL and passes them to the API', async () => {
    renderApp('/products?q=shoe&category=shoes&minPrice=100000&sort=price_asc&page=2')

    await screen.findByText('Urban Runner Sneakers')

    expect(catalogApi.fetchProducts).toHaveBeenCalledWith({
      q: 'shoe',
      category: 'shoes',
      minPrice: 100000,
      maxPrice: undefined,
      sort: 'price_asc',
      page: 2,
      size: 12,
    })
  })

  it('updates the search results when searching and picking a category', async () => {
    const user = userEvent.setup()
    const router = renderApp('/products')
    await screen.findByText('Urban Runner Sneakers')

    await user.type(screen.getByRole('searchbox', { name: 'Search products' }), 'sneaker')
    await user.click(screen.getByRole('button', { name: 'Search' }))
    expect(router.state.location.search).toBe('?q=sneaker')

    await user.click(await screen.findByRole('button', { name: 'Shoes' }))
    expect(router.state.location.search).toBe('?q=sneaker&category=shoes')

    await user.selectOptions(screen.getByLabelText('Sort by'), 'price_desc')
    expect(router.state.location.search).toContain('sort=price_desc')
  })

  it('offers to clear filters when nothing matches', async () => {
    vi.mocked(catalogApi.fetchProducts).mockResolvedValue(pageOf([]))
    const user = userEvent.setup()
    const router = renderApp('/products?q=zzz')

    expect(await screen.findByRole('heading', { name: 'No products found' })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Clear filters' }))

    expect(router.state.location.search).toBe('')
  })

  it('moves between result pages', async () => {
    vi.mocked(catalogApi.fetchProducts).mockResolvedValue(
      pageOf([sneakers], { totalElements: 30, totalPages: 3 }),
    )
    const user = userEvent.setup()
    const router = renderApp('/products')

    await user.click(await screen.findByRole('button', { name: /Next/ }))

    expect(router.state.location.search).toBe('?page=1')
  })

  it('redirects friendly category links to the filtered list', async () => {
    const router = renderApp('/categories/shoes')

    await screen.findByText('Urban Runner Sneakers')

    expect(router.state.location.pathname).toBe('/products')
    expect(router.state.location.search).toBe('?category=shoes')
  })
})

describe('product detail', () => {
  beforeEach(() => {
    signOut()
    vi.resetAllMocks()
    vi.mocked(catalogApi.fetchCategories).mockResolvedValue(categories)
    vi.mocked(catalogApi.fetchProduct).mockResolvedValue(sneakersDetail)
  })

  it('preselects the first available option and warns about low stock', async () => {
    renderApp('/products/urban-runner-sneakers')

    expect(await screen.findByRole('heading', { name: 'Urban Runner Sneakers' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Size 41' })).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByRole('button', { name: 'Size 40' })).toBeDisabled()
    expect(screen.getByText('Only 3 left')).toBeInTheDocument()
    expect(screen.getByText('URB-41')).toBeInTheDocument()
  })

  it('updates price and stock message when another option is chosen', async () => {
    const user = userEvent.setup()
    renderApp('/products/urban-runner-sneakers')

    await user.click(await screen.findByRole('button', { name: 'Size 42' }))

    expect(screen.getByText(/599\.000/)).toBeInTheDocument()
    expect(screen.getByText('In stock')).toBeInTheDocument()
  })

  it('shows a friendly page when the product does not exist', async () => {
    vi.mocked(catalogApi.fetchProduct).mockRejectedValue(new ApiError(404, 'Product not found: nope'))
    renderApp('/products/nope')

    expect(await screen.findByRole('heading', { name: 'Product not found' })).toBeInTheDocument()
  })
})
