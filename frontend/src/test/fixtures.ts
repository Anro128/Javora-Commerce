import type { PageResponse } from '@/api/types'
import type { CategoryNode, ProductDetail, ProductSummary } from '@/features/catalog/types'

export const categories: CategoryNode[] = [
  {
    id: 1,
    name: 'Fashion',
    slug: 'fashion',
    children: [{ id: 2, name: 'Shoes', slug: 'shoes', children: [] }],
  },
  { id: 3, name: 'Home & Living', slug: 'home-living', children: [] },
]

export const sneakers: ProductSummary = {
  id: 10,
  name: 'Urban Runner Sneakers',
  slug: 'urban-runner-sneakers',
  price: 549000,
  imageUrl: null,
  categoryName: 'Shoes',
  categorySlug: 'shoes',
  inStock: true,
}

export const candle: ProductSummary = {
  id: 11,
  name: 'Scented Soy Candle',
  slug: 'scented-soy-candle',
  price: 89000,
  imageUrl: 'http://cdn.test/products/candle.png',
  categoryName: 'Home & Living',
  categorySlug: 'home-living',
  inStock: false,
}

export function pageOf<T>(content: T[], overrides: Partial<PageResponse<T>> = {}): PageResponse<T> {
  return {
    content,
    page: 0,
    size: 12,
    totalElements: content.length,
    totalPages: content.length === 0 ? 0 : 1,
    last: true,
    ...overrides,
  }
}

export const sneakersDetail: ProductDetail = {
  id: 10,
  name: 'Urban Runner Sneakers',
  slug: 'urban-runner-sneakers',
  description: 'Lightweight everyday sneakers.',
  price: 549000,
  weightGram: 800,
  categoryName: 'Shoes',
  categorySlug: 'shoes',
  images: [],
  variants: [
    { id: 100, sku: 'URB-40', name: 'Size 40', price: 549000, available: 0, inStock: false },
    { id: 101, sku: 'URB-41', name: 'Size 41', price: 549000, available: 3, inStock: true },
    { id: 102, sku: 'URB-42', name: 'Size 42', price: 599000, available: 20, inStock: true },
  ],
}
