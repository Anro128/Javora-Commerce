import { SearchIcon } from 'lucide-react'
import { useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router'
import { EmptyState } from '@/components/common/EmptyState'
import { Pagination } from '@/components/common/Pagination'
import { NativeSelect } from '@/components/form/NativeSelect'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { ProductCard, ProductGridSkeleton } from '@/features/catalog/components/ProductCard'
import { useCategories, useProducts } from '@/features/catalog/hooks'
import type { CategoryNode, ProductQuery, ProductSort } from '@/features/catalog/types'
import { cn } from '@/lib/utils'

const PAGE_SIZE = 12

const SORT_OPTIONS: { value: ProductSort; label: string }[] = [
  { value: 'newest', label: 'Newest' },
  { value: 'price_asc', label: 'Price: low to high' },
  { value: 'price_desc', label: 'Price: high to low' },
  { value: 'name', label: 'Name: A to Z' },
]

function toNumber(value: string | null): number | undefined {
  if (value === null || value.trim() === '') {
    return undefined
  }
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed >= 0 ? parsed : undefined
}

function CategoryList({
  nodes,
  active,
  onSelect,
  depth = 0,
}: {
  nodes: CategoryNode[]
  active?: string
  onSelect: (slug: string) => void
  depth?: number
}) {
  return (
    <ul className="flex flex-col gap-1">
      {nodes.map((node) => (
        <li key={node.id}>
          <button
            type="button"
            onClick={() => onSelect(node.slug)}
            style={{ paddingLeft: `${depth * 12 + 8}px` }}
            className={cn(
              'hover:bg-muted w-full rounded-md py-1.5 pr-2 text-left text-sm transition-colors',
              active === node.slug && 'bg-muted font-semibold',
            )}
          >
            {node.name}
          </button>
          {node.children.length > 0 ? (
            <CategoryList nodes={node.children} active={active} onSelect={onSelect} depth={depth + 1} />
          ) : null}
        </li>
      ))}
    </ul>
  )
}

export function ProductListPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const categories = useCategories()

  const query: ProductQuery = {
    q: searchParams.get('q') ?? undefined,
    category: searchParams.get('category') ?? undefined,
    minPrice: toNumber(searchParams.get('minPrice')),
    maxPrice: toNumber(searchParams.get('maxPrice')),
    sort: (searchParams.get('sort') as ProductSort | null) ?? 'newest',
    page: Math.max(0, Number(searchParams.get('page') ?? 0) || 0),
    size: PAGE_SIZE,
  }
  const products = useProducts(query)

  const [searchText, setSearchText] = useState(query.q ?? '')
  const [minPrice, setMinPrice] = useState(query.minPrice?.toString() ?? '')
  const [maxPrice, setMaxPrice] = useState(query.maxPrice?.toString() ?? '')

  // Filters live in the URL so results can be shared and the back button works.
  const update = (changes: Record<string, string | undefined>, resetPage = true) => {
    const next = new URLSearchParams(searchParams)
    Object.entries(changes).forEach(([key, value]) => {
      if (value === undefined || value === '') {
        next.delete(key)
      } else {
        next.set(key, value)
      }
    })
    if (resetPage) {
      next.delete('page')
    }
    setSearchParams(next)
  }

  const submitSearch = (event: FormEvent) => {
    event.preventDefault()
    update({ q: searchText.trim() })
  }

  const applyPrice = (event: FormEvent) => {
    event.preventDefault()
    update({ minPrice: minPrice.trim(), maxPrice: maxPrice.trim() })
  }

  const clearFilters = () => {
    setSearchText('')
    setMinPrice('')
    setMaxPrice('')
    setSearchParams(new URLSearchParams())
  }

  const hasFilters = !!(
    query.q ||
    query.category ||
    query.minPrice !== undefined ||
    query.maxPrice !== undefined
  )

  return (
    <div className="grid gap-8 md:grid-cols-[240px_1fr]">
      <aside className="flex flex-col gap-6">
        <section aria-labelledby="filter-categories">
          <h2 id="filter-categories" className="mb-2 text-sm font-semibold">
            Categories
          </h2>
          <button
            type="button"
            onClick={() => update({ category: undefined })}
            className={cn(
              'hover:bg-muted mb-1 w-full rounded-md px-2 py-1.5 text-left text-sm transition-colors',
              !query.category && 'bg-muted font-semibold',
            )}
          >
            All products
          </button>
          <CategoryList
            nodes={categories.data ?? []}
            active={query.category}
            onSelect={(slug) => update({ category: slug })}
          />
        </section>

        <form onSubmit={applyPrice} className="flex flex-col gap-3" aria-label="Price filter">
          <h2 className="text-sm font-semibold">Price (IDR)</h2>
          <div className="flex items-center gap-2">
            <div className="flex-1">
              <Label htmlFor="minPrice" className="sr-only">
                Minimum price
              </Label>
              <Input
                id="minPrice"
                type="number"
                min={0}
                inputMode="numeric"
                placeholder="Min"
                value={minPrice}
                onChange={(event) => setMinPrice(event.target.value)}
              />
            </div>
            <span aria-hidden>-</span>
            <div className="flex-1">
              <Label htmlFor="maxPrice" className="sr-only">
                Maximum price
              </Label>
              <Input
                id="maxPrice"
                type="number"
                min={0}
                inputMode="numeric"
                placeholder="Max"
                value={maxPrice}
                onChange={(event) => setMaxPrice(event.target.value)}
              />
            </div>
          </div>
          <Button type="submit" variant="outline" size="sm">
            Apply
          </Button>
        </form>
      </aside>

      <section className="flex min-w-0 flex-col gap-6" aria-labelledby="products-heading">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <h1 id="products-heading" className="text-2xl font-bold tracking-tight">
            Products
          </h1>
          <div className="flex flex-col gap-2 sm:flex-row">
            <form onSubmit={submitSearch} className="flex gap-2" role="search">
              <Label htmlFor="product-search" className="sr-only">
                Search products
              </Label>
              <Input
                id="product-search"
                type="search"
                placeholder="Search products"
                value={searchText}
                onChange={(event) => setSearchText(event.target.value)}
                className="sm:w-56"
              />
              <Button type="submit" size="icon" aria-label="Search">
                <SearchIcon />
              </Button>
            </form>
            <div>
              <Label htmlFor="product-sort" className="sr-only">
                Sort by
              </Label>
              <NativeSelect
                id="product-sort"
                value={query.sort}
                onChange={(event) =>
                  update({ sort: event.target.value === 'newest' ? undefined : event.target.value })
                }
                className="sm:w-48"
              >
                {SORT_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </NativeSelect>
            </div>
          </div>
        </div>

        {products.isPending ? (
          <ProductGridSkeleton count={PAGE_SIZE} />
        ) : products.isError ? (
          <EmptyState
            title="Something went wrong"
            description="We could not load the products. Please try again."
          />
        ) : products.data.content.length === 0 ? (
          <EmptyState
            title="No products found"
            description="Try a different search or remove some filters."
            action={
              hasFilters ? (
                <Button variant="outline" onClick={clearFilters}>
                  Clear filters
                </Button>
              ) : undefined
            }
          />
        ) : (
          <>
            <p className="text-muted-foreground text-sm" aria-live="polite">
              {products.data.totalElements} {products.data.totalElements === 1 ? 'product' : 'products'}
              {hasFilters ? (
                <>
                  {' · '}
                  <Link to="/products" className="underline-offset-4 hover:underline" onClick={clearFilters}>
                    Clear filters
                  </Link>
                </>
              ) : null}
            </p>
            <div
              className={cn(
                'grid grid-cols-2 gap-4 md:grid-cols-3 xl:grid-cols-4',
                products.isPlaceholderData && 'opacity-60 transition-opacity',
              )}
            >
              {products.data.content.map((product) => (
                <ProductCard key={product.id} product={product} />
              ))}
            </div>
            <Pagination
              page={products.data.page}
              totalPages={products.data.totalPages}
              onPageChange={(page) => {
                update({ page: page === 0 ? undefined : String(page) }, false)
                window.scrollTo({ top: 0, behavior: 'smooth' })
              }}
            />
          </>
        )}
      </section>
    </div>
  )
}
