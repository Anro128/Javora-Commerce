import { Outlet } from 'react-router'
import { AdminSidebar } from '@/components/layout/AdminSidebar'

export function AdminLayout() {
  return (
    <div className="flex min-h-svh">
      <AdminSidebar />
      <main className="flex-1 overflow-x-hidden p-6">
        <Outlet />
      </main>
    </div>
  )
}
