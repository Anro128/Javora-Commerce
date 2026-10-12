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

export const adminNavItems = [
  { to: '/admin', label: 'Dashboard', icon: LayoutDashboardIcon },
  { to: '/admin/products', label: 'Products', icon: PackageIcon },
  { to: '/admin/categories', label: 'Categories', icon: FolderTreeIcon },
  { to: '/admin/inventory', label: 'Inventory', icon: BoxesIcon },
  { to: '/admin/orders', label: 'Orders', icon: ReceiptIcon },
  { to: '/admin/vouchers', label: 'Vouchers', icon: TicketPercentIcon },
  { to: '/admin/users', label: 'Users', icon: UsersIcon },
  { to: '/admin/reports', label: 'Reports', icon: BarChart3Icon },
]
