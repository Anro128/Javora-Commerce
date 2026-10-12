import { ArchiveIcon, PencilIcon, PlusIcon, SearchIcon } from 'lucide-react'
import { useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router'
import { toast } from 'sonner'
import { EmptyState } from '@/components/common/EmptyState'
import { Pagination } from '@/components/common/Pagination'
import { NativeSelect } from '@/components/form/NativeSelect'
import { PageSpinner } from '@/components/layout/PageSpinner'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { StatusBadge } from '@/features/admin/components/StatusBadge'
import { useAdminProducts, useChangeProductStatus } from '@/features/admin/hooks'
import type { ProductStatus } from '@/features/admin/types'
import { ProductImage } from '@/features/catalog/components/ProductImage'
import { errorMessage } from '@/lib/form'
import { formatRupiah } from '@/lib/format'

const STATUSES: ProductStatus[] = ['ACTIVE', 'DRAFT', 'ARCHIVED']

export function AdminProductsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const q = searchParams.get('q') ?? ''
  const status = (searchParams.get('status') as ProductStatus | null) ?? undefined
  const page = Math.max(0, Number(searchParams.get('page') ?? 0) || 0)

  const products = useAdminProducts({ q: q || undefined, status, page, size: 20 })
  const changeStatus = useChangeProductStatus()
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

  const archive = (id: number, name: string) => {
    if (!window.confirm(`Archive "${name}"? It will disappear from the shop.`)) {
      return
    }
    changeStatus.mutate(
      { id, status: 'ARCHIVED' },
      {
        onSuccess: () => toast.success('Product archived'),
        onError: (error) => toast.error(errorMessage(error) ?? 'Could not archive the product'),
      },
    )
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold tracking-tight">Products</h1>
        <Button asChild>
          <Link to="/admin/products/new">
            <PlusIcon /> New product
          </Link>
        </Button>
      </div>

      <div className="flex flex-col gap-3 sm:flex-row">
        <form onSubmit={submitSearch} className="flex flex-1 gap-2" role="search">
          <Input
            type="search"
            aria-label="Search products"
            placeholder="Search by name"
            value={searchText}
            onChange={(event) => setSearchText(event.target.value)}
          />
          <Button type="submit" size="icon" aria-label="Search">
            <SearchIcon />
          </Button>
        </form>
        <NativeSelect
          aria-label="Filter by status"
          value={status ?? ''}
          onChange={(event) => update({ status: event.target.value })}
          className="sm:w-48"
        >
          <option value="">All statuses</option>
          {STATUSES.map((item) => (
            <option key={item} value={item}>
              {item.charAt(0) + item.slice(1).toLowerCase()}
            </option>
          ))}
        </NativeSelect>
      </div>

      {products.isPending ? (
        <PageSpinner />
      ) : products.isError ? (
        <p className="text-destructive">{errorMessage(products.error)}</p>
      ) : products.data.content.length === 0 ? (
        <EmptyState
          title="No products found"
          description="Create your first product or adjust the filters."
          action={
            <Button asChild>
              <Link to="/admin/products/new">Create product</Link>
            </Button>
          }
        />
      ) : (
        <>
          <div className="overflow-x-auto rounded-lg border">
            <table className="w-full text-left text-sm">
              <thead className="bg-muted/50 text-muted-foreground">
                <tr>
                  <th className="px-4 py-3 font-medium">Product</th>
                  <th className="px-4 py-3 font-medium">Category</th>
                  <th className="px-4 py-3 text-right font-medium">Price</th>
                  <th className="px-4 py-3 text-right font-medium">Stock</th>
                  <th className="px-4 py-3 font-medium">Status</th>
                  <th className="px-4 py-3 text-right font-medium">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {products.data.content.map((product) => (
                  <tr key={product.id}>
                    <td className="px-4 py-3">
                      <Link
                        to={`/admin/products/${product.id}`}
                        className="flex items-center gap-3 hover:underline"
                      >
                        <span className="bg-muted size-10 shrink-0 overflow-hidden rounded-md">
                          <ProductImage src={product.imageUrl} alt={product.name} className="text-base" />
                        </span>
                        <span className="font-medium">{product.name}</span>
                      </Link>
                    </td>
                    <td className="text-muted-foreground px-4 py-3">{product.categoryName ?? '-'}</td>
                    <td className="px-4 py-3 text-right">{formatRupiah(product.basePrice)}</td>
                    <td className="px-4 py-3 text-right">
                      {product.availableStock}
                      <span className="text-muted-foreground"> / {product.variantCount} var.</span>
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge status={product.status} />
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex justify-end gap-1">
                        <Button variant="ghost" size="icon" asChild>
                          <Link to={`/admin/products/${product.id}`} aria-label={`Edit ${product.name}`}>
                            <PencilIcon />
                          </Link>
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          aria-label={`Archive ${product.name}`}
                          disabled={product.status === 'ARCHIVED' || changeStatus.isPending}
                          onClick={() => archive(product.id, product.name)}
                        >
                          <ArchiveIcon />
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination
            page={products.data.page}
            totalPages={products.data.totalPages}
            onPageChange={(next) => update({ page: next === 0 ? undefined : String(next) })}
          />
        </>
      )}
    </div>
  )
}
