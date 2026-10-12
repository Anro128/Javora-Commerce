import { Navigate, useParams } from 'react-router'

// Friendly /categories/:slug links resolve to the filtered product list.
export function CategoryRedirectPage() {
  const { slug } = useParams()
  return <Navigate to={slug ? `/products?category=${encodeURIComponent(slug)}` : '/products'} replace />
}
