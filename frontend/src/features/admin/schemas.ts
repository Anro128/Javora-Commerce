import { z } from 'zod'

const number = (label: string) => z.number({ error: `${label} must be a number` })

export const variantRowSchema = z.object({
  sku: z
    .string()
    .trim()
    .min(1, 'SKU is required')
    .regex(/^[A-Za-z0-9._-]{1,64}$/, 'Use letters, digits, dots, dashes, or underscores'),
  name: z.string().trim().min(1, 'Variant name is required').max(100, 'Variant name is too long'),
  price: number('Price').min(0, 'Price must not be negative'),
  stock: number('Stock').int('Stock must be a whole number').min(0, 'Stock must not be negative'),
})

export const productSchema = z.object({
  categoryId: z.number({ error: 'Select a category' }).int().positive('Select a category'),
  name: z.string().trim().min(1, 'Name is required').max(200, 'Name is too long'),
  description: z.string().max(10000, 'Description is too long'),
  basePrice: number('Base price').min(0, 'Base price must not be negative'),
  weightGram: number('Weight')
    .int('Weight must be a whole number')
    .positive('Weight must be greater than zero'),
  status: z.enum(['DRAFT', 'ACTIVE', 'ARCHIVED']),
  variants: z.array(variantRowSchema),
})

export const createProductSchema = productSchema.refine((values) => values.variants.length > 0, {
  message: 'Add at least one variant',
  path: ['variants'],
})

export const categorySchema = z.object({
  name: z.string().trim().min(1, 'Name is required').max(100, 'Name is too long'),
  parentId: z.string(),
  description: z.string().max(500, 'Description is too long'),
  sortOrder: number('Sort order')
    .int('Sort order must be a whole number')
    .min(0, 'Sort order must not be negative'),
  active: z.boolean(),
})

export type ProductFormValues = z.infer<typeof productSchema>
export type VariantRowValues = z.infer<typeof variantRowSchema>
export type CategoryFormValues = z.infer<typeof categorySchema>
