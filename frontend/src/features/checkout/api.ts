import { apiClient } from '@/api/client'
import type { ApiResponse } from '@/api/types'
import type { CheckoutPreview, CheckoutRequest, Order, ShippingOption } from '@/features/checkout/types'

export async function fetchShippingOptions(addressId: number): Promise<ShippingOption[]> {
  const { data } = await apiClient.get<ApiResponse<ShippingOption[]>>('/shipping/options', {
    params: { addressId },
  })
  return data.data
}

export async function previewCheckout(request: CheckoutRequest): Promise<CheckoutPreview> {
  const { data } = await apiClient.post<ApiResponse<CheckoutPreview>>('/checkout/preview', request)
  return data.data
}

export async function placeOrder(request: CheckoutRequest): Promise<Order> {
  const { data } = await apiClient.post<ApiResponse<Order>>('/checkout', request)
  return data.data
}

export async function fetchOrder(orderNumber: string): Promise<Order> {
  const { data } = await apiClient.get<ApiResponse<Order>>(`/orders/${orderNumber}`)
  return data.data
}

export async function cancelOrder(orderNumber: string): Promise<Order> {
  const { data } = await apiClient.post<ApiResponse<Order>>(`/orders/${orderNumber}/cancel`)
  return data.data
}
