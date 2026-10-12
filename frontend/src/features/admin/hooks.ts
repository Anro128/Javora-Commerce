import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import * as adminApi from '@/features/admin/api'
import type {
  AdminProductQuery,
  CategoryInput,
  InventoryQuery,
  ProductCreateInput,
  ProductStatus,
  ProductUpdateInput,
  VariantCreateInput,
  VariantUpdateInput,
} from '@/features/admin/types'

// Catalog data feeds both the admin screens and the public shop, so both caches are refreshed.
function useRefreshCatalog() {
  const queryClient = useQueryClient()
  return () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: ['admin'] }),
      queryClient.invalidateQueries({ queryKey: ['products'] }),
      queryClient.invalidateQueries({ queryKey: ['product'] }),
      queryClient.invalidateQueries({ queryKey: ['categories'] }),
    ])
}

export function useAdminCategories() {
  return useQuery({ queryKey: ['admin', 'categories'], queryFn: adminApi.fetchAdminCategories })
}

export function useSaveCategory() {
  const refresh = useRefreshCatalog()
  return useMutation({
    mutationFn: ({ id, input }: { id?: number; input: CategoryInput }) =>
      id === undefined ? adminApi.createCategory(input) : adminApi.updateCategory(id, input),
    onSuccess: refresh,
  })
}

export function useDeleteCategory() {
  const refresh = useRefreshCatalog()
  return useMutation({
    mutationFn: (id: number) => adminApi.deleteCategory(id),
    onSuccess: refresh,
  })
}

export function useAdminProducts(query: AdminProductQuery) {
  return useQuery({
    queryKey: ['admin', 'products', query],
    queryFn: () => adminApi.fetchAdminProducts(query),
    placeholderData: keepPreviousData,
  })
}

export function useAdminProduct(id: number | undefined) {
  return useQuery({
    queryKey: ['admin', 'product', id],
    queryFn: () => adminApi.fetchAdminProduct(id ?? 0),
    enabled: id !== undefined,
  })
}

export function useCreateProduct() {
  const refresh = useRefreshCatalog()
  return useMutation({
    mutationFn: (input: ProductCreateInput) => adminApi.createProduct(input),
    onSuccess: refresh,
  })
}

export function useUpdateProduct(id: number) {
  const refresh = useRefreshCatalog()
  return useMutation({
    mutationFn: (input: ProductUpdateInput) => adminApi.updateProduct(id, input),
    onSuccess: refresh,
  })
}

export function useChangeProductStatus() {
  const refresh = useRefreshCatalog()
  return useMutation({
    mutationFn: ({ id, status }: { id: number; status: ProductStatus }) =>
      adminApi.changeProductStatus(id, status),
    onSuccess: refresh,
  })
}

export function useAddVariant(productId: number) {
  const refresh = useRefreshCatalog()
  return useMutation({
    mutationFn: (input: VariantCreateInput) => adminApi.addVariant(productId, input),
    onSuccess: refresh,
  })
}

export function useUpdateVariant(productId: number) {
  const refresh = useRefreshCatalog()
  return useMutation({
    mutationFn: ({ variantId, input }: { variantId: number; input: VariantUpdateInput }) =>
      adminApi.updateVariant(productId, variantId, input),
    onSuccess: refresh,
  })
}

export function useUploadImage(productId: number) {
  const refresh = useRefreshCatalog()
  return useMutation({
    mutationFn: (file: File) => adminApi.uploadImage(productId, file),
    onSuccess: refresh,
  })
}

export function useMakePrimaryImage(productId: number) {
  const refresh = useRefreshCatalog()
  return useMutation({
    mutationFn: (imageId: number) => adminApi.makePrimaryImage(productId, imageId),
    onSuccess: refresh,
  })
}

export function useDeleteImage(productId: number) {
  const refresh = useRefreshCatalog()
  return useMutation({
    mutationFn: (imageId: number) => adminApi.deleteImage(productId, imageId),
    onSuccess: refresh,
  })
}

export function useInventory(query: InventoryQuery) {
  return useQuery({
    queryKey: ['admin', 'inventory', query],
    queryFn: () => adminApi.fetchInventory(query),
    placeholderData: keepPreviousData,
  })
}

export function useSetStock() {
  const refresh = useRefreshCatalog()
  return useMutation({
    mutationFn: ({ variantId, quantity }: { variantId: number; quantity: number }) =>
      adminApi.setStock(variantId, quantity),
    onSuccess: refresh,
  })
}
