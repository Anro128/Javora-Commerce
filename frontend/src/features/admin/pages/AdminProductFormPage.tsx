import { ArrowLeftIcon, ExternalLinkIcon } from 'lucide-react'
import { Link, useNavigate, useParams } from 'react-router'
import { toast } from 'sonner'
import { PageSpinner } from '@/components/layout/PageSpinner'
import { Button } from '@/components/ui/button'
import { ImageManager } from '@/features/admin/components/ImageManager'
import { ProductForm } from '@/features/admin/components/ProductForm'
import { StatusBadge } from '@/features/admin/components/StatusBadge'
import { VariantManager } from '@/features/admin/components/VariantManager'
import { useAdminProduct, useCreateProduct, useUpdateProduct } from '@/features/admin/hooks'
import type { ProductFormValues } from '@/features/admin/schemas'
import type { AdminProduct } from '@/features/admin/types'

function toProductInput(values: ProductFormValues) {
  return {
    categoryId: values.categoryId,
    name: values.name.trim(),
    description: values.description.trim() || undefined,
    basePrice: values.basePrice,
    weightGram: values.weightGram,
    status: values.status,
  }
}

function CreateProduct() {
  const navigate = useNavigate()
  const create = useCreateProduct()

  const onSubmit = (values: ProductFormValues) => {
    create.mutate(
      {
        ...toProductInput(values),
        variants: values.variants.map((variant) => ({
          ...variant,
          sku: variant.sku.trim(),
          name: variant.name.trim(),
        })),
      },
      {
        onSuccess: (product) => {
          toast.success('Product created. You can now add images.')
          navigate(`/admin/products/${product.id}`, { replace: true })
        },
      },
    )
  }

  return <ProductForm mode="create" isSaving={create.isPending} error={create.error} onSubmit={onSubmit} />
}

function EditProduct({ product }: { product: AdminProduct }) {
  const update = useUpdateProduct(product.id)

  const onSubmit = (values: ProductFormValues) => {
    update.mutate(toProductInput(values), { onSuccess: () => toast.success('Product saved') })
  }

  return (
    <div className="flex flex-col gap-10">
      <div className="flex items-center gap-3">
        <StatusBadge status={product.status} />
        {product.status === 'ACTIVE' ? (
          <Link
            to={`/products/${product.slug}`}
            target="_blank"
            className="text-muted-foreground inline-flex items-center gap-1 text-sm underline-offset-4 hover:underline"
          >
            View in shop <ExternalLinkIcon className="size-3" />
          </Link>
        ) : null}
      </div>
      <ProductForm
        // Re-mounts after a save so the form always matches the stored product.
        key={product.updatedAt}
        mode="edit"
        product={product}
        isSaving={update.isPending}
        error={update.error}
        onSubmit={onSubmit}
      />
      <VariantManager productId={product.id} variants={product.variants} />
      <ImageManager productId={product.id} images={product.images} />
    </div>
  )
}

export function AdminProductFormPage() {
  const { id } = useParams()
  const isNew = id === undefined || id === 'new'
  const productId = isNew ? undefined : Number(id)
  const product = useAdminProduct(productId)

  return (
    <div className="mx-auto flex max-w-4xl flex-col gap-6">
      <div className="flex flex-col gap-2">
        <Button variant="ghost" size="sm" asChild className="self-start">
          <Link to="/admin/products">
            <ArrowLeftIcon /> Products
          </Link>
        </Button>
        <h1 className="text-2xl font-bold tracking-tight">
          {isNew ? 'New product' : (product.data?.name ?? 'Edit product')}
        </h1>
      </div>

      {isNew ? (
        <CreateProduct />
      ) : product.isPending ? (
        <PageSpinner />
      ) : product.isError ? (
        <p className="text-destructive">This product could not be loaded.</p>
      ) : (
        <EditProduct product={product.data} />
      )}
    </div>
  )
}
