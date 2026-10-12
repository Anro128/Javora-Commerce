import { Link } from 'react-router'
import { ProductImage } from '@/features/catalog/components/ProductImage'
import type { ProductSummary } from '@/features/catalog/types'
import { formatRupiah } from '@/lib/format'

export function ProductCard({ product }: { product: ProductSummary }) {
  return (
    <Link
      to={`/products/${product.slug}`}
      className="group focus-visible:ring-ring/50 flex flex-col gap-3 rounded-lg outline-none focus-visible:ring-[3px]"
    >
      <div className="bg-muted relative aspect-square overflow-hidden rounded-lg">
        <ProductImage
          src={product.imageUrl}
          alt={product.name}
          className="transition-transform duration-300 group-hover:scale-105"
        />
        {product.inStock ? null : (
          <span className="bg-background/90 absolute top-2 left-2 rounded-full px-2 py-1 text-xs font-medium">
            Out of stock
          </span>
        )}
      </div>
      <div className="flex flex-col gap-1">
        {product.categoryName ? (
          <span className="text-muted-foreground text-xs">{product.categoryName}</span>
        ) : null}
        <h3 className="line-clamp-2 text-sm leading-snug font-medium">{product.name}</h3>
        <p className="font-semibold">{formatRupiah(product.price)}</p>
      </div>
    </Link>
  )
}

export function ProductGridSkeleton({ count = 8 }: { count?: number }) {
  return (
    <div className="grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-4" aria-hidden>
      {Array.from({ length: count }, (_, index) => (
        <div key={index} className="flex flex-col gap-3">
          <div className="bg-muted aspect-square animate-pulse rounded-lg" />
          <div className="bg-muted h-4 w-3/4 animate-pulse rounded" />
          <div className="bg-muted h-4 w-1/3 animate-pulse rounded" />
        </div>
      ))}
    </div>
  )
}
