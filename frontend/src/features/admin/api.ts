import { apiClient } from '@/api/client'
import type { ApiResponse, PageResponse } from '@/api/types'
import type {
  AdminCategory,
  AdminProduct,
  AdminProductQuery,
  AdminProductSummary,
  CategoryInput,
  InventoryQuery,
  InventoryRow,
  ProductCreateInput,
  ProductStatus,
  ProductUpdateInput,
  VariantCreateInput,
  VariantUpdateInput,
} from '@/features/admin/types'

async function unwrap<T>(request: Promise<{ data: ApiResponse<T> }>): Promise<T> {
  const { data } = await request
  return data.data
}

export function fetchAdminCategories() {
  return unwrap(apiClient.get<ApiResponse<AdminCategory[]>>('/admin/categories'))
}

export function createCategory(input: CategoryInput) {
  return unwrap(apiClient.post<ApiResponse<AdminCategory>>('/admin/categories', input))
}

export function updateCategory(id: number, input: CategoryInput) {
  return unwrap(apiClient.put<ApiResponse<AdminCategory>>(`/admin/categories/${id}`, input))
}

export async function deleteCategory(id: number): Promise<void> {
  await apiClient.delete(`/admin/categories/${id}`)
}

export function fetchAdminProducts(query: AdminProductQuery) {
  return unwrap(
    apiClient.get<ApiResponse<PageResponse<AdminProductSummary>>>('/admin/products', { params: query }),
  )
}

export function fetchAdminProduct(id: number) {
  return unwrap(apiClient.get<ApiResponse<AdminProduct>>(`/admin/products/${id}`))
}

export function createProduct(input: ProductCreateInput) {
  return unwrap(apiClient.post<ApiResponse<AdminProduct>>('/admin/products', input))
}

export function updateProduct(id: number, input: ProductUpdateInput) {
  return unwrap(apiClient.put<ApiResponse<AdminProduct>>(`/admin/products/${id}`, input))
}

export function changeProductStatus(id: number, status: ProductStatus) {
  return unwrap(apiClient.patch<ApiResponse<AdminProduct>>(`/admin/products/${id}/status`, { status }))
}

export function addVariant(productId: number, input: VariantCreateInput) {
  return unwrap(apiClient.post<ApiResponse<AdminProduct>>(`/admin/products/${productId}/variants`, input))
}

export function updateVariant(productId: number, variantId: number, input: VariantUpdateInput) {
  return unwrap(
    apiClient.put<ApiResponse<AdminProduct>>(`/admin/products/${productId}/variants/${variantId}`, input),
  )
}

export function uploadImage(productId: number, file: File) {
  const body = new FormData()
  body.append('file', file)
  return unwrap(
    apiClient.post<ApiResponse<AdminProduct>>(`/admin/products/${productId}/images`, body, {
      // Lets the browser set the multipart boundary instead of the default JSON header.
      headers: { 'Content-Type': undefined },
    }),
  )
}

export function makePrimaryImage(productId: number, imageId: number) {
  return unwrap(
    apiClient.put<ApiResponse<AdminProduct>>(`/admin/products/${productId}/images/${imageId}/primary`),
  )
}

export function deleteImage(productId: number, imageId: number) {
  return unwrap(apiClient.delete<ApiResponse<AdminProduct>>(`/admin/products/${productId}/images/${imageId}`))
}

export function fetchInventory(query: InventoryQuery) {
  return unwrap(apiClient.get<ApiResponse<PageResponse<InventoryRow>>>('/admin/inventory', { params: query }))
}

export async function setStock(variantId: number, quantity: number): Promise<void> {
  await apiClient.put(`/admin/inventory/${variantId}`, { quantity })
}
