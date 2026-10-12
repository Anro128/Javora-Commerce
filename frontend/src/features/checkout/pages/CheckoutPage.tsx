import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Loader2Icon } from 'lucide-react'
import { useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { toast } from 'sonner'
import { EmptyState } from '@/components/common/EmptyState'
import { FormError } from '@/components/form/FormError'
import { PageSpinner } from '@/components/layout/PageSpinner'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { AddressForm } from '@/features/account/components/AddressForm'
import { useAddresses, useCreateAddress } from '@/features/account/hooks'
import { CART_KEY, useCart } from '@/features/cart/hooks'
import { fetchShippingOptions, placeOrder, previewCheckout } from '@/features/checkout/api'
import type { ShippingOption } from '@/features/checkout/types'
import { errorMessage } from '@/lib/form'
import { formatRupiah } from '@/lib/format'
import { cn } from '@/lib/utils'

function optionKey(option: Pick<ShippingOption, 'courier' | 'service'>) {
  return `${option.courier}:${option.service}`
}

function etdText(option: ShippingOption) {
  return option.etdMinDays === option.etdMaxDays
    ? `${option.etdMinDays} day`
    : `${option.etdMinDays}-${option.etdMaxDays} days`
}

function StepCard({ number, title, children }: { number: number; title: string; children: React.ReactNode }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-3">
          <span className="bg-primary text-primary-foreground flex size-7 items-center justify-center rounded-full text-sm">
            {number}
          </span>
          {title}
        </CardTitle>
      </CardHeader>
      <CardContent>{children}</CardContent>
    </Card>
  )
}

export function CheckoutPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { cart, isPending: cartPending } = useCart()
  const addresses = useAddresses()
  const createAddress = useCreateAddress()

  const [chosenAddressId, setChosenAddressId] = useState<number | null>(null)
  const [chosenShipping, setChosenShipping] = useState<string | null>(null)
  const [addingAddress, setAddingAddress] = useState(false)

  // Falls back to the default (first) address until the shopper picks another one.
  const addressId = chosenAddressId ?? addresses.data?.[0]?.id ?? null
  const cartSignature = useMemo(
    () => cart.items.map((line) => `${line.variantId}x${line.quantity}`).join(','),
    [cart.items],
  )

  const shippingOptions = useQuery({
    queryKey: ['shipping-options', addressId, cartSignature],
    queryFn: () => fetchShippingOptions(addressId ?? 0),
    enabled: addressId !== null && cart.items.length > 0,
  })
  const selectedShipping =
    shippingOptions.data?.find((option) => optionKey(option) === chosenShipping) ?? shippingOptions.data?.[0]

  const request =
    addressId !== null && selectedShipping
      ? { addressId, courier: selectedShipping.courier, service: selectedShipping.service }
      : null
  const preview = useQuery({
    queryKey: ['checkout-preview', request, cartSignature],
    queryFn: () => previewCheckout(request!),
    enabled: request !== null,
    retry: false,
  })

  const order = useMutation({
    mutationFn: () => placeOrder(request!),
    onSuccess: async (created) => {
      await queryClient.invalidateQueries({ queryKey: CART_KEY })
      toast.success('Order placed')
      navigate(`/account/orders/${created.orderNumber}`, { replace: true })
    },
    onError: (error) => {
      toast.error(errorMessage(error) ?? 'Could not place the order')
      void queryClient.invalidateQueries({ queryKey: CART_KEY })
    },
  })

  if (cartPending || addresses.isPending) {
    return <PageSpinner />
  }
  if (cart.items.length === 0) {
    return (
      <EmptyState
        title="Your cart is empty"
        description="Add something to your cart before checking out."
        action={
          <Button asChild>
            <Link to="/products">Browse products</Link>
          </Button>
        }
      />
    )
  }

  const hasAddresses = (addresses.data?.length ?? 0) > 0
  const showAddressForm = addingAddress || !hasAddresses

  return (
    <div className="grid gap-8 lg:grid-cols-[1fr_380px]">
      <div className="flex flex-col gap-6">
        <h1 className="text-2xl font-bold tracking-tight">Checkout</h1>

        <StepCard number={1} title="Delivery address">
          {showAddressForm ? (
            <AddressForm
              isSaving={createAddress.isPending}
              error={createAddress.error}
              onCancel={() => setAddingAddress(false)}
              onSubmit={(input) =>
                createAddress.mutate(input, {
                  onSuccess: (created) => {
                    setChosenAddressId(created.id)
                    setAddingAddress(false)
                    toast.success('Address saved')
                  },
                })
              }
            />
          ) : (
            <div className="flex flex-col gap-3">
              <ul className="flex flex-col gap-3" role="radiogroup" aria-label="Delivery address">
                {addresses.data?.map((address) => (
                  <li key={address.id}>
                    <label
                      className={cn(
                        'flex cursor-pointer gap-3 rounded-lg border p-3',
                        addressId === address.id && 'border-primary ring-primary/30 ring-2',
                      )}
                    >
                      <input
                        type="radio"
                        name="address"
                        className="mt-1"
                        checked={addressId === address.id}
                        onChange={() => {
                          setChosenAddressId(address.id)
                          setChosenShipping(null)
                        }}
                      />
                      <span className="text-sm">
                        <span className="block font-medium">
                          {address.recipientName}
                          {address.label ? ` · ${address.label}` : ''}
                        </span>
                        <span className="text-muted-foreground block">{address.phone}</span>
                        <span className="text-muted-foreground block">
                          {address.street}, {address.district}, {address.city}, {address.province}{' '}
                          {address.postalCode}
                        </span>
                      </span>
                    </label>
                  </li>
                ))}
              </ul>
              <Button
                type="button"
                variant="outline"
                size="sm"
                className="self-start"
                onClick={() => setAddingAddress(true)}
              >
                Add a new address
              </Button>
            </div>
          )}
        </StepCard>

        <StepCard number={2} title="Shipping method">
          {addressId === null ? (
            <p className="text-muted-foreground text-sm">Choose a delivery address first.</p>
          ) : shippingOptions.isPending ? (
            <p className="text-muted-foreground text-sm">Loading shipping options...</p>
          ) : shippingOptions.isError ? (
            <FormError message={errorMessage(shippingOptions.error)} />
          ) : (
            <ul className="flex flex-col gap-3" role="radiogroup" aria-label="Shipping method">
              {shippingOptions.data.map((option) => (
                <li key={optionKey(option)}>
                  <label
                    className={cn(
                      'flex cursor-pointer items-center justify-between gap-3 rounded-lg border p-3',
                      selectedShipping &&
                        optionKey(selectedShipping) === optionKey(option) &&
                        'border-primary ring-primary/30 ring-2',
                    )}
                  >
                    <span className="flex items-center gap-3 text-sm">
                      <input
                        type="radio"
                        name="shipping"
                        checked={!!selectedShipping && optionKey(selectedShipping) === optionKey(option)}
                        onChange={() => setChosenShipping(optionKey(option))}
                      />
                      <span>
                        <span className="block font-medium">
                          {option.courierName} {option.serviceName}
                        </span>
                        <span className="text-muted-foreground block">Arrives in {etdText(option)}</span>
                      </span>
                    </span>
                    <span className="font-semibold">{formatRupiah(option.cost)}</span>
                  </label>
                </li>
              ))}
            </ul>
          )}
        </StepCard>
      </div>

      <aside>
        <Card className="sticky top-24">
          <CardHeader>
            <CardTitle>Review order</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-4">
            <ul className="flex flex-col gap-2 text-sm">
              {cart.items.map((line) => (
                <li key={line.variantId} className="flex justify-between gap-3">
                  <span className="min-w-0">
                    <span className="block truncate">{line.productName}</span>
                    <span className="text-muted-foreground">
                      {line.variantName} × {line.quantity}
                    </span>
                  </span>
                  <span className="shrink-0">{formatRupiah(line.lineTotal)}</span>
                </li>
              ))}
            </ul>

            <dl className="flex flex-col gap-2 border-t pt-4 text-sm">
              <div className="flex justify-between">
                <dt className="text-muted-foreground">Subtotal</dt>
                <dd>{formatRupiah(preview.data?.subtotal ?? cart.subtotal)}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-muted-foreground">Shipping</dt>
                <dd>{preview.data ? formatRupiah(preview.data.shippingCost) : '-'}</dd>
              </div>
              <div className="flex justify-between border-t pt-3 text-lg font-semibold">
                <dt>Total</dt>
                <dd>{preview.data ? formatRupiah(preview.data.total) : '-'}</dd>
              </div>
            </dl>

            {preview.isError ? <FormError message={errorMessage(preview.error)} /> : null}

            <Button
              size="lg"
              disabled={!request || !preview.data || order.isPending || cart.hasIssues}
              onClick={() => order.mutate()}
            >
              {order.isPending ? <Loader2Icon className="animate-spin" /> : null}
              Place order
            </Button>
            <p className="text-muted-foreground text-xs">
              Stock is reserved for you as soon as the order is placed.
            </p>
          </CardContent>
        </Card>
      </aside>
    </div>
  )
}
