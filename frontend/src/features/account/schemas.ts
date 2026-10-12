import { z } from 'zod'

const required = (label: string, max = 100) =>
  z.string().trim().min(1, `${label} is required`).max(max, `${label} is too long`)

export const addressSchema = z.object({
  label: z.string().trim().max(50, 'Label is too long'),
  recipientName: required('Recipient name'),
  phone: z
    .string()
    .trim()
    .min(1, 'Phone number is required')
    .regex(/^\+?[0-9]{8,15}$/, 'Phone number must contain 8 to 15 digits'),
  province: required('Province'),
  city: required('City'),
  district: required('District'),
  postalCode: z
    .string()
    .trim()
    .min(1, 'Postal code is required')
    .regex(/^[0-9]{5}$/, 'Postal code must contain 5 digits'),
  street: required('Street address', 255),
  defaultAddress: z.boolean(),
})

export type AddressValues = z.infer<typeof addressSchema>
