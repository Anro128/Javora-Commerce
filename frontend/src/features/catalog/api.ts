import { apiClient } from '@/api/client'
import type { ApiResponse, PageResponse } from '@/api/types'
import type { CategoryNode, ProductDetail, ProductQuery, ProductSummary } from '@/features/catalog/types'

export async function fetchCategories(): Promise<CategoryNode[]> {
  const { data } = await apiClient.get<ApiResponse<CategoryNode[]>>('/public/categories')
  return data.data
}

export async function fetchProducts(query: ProductQuery): Promise<PageResponse<ProductSummary>> {
  const { data } = await apiClient.get<ApiResponse<PageResponse<ProductSummary>>>('/public/products', {
    params: query,
  })
  return data.data
}

export async function fetchProduct(slug: string): Promise<ProductDetail> {
  const { data } = await apiClient.get<ApiResponse<ProductDetail>>(`/public/products/${slug}`)
  return data.data
}
