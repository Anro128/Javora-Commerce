import { PencilIcon, PlusIcon, StarIcon, Trash2Icon } from 'lucide-react'
import { useState } from 'react'
import { toast } from 'sonner'
import { PageSpinner } from '@/components/layout/PageSpinner'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { AddressForm } from '@/features/account/components/AddressForm'
import {
  useAddresses,
  useCreateAddress,
  useDeleteAddress,
  useSetDefaultAddress,
  useUpdateAddress,
} from '@/features/account/hooks'
import type { Address, AddressInput } from '@/features/account/types'
import { errorMessage } from '@/lib/form'

type Editing = { mode: 'new' } | { mode: 'edit'; address: Address } | null

export function AddressesPage() {
  const addresses = useAddresses()
  const createAddress = useCreateAddress()
  const updateAddress = useUpdateAddress()
  const setDefault = useSetDefaultAddress()
  const deleteAddress = useDeleteAddress()
  const [editing, setEditing] = useState<Editing>(null)

  const closeForm = () => setEditing(null)

  const handleSave = (input: AddressInput) => {
    if (editing?.mode === 'edit') {
      updateAddress.mutate(
        { id: editing.address.id, input },
        {
          onSuccess: () => {
            toast.success('Address updated')
            closeForm()
          },
        },
      )
      return
    }
    createAddress.mutate(input, {
      onSuccess: () => {
        toast.success('Address added')
        closeForm()
      },
    })
  }

  const handleDelete = (address: Address) => {
    if (!window.confirm(`Delete the address for ${address.recipientName}?`)) {
      return
    }
    deleteAddress.mutate(address.id, { onSuccess: () => toast.success('Address deleted') })
  }

  if (addresses.isPending) {
    return <PageSpinner />
  }
  if (addresses.isError) {
    return <p className="text-destructive">{errorMessage(addresses.error)}</p>
  }

  const list = addresses.data
  const saveError = editing?.mode === 'edit' ? updateAddress.error : createAddress.error
  const isSaving = createAddress.isPending || updateAddress.isPending

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold">Saved addresses</h2>
        {editing ? null : (
          <Button size="sm" onClick={() => setEditing({ mode: 'new' })}>
            <PlusIcon /> Add address
          </Button>
        )}
      </div>

      {editing ? (
        <Card>
          <CardHeader>
            <CardTitle>{editing.mode === 'edit' ? 'Edit address' : 'New address'}</CardTitle>
          </CardHeader>
          <CardContent>
            <AddressForm
              key={editing.mode === 'edit' ? editing.address.id : 'new'}
              address={editing.mode === 'edit' ? editing.address : undefined}
              isSaving={isSaving}
              error={saveError}
              onSubmit={handleSave}
              onCancel={closeForm}
            />
          </CardContent>
        </Card>
      ) : null}

      {list.length === 0 && !editing ? (
        <p className="text-muted-foreground">You have not saved any addresses yet.</p>
      ) : (
        <ul className="grid gap-4 md:grid-cols-2">
          {list.map((address) => (
            <li key={address.id}>
              <Card className="h-full">
                <CardHeader>
                  <CardTitle className="flex items-center gap-2 text-base">
                    {address.label || address.recipientName}
                    {address.defaultAddress ? (
                      <span className="bg-primary text-primary-foreground rounded-full px-2 py-0.5 text-xs font-medium">
                        Default
                      </span>
                    ) : null}
                  </CardTitle>
                </CardHeader>
                <CardContent className="flex flex-col gap-3 text-sm">
                  <address className="text-muted-foreground not-italic">
                    <p className="text-foreground font-medium">{address.recipientName}</p>
                    <p>{address.phone}</p>
                    <p>{address.street}</p>
                    <p>
                      {address.district}, {address.city}
                    </p>
                    <p>
                      {address.province} {address.postalCode}
                    </p>
                  </address>
                  <div className="flex flex-wrap gap-2">
                    <Button size="sm" variant="outline" onClick={() => setEditing({ mode: 'edit', address })}>
                      <PencilIcon /> Edit
                    </Button>
                    {address.defaultAddress ? null : (
                      <Button
                        size="sm"
                        variant="outline"
                        disabled={setDefault.isPending}
                        onClick={() => setDefault.mutate(address.id)}
                      >
                        <StarIcon /> Make default
                      </Button>
                    )}
                    <Button size="sm" variant="outline" onClick={() => handleDelete(address)}>
                      <Trash2Icon /> Delete
                    </Button>
                  </div>
                </CardContent>
              </Card>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
