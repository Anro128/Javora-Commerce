import { SearchIcon } from 'lucide-react'
import { useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router'
import { toast } from 'sonner'
import { EmptyState } from '@/components/common/EmptyState'
import { Pagination } from '@/components/common/Pagination'
import { PageSpinner } from '@/components/layout/PageSpinner'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { useInventory, useSetStock } from '@/features/admin/hooks'
import type { InventoryRow } from '@/features/admin/types'
import { errorMessage } from '@/lib/form'
import { cn } from '@/lib/utils'

function StockRow({ row }: { row: InventoryRow }) {
  const setStock = useSetStock()
  const [quantity, setQuantity] = useState(String(row.quantity))
  const parsed = Number(quantity)
  const valid = Number.isInteger(parsed) && parsed >= row.reserved
  const changed = parsed !== row.quantity

  const save = () => {
    setStock.mutate(
      { variantId: row.variantId, quantity: parsed },
      {
        onSuccess: () => toast.success(`Stock for ${row.sku} updated`),
        onError: (error) => toast.error(errorMessage(error) ?? 'Could not update the stock'),
      },
    )
  }

  return (
    <tr className={cn(row.lowStock && 'bg-amber-50/60 dark:bg-amber-950/20')}>
      <td className="px-4 py-3">
        <Link to={`/admin/products/${row.productId}`} className="font-medium hover:underline">
          {row.productName}
        </Link>
        <p className="text-muted-foreground text-xs">{row.variantName}</p>
      </td>
      <td className="text-muted-foreground px-4 py-3">{row.sku}</td>
      <td className="px-4 py-3 text-right">{row.reserved}</td>
      <td className={cn('px-4 py-3 text-right font-medium', row.available === 0 && 'text-destructive')}>
        {row.available}
        {row.lowStock && row.available > 0 ? <span className="ml-1 text-xs text-amber-600">low</span> : null}
      </td>
      <td className="px-4 py-3">
        <div className="flex justify-end gap-2">
          <Input
            type="number"
            min={row.reserved}
            aria-label={`Stock for ${row.sku}`}
            value={quantity}
            onChange={(event) => setQuantity(event.target.value)}
            className="w-24"
          />
          <Button size="sm" disabled={!changed || !valid || setStock.isPending} onClick={save}>
            Save
          </Button>
        </div>
      </td>
    </tr>
  )
}

export function AdminInventoryPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const q = searchParams.get('q') ?? ''
  const lowStock = searchParams.get('lowStock') === 'true'
  const page = Math.max(0, Number(searchParams.get('page') ?? 0) || 0)

  const inventory = useInventory({ q: q || undefined, lowStock, page, size: 20 })
  const [searchText, setSearchText] = useState(q)

  const update = (changes: Record<string, string | undefined>) => {
    const next = new URLSearchParams(searchParams)
    Object.entries(changes).forEach(([key, value]) => (value ? next.set(key, value) : next.delete(key)))
    if (!('page' in changes)) {
      next.delete('page')
    }
    setSearchParams(next)
  }

  const submitSearch = (event: FormEvent) => {
    event.preventDefault()
    update({ q: searchText.trim() })
  }

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-bold tracking-tight">Inventory</h1>

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
        <form onSubmit={submitSearch} className="flex flex-1 gap-2" role="search">
          <Input
            type="search"
            aria-label="Search inventory"
            placeholder="Search by product, variant, or SKU"
            value={searchText}
            onChange={(event) => setSearchText(event.target.value)}
          />
          <Button type="submit" size="icon" aria-label="Search">
            <SearchIcon />
          </Button>
        </form>
        <label className="flex items-center gap-2 text-sm">
          <input
            type="checkbox"
            className="size-4"
            checked={lowStock}
            onChange={(event) => update({ lowStock: event.target.checked ? 'true' : undefined })}
          />
          Low stock only
        </label>
      </div>

      {inventory.isPending ? (
        <PageSpinner />
      ) : inventory.isError ? (
        <p className="text-destructive">{errorMessage(inventory.error)}</p>
      ) : inventory.data.content.length === 0 ? (
        <EmptyState
          title={lowStock ? 'Nothing is running low' : 'No inventory found'}
          description={
            lowStock ? 'All matching variants have healthy stock.' : 'Adjust the search to see more items.'
          }
        />
      ) : (
        <>
          <div className="overflow-x-auto rounded-lg border">
            <table className="w-full text-left text-sm">
              <thead className="bg-muted/50 text-muted-foreground">
                <tr>
                  <th className="px-4 py-3 font-medium">Product</th>
                  <th className="px-4 py-3 font-medium">SKU</th>
                  <th className="px-4 py-3 text-right font-medium">Reserved</th>
                  <th className="px-4 py-3 text-right font-medium">Available</th>
                  <th className="px-4 py-3 text-right font-medium">Total stock</th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {inventory.data.content.map((row) => (
                  // The key includes quantity so the input follows stock changes made elsewhere.
                  <StockRow key={`${row.variantId}-${row.quantity}`} row={row} />
                ))}
              </tbody>
            </table>
          </div>
          <Pagination
            page={inventory.data.page}
            totalPages={inventory.data.totalPages}
            onPageChange={(next) => update({ page: next === 0 ? undefined : String(next) })}
          />
        </>
      )}
    </div>
  )
}
