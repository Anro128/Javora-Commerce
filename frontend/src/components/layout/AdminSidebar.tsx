import {
  BarChart3Icon,
  BoxesIcon,
  FolderTreeIcon,
  LayoutDashboardIcon,
  PackageIcon,
  ReceiptIcon,
  TicketPercentIcon,
  UsersIcon,
} from 'lucide-react'
import { Link, NavLink } from 'react-router'
import { cn } from '@/lib/utils'

const adminNavItems = [
  { to: '/admin', label: 'Dashboard', icon: LayoutDashboardIcon },
  { to: '/admin/products', label: 'Products', icon: PackageIcon },
  { to: '/admin/categories', label: 'Categories', icon: FolderTreeIcon },
  { to: '/admin/inventory', label: 'Inventory', icon: BoxesIcon },
  { to: '/admin/orders', label: 'Orders', icon: ReceiptIcon },
  { to: '/admin/vouchers', label: 'Vouchers', icon: TicketPercentIcon },
  { to: '/admin/users', label: 'Users', icon: UsersIcon },
  { to: '/admin/reports', label: 'Reports', icon: BarChart3Icon },
]

export function AdminSidebar() {
  return (
    <aside className="bg-muted/40 hidden w-60 shrink-0 border-r md:block">
      <div className="flex h-16 items-center border-b px-6">
        <Link to="/admin" className="text-lg font-bold tracking-tight">
          Javora Admin
        </Link>
      </div>
      <nav className="flex flex-col gap-1 p-3">
        {adminNavItems.map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            end
            className={({ isActive }) =>
              cn(
                'flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors',
                isActive
                  ? 'bg-primary text-primary-foreground'
                  : 'text-muted-foreground hover:bg-muted hover:text-foreground',
              )
            }
          >
            <Icon className="size-4" />
            {label}
          </NavLink>
        ))}
      </nav>
    </aside>
  )
}
