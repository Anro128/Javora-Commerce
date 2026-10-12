import { create } from 'zustand'
import { createJSONStorage, persist } from 'zustand/middleware'
import type { CartItemInput } from '@/features/cart/types'

export const MAX_LINE_QUANTITY = 99

interface GuestCartState {
  items: CartItemInput[]
  add: (variantId: number, quantity: number) => void
  setQuantity: (variantId: number, quantity: number) => void
  remove: (variantId: number) => void
  clear: () => void
}

function clamp(quantity: number) {
  return Math.min(MAX_LINE_QUANTITY, Math.max(1, Math.floor(quantity)))
}

// Only variant ids and quantities are stored; prices and stock are always fetched fresh.
export const useGuestCart = create<GuestCartState>()(
  persist(
    (set) => ({
      items: [],
      add: (variantId, quantity) =>
        set((state) => {
          const existing = state.items.find((item) => item.variantId === variantId)
          if (existing) {
            return {
              items: state.items.map((item) =>
                item.variantId === variantId ? { ...item, quantity: clamp(item.quantity + quantity) } : item,
              ),
            }
          }
          return { items: [...state.items, { variantId, quantity: clamp(quantity) }] }
        }),
      setQuantity: (variantId, quantity) =>
        set((state) => ({
          items: state.items.map((item) =>
            item.variantId === variantId ? { ...item, quantity: clamp(quantity) } : item,
          ),
        })),
      remove: (variantId) =>
        set((state) => ({ items: state.items.filter((item) => item.variantId !== variantId) })),
      clear: () => set({ items: [] }),
    }),
    {
      name: 'javora.guest-cart',
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({ items: state.items }),
    },
  ),
)
