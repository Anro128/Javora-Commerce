import type { CartLine } from '@/features/cart/types'

export interface ShippingOption {
  courier: string
  courierName: string
  service: string
  serviceName: string
  cost: number
  etdMinDays: number
  etdMaxDays: number
}

export interface CheckoutRequest {
  addressId: number
  courier: string
  service: string
}

export interface ShippingAddress {
  recipientName: string
  phone: string
  province: string
  city: string
  district: string
  postalCode: string
  street: string
}

export interface CheckoutPreview {
  items: CartLine[]
  subtotal: number
  shippingCost: number
  discount: number
  total: number
  totalWeightGram: number
  shipping: ShippingOption
  address: ShippingAddress
}

export type OrderStatus =
  'PENDING_PAYMENT' | 'PAID' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'COMPLETED' | 'CANCELLED' | 'REFUNDED'

export interface OrderItem {
  variantId: number
  productId: number
  productName: string
  variantName: string
  sku: string
  unitPrice: number
  quantity: number
  lineTotal: number
}

export interface Order {
  orderNumber: string
  status: OrderStatus
  subtotal: number
  shippingCost: number
  discount: number
  total: number
  courier: string
  courierService: string
  address: ShippingAddress
  items: OrderItem[]
  createdAt: string
  expiresAt: string
  cancelledAt: string | null
}
