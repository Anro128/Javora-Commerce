import { zodResolver } from '@hookform/resolvers/zod'
import { Loader2Icon, PlusIcon, Trash2Icon } from 'lucide-react'
import { useEffect, useMemo } from 'react'
import { useFieldArray, useForm } from 'react-hook-form'
import { FormError } from '@/components/form/FormError'
import { FormField } from '@/components/form/FormField'
import { SelectField } from '@/components/form/NativeSelect'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { categoryLabel, flattenCategories } from '@/features/admin/categoryTree'
import { useAdminCategories } from '@/features/admin/hooks'
import {
  createProductSchema,
  productSchema,
  type ProductFormValues,
  type VariantRowValues,
} from '@/features/admin/schemas'
import type { AdminProduct } from '@/features/admin/types'
import { applyFieldErrors, errorMessage } from '@/lib/form'

interface ProductFormProps {
  mode: 'create' | 'edit'
  product?: AdminProduct
  isSaving: boolean
  error: unknown
  onSubmit: (values: ProductFormValues) => void
}

const EMPTY_VARIANT: VariantRowValues = { sku: '', name: '', price: 0, stock: 0 }

export function ProductForm({ mode, product, isSaving, error, onSubmit }: ProductFormProps) {
  const categories = useAdminCategories()

  const options = useMemo(
    () =>
      flattenCategories(categories.data ?? []).filter(
        (option) => option.category.active || option.category.id === product?.categoryId,
      ),
    [categories.data, product?.categoryId],
  )

  const {
    register,
    control,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<ProductFormValues>({
    resolver: zodResolver(mode === 'create' ? createProductSchema : productSchema),
    defaultValues: {
      categoryId: product?.categoryId,
      name: product?.name ?? '',
      description: product?.description ?? '',
      basePrice: product?.basePrice ?? 0,
      weightGram: product?.weightGram ?? 500,
      status: product?.status ?? 'DRAFT',
      variants: mode === 'create' ? [{ ...EMPTY_VARIANT, name: 'Default' }] : [],
    },
  })
  const variants = useFieldArray({ control, name: 'variants' })

  useEffect(() => {
    applyFieldErrors(error, setError)
  }, [error, setError])

  const statuses =
    mode === 'create' ? (['DRAFT', 'ACTIVE'] as const) : (['DRAFT', 'ACTIVE', 'ARCHIVED'] as const)

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-6" noValidate>
      <FormError message={errorMessage(error)} />

      <div className="grid gap-4 md:grid-cols-2">
        <div className="md:col-span-2">
          <FormField label="Name" error={errors.name?.message} {...register('name')} />
        </div>
        <SelectField
          label="Category"
          error={errors.categoryId?.message}
          {...register('categoryId', { setValueAs: (value) => (value === '' ? undefined : Number(value)) })}
        >
          <option value="">Select a category</option>
          {options.map((option) => (
            <option key={option.category.id} value={option.category.id}>
              {categoryLabel(option)}
            </option>
          ))}
        </SelectField>
        <SelectField label="Status" error={errors.status?.message} {...register('status')}>
          {statuses.map((status) => (
            <option key={status} value={status}>
              {status.charAt(0) + status.slice(1).toLowerCase()}
            </option>
          ))}
        </SelectField>
        <FormField
          label="Base price (IDR)"
          type="number"
          step="any"
          min={0}
          error={errors.basePrice?.message}
          {...register('basePrice', { valueAsNumber: true })}
        />
        <FormField
          label="Weight (grams)"
          type="number"
          min={1}
          error={errors.weightGram?.message}
          {...register('weightGram', { valueAsNumber: true })}
        />
        <div className="flex flex-col gap-2 md:col-span-2">
          <Label htmlFor="description">Description</Label>
          <Textarea id="description" rows={5} {...register('description')} />
          {errors.description?.message ? (
            <p className="text-destructive text-sm">{errors.description.message}</p>
          ) : null}
        </div>
      </div>

      {mode === 'create' ? (
        <fieldset className="flex flex-col gap-3">
          <legend className="mb-1 text-sm font-semibold">Variants</legend>
          {typeof errors.variants?.message === 'string' ? (
            <p className="text-destructive text-sm">{errors.variants.message}</p>
          ) : null}
          {variants.fields.map((field, index) => (
            <div
              key={field.id}
              className="grid items-start gap-3 rounded-lg border p-3 md:grid-cols-[1fr_1fr_140px_100px_auto]"
            >
              <FormField
                label="SKU"
                error={errors.variants?.[index]?.sku?.message}
                {...register(`variants.${index}.sku`)}
              />
              <FormField
                label="Name"
                error={errors.variants?.[index]?.name?.message}
                {...register(`variants.${index}.name`)}
              />
              <FormField
                label="Price"
                type="number"
                step="any"
                min={0}
                error={errors.variants?.[index]?.price?.message}
                {...register(`variants.${index}.price`, { valueAsNumber: true })}
              />
              <FormField
                label="Stock"
                type="number"
                min={0}
                error={errors.variants?.[index]?.stock?.message}
                {...register(`variants.${index}.stock`, { valueAsNumber: true })}
              />
              <Button
                type="button"
                variant="ghost"
                size="icon"
                className="md:mt-6"
                aria-label={`Remove variant ${index + 1}`}
                disabled={variants.fields.length === 1}
                onClick={() => variants.remove(index)}
              >
                <Trash2Icon />
              </Button>
            </div>
          ))}
          <Button
            type="button"
            variant="outline"
            size="sm"
            className="self-start"
            onClick={() => variants.append(EMPTY_VARIANT)}
          >
            <PlusIcon /> Add variant
          </Button>
        </fieldset>
      ) : null}

      <div>
        <Button type="submit" disabled={isSaving}>
          {isSaving ? <Loader2Icon className="animate-spin" /> : null}
          {mode === 'create' ? 'Create product' : 'Save changes'}
        </Button>
      </div>
    </form>
  )
}
