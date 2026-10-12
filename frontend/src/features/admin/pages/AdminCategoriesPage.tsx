import { zodResolver } from '@hookform/resolvers/zod'
import { Loader2Icon, PencilIcon, PlusIcon, Trash2Icon } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { useForm } from 'react-hook-form'
import { toast } from 'sonner'
import { EmptyState } from '@/components/common/EmptyState'
import { FormError } from '@/components/form/FormError'
import { FormField } from '@/components/form/FormField'
import { SelectField } from '@/components/form/NativeSelect'
import { PageSpinner } from '@/components/layout/PageSpinner'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Label } from '@/components/ui/label'
import { categoryLabel, descendantIds, flattenCategories } from '@/features/admin/categoryTree'
import { useAdminCategories, useDeleteCategory, useSaveCategory } from '@/features/admin/hooks'
import { categorySchema, type CategoryFormValues } from '@/features/admin/schemas'
import type { AdminCategory } from '@/features/admin/types'
import { applyFieldErrors, errorMessage } from '@/lib/form'
import { cn } from '@/lib/utils'

function CategoryForm({
  editing,
  categories,
  onDone,
}: {
  editing: AdminCategory | null
  categories: AdminCategory[]
  onDone: () => void
}) {
  const save = useSaveCategory()

  const parentOptions = useMemo(() => {
    const blocked = editing ? descendantIds(categories, editing.id) : new Set<number>()
    return flattenCategories(categories).filter((option) => !blocked.has(option.category.id))
  }, [categories, editing])

  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<CategoryFormValues>({
    resolver: zodResolver(categorySchema),
    defaultValues: { name: '', parentId: '', description: '', sortOrder: 0, active: true },
  })

  useEffect(() => {
    reset({
      name: editing?.name ?? '',
      parentId: editing?.parentId ? String(editing.parentId) : '',
      description: editing?.description ?? '',
      sortOrder: editing?.sortOrder ?? 0,
      active: editing?.active ?? true,
    })
    save.reset()
    // The form is reset only when a different category is picked for editing.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [editing, reset])

  useEffect(() => {
    applyFieldErrors(save.error, setError)
  }, [save.error, setError])

  const onSubmit = (values: CategoryFormValues) => {
    save.mutate(
      {
        id: editing?.id,
        input: {
          name: values.name.trim(),
          parentId: values.parentId ? Number(values.parentId) : null,
          description: values.description.trim() || undefined,
          sortOrder: values.sortOrder,
          active: values.active,
        },
      },
      {
        onSuccess: () => {
          toast.success(editing ? 'Category updated' : 'Category created')
          onDone()
        },
      },
    )
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>{editing ? `Edit ${editing.name}` : 'New category'}</CardTitle>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4" noValidate>
          <FormError message={errorMessage(save.error)} />
          <FormField label="Name" error={errors.name?.message} {...register('name')} />
          <SelectField label="Parent category" error={errors.parentId?.message} {...register('parentId')}>
            <option value="">None (top level)</option>
            {parentOptions.map((option) => (
              <option key={option.category.id} value={option.category.id}>
                {categoryLabel(option)}
              </option>
            ))}
          </SelectField>
          <FormField label="Description" error={errors.description?.message} {...register('description')} />
          <FormField
            label="Sort order"
            type="number"
            min={0}
            hint="Lower numbers appear first."
            error={errors.sortOrder?.message}
            {...register('sortOrder', { valueAsNumber: true })}
          />
          <div className="flex items-center gap-2">
            <input id="category-active" type="checkbox" className="size-4" {...register('active')} />
            <Label htmlFor="category-active">Visible in the shop</Label>
          </div>
          <div className="flex gap-2">
            <Button type="submit" disabled={save.isPending}>
              {save.isPending ? <Loader2Icon className="animate-spin" /> : null}
              {editing ? 'Save category' : 'Create category'}
            </Button>
            {editing ? (
              <Button type="button" variant="outline" onClick={onDone}>
                Cancel
              </Button>
            ) : null}
          </div>
        </form>
      </CardContent>
    </Card>
  )
}

export function AdminCategoriesPage() {
  const categories = useAdminCategories()
  const deleteCategory = useDeleteCategory()
  const [editing, setEditing] = useState<AdminCategory | null>(null)

  const remove = (category: AdminCategory) => {
    if (!window.confirm(`Delete "${category.name}"?`)) {
      return
    }
    deleteCategory.mutate(category.id, {
      onSuccess: () => {
        toast.success('Category deleted')
        setEditing((current) => (current?.id === category.id ? null : current))
      },
      onError: (error) => toast.error(errorMessage(error) ?? 'Could not delete the category'),
    })
  }

  if (categories.isPending) {
    return <PageSpinner />
  }
  if (categories.isError) {
    return <p className="text-destructive">{errorMessage(categories.error)}</p>
  }

  const options = flattenCategories(categories.data)

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold tracking-tight">Categories</h1>
        {editing ? (
          <Button variant="outline" onClick={() => setEditing(null)}>
            <PlusIcon /> New category
          </Button>
        ) : null}
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_380px]">
        {options.length === 0 ? (
          <EmptyState title="No categories yet" description="Create a category to start adding products." />
        ) : (
          <ul className="divide-y rounded-lg border">
            {options.map((option) => (
              <li
                key={option.category.id}
                className={cn(
                  'flex items-center justify-between gap-3 px-4 py-3',
                  editing?.id === option.category.id && 'bg-muted/60',
                )}
              >
                <div className="min-w-0" style={{ paddingLeft: `${option.depth * 20}px` }}>
                  <p className="truncate font-medium">{option.category.name}</p>
                  <p className="text-muted-foreground truncate text-xs">
                    /{option.category.slug}
                    {option.category.active ? '' : ' · hidden'}
                  </p>
                </div>
                <div className="flex shrink-0 gap-1">
                  <Button
                    variant="ghost"
                    size="icon"
                    aria-label={`Edit ${option.category.name}`}
                    onClick={() => setEditing(option.category)}
                  >
                    <PencilIcon />
                  </Button>
                  <Button
                    variant="ghost"
                    size="icon"
                    aria-label={`Delete ${option.category.name}`}
                    disabled={deleteCategory.isPending}
                    onClick={() => remove(option.category)}
                  >
                    <Trash2Icon />
                  </Button>
                </div>
              </li>
            ))}
          </ul>
        )}

        <CategoryForm editing={editing} categories={categories.data} onDone={() => setEditing(null)} />
      </div>
    </div>
  )
}
