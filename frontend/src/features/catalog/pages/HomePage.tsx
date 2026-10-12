import { Link } from 'react-router'
import { Button } from '@/components/ui/button'
import { ProductCard, ProductGridSkeleton } from '@/features/catalog/components/ProductCard'
import { useCategories, useProducts } from '@/features/catalog/hooks'

const LATEST_QUERY = { size: 8, sort: 'newest' } as const

export function HomePage() {
  const categories = useCategories()
  const latest = useProducts(LATEST_QUERY)

  return (
    <div className="flex flex-col gap-12">
      <section className="bg-muted/50 flex flex-col items-center gap-6 rounded-2xl px-6 py-16 text-center">
        <h1 className="text-4xl font-bold tracking-tight md:text-5xl">Welcome to Javora</h1>
        <p className="text-muted-foreground max-w-xl text-lg">
          Discover quality products at the best prices, delivered right to your door.
        </p>
        <Button size="lg" asChild>
          <Link to="/products">Start shopping</Link>
        </Button>
      </section>

      {categories.data && categories.data.length > 0 ? (
        <section className="flex flex-col gap-4" aria-labelledby="home-categories">
          <h2 id="home-categories" className="text-2xl font-semibold tracking-tight">
            Shop by category
          </h2>
          <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
            {categories.data.map((category) => (
              <Link
                key={category.id}
                to={`/products?category=${category.slug}`}
                className="hover:bg-muted flex min-h-20 items-center justify-center rounded-lg border px-4 py-6 text-center font-medium transition-colors"
              >
                {category.name}
              </Link>
            ))}
          </div>
        </section>
      ) : null}

      <section className="flex flex-col gap-4" aria-labelledby="home-latest">
        <div className="flex items-end justify-between">
          <h2 id="home-latest" className="text-2xl font-semibold tracking-tight">
            New arrivals
          </h2>
          <Link to="/products" className="text-muted-foreground text-sm underline-offset-4 hover:underline">
            View all
          </Link>
        </div>
        {latest.isPending ? (
          <ProductGridSkeleton />
        ) : latest.isError ? (
          <p className="text-muted-foreground">Products could not be loaded right now.</p>
        ) : latest.data.content.length === 0 ? (
          <p className="text-muted-foreground">Products will appear here soon.</p>
        ) : (
          <div className="grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-4">
            {latest.data.content.map((product) => (
              <ProductCard key={product.id} product={product} />
            ))}
          </div>
        )}
      </section>
    </div>
  )
}
