import { apiClient } from '@/api/client'
import type { ApiResponse } from '@/api/types'
import type { Address, AddressInput, ProfileInput } from '@/features/account/types'
import type { User } from '@/features/auth/types'

export async function updateProfile(input: ProfileInput): Promise<User> {
  const { data } = await apiClient.put<ApiResponse<User>>('/users/me', input)
  return data.data
}

export async function fetchAddresses(): Promise<Address[]> {
  const { data } = await apiClient.get<ApiResponse<Address[]>>('/users/me/addresses')
  return data.data
}

export async function createAddress(input: AddressInput): Promise<Address> {
  const { data } = await apiClient.post<ApiResponse<Address>>('/users/me/addresses', input)
  return data.data
}

export async function updateAddress(id: number, input: AddressInput): Promise<Address> {
  const { data } = await apiClient.put<ApiResponse<Address>>(`/users/me/addresses/${id}`, input)
  return data.data
}

export async function setDefaultAddress(id: number): Promise<Address> {
  const { data } = await apiClient.put<ApiResponse<Address>>(`/users/me/addresses/${id}/default`)
  return data.data
}

export async function deleteAddress(id: number): Promise<void> {
  await apiClient.delete(`/users/me/addresses/${id}`)
}
