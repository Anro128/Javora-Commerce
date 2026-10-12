import { apiClient } from '@/api/client'
import type { ApiResponse } from '@/api/types'
import type { Cart, CartItemInput } from '@/features/cart/types'

async function unwrap(request: Promise<{ data: ApiResponse<Cart> }>): Promise<Cart> {
  const { data } = await request
  return data.data
}

export function fetchCart() {
  return unwrap(apiClient.get<ApiResponse<Cart>>('/cart'))
}

export function addCartItem(input: CartItemInput) {
  return unwrap(apiClient.post<ApiResponse<Cart>>('/cart/items', input))
}

export function updateCartItem(variantId: number, quantity: number) {
  return unwrap(apiClient.put<ApiResponse<Cart>>(`/cart/items/${variantId}`, { quantity }))
}

export function removeCartItem(variantId: number) {
  return unwrap(apiClient.delete<ApiResponse<Cart>>(`/cart/items/${variantId}`))
}

export function mergeCart(items: CartItemInput[]) {
  return unwrap(apiClient.post<ApiResponse<Cart>>('/cart/merge', { items }))
}

// Prices a browser-only cart for visitors who are not logged in.
export function previewGuestCart(items: CartItemInput[]) {
  return unwrap(apiClient.post<ApiResponse<Cart>>('/public/cart/preview', { items }))
}
