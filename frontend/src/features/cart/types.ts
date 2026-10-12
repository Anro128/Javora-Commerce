export type CartIssue = 'UNAVAILABLE' | 'OUT_OF_STOCK' | 'LOW_STOCK'

export interface CartLine {
  variantId: number
  productId: number
  productName: string
  productSlug: string
  variantName: string
  sku: string
  imageUrl: string | null
  unitPrice: number
  quantity: number
  lineTotal: number
  weightGram: number
  available: number
  purchasable: boolean
  issue: CartIssue | null
}

export interface Cart {
  items: CartLine[]
  subtotal: number
  totalWeightGram: number
  itemCount: number
  hasIssues: boolean
}

export interface CartItemInput {
  variantId: number
  quantity: number
}
