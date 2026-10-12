export interface Address {
  id: number
  label: string | null
  recipientName: string
  phone: string
  province: string
  city: string
  district: string
  postalCode: string
  street: string
  defaultAddress: boolean
}

export interface AddressInput {
  label?: string
  recipientName: string
  phone: string
  province: string
  city: string
  district: string
  postalCode: string
  street: string
  defaultAddress: boolean
}

export interface ProfileInput {
  fullName: string
  phone?: string
}
