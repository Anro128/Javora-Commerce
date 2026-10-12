import { zodResolver } from '@hookform/resolvers/zod'
import { Loader2Icon } from 'lucide-react'
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { FormError } from '@/components/form/FormError'
import { FormField } from '@/components/form/FormField'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import { addressSchema, type AddressValues } from '@/features/account/schemas'
import type { Address, AddressInput } from '@/features/account/types'
import { applyFieldErrors, errorMessage } from '@/lib/form'

interface AddressFormProps {
  address?: Address
  isSaving: boolean
  error: unknown
  onSubmit: (input: AddressInput) => void
  onCancel: () => void
}

export function AddressForm({ address, isSaving, error, onSubmit, onCancel }: AddressFormProps) {
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<AddressValues>({
    resolver: zodResolver(addressSchema),
    defaultValues: {
      label: address?.label ?? '',
      recipientName: address?.recipientName ?? '',
      phone: address?.phone ?? '',
      province: address?.province ?? '',
      city: address?.city ?? '',
      district: address?.district ?? '',
      postalCode: address?.postalCode ?? '',
      street: address?.street ?? '',
      defaultAddress: address?.defaultAddress ?? false,
    },
  })

  // Server side field errors arrive after the request, so they are applied when the error changes.
  useEffect(() => {
    applyFieldErrors(error, setError)
  }, [error, setError])

  const submit = (values: AddressValues) => {
    onSubmit({ ...values, label: values.label || undefined })
  }

  return (
    <form onSubmit={handleSubmit(submit)} className="flex flex-col gap-4" noValidate>
      <FormError message={errorMessage(error)} />
      <div className="grid gap-4 sm:grid-cols-2">
        <FormField
          label="Label (optional)"
          placeholder="Home, Office"
          error={errors.label?.message}
          {...register('label')}
        />
        <FormField
          label="Recipient name"
          error={errors.recipientName?.message}
          {...register('recipientName')}
        />
        <FormField label="Phone" type="tel" error={errors.phone?.message} {...register('phone')} />
        <FormField label="Province" error={errors.province?.message} {...register('province')} />
        <FormField label="City" error={errors.city?.message} {...register('city')} />
        <FormField label="District" error={errors.district?.message} {...register('district')} />
        <FormField
          label="Postal code"
          inputMode="numeric"
          error={errors.postalCode?.message}
          {...register('postalCode')}
        />
      </div>
      <FormField label="Street address" error={errors.street?.message} {...register('street')} />
      <div className="flex items-center gap-2">
        <input id="defaultAddress" type="checkbox" className="size-4" {...register('defaultAddress')} />
        <Label htmlFor="defaultAddress">Use as my default address</Label>
      </div>
      <div className="flex gap-2">
        <Button type="submit" disabled={isSaving}>
          {isSaving ? <Loader2Icon className="animate-spin" /> : null}
          Save address
        </Button>
        <Button type="button" variant="outline" onClick={onCancel}>
          Cancel
        </Button>
      </div>
    </form>
  )
}
