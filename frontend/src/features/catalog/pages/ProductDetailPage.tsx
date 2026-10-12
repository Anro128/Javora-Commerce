import { Link, useNavigate, useParams } from 'react-router'
import { useState } from 'react'
import { PageSpinner } from '@/components/layout/PageSpinner'
import { Button } from '@/components/ui/button'
import { ProductImage } from '@/features/catalog/components/ProductImage'
import { useProduct } from '@/features/catalog/hooks'
import type { ProductDetail } from '@/features/catalog/types'
import { ApiError } from '@/api/client'
import { MAX_LINE_QUANTITY } from '@/features/cart/guestCart'
import { useAddToCart } from '@/features/cart/hooks'
import { errorMessage } from '@/lib/form'
import { toast } from 'sonner'
import { formatRupiah } from '@/lib/format'
import { cn } from '@/lib/utils'

const LOW_STOCK_THRESHOLD = 5

function ProductDetailView({ product }: { product: ProductDetail }) {
  const navigate = useNavigate()
  const addToCart = useAddToCart()
  const [quantity, setQuantity] = useState(1)
  const [imageIndex, setImageIndex] = useState(0)
  const firstAvailable = product.variants.find((variant) => variant.inStock) ?? product.variants[0]
  const [variantId, setVariantId] = useState<number | undefined>(firstAvailable?.id)

  const variant = product.variants.find((candidate) => candidate.id === variantId)
  const price = variant?.price ?? product.price
  const image = product.images[imageIndex] ?? product.images[0]
  const maxQuantity = Math.min(MAX_LINE_QUANTITY, variant?.available ?? 1)
  const safeQuantity = Math.min(quantity, Math.max(maxQuantity, 1))

  const handleAdd = () => {
    if (!variant) {
      return
    }
    addToCart.mutate(
      { variantId: variant.id, quantity: safeQuantity },
      {
        onSuccess: () =>
          toast.success(`${product.name} added to cart`, {
            action: { label: 'View cart', onClick: () => navigate('/cart') },
          }),
        onError: (error) => toast.error(errorMessage(error) ?? 'Could not add to cart'),
      },
    )
  }

  return (
    <div className="flex flex-col gap-6">
      <nav aria-label="Breadcrumb" className="text-muted-foreground text-sm">
        <Link to="/products" className="underline-offset-4 hover:underline">
          Products
        </Link>
        <span aria-hidden> / </span>
        <Link
          to={`/products?category=${product.categorySlug}`}
          className="underline-offset-4 hover:underline"
        >
          {product.categoryName}
        </Link>
      </nav>

      <div className="grid gap-8 md:grid-cols-2">
        <div className="flex flex-col gap-3">
          <div className="bg-muted aspect-square overflow-hidden rounded-xl">
            <ProductImage src={image?.url} alt={product.name} />
          </div>
          {product.images.length > 1 ? (
            <ul className="flex flex-wrap gap-2" aria-label="Product images">
              {product.images.map((item, index) => (
                <li key={item.id}>
                  <button
                    type="button"
                    onClick={() => setImageIndex(index)}
                    aria-label={`Show image ${index + 1}`}
                    aria-current={index === imageIndex}
                    className={cn(
                      'size-16 overflow-hidden rounded-md border-2',
                      index === imageIndex ? 'border-primary' : 'border-transparent',
                    )}
                  >
                    <img src={item.url} alt="" className="size-full object-cover" />
                  </button>
                </li>
              ))}
            </ul>
          ) : null}
        </div>

        <div className="flex flex-col gap-6">
          <div className="flex flex-col gap-2">
            <h1 className="text-3xl font-bold tracking-tight">{product.name}</h1>
            <p className="text-2xl font-semibold">{formatRupiah(price)}</p>
          </div>

          {product.variants.length > 0 ? (
            <fieldset className="flex flex-col gap-3">
              <legend className="mb-2 text-sm font-medium">Option</legend>
              <div className="flex flex-wrap gap-2">
                {product.variants.map((item) => (
                  <Button
                    key={item.id}
                    type="button"
                    variant={item.id === variantId ? 'default' : 'outline'}
                    disabled={!item.inStock}
                    aria-pressed={item.id === variantId}
                    onClick={() => setVariantId(item.id)}
                  >
                    {item.name}
                  </Button>
                ))}
              </div>
              {variant ? (
                <p
                  className={cn(
                    'text-sm',
                    variant.inStock ? 'text-muted-foreground' : 'text-destructive',
                    variant.inStock && variant.available <= LOW_STOCK_THRESHOLD && 'text-amber-600',
                  )}
                  aria-live="polite"
                >
                  {!variant.inStock
                    ? 'Out of stock'
                    : variant.available <= LOW_STOCK_THRESHOLD
                      ? `Only ${variant.available} left`
                      : 'In stock'}
                </p>
              ) : null}
            </fieldset>
          ) : (
            <p className="text-destructive text-sm">This product is currently unavailable.</p>
          )}

          {variant ? (
            <div className="flex flex-wrap items-center gap-3">
              <div className="flex items-center gap-1" role="group" aria-label="Quantity">
                <Button
                  type="button"
                  variant="outline"
                  size="icon"
                  aria-label="Decrease quantity"
                  disabled={safeQuantity <= 1}
                  onClick={() => setQuantity(safeQuantity - 1)}
                >
                  −
                </Button>
                <span className="w-10 text-center font-medium" aria-live="polite">
                  {safeQuantity}
                </span>
                <Button
                  type="button"
                  variant="outline"
                  size="icon"
                  aria-label="Increase quantity"
                  disabled={safeQuantity >= maxQuantity}
                  onClick={() => setQuantity(safeQuantity + 1)}
                >
                  +
                </Button>
              </div>
              <Button size="lg" disabled={!variant.inStock || addToCart.isPending} onClick={handleAdd}>
                {variant.inStock ? 'Add to cart' : 'Out of stock'}
              </Button>
            </div>
          ) : null}

          {product.description ? (
            <section aria-labelledby="product-description" className="flex flex-col gap-2">
              <h2 id="product-description" className="font-semibold">
                Description
              </h2>
              <p className="text-muted-foreground text-sm leading-relaxed whitespace-pre-line">
                {product.description}
              </p>
            </section>
          ) : null}

          <dl className="text-muted-foreground grid grid-cols-[auto_1fr] gap-x-4 gap-y-1 text-sm">
            <dt>Weight</dt>
            <dd>{product.weightGram} g</dd>
            {variant ? (
              <>
                <dt>SKU</dt>
                <dd>{variant.sku}</dd>
              </>
            ) : null}
          </dl>
        </div>
      </div>
    </div>
  )
}

export function ProductDetailPage() {
  const { slug } = useParams()
  const product = useProduct(slug)

  if (product.isPending) {
    return <PageSpinner />
  }

  if (product.isError) {
    const notFound = product.error instanceof ApiError && product.error.status === 404
    return (
      <section className="flex flex-col items-center gap-4 py-16 text-center">
        <h1 className="text-2xl font-semibold">{notFound ? 'Product not found' : 'Something went wrong'}</h1>
        <p className="text-muted-foreground">
          {notFound
            ? 'This product is no longer available.'
            : 'We could not load this product. Please try again.'}
        </p>
        <Button variant="outline" asChild>
          <Link to="/products">Browse products</Link>
        </Button>
      </section>
    )
  }

  // The key resets the selected option and image when navigating between products.
  return <ProductDetailView key={product.data.id} product={product.data} />
}
