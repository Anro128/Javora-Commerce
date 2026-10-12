import { useQueryClient } from '@tanstack/react-query'
import { useEffect, useRef } from 'react'
import { useAuthStore } from '@/features/auth/store'
import { mergeCart } from '@/features/cart/api'
import { useGuestCart } from '@/features/cart/guestCart'
import { CART_KEY } from '@/features/cart/hooks'

// Moves the browser cart into the account cart at login and drops cached carts at logout.
export function CartSync() {
  const status = useAuthStore((state) => state.status)
  const queryClient = useQueryClient()
  const previous = useRef(status)

  useEffect(() => {
    const before = previous.current
    previous.current = status

    if (status === 'authenticated') {
      const items = useGuestCart.getState().items
      if (items.length === 0) {
        return
      }
      mergeCart(items)
        .then(() => {
          useGuestCart.getState().clear()
          return queryClient.invalidateQueries({ queryKey: CART_KEY })
        })
        // The guest cart is kept when merging fails, so nothing is lost and the next login retries.
        .catch(() => undefined)
    } else if (status === 'anonymous' && before === 'authenticated') {
      queryClient.removeQueries({ queryKey: CART_KEY })
    }
  }, [status, queryClient])

  return null
}
