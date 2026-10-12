import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import * as accountApi from '@/features/account/api'
import type { AddressInput, ProfileInput } from '@/features/account/types'
import { useAuthStore } from '@/features/auth/store'

const ADDRESSES_KEY = ['addresses'] as const

export function useUpdateProfile() {
  const setUser = useAuthStore((state) => state.setUser)
  return useMutation({
    mutationFn: (input: ProfileInput) => accountApi.updateProfile(input),
    onSuccess: (user) => setUser(user),
  })
}

export function useAddresses() {
  return useQuery({ queryKey: ADDRESSES_KEY, queryFn: accountApi.fetchAddresses })
}

function useAddressMutation<TVariables>(mutationFn: (variables: TVariables) => Promise<unknown>) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ADDRESSES_KEY }),
  })
}

export function useCreateAddress() {
  return useAddressMutation((input: AddressInput) => accountApi.createAddress(input))
}

export function useUpdateAddress() {
  return useAddressMutation(({ id, input }: { id: number; input: AddressInput }) =>
    accountApi.updateAddress(id, input),
  )
}

export function useSetDefaultAddress() {
  return useAddressMutation((id: number) => accountApi.setDefaultAddress(id))
}

export function useDeleteAddress() {
  return useAddressMutation((id: number) => accountApi.deleteAddress(id))
}
