import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach } from 'vitest'
import { ApiError } from '@/api/client'
import { descendantIds, flattenCategories } from '@/features/admin/categoryTree'
import * as adminApi from '@/features/admin/api'
import type { AdminCategory, AdminProduct, AdminProductSummary, InventoryRow } from '@/features/admin/types'
import * as catalogApi from '@/features/catalog/api'
import { pageOf } from '@/test/fixtures'
import { admin, renderApp, signInAs } from '@/test/utils'

vi.mock('@/features/auth/api')
vi.mock('@/features/catalog/api')
vi.mock('@/features/admin/api')

const adminCategories: AdminCategory[] = [
  { id: 2, parentId: 1, name: 'Shoes', slug: 'shoes', description: null, sortOrder: 0, active: true },
  { id: 1, parentId: null, name: 'Fashion', slug: 'fashion', description: null, sortOrder: 0, active: true },
  { id: 3, parentId: null, name: 'Hidden', slug: 'hidden', description: null, sortOrder: 1, active: false },
]

const productRow: AdminProductSummary = {
  id: 10,
  name: 'Urban Runner Sneakers',
  slug: 'urban-runner-sneakers',
  categoryName: 'Shoes',
  basePrice: 549000,
  status: 'ACTIVE',
  variantCount: 3,
  availableStock: 23,
  imageUrl: null,
  createdAt: '2026-01-01T00:00:00Z',
}

const createdProduct: AdminProduct = {
  id: 55,
  categoryId: 2,
  name: 'Canvas Tote',
  slug: 'canvas-tote',
  description: null,
  basePrice: 120000,
  weightGram: 300,
  status: 'DRAFT',
  images: [],
  variants: [
    { id: 900, sku: 'TOTE-1', name: 'Default', price: 120000, active: true, quantity: 10, reserved: 0 },
  ],
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
}

const lowStockRow: InventoryRow = {
  variantId: 7,
  sku: 'ESS-TEE-L',
  variantName: 'Large',
  productId: 3,
  productName: 'Essential Cotton Tee',
  quantity: 4,
  reserved: 1,
  available: 3,
  lowStock: true,
}

beforeEach(() => {
  signInAs(admin)
  vi.resetAllMocks()
  vi.mocked(catalogApi.fetchCategories).mockResolvedValue([])
  vi.mocked(adminApi.fetchAdminCategories).mockResolvedValue(adminCategories)
})

describe('category tree helpers', () => {
  it('orders children directly below their parent', () => {
    const flat = flattenCategories(adminCategories)

    expect(flat.map((option) => [option.category.name, option.depth])).toEqual([
      ['Fashion', 0],
      ['Shoes', 1],
      ['Hidden', 0],
    ])
  })

  it('collects all descendants including the root', () => {
    expect([...descendantIds(adminCategories, 1)].sort()).toEqual([1, 2])
    expect([...descendantIds(adminCategories, 2)]).toEqual([2])
  })
})

describe('admin products', () => {
  it('lists products and archives one after confirmation', async () => {
    vi.mocked(adminApi.fetchAdminProducts).mockResolvedValue(pageOf([productRow]))
    vi.mocked(adminApi.changeProductStatus).mockResolvedValue({ ...createdProduct, status: 'ARCHIVED' })
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    const user = userEvent.setup()
    renderApp('/admin/products')

    expect(await screen.findByText('Urban Runner Sneakers')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Archive Urban Runner Sneakers' }))

    await waitFor(() => expect(adminApi.changeProductStatus).toHaveBeenCalledWith(10, 'ARCHIVED'))
  })

  it('does not archive when the confirmation is declined', async () => {
    vi.mocked(adminApi.fetchAdminProducts).mockResolvedValue(pageOf([productRow]))
    vi.spyOn(window, 'confirm').mockReturnValue(false)
    const user = userEvent.setup()
    renderApp('/admin/products')

    await user.click(await screen.findByRole('button', { name: 'Archive Urban Runner Sneakers' }))

    expect(adminApi.changeProductStatus).not.toHaveBeenCalled()
  })

  it('validates the create form before sending anything', async () => {
    const user = userEvent.setup()
    renderApp('/admin/products/new')

    await user.click(await screen.findByRole('button', { name: 'Create product' }))

    expect(await screen.findByText('Name is required')).toBeInTheDocument()
    expect(screen.getByText('Select a category', { selector: 'p' })).toBeInTheDocument()
    expect(screen.getByText('SKU is required')).toBeInTheDocument()
    expect(adminApi.createProduct).not.toHaveBeenCalled()
  })

  it('creates a product with its first variant and opens it for editing', async () => {
    vi.mocked(adminApi.createProduct).mockResolvedValue(createdProduct)
    vi.mocked(adminApi.fetchAdminProduct).mockResolvedValue(createdProduct)
    const user = userEvent.setup()
    const router = renderApp('/admin/products/new')

    await user.type((await screen.findAllByLabelText('Name'))[0], '  Canvas Tote ')
    await screen.findByRole('option', { name: '— Shoes' })
    await user.selectOptions(screen.getByLabelText('Category'), '2')
    await user.clear(screen.getByLabelText('Base price (IDR)'))
    await user.type(screen.getByLabelText('Base price (IDR)'), '120000')
    await user.type(screen.getByLabelText('SKU'), 'TOTE-1')
    await user.clear(screen.getAllByLabelText('Price')[0])
    await user.type(screen.getAllByLabelText('Price')[0], '120000')
    await user.clear(screen.getByLabelText('Stock'))
    await user.type(screen.getByLabelText('Stock'), '10')
    await user.click(screen.getByRole('button', { name: 'Create product' }))

    await waitFor(() => expect(adminApi.createProduct).toHaveBeenCalled())
    expect(vi.mocked(adminApi.createProduct).mock.calls[0][0]).toEqual({
      categoryId: 2,
      name: 'Canvas Tote',
      description: undefined,
      basePrice: 120000,
      weightGram: 500,
      status: 'DRAFT',
      variants: [{ sku: 'TOTE-1', name: 'Default', price: 120000, stock: 10 }],
    })
    await waitFor(() => expect(router.state.location.pathname).toBe('/admin/products/55'))
    expect(await screen.findByRole('heading', { name: 'Variants and stock' })).toBeInTheDocument()
  })

  it('shows server conflicts such as a duplicate SKU', async () => {
    vi.mocked(adminApi.createProduct).mockRejectedValue(new ApiError(409, 'SKU is already in use: TOTE-1'))
    const user = userEvent.setup()
    renderApp('/admin/products/new')

    await user.type((await screen.findAllByLabelText('Name'))[0], 'Canvas Tote')
    await screen.findByRole('option', { name: '— Shoes' })
    await user.selectOptions(screen.getByLabelText('Category'), '2')
    await user.type(screen.getByLabelText('SKU'), 'TOTE-1')
    await user.click(screen.getByRole('button', { name: 'Create product' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('SKU is already in use: TOTE-1')
  })

  it('does not offer inactive categories for new products', async () => {
    renderApp('/admin/products/new')

    const select = await screen.findByLabelText('Category')
    await waitFor(() => expect(within(select).getByRole('option', { name: '— Shoes' })).toBeInTheDocument())

    expect(within(select).queryByRole('option', { name: 'Hidden' })).not.toBeInTheDocument()
  })
})

describe('admin inventory', () => {
  it('highlights low stock and saves a changed quantity', async () => {
    vi.mocked(adminApi.fetchInventory).mockResolvedValue(pageOf([lowStockRow]))
    vi.mocked(adminApi.setStock).mockResolvedValue()
    const user = userEvent.setup()
    renderApp('/admin/inventory')

    expect(await screen.findByText('Essential Cotton Tee')).toBeInTheDocument()
    expect(screen.getByText('low')).toBeInTheDocument()

    const save = screen.getByRole('button', { name: 'Save' })
    expect(save).toBeDisabled()

    const input = screen.getByLabelText('Stock for ESS-TEE-L')
    await user.clear(input)
    await user.type(input, '40')
    expect(save).toBeEnabled()
    await user.click(save)

    await waitFor(() => expect(adminApi.setStock).toHaveBeenCalledWith(7, 40))
  })

  it('refuses a quantity below what is already reserved', async () => {
    vi.mocked(adminApi.fetchInventory).mockResolvedValue(pageOf([lowStockRow]))
    const user = userEvent.setup()
    renderApp('/admin/inventory')

    const input = await screen.findByLabelText('Stock for ESS-TEE-L')
    await user.clear(input)
    await user.type(input, '0')

    expect(screen.getByRole('button', { name: 'Save' })).toBeDisabled()
  })

  it('passes the low stock filter to the API', async () => {
    vi.mocked(adminApi.fetchInventory).mockResolvedValue(pageOf([]))
    const user = userEvent.setup()
    renderApp('/admin/inventory')

    await user.click(await screen.findByRole('checkbox', { name: 'Low stock only' }))

    await waitFor(() =>
      expect(adminApi.fetchInventory).toHaveBeenLastCalledWith({
        q: undefined,
        lowStock: true,
        page: 0,
        size: 20,
      }),
    )
    expect(await screen.findByRole('heading', { name: 'Nothing is running low' })).toBeInTheDocument()
  })
})

describe('admin categories', () => {
  it('shows the tree and blocks deleting a category that is in use', async () => {
    vi.mocked(adminApi.deleteCategory).mockRejectedValue(
      new ApiError(409, 'This category still contains products'),
    )
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    const user = userEvent.setup()
    renderApp('/admin/categories')

    expect(await screen.findByText('/fashion')).toBeInTheDocument()
    expect(screen.getByText('/shoes')).toBeInTheDocument()
    expect(screen.getByText('/hidden · hidden')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Delete Shoes' }))

    await waitFor(() => expect(adminApi.deleteCategory).toHaveBeenCalledWith(2))
  })

  it('creates a subcategory under the chosen parent', async () => {
    vi.mocked(adminApi.createCategory).mockResolvedValue({
      id: 9,
      parentId: 1,
      name: 'Bags',
      slug: 'bags',
      description: null,
      sortOrder: 0,
      active: true,
    })
    const user = userEvent.setup()
    renderApp('/admin/categories')

    await user.type(await screen.findByLabelText('Name'), 'Bags')
    await user.selectOptions(screen.getByLabelText('Parent category'), 'Fashion')
    await user.click(screen.getByRole('button', { name: 'Create category' }))

    await waitFor(() =>
      expect(adminApi.createCategory).toHaveBeenCalledWith({
        name: 'Bags',
        parentId: 1,
        description: undefined,
        sortOrder: 0,
        active: true,
      }),
    )
  })
})
