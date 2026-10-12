import { keepPreviousData, useQuery } from '@tanstack/react-query'
import * as catalogApi from '@/features/catalog/api'
import type { ProductQuery } from '@/features/catalog/types'

export function useCategories() {
  return useQuery({ queryKey: ['categories'], queryFn: catalogApi.fetchCategories, staleTime: 5 * 60_000 })
}

export function useProducts(query: ProductQuery) {
  return useQuery({
    queryKey: ['products', query],
    queryFn: () => catalogApi.fetchProducts(query),
    // Keeps the previous page visible while the next one loads so the grid does not flash.
    placeholderData: keepPreviousData,
  })
}

export function useProduct(slug: string | undefined) {
  return useQuery({
    queryKey: ['product', slug],
    queryFn: () => catalogApi.fetchProduct(slug ?? ''),
    enabled: !!slug,
    retry: false,
  })
}
