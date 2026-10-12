import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach, beforeEach, vi } from 'vitest'

// Keeps every test off the network; individual tests override these with mockResolvedValue.
vi.mock('@/features/cart/api', () => {
  const emptyCart = { items: [], subtotal: 0, totalWeightGram: 0, itemCount: 0, hasIssues: false }
  return {
    fetchCart: vi.fn(async () => emptyCart),
    addCartItem: vi.fn(async () => emptyCart),
    updateCartItem: vi.fn(async () => emptyCart),
    removeCartItem: vi.fn(async () => emptyCart),
    mergeCart: vi.fn(async () => emptyCart),
    previewGuestCart: vi.fn(async () => emptyCart),
  }
})

// jsdom lacks matchMedia, which the toast theme detection relies on.
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: (query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => {},
    removeListener: () => {},
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => false,
  }),
})

beforeEach(() => {
  localStorage.clear()
})

afterEach(() => {
  cleanup()
})
