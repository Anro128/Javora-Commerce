import { formatRupiah } from '@/lib/format'

describe('formatRupiah', () => {
  it('formats numbers as Indonesian Rupiah without decimals', () => {
    expect(formatRupiah(150000).replace(/\s/g, ' ')).toBe('Rp 150.000')
  })

  it('accepts numeric strings from the API', () => {
    expect(formatRupiah('2500000.00').replace(/\s/g, ' ')).toBe('Rp 2.500.000')
  })
})
