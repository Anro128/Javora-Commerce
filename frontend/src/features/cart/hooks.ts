import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import * as cartApi from '@/features/cart/api'
import { useGuestCart } from '@/features/cart/guestCart'
import type { Cart } from '@/features/cart/types'
import { useAuthStore } from '@/features/auth/store'

const EMPTY_CART: Cart = { items: [], subtotal: 0, totalWeightGram: 0, itemCount: 0, hasIssues: false }

export const CART_KEY = ['cart'] as const

// One hook for both worlds: the server cart when logged in, the browser cart for guests.
export function useCart() {
  const status = useAuthStore((state) => state.status)
  const guestItems = useGuestCart((state) => state.items)

  const serverCart = useQuery({
    queryKey: [...CART_KEY, 'server'],
    queryFn: cartApi.fetchCart,
    enabled: status === 'authenticated',
  })
  const guestCart = useQuery({
    queryKey: [...CART_KEY, 'guest', guestItems],
    queryFn: () => cartApi.previewGuestCart(guestItems),
    enabled: status === 'anonymous' && guestItems.length > 0,
    placeholderData: keepPreviousData,
  })

  if (status === 'authenticated') {
    return {
      cart: serverCart.data ?? EMPTY_CART,
      isPending: serverCart.isPending,
      isError: serverCart.isError,
    }
  }
  if (status === 'anonymous') {
    const empty = guestItems.length === 0
    return {
      cart: empty ? EMPTY_CART : (guestCart.data ?? EMPTY_CART),
      isPending: !empty && guestCart.isPending,
      isError: !empty && guestCart.isError,
    }
  }
  return { cart: EMPTY_CART, isPending: true, isError: false }
}

// Badge number for the header without waiting for any request.
export function useCartCount() {
  const status = useAuthStore((state) => state.status)
  const guestItems = useGuestCart((state) => state.items)
  const serverCart = useQuery({
    queryKey: [...CART_KEY, 'server'],
    queryFn: cartApi.fetchCart,
    enabled: status === 'authenticated',
  })

  if (status === 'authenticated') {
    return serverCart.data?.itemCount ?? 0
  }
  return guestItems.reduce((total, item) => total + item.quantity, 0)
}

function useCartMutations() {
  const status = useAuthStore((state) => state.status)
  const queryClient = useQueryClient()
  const guest = useGuestCart.getState
  const refresh = () => queryClient.invalidateQueries({ queryKey: CART_KEY })
  return { isAuthenticated: status === 'authenticated', guest, refresh, queryClient }
}

export function useAddToCart() {
  const { isAuthenticated, guest, refresh } = useCartMutations()
  return useMutation({
    mutationFn: async ({ variantId, quantity }: { variantId: number; quantity: number }) => {
      if (isAuthenticated) {
        return cartApi.addCartItem({ variantId, quantity })
      }
      guest().add(variantId, quantity)
      return null
    },
    onSuccess: refresh,
  })
}

export function useUpdateCartItem() {
  const { isAuthenticated, guest, refresh } = useCartMutations()
  return useMutation({
    mutationFn: async ({ variantId, quantity }: { variantId: number; quantity: number }) => {
      if (isAuthenticated) {
        return cartApi.updateCartItem(variantId, quantity)
      }
      guest().setQuantity(variantId, quantity)
      return null
    },
    onSuccess: refresh,
  })
}

export function useRemoveCartItem() {
  const { isAuthenticated, guest, refresh } = useCartMutations()
  return useMutation({
    mutationFn: async (variantId: number) => {
      if (isAuthenticated) {
        return cartApi.removeCartItem(variantId)
      }
      guest().remove(variantId)
      return null
    },
    onSuccess: refresh,
  })
}
