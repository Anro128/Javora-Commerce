import { StarIcon, Trash2Icon, UploadIcon } from 'lucide-react'
import { useRef } from 'react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { useDeleteImage, useMakePrimaryImage, useUploadImage } from '@/features/admin/hooks'
import type { AdminImage } from '@/features/admin/types'
import { errorMessage } from '@/lib/form'

const MAX_BYTES = 5 * 1024 * 1024
const ACCEPTED_TYPES = ['image/jpeg', 'image/png', 'image/webp']

export function ImageManager({ productId, images }: { productId: number; images: AdminImage[] }) {
  const inputRef = useRef<HTMLInputElement>(null)
  const upload = useUploadImage(productId)
  const makePrimary = useMakePrimaryImage(productId)
  const remove = useDeleteImage(productId)

  const handleFiles = async (files: FileList | null) => {
    if (!files) {
      return
    }
    for (const file of Array.from(files)) {
      if (!ACCEPTED_TYPES.includes(file.type)) {
        toast.error(`${file.name}: only JPEG, PNG, and WebP images are supported`)
        continue
      }
      if (file.size > MAX_BYTES) {
        toast.error(`${file.name}: image must be at most 5 MB`)
        continue
      }
      try {
        await upload.mutateAsync(file)
        toast.success(`${file.name} uploaded`)
      } catch (error) {
        toast.error(errorMessage(error) ?? `${file.name} could not be uploaded`)
      }
    }
    if (inputRef.current) {
      inputRef.current.value = ''
    }
  }

  return (
    <section className="flex flex-col gap-3" aria-labelledby="images-heading">
      <div className="flex items-center justify-between">
        <h2 id="images-heading" className="text-lg font-semibold">
          Images
        </h2>
        <Button
          type="button"
          size="sm"
          variant="outline"
          disabled={upload.isPending}
          onClick={() => inputRef.current?.click()}
        >
          <UploadIcon /> {upload.isPending ? 'Uploading...' : 'Upload images'}
        </Button>
        <input
          ref={inputRef}
          type="file"
          accept={ACCEPTED_TYPES.join(',')}
          multiple
          className="sr-only"
          aria-label="Upload product images"
          onChange={(event) => void handleFiles(event.target.files)}
        />
      </div>

      {images.length === 0 ? (
        <p className="text-muted-foreground text-sm">
          No images yet. The first image becomes the main photo.
        </p>
      ) : (
        <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
          {images.map((image, index) => (
            <li key={image.id} className="flex flex-col gap-2">
              <div className="bg-muted relative aspect-square overflow-hidden rounded-lg border">
                <img src={image.url} alt={`Product image ${index + 1}`} className="size-full object-cover" />
                {index === 0 ? (
                  <span className="bg-primary text-primary-foreground absolute top-2 left-2 rounded-full px-2 py-0.5 text-xs">
                    Main
                  </span>
                ) : null}
              </div>
              <div className="flex gap-2">
                {index === 0 ? null : (
                  <Button
                    type="button"
                    size="sm"
                    variant="outline"
                    disabled={makePrimary.isPending}
                    onClick={() => makePrimary.mutate(image.id)}
                  >
                    <StarIcon /> Make main
                  </Button>
                )}
                <Button
                  type="button"
                  size="icon"
                  variant="outline"
                  aria-label={`Delete image ${index + 1}`}
                  disabled={remove.isPending}
                  onClick={() => remove.mutate(image.id, { onSuccess: () => toast.success('Image deleted') })}
                >
                  <Trash2Icon />
                </Button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
