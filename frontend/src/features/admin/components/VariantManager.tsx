import { Loader2Icon, PlusIcon } from 'lucide-react'
import { useState } from 'react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useAddVariant, useSetStock, useUpdateVariant } from '@/features/admin/hooks'
import type { AdminVariant } from '@/features/admin/types'
import { errorMessage } from '@/lib/form'

function VariantRow({ productId, variant }: { productId: number; variant: AdminVariant }) {
  const updateVariant = useUpdateVariant(productId)
  const setStock = useSetStock()
  const [sku, setSku] = useState(variant.sku)
  const [name, setName] = useState(variant.name)
  const [price, setPrice] = useState(String(variant.price))
  const [active, setActive] = useState(variant.active)
  const [quantity, setQuantity] = useState(String(variant.quantity))

  const isSaving = updateVariant.isPending || setStock.isPending

  const save = async () => {
    const parsedPrice = Number(price)
    const parsedQuantity = Number(quantity)
    if (!Number.isFinite(parsedPrice) || parsedPrice < 0) {
      toast.error('Enter a valid price')
      return
    }
    if (!Number.isInteger(parsedQuantity) || parsedQuantity < 0) {
      toast.error('Stock must be a whole number of zero or more')
      return
    }
    try {
      await updateVariant.mutateAsync({
        variantId: variant.id,
        input: { sku: sku.trim(), name: name.trim(), price: parsedPrice, active },
      })
      if (parsedQuantity !== variant.quantity) {
        await setStock.mutateAsync({ variantId: variant.id, quantity: parsedQuantity })
      }
      toast.success(`Saved ${sku.trim()}`)
    } catch (error) {
      toast.error(errorMessage(error) ?? 'Could not save the variant')
    }
  }

  return (
    <li className="grid items-end gap-3 rounded-lg border p-3 md:grid-cols-[1fr_1fr_130px_100px_auto_auto]">
      <div className="flex flex-col gap-1">
        <Label htmlFor={`sku-${variant.id}`}>SKU</Label>
        <Input id={`sku-${variant.id}`} value={sku} onChange={(event) => setSku(event.target.value)} />
      </div>
      <div className="flex flex-col gap-1">
        <Label htmlFor={`name-${variant.id}`}>Name</Label>
        <Input id={`name-${variant.id}`} value={name} onChange={(event) => setName(event.target.value)} />
      </div>
      <div className="flex flex-col gap-1">
        <Label htmlFor={`price-${variant.id}`}>Price</Label>
        <Input
          id={`price-${variant.id}`}
          type="number"
          min={0}
          step="any"
          value={price}
          onChange={(event) => setPrice(event.target.value)}
        />
      </div>
      <div className="flex flex-col gap-1">
        <Label htmlFor={`stock-${variant.id}`}>Stock</Label>
        <Input
          id={`stock-${variant.id}`}
          type="number"
          min={variant.reserved}
          value={quantity}
          onChange={(event) => setQuantity(event.target.value)}
        />
      </div>
      <div className="flex items-center gap-2 pb-2">
        <input
          id={`active-${variant.id}`}
          type="checkbox"
          className="size-4"
          checked={active}
          onChange={(event) => setActive(event.target.checked)}
        />
        <Label htmlFor={`active-${variant.id}`}>Active</Label>
      </div>
      <Button
        type="button"
        size="sm"
        disabled={isSaving}
        onClick={save}
        aria-label={`Save variant ${variant.sku}`}
      >
        {isSaving ? <Loader2Icon className="animate-spin" /> : null}
        Save
      </Button>
      {variant.reserved > 0 ? (
        <p className="text-muted-foreground text-xs md:col-span-6">
          {variant.reserved} reserved by pending orders
        </p>
      ) : null}
    </li>
  )
}

function AddVariantForm({ productId }: { productId: number }) {
  const addVariant = useAddVariant(productId)
  const [sku, setSku] = useState('')
  const [name, setName] = useState('')
  const [price, setPrice] = useState('')
  const [stock, setStock] = useState('0')

  const submit = (event: React.FormEvent) => {
    event.preventDefault()
    const parsedPrice = Number(price)
    const parsedStock = Number(stock)
    if (
      !sku.trim() ||
      !name.trim() ||
      !Number.isFinite(parsedPrice) ||
      parsedPrice < 0 ||
      !Number.isInteger(parsedStock) ||
      parsedStock < 0
    ) {
      toast.error('Fill in the SKU, name, a valid price, and a whole stock number')
      return
    }
    addVariant.mutate(
      { sku: sku.trim(), name: name.trim(), price: parsedPrice, stock: parsedStock },
      {
        onSuccess: () => {
          toast.success('Variant added')
          setSku('')
          setName('')
          setPrice('')
          setStock('0')
        },
        onError: (error) => toast.error(errorMessage(error) ?? 'Could not add the variant'),
      },
    )
  }

  return (
    <form
      onSubmit={submit}
      className="grid items-end gap-3 rounded-lg border border-dashed p-3 md:grid-cols-[1fr_1fr_130px_100px_auto]"
    >
      <div className="flex flex-col gap-1">
        <Label htmlFor="new-sku">New SKU</Label>
        <Input id="new-sku" value={sku} onChange={(event) => setSku(event.target.value)} />
      </div>
      <div className="flex flex-col gap-1">
        <Label htmlFor="new-name">New variant name</Label>
        <Input id="new-name" value={name} onChange={(event) => setName(event.target.value)} />
      </div>
      <div className="flex flex-col gap-1">
        <Label htmlFor="new-price">Price</Label>
        <Input
          id="new-price"
          type="number"
          min={0}
          step="any"
          value={price}
          onChange={(event) => setPrice(event.target.value)}
        />
      </div>
      <div className="flex flex-col gap-1">
        <Label htmlFor="new-stock">Stock</Label>
        <Input
          id="new-stock"
          type="number"
          min={0}
          value={stock}
          onChange={(event) => setStock(event.target.value)}
        />
      </div>
      <Button type="submit" size="sm" variant="outline" disabled={addVariant.isPending}>
        <PlusIcon /> Add variant
      </Button>
    </form>
  )
}

export function VariantManager({ productId, variants }: { productId: number; variants: AdminVariant[] }) {
  return (
    <section className="flex flex-col gap-3" aria-labelledby="variants-heading">
      <h2 id="variants-heading" className="text-lg font-semibold">
        Variants and stock
      </h2>
      <ul className="flex flex-col gap-3">
        {variants.map((variant) => (
          // The key includes quantity so a stock change elsewhere refreshes the row inputs.
          <VariantRow key={`${variant.id}-${variant.quantity}`} productId={productId} variant={variant} />
        ))}
      </ul>
      <AddVariantForm productId={productId} />
    </section>
  )
}
