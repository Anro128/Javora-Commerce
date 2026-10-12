import { Outlet } from 'react-router'
import { Footer } from '@/components/layout/Footer'
import { Header } from '@/components/layout/Header'

export function RootLayout() {
  return (
    <div className="flex min-h-svh flex-col">
      <Header />
      <main className="container mx-auto flex-1 px-4 py-8">
        <Outlet />
      </main>
      <Footer />
    </div>
  )
}
