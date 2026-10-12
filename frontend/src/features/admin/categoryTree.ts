import type { AdminCategory } from '@/features/admin/types'

export interface CategoryOption {
  category: AdminCategory
  depth: number
}

// Orders a flat category list depth first so children appear directly below their parent.
export function flattenCategories(categories: AdminCategory[]): CategoryOption[] {
  const byParent = new Map<number | null, AdminCategory[]>()
  categories.forEach((category) => {
    const siblings = byParent.get(category.parentId) ?? []
    siblings.push(category)
    byParent.set(category.parentId, siblings)
  })

  const result: CategoryOption[] = []
  const visit = (parentId: number | null, depth: number) => {
    ;(byParent.get(parentId) ?? []).forEach((category) => {
      result.push({ category, depth })
      visit(category.id, depth + 1)
    })
  }
  visit(null, 0)
  return result
}

// A category and everything below it, used to stop a category being moved into itself.
export function descendantIds(categories: AdminCategory[], rootId: number): Set<number> {
  const ids = new Set<number>([rootId])
  let added = true
  while (added) {
    added = false
    categories.forEach((category) => {
      if (category.parentId !== null && ids.has(category.parentId) && !ids.has(category.id)) {
        ids.add(category.id)
        added = true
      }
    })
  }
  return ids
}

export function categoryLabel(option: CategoryOption): string {
  return `${'— '.repeat(option.depth)}${option.category.name}`
}
