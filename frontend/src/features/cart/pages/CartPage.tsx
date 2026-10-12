import { MinusIcon, PlusIcon, Trash2Icon } from 'lucide-react'
import { Link, useNavigate } from 'react-router'
import { toast } from 'sonner'
import { EmptyState } from '@/components/common/EmptyState'
import { PageSpinner } from '@/components/layout/PageSpinner'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { useAuthStore } from '@/features/auth/store'
import { MAX_LINE_QUANTITY } from '@/features/cart/guestCart'
import { useCart, useRemoveCartItem, useUpdateCartItem } from '@/features/cart/hooks'
import type { CartLine } from '@/features/cart/types'
import { ProductImage } from '@/features/catalog/components/ProductImage'
import { errorMessage } from '@/lib/form'
import { formatRupiah } from '@/lib/format'

const ISSUE_TEXT: Record<NonNullable<CartLine['issue']>, string> = {
  UNAVAILABLE: 'This item is no longer available. Please remove it to continue.',
  OUT_OF_STOCK: 'This item is out of stock. Please remove it to continue.',
  LOW_STOCK: 'Not enough stock for this quantity.',
}

function CartLineRow({ line }: { line: CartLine }) {
  const update = useUpdateCartItem()
  const remove = useRemoveCartItem()
  const busy = update.isPending || remove.isPending

  const setQuantity = (quantity: number) => {
    update.mutate(
      { variantId: line.variantId, quantity },
      { onError: (error) => toast.error(errorMessage(error) ?? 'Could not update the quantity') },
    )
  }
  const maxQuantity = Math.min(MAX_LINE_QUANTITY, Math.max(line.available, 1))

  return (
    <li className="flex gap-4 py-4">
      <Link
        to={`/products/${line.productSlug}`}
        className="bg-muted size-20 shrink-0 overflow-hidden rounded-lg sm:size-24"
      >
        <ProductImage src={line.imageUrl} alt={line.productName} className="text-2xl" />
      </Link>

      <div className="flex min-w-0 flex-1 flex-col gap-2">
        <div className="flex items-start justify-between gap-2">
          <div className="min-w-0">
            <Link to={`/products/${line.productSlug}`} className="line-clamp-2 font-medium hover:underline">
              {line.productName}
            </Link>
            <p className="text-muted-foreground text-sm">{line.variantName}</p>
          </div>
          <p className="shrink-0 font-semibold">{formatRupiah(line.lineTotal)}</p>
        </div>

        <p className="text-muted-foreground text-sm">{formatRupiah(line.unitPrice)} each</p>

        {line.issue ? (
          <p role="alert" className="text-destructive text-sm">
            {ISSUE_TEXT[line.issue]}
            {line.issue === 'LOW_STOCK' ? ` Only ${line.available} left.` : ''}
          </p>
        ) : null}

        <div className="flex items-center justify-between">
          <div
            className="flex items-center gap-1"
            role="group"
            aria-label={`Quantity of ${line.productName}`}
          >
            <Button
              type="button"
              variant="outline"
              size="icon"
              className="size-8"
              aria-label={`Decrease quantity of ${line.productName}`}
              disabled={busy || line.quantity <= 1}
              onClick={() => setQuantity(line.quantity - 1)}
            >
              <MinusIcon />
            </Button>
            <span className="w-10 text-center text-sm font-medium" aria-live="polite">
              {line.quantity}
            </span>
            <Button
              type="button"
              variant="outline"
              size="icon"
              className="size-8"
              aria-label={`Increase quantity of ${line.productName}`}
              disabled={busy || line.quantity >= maxQuantity || !line.purchasable}
              onClick={() => setQuantity(line.quantity + 1)}
            >
              <PlusIcon />
            </Button>
          </div>
          <Button
            type="button"
            variant="ghost"
            size="sm"
            aria-label={`Remove ${line.productName} from cart`}
            disabled={busy}
            onClick={() => remove.mutate(line.variantId)}
          >
            <Trash2Icon /> Remove
          </Button>
        </div>
      </div>
    </li>
  )
}

export function CartPage() {
  const navigate = useNavigate()
  const status = useAuthStore((state) => state.status)
  const { cart, isPending, isError } = useCart()

  if (isPending) {
    return <PageSpinner />
  }
  if (isError) {
    return (
      <EmptyState title="Something went wrong" description="We could not load your cart. Please try again." />
    )
  }
  if (cart.items.length === 0) {
    return (
      <EmptyState
        title="Your cart is empty"
        description="Browse the shop and add something you like."
        action={
          <Button asChild>
            <Link to="/products">Start shopping</Link>
          </Button>
        }
      />
    )
  }

  const goToCheckout = () =>
    navigate(status === 'authenticated' ? '/checkout' : '/login', {
      state: status === 'authenticated' ? undefined : { from: { pathname: '/checkout' } },
    })

  return (
    <div className="grid gap-8 lg:grid-cols-[1fr_360px]">
      <section aria-labelledby="cart-heading">
        <h1 id="cart-heading" className="mb-2 text-2xl font-bold tracking-tight">
          Shopping cart
        </h1>
        <ul className="divide-y border-y">
          {cart.items.map((line) => (
            <CartLineRow key={line.variantId} line={line} />
          ))}
        </ul>
      </section>

      <aside>
        <Card>
          <CardHeader>
            <CardTitle>Order summary</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-4">
            <dl className="flex flex-col gap-2 text-sm">
              <div className="flex justify-between">
                <dt className="text-muted-foreground">Items ({cart.itemCount})</dt>
                <dd className="font-medium">{formatRupiah(cart.subtotal)}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-muted-foreground">Shipping</dt>
                <dd className="text-muted-foreground">Calculated at checkout</dd>
              </div>
            </dl>
            <div className="flex justify-between border-t pt-4 text-lg font-semibold">
              <span>Subtotal</span>
              <span>{formatRupiah(cart.subtotal)}</span>
            </div>
            <Button size="lg" disabled={cart.hasIssues} onClick={goToCheckout}>
              {status === 'authenticated' ? 'Proceed to checkout' : 'Log in to checkout'}
            </Button>
            {cart.hasIssues ? (
              <p className="text-muted-foreground text-xs">Fix the highlighted items to continue.</p>
            ) : null}
            <Button variant="ghost" asChild>
              <Link to="/products">Continue shopping</Link>
            </Button>
          </CardContent>
        </Card>
      </aside>
    </div>
  )
}
