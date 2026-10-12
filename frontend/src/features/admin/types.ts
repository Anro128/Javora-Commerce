export type ProductStatus = 'DRAFT' | 'ACTIVE' | 'ARCHIVED'

export interface AdminCategory {
  id: number
  parentId: number | null
  name: string
  slug: string
  description: string | null
  sortOrder: number
  active: boolean
}

export interface CategoryInput {
  name: string
  parentId: number | null
  description?: string
  sortOrder: number
  active: boolean
}

export interface AdminProductSummary {
  id: number
  name: string
  slug: string
  categoryName: string | null
  basePrice: number
  status: ProductStatus
  variantCount: number
  availableStock: number
  imageUrl: string | null
  createdAt: string
}

export interface AdminVariant {
  id: number
  sku: string
  name: string
  price: number
  active: boolean
  quantity: number
  reserved: number
}

export interface AdminImage {
  id: number
  url: string
  sortOrder: number
}

export interface AdminProduct {
  id: number
  categoryId: number
  name: string
  slug: string
  description: string | null
  basePrice: number
  weightGram: number
  status: ProductStatus
  images: AdminImage[]
  variants: AdminVariant[]
  createdAt: string
  updatedAt: string
}

export interface VariantCreateInput {
  sku: string
  name: string
  price: number
  stock: number
}

export interface VariantUpdateInput {
  sku: string
  name: string
  price: number
  active: boolean
}

export interface ProductUpdateInput {
  categoryId: number
  name: string
  description?: string
  basePrice: number
  weightGram: number
  status: ProductStatus
}

export interface ProductCreateInput extends ProductUpdateInput {
  variants: VariantCreateInput[]
}

export interface InventoryRow {
  variantId: number
  sku: string
  variantName: string
  productId: number
  productName: string
  quantity: number
  reserved: number
  available: number
  lowStock: boolean
}

export interface AdminProductQuery {
  q?: string
  status?: ProductStatus
  page?: number
  size?: number
}

export interface InventoryQuery {
  q?: string
  lowStock?: boolean
  page?: number
  size?: number
}
