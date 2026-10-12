import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useParams } from 'react-router'
import { toast } from 'sonner'
import { ApiError } from '@/api/client'
import { PageSpinner } from '@/components/layout/PageSpinner'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { cancelOrder, fetchOrder } from '@/features/checkout/api'
import type { OrderStatus } from '@/features/checkout/types'
import { errorMessage } from '@/lib/form'
import { formatRupiah } from '@/lib/format'
import { cn } from '@/lib/utils'

const STATUS_LABELS: Record<OrderStatus, string> = {
  PENDING_PAYMENT: 'Awaiting payment',
  PAID: 'Paid',
  PROCESSING: 'Processing',
  SHIPPED: 'Shipped',
  DELIVERED: 'Delivered',
  COMPLETED: 'Completed',
  CANCELLED: 'Cancelled',
  REFUNDED: 'Refunded',
}

const STATUS_STYLES: Partial<Record<OrderStatus, string>> = {
  PENDING_PAYMENT: 'bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-300',
  CANCELLED: 'bg-muted text-muted-foreground',
  REFUNDED: 'bg-muted text-muted-foreground',
}

const dateFormatter = new Intl.DateTimeFormat('en-GB', { dateStyle: 'medium', timeStyle: 'short' })

export function OrderDetailPage() {
  const { orderNumber = '' } = useParams()
  const queryClient = useQueryClient()
  const order = useQuery({
    queryKey: ['order', orderNumber],
    queryFn: () => fetchOrder(orderNumber),
    retry: false,
  })

  const cancel = useMutation({
    mutationFn: () => cancelOrder(orderNumber),
    onSuccess: (updated) => {
      queryClient.setQueryData(['order', orderNumber], updated)
      toast.success('Order cancelled')
    },
    onError: (error) => toast.error(errorMessage(error) ?? 'Could not cancel the order'),
  })

  if (order.isPending) {
    return <PageSpinner />
  }
  if (order.isError) {
    const notFound = order.error instanceof ApiError && order.error.status === 404
    return (
      <section className="flex flex-col items-center gap-4 py-16 text-center">
        <h1 className="text-2xl font-semibold">{notFound ? 'Order not found' : 'Something went wrong'}</h1>
        <Button variant="outline" asChild>
          <Link to="/products">Back to shop</Link>
        </Button>
      </section>
    )
  }

  const data = order.data
  const confirmCancel = () => {
    if (window.confirm('Cancel this order? The reserved items will be released.')) {
      cancel.mutate()
    }
  }

  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Order {data.orderNumber}</h1>
          <p className="text-muted-foreground text-sm">
            Placed on {dateFormatter.format(new Date(data.createdAt))}
          </p>
        </div>
        <span
          className={cn(
            'rounded-full px-3 py-1 text-sm font-medium',
            STATUS_STYLES[data.status] ?? 'bg-green-100 text-green-800 dark:bg-green-950 dark:text-green-300',
          )}
        >
          {STATUS_LABELS[data.status]}
        </span>
      </div>

      {data.status === 'PENDING_PAYMENT' ? (
        <div className="flex flex-col gap-3 rounded-lg border border-amber-300 bg-amber-50 p-4 text-sm text-amber-900 sm:flex-row sm:items-center sm:justify-between dark:border-amber-700 dark:bg-amber-950 dark:text-amber-100">
          <p>
            Your items are reserved until {dateFormatter.format(new Date(data.expiresAt))}. Unpaid orders are
            cancelled after that.
          </p>
          <Button variant="outline" size="sm" disabled={cancel.isPending} onClick={confirmCancel}>
            Cancel order
          </Button>
        </div>
      ) : null}

      <Card>
        <CardHeader>
          <CardTitle>Items</CardTitle>
        </CardHeader>
        <CardContent>
          <ul className="divide-y">
            {data.items.map((item) => (
              <li key={item.variantId} className="flex justify-between gap-4 py-3 text-sm">
                <span>
                  <span className="block font-medium">{item.productName}</span>
                  <span className="text-muted-foreground">
                    {item.variantName} · {item.quantity} × {formatRupiah(item.unitPrice)}
                  </span>
                </span>
                <span className="font-medium">{formatRupiah(item.lineTotal)}</span>
              </li>
            ))}
          </ul>
        </CardContent>
      </Card>

      <div className="grid gap-6 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Delivery address</CardTitle>
          </CardHeader>
          <CardContent>
            <address className="text-muted-foreground text-sm not-italic">
              <p className="text-foreground font-medium">{data.address.recipientName}</p>
              <p>{data.address.phone}</p>
              <p>{data.address.street}</p>
              <p>
                {data.address.district}, {data.address.city}
              </p>
              <p>
                {data.address.province} {data.address.postalCode}
              </p>
            </address>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Payment summary</CardTitle>
          </CardHeader>
          <CardContent>
            <dl className="flex flex-col gap-2 text-sm">
              <div className="flex justify-between">
                <dt className="text-muted-foreground">Subtotal</dt>
                <dd>{formatRupiah(data.subtotal)}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-muted-foreground">
                  Shipping ({data.courier.toUpperCase()} {data.courierService.toUpperCase()})
                </dt>
                <dd>{formatRupiah(data.shippingCost)}</dd>
              </div>
              {data.discount > 0 ? (
                <div className="flex justify-between">
                  <dt className="text-muted-foreground">Discount</dt>
                  <dd>-{formatRupiah(data.discount)}</dd>
                </div>
              ) : null}
              <div className="flex justify-between border-t pt-3 text-base font-semibold">
                <dt>Total</dt>
                <dd>{formatRupiah(data.total)}</dd>
              </div>
            </dl>
          </CardContent>
        </Card>
      </div>

      <Button variant="ghost" asChild className="self-start">
        <Link to="/products">Continue shopping</Link>
      </Button>
    </div>
  )
}
