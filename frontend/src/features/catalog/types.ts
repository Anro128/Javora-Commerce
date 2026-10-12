export interface CategoryNode {
  id: number
  name: string
  slug: string
  children: CategoryNode[]
}

export interface ProductSummary {
  id: number
  name: string
  slug: string
  price: number
  imageUrl: string | null
  categoryName: string | null
  categorySlug: string | null
  inStock: boolean
}

export interface ProductImage {
  id: number
  url: string
}

export interface ProductVariant {
  id: number
  sku: string
  name: string
  price: number
  available: number
  inStock: boolean
}

export interface ProductDetail {
  id: number
  name: string
  slug: string
  description: string | null
  price: number
  weightGram: number
  categoryName: string
  categorySlug: string
  images: ProductImage[]
  variants: ProductVariant[]
}

export type ProductSort = 'newest' | 'price_asc' | 'price_desc' | 'name'

export interface ProductQuery {
  q?: string
  category?: string
  minPrice?: number
  maxPrice?: number
  sort?: ProductSort
  page?: number
  size?: number
}
