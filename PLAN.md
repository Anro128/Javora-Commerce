# Javora Commerce — Rencana Pengembangan

Aplikasi e-commerce full-stack: backend **Spring Boot** (arsitektur *modular monolith*) dan frontend **React**, dalam **satu repository (monorepo)**.

---

## 1. Tujuan & Ruang Lingkup

### Tujuan
- Membangun backend e-commerce yang aman, mudah dikembangkan, dan siap produksi.
- Menyediakan REST API yang dipakai oleh frontend React (toko + admin panel) dan bisa dipakai ulang oleh aplikasi mobile nanti.
- Satu repo, satu perintah build, satu artefak deploy.
- Mendukung alur belanja lengkap: jelajah produk → keranjang → checkout → pembayaran → pengiriman.

### Peran Pengguna
| Peran | Kemampuan |
|---|---|
| **Guest** | Melihat katalog, mencari produk, melihat detail & ulasan |
| **Customer** | Semua kemampuan Guest + keranjang, checkout, riwayat pesanan, wishlist, ulasan, kelola alamat |
| **Admin** | Kelola produk, kategori, stok, pesanan, voucher, pengguna, laporan penjualan |

### Di luar cakupan MVP (dipertimbangkan nanti)
- Multi-vendor / marketplace (banyak penjual)
- Live chat, rekomendasi berbasis ML, program loyalitas

---

## 2. Tech Stack

### 2.1 Backend

| Lapisan | Pilihan | Alasan |
|---|---|---|
| Bahasa | **Java 21 LTS** (atau 25 LTS) | Virtual threads, records, pattern matching |
| Framework | **Spring Boot 3.5.x / 4.0.x** | Ekosistem matang, standar industri |
| Build tool | **Maven** | Paling umum, dokumentasi melimpah |
| Web | Spring Web (REST) | |
| Keamanan | Spring Security + **JWT** (access + refresh token) | Stateless, cocok untuk SPA/mobile |
| Database | **PostgreSQL 16+** | ACID, JSONB, full-text search |
| ORM | Spring Data JPA (Hibernate) | |
| Migrasi DB | **Flyway** | Skema terversi dan dapat diulang |
| Cache | **Redis** | Cache katalog, keranjang, rate limiting |
| Validasi | Jakarta Bean Validation | |
| Mapping DTO | **MapStruct** | Mapping compile-time, cepat & aman |
| Dokumentasi API | **springdoc-openapi** (Swagger UI) | |
| File/gambar | **MinIO / AWS S3** | Penyimpanan gambar produk |
| Email | Spring Mail + Thymeleaf template | Verifikasi akun, notifikasi pesanan |
| Pembayaran | **Midtrans** (Snap + webhook) | Mendukung VA, e-wallet, QRIS di Indonesia |
| Ongkir | **RajaOngkir / Biteship** | Hitung ongkir JNE, J&T, SiCepat, dll. |
| Testing | JUnit 5, Mockito, **Testcontainers**, REST Assured | |
| Observability | Spring Boot Actuator, Micrometer, Prometheus, Grafana | |
| Container | **Docker + Docker Compose** | Lingkungan dev konsisten |
| CI/CD | GitHub Actions | Build, test, scan, deploy |

### 2.2 Frontend

| Kebutuhan | Pilihan | Alasan |
|---|---|---|
| Build tool | **Vite** | Dev server cepat, hasil build statis yang mudah disajikan Spring Boot |
| Library UI | **React 19 + TypeScript** | Type-safe, ekosistem terbesar |
| Routing | **React Router** | Routing SPA, lazy loading halaman admin |
| Data server | **TanStack Query** | Caching, refetch, loading/error state otomatis |
| State klien | **Zustand** | Ringan; untuk sesi auth & UI state |
| Styling | **Tailwind CSS** + **shadcn/ui** | Komponen siap pakai yang mudah dikustomisasi |
| Form & validasi | **React Hook Form + Zod** | Validasi skema, performa baik |
| HTTP client | **Axios** | Interceptor untuk token & refresh otomatis |
| Tipe API | **openapi-typescript** | Tipe TypeScript di-generate dari OpenAPI backend, jadi kontrak FE–BE selalu sinkron |
| Pembayaran | **Midtrans Snap.js** | Popup pembayaran |
| Grafik admin | **Recharts** | Laporan penjualan |
| Testing | **Vitest + React Testing Library**, **Playwright** (E2E) | |
| Lint/format | oxlint + Prettier | Bawaan template Vite terbaru |

---

## 3. Arsitektur

### 3.1 Modular Monolith
Satu aplikasi Spring Boot, tetapi kode dibagi per **domain/modul** dengan batasan yang jelas. Modul hanya berkomunikasi lewat *service interface* publik atau **domain event** (`ApplicationEventPublisher`), bukan mengakses repository modul lain secara langsung.

> Opsional: gunakan **Spring Modulith** untuk memverifikasi batasan modul secara otomatis lewat test.

```
┌─────────────────────────────────────────────────────────┐
│      React SPA (toko + admin)  — disajikan di  /        │
└───────────────────────────┬─────────────────────────────┘
                            │ fetch /api/v1/**
┌───────────────────────────▼─────────────────────────────┐
│                     API Layer (REST)                    │
├──────────┬──────────┬─────────┬─────────┬───────────────┤
│   Auth   │ Catalog  │  Cart   │  Order  │  Payment ...  │
│  & User  │          │         │         │               │
├──────────┴──────────┴─────────┴─────────┴───────────────┤
│        Shared Kernel (exception, util, config)          │
├─────────────────────────────────────────────────────────┤
│      PostgreSQL   │   Redis   │   S3/MinIO   │  SMTP    │
└─────────────────────────────────────────────────────────┘
```

### 3.2 Layering per Modul
```
controller  →  service  →  repository  →  entity
    ↑ DTO (request/response)    ↑ mapper (MapStruct)
```
- **Controller**: hanya menerima request, validasi, memanggil service, mengembalikan DTO.
- **Service**: logika bisnis & batas transaksi (`@Transactional`).
- **Repository**: akses data (Spring Data JPA).
- **Entity tidak pernah dikembalikan langsung ke client** — selalu lewat DTO.

---

### 3.3 Integrasi Frontend–Backend dalam Satu Repo

**Saat development**: dua proses terpisah agar mendapat hot reload.
- Backend: `./mvnw spring-boot:run` → `http://localhost:8080`
- Frontend: `npm run dev` → `http://localhost:5173`, dengan **Vite proxy** meneruskan `/api/**` ke `:8080` (tidak perlu konfigurasi CORS).

**Saat build/produksi**: satu artefak.
- `./mvnw package` menjalankan **frontend-maven-plugin**: install Node → `npm ci` → `npm run build`.
- Hasil `frontend/dist` disalin ke `static/` di dalam JAR backend.
- Spring Boot menyajikan SPA di `/` dan API di `/api/v1/**`. Satu origin, jadi tidak ada CORS dan cookie refresh token bekerja dengan aman.
- Semua route non-API yang bukan file statis (mis. `/products/sepatu-x`, `/admin/orders`) di-*forward* ke `index.html` agar React Router yang menangani.

> Alternatif bila nanti perlu diskalakan terpisah: deploy `frontend/dist` ke CDN/Nginx dan backend sebagai service tersendiri. Struktur repo tidak perlu diubah.

---

## 4. Struktur Proyek (Monorepo)

```
javora-commerce/
├── README.md
├── docker-compose.yml          # postgres, redis, minio, mailpit
├── .github/workflows/ci.yml    # build & test backend + frontend
├── .gitignore
├── .editorconfig
│
├── frontend/                   # React + Vite + TypeScript
│   ├── package.json
│   ├── vite.config.ts          # proxy /api -> localhost:8080
│   ├── index.html
│   └── src/
│       ├── main.tsx
│       ├── app/                # router, providers (QueryClient, theme)
│       ├── api/                # axios instance, interceptor, tipe hasil generate OpenAPI
│       ├── components/
│       │   ├── ui/             # shadcn/ui (Button, Input, Dialog, ...)
│       │   └── layout/         # Header, Footer, AdminSidebar
│       ├── features/           # dikelompokkan per domain, sejajar dengan modul backend
│       │   ├── auth/           # LoginPage, RegisterPage, useAuth, authStore
│       │   ├── catalog/        # ProductListPage, ProductDetailPage, ProductCard
│       │   ├── cart/
│       │   ├── checkout/
│       │   ├── orders/
│       │   ├── account/        # profil, alamat, wishlist
│       │   └── admin/          # dashboard, produk, pesanan, voucher, laporan
│       ├── hooks/
│       ├── lib/                # formatRupiah, utils
│       └── routes/             # ProtectedRoute, AdminRoute
│
└── backend/                    # Spring Boot
    ├── pom.xml                 # termasuk frontend-maven-plugin
    ├── mvnw
    └── src/
        ├── main/
        │   ├── java/com/javora/commerce/
        │   │   ├── JavoraCommerceApplication.java
        │   │   ├── common/                # shared kernel
        │   │   │   ├── config/            # Security, OpenAPI, Redis, CORS
        │   │   │   ├── exception/         # GlobalExceptionHandler, custom exceptions
        │   │   │   ├── response/          # ApiResponse, PageResponse
        │   │   │   ├── audit/             # BaseEntity (createdAt, updatedAt)
        │   │   │   └── util/
        │   │   ├── auth/                  # login, register, JWT, refresh token
        │   │   ├── user/                  # profil, alamat
        │   │   ├── catalog/               # produk, kategori, varian, gambar
        │   │   ├── inventory/             # stok & reservasi stok
        │   │   ├── cart/                  # keranjang
        │   │   ├── order/                 # checkout & pesanan
        │   │   ├── payment/               # integrasi Midtrans
        │   │   ├── shipping/              # ongkir & resi
        │   │   ├── promotion/             # voucher & diskon
        │   │   ├── review/                # ulasan & rating
        │   │   ├── wishlist/
        │   │   ├── notification/          # email
        │   │   ├── admin/                 # dashboard & laporan
        │   │   └── web/                   # SpaForwardController (route non-API -> index.html)
        │   │       (tiap modul: controller/ service/ repository/ entity/ dto/ mapper/ event/)
        │   └── resources/
        │       ├── application.yml
        │       ├── application-dev.yml
        │       ├── application-prod.yml
        │       ├── db/migration/          # V1__init.sql, V2__...
        │       ├── templates/email/
        │       └── static/                # diisi otomatis dari frontend/dist saat build (di-.gitignore)
        └── test/
            └── java/com/javora/commerce/  # unit & integration test
```

---

## 5. Desain Database (Entitas Inti)

```
users ──< addresses
users ──< refresh_tokens
users ──1 carts ──< cart_items >── product_variants
users ──< orders ──< order_items >── product_variants
orders ──1 payments
orders ──1 shipments
orders >── vouchers (opsional)
categories ──< categories (parent/child)
categories ──< products ──< product_variants ──1 inventory
products ──< product_images
products ──< reviews >── users
users ──< wishlist_items >── products
```

### Tabel Utama
| Tabel | Kolom Penting |
|---|---|
| `users` | id, email (unique), password_hash, full_name, phone, role, enabled, email_verified, created_at |
| `addresses` | id, user_id, recipient, phone, province, city, district, postal_code, detail, is_default |
| `categories` | id, parent_id, name, slug (unique), image_url |
| `products` | id, category_id, name, slug (unique), description, base_price, status (DRAFT/ACTIVE/ARCHIVED), weight_gram |
| `product_variants` | id, product_id, sku (unique), name (mis. "Merah / XL"), price, attributes (JSONB) |
| `product_images` | id, product_id, url, sort_order |
| `inventory` | variant_id, quantity, reserved_quantity, **version** (optimistic lock) |
| `carts` / `cart_items` | cart_id, variant_id, quantity |
| `orders` | id, order_number (unique), user_id, status, subtotal, shipping_cost, discount, total, shipping_address (snapshot JSONB), voucher_id, created_at |
| `order_items` | order_id, variant_id, product_name, variant_name, price, quantity (**snapshot** harga saat beli) |
| `payments` | id, order_id, provider, transaction_id, method, status, amount, paid_at, raw_response (JSONB) |
| `shipments` | id, order_id, courier, service, tracking_number, status, shipped_at, delivered_at |
| `vouchers` | id, code (unique), type (PERCENT/FIXED), value, min_purchase, max_discount, quota, used_count, valid_from, valid_until |
| `reviews` | id, product_id, user_id, order_item_id, rating (1–5), comment |

### Prinsip Desain
- Uang disimpan sebagai **`NUMERIC(15,2)`** / `BigDecimal` — **jangan pakai `double`**.
- Order menyimpan **snapshot** harga, nama produk, dan alamat agar riwayat tidak berubah ketika data master diubah.
- Gunakan **soft delete** untuk produk (status `ARCHIVED`) agar order lama tetap valid.
- Index pada kolom pencarian: `slug`, `sku`, `email`, `order_number`, `orders(user_id, created_at)`.

---

## 6. Alur Bisnis Kunci

### 6.1 Status Pesanan (State Machine)
```
PENDING_PAYMENT ──(bayar sukses)──> PAID ──> PROCESSING ──> SHIPPED ──> DELIVERED ──> COMPLETED
       │                                │
       ├──(timeout 24 jam)──> CANCELLED │
       └──(user batal)──────> CANCELLED └──(admin batal/refund)──> REFUNDED
```
Transisi status divalidasi di satu tempat (`OrderStatus.canTransitionTo(...)`) agar tidak ada lompatan status yang ilegal.

### 6.2 Checkout
1. Validasi keranjang (produk aktif, harga terbaru).
2. Hitung subtotal, ongkir (API kurir), dan diskon voucher.
3. **Reservasi stok** (`reserved_quantity += qty`) dengan optimistic locking → cegah *overselling*.
4. Buat order berstatus `PENDING_PAYMENT` + snapshot item.
5. Buat transaksi Midtrans → kembalikan `snap_token` / `redirect_url` ke client.
6. Kosongkan keranjang.

Semua langkah 3–4 dalam **satu transaksi DB**.

### 6.3 Pembayaran (Webhook)
1. Midtrans memanggil `POST /api/v1/payments/webhook`.
2. **Verifikasi signature** (SHA-512 dari order_id + status_code + gross_amount + server_key).
3. Proses secara **idempoten** (webhook bisa dikirim lebih dari sekali).
4. Jika sukses → order `PAID`, stok dikurangi permanen (`quantity -= qty`, `reserved -= qty`), terbitkan event `OrderPaidEvent` → kirim email.
5. Jika gagal/expired → order `CANCELLED`, lepaskan reservasi stok.

### 6.4 Pembatalan Otomatis
Job terjadwal (`@Scheduled`, tiap 5 menit) membatalkan order `PENDING_PAYMENT` yang lewat batas waktu dan melepaskan stok. Gunakan **ShedLock** bila aplikasi dijalankan lebih dari satu instance.

---

## 7. Desain REST API

Prefix: `/api/v1`. Format respons konsisten:
```json
{ "success": true, "message": "OK", "data": { ... }, "timestamp": "..." }
```
Error mengikuti **RFC 7807 (Problem Details)** via `ProblemDetail` bawaan Spring.

### Auth
| Method | Endpoint | Akses |
|---|---|---|
| POST | `/auth/register` | Public |
| POST | `/auth/login` | Public |
| POST | `/auth/refresh` | Public |
| POST | `/auth/logout` | User |
| POST | `/auth/verify-email` | Public |
| POST | `/auth/forgot-password` · `/auth/reset-password` | Public |

### Katalog
| Method | Endpoint | Akses |
|---|---|---|
| GET | `/products?q=&category=&minPrice=&maxPrice=&sort=&page=&size=` | Public |
| GET | `/products/{slug}` | Public |
| GET | `/categories` | Public |
| GET | `/products/{id}/reviews` | Public |

### Customer
| Method | Endpoint |
|---|---|
| GET/PUT | `/users/me` |
| GET/POST/PUT/DELETE | `/users/me/addresses` |
| GET | `/cart` |
| POST/PUT/DELETE | `/cart/items` · `/cart/items/{id}` |
| POST | `/checkout/preview` (hitung total, ongkir, diskon) |
| POST | `/checkout` |
| GET | `/orders` · `/orders/{orderNumber}` |
| POST | `/orders/{orderNumber}/cancel` |
| POST | `/orders/{orderNumber}/complete` (konfirmasi terima) |
| POST | `/products/{id}/reviews` |
| GET/POST/DELETE | `/wishlist` |
| GET | `/shipping/cost?destination=&weight=&courier=` |

### Admin (`/admin/**`, role `ADMIN`)
| Method | Endpoint |
|---|---|
| CRUD | `/admin/products`, `/admin/categories`, `/admin/vouchers` |
| POST | `/admin/products/{id}/images` (upload) |
| PUT | `/admin/inventory/{variantId}` |
| GET | `/admin/orders?status=` |
| PATCH | `/admin/orders/{id}/status` · `/admin/orders/{id}/ship` (input resi) |
| GET | `/admin/users` |
| GET | `/admin/reports/sales?from=&to=` |

### Webhook
| POST | `/payments/webhook` | Public (diverifikasi signature) |
|---|---|---|

---

## 8. Desain Frontend (UI)

Satu aplikasi React untuk toko dan admin panel. Halaman admin di-*lazy load* (`React.lazy`) agar tidak membebani bundle pembeli.

### 8.1 Halaman Toko (Customer)
| Route | Halaman | Isi Utama |
|---|---|---|
| `/` | Beranda | Banner, kategori unggulan, produk terbaru/terlaris |
| `/products` | Katalog | Grid produk, filter (kategori, harga), sort, pencarian, pagination (filter disimpan di URL query) |
| `/products/:slug` | Detail Produk | Galeri gambar, pilih varian, stok, tombol tambah ke keranjang, ulasan |
| `/categories/:slug` | Produk per kategori | |
| `/cart` | Keranjang | Ubah qty, hapus item, ringkasan harga |
| `/checkout` | Checkout | Pilih alamat → pilih kurir & layanan → voucher → ringkasan → bayar (popup Midtrans Snap) |
| `/checkout/result` | Hasil Pembayaran | Status sukses/pending/gagal |
| `/login`, `/register` | Auth | |
| `/forgot-password`, `/reset-password`, `/verify-email` | Auth | |
| `/account` | Profil | Edit data diri, ganti password |
| `/account/addresses` | Alamat | CRUD alamat, set default |
| `/account/orders` | Riwayat Pesanan | Daftar + filter status |
| `/account/orders/:orderNumber` | Detail Pesanan | Item, timeline status, resi, tombol bayar ulang/batal/konfirmasi terima, beri ulasan |
| `/account/wishlist` | Wishlist | |

### 8.2 Halaman Admin (`/admin/**`, hanya role `ADMIN`)
| Route | Halaman |
|---|---|
| `/admin` | Dashboard: total penjualan, jumlah order, produk terlaris, grafik pendapatan |
| `/admin/products` · `/admin/products/new` · `/admin/products/:id` | Tabel produk, form produk + varian + upload gambar |
| `/admin/categories` | Kelola kategori (tree) |
| `/admin/inventory` | Stok per varian, peringatan stok menipis |
| `/admin/orders` · `/admin/orders/:id` | Kelola pesanan, ubah status, input resi |
| `/admin/vouchers` | Kelola voucher |
| `/admin/users` | Daftar pengguna, aktif/nonaktifkan |
| `/admin/reports` | Laporan penjualan per periode |

### 8.3 Pola Frontend
- **Auth**: access token disimpan **di memori** (Zustand, bukan `localStorage`), refresh token di **cookie `httpOnly`**. Interceptor Axios otomatis memanggil `/auth/refresh` saat menerima 401 lalu mengulang request.
- **Route guard**: `ProtectedRoute` (harus login) dan `AdminRoute` (harus ADMIN). Keamanan sebenarnya tetap di backend; guard hanya untuk UX.
- **Data fetching**: semua panggilan API lewat custom hook TanStack Query (`useProducts`, `useCart`, `useCreateOrder`) dengan invalidasi cache setelah mutasi.
- **Keranjang guest**: disimpan di `localStorage`, lalu digabung ke keranjang server saat login.
- **Tipe API**: `npm run gen:api` membuat tipe dari `/v3/api-docs`, sehingga perubahan DTO backend langsung terdeteksi di frontend saat kompilasi.
- **UX**: skeleton loading, toast notifikasi, empty state, error boundary, format Rupiah (`Intl.NumberFormat('id-ID')`).
- **Responsif**: *mobile-first*, karena mayoritas pembeli memakai HP.
- **SEO**: SPA kurang optimal untuk SEO. Untuk MVP cukup meta tag dinamis (`react-helmet-async`); pertimbangkan migrasi ke Next.js bila SEO jadi prioritas.

---

## 9. Keamanan

- **Password**: BCrypt (atau Argon2).
- **JWT**: access token berumur pendek (15 menit), dikirim di header `Authorization`. Refresh token (7 hari) dikirim sebagai **cookie `httpOnly`, `Secure`, `SameSite=Strict`**, disimpan (hash-nya) di DB dengan **rotasi** dan bisa dicabut.
- Karena SPA dan API satu origin di produksi, risiko CSRF minimal; endpoint `/auth/refresh` tetap dilindungi `SameSite=Strict`.
- **Otorisasi**: `@PreAuthorize` + pengecekan kepemilikan resource (user hanya bisa melihat order miliknya — cegah IDOR).
- **Validasi input** di semua DTO request; jangan percaya harga dari client — selalu hitung ulang di server.
- **Rate limiting** untuk login, register, forgot-password (Bucket4j + Redis).
- **CORS** tidak diperlukan (satu origin); bila frontend dipisah nanti, batasi hanya ke domain frontend.
- **Content Security Policy** untuk SPA (izinkan domain Midtrans Snap & CDN gambar).
- **Secrets** (DB password, JWT secret, Midtrans key) lewat environment variable, **jangan di-commit**.
- Verifikasi signature webhook pembayaran.
- Upload file: batasi tipe (jpg/png/webp) & ukuran, nama file di-generate ulang.
- Header keamanan (HSTS, X-Content-Type-Options) & HTTPS di produksi.
- Audit dependency rutin (OWASP Dependency-Check / Dependabot).

---

## 10. Performa & Skalabilitas

- **Pagination** di semua endpoint list (`Pageable`).
- Hindari **N+1 query**: gunakan `@EntityGraph` / fetch join; aktifkan log SQL saat development.
- **Cache Redis** untuk daftar kategori & detail produk (`@Cacheable`, invalidasi saat admin update).
- Pencarian produk: mulai dengan PostgreSQL full-text search (`tsvector`); migrasi ke Elasticsearch/OpenSearch bila katalog sangat besar.
- Gambar disajikan lewat CDN.
- Aktifkan **virtual threads** (`spring.threads.virtual.enabled=true`).
- Proses lambat (email, notifikasi) secara asinkron (`@Async` / `@TransactionalEventListener(phase = AFTER_COMMIT)`).

---

## 11. Strategi Testing

| Jenis | Alat | Cakupan |
|---|---|---|
| Unit test | JUnit 5 + Mockito | Logika service: kalkulasi harga, voucher, transisi status |
| Repository test | `@DataJpaTest` + Testcontainers (PostgreSQL) | Query custom |
| Web layer test | `@WebMvcTest` + MockMvc | Validasi, security, format respons |
| Integration test | `@SpringBootTest` + Testcontainers + REST Assured | Alur end-to-end: register → checkout → webhook |
| Concurrency test | Thread paralel | Pastikan stok tidak oversell |
| Mock eksternal | WireMock | Midtrans & API ongkir |
| Frontend unit/komponen | Vitest + React Testing Library | Komponen, hook, util (format harga, validasi form) |
| Frontend mock API | MSW (Mock Service Worker) | Test UI tanpa backend |
| End-to-end | Playwright | Alur nyata di browser: daftar → belanja → checkout; admin tambah produk |

Target: **≥ 80% coverage** pada layer service backend (JaCoCo); alur kritis frontend tercakup E2E.

---

## 12. DevOps & Deployment

### Lingkungan Lokal (`docker-compose.yml`)
- `postgres`, `redis`, `minio`, `mailpit` (SMTP palsu untuk lihat email).

### Profil Spring
- `dev` — log SQL aktif, Swagger aktif.
- `test` — Testcontainers.
- `prod` — Swagger dimatikan/diproteksi, log JSON.

### Menjalankan Lokal
```bash
docker compose up -d                       # infrastruktur
cd backend  && ./mvnw spring-boot:run      # API di :8080
cd frontend && npm run dev                 # UI di :5173 (proxy ke :8080)
```

### CI/CD (GitHub Actions)
Workflow dengan *path filter* agar job hanya jalan bila foldernya berubah:
1. **Frontend**: `npm ci` → lint → type-check → `vitest`
2. **Backend**: `./mvnw verify` (unit + integration test dengan Testcontainers)
3. **E2E**: Playwright terhadap aplikasi yang sudah dibangun (di branch `main`/PR ke `main`)
4. Coverage & static analysis (SonarCloud / SpotBugs opsional)
5. Build **satu Docker image** (multi-stage: Node build frontend → Maven build backend + static → JRE runtime)
6. Push ke registry → deploy (VPS dengan Docker, Railway, Render, atau AWS ECS/GCP Cloud Run)

### Observability
- Actuator: `/actuator/health`, `/actuator/prometheus`
- Log terstruktur + **correlation ID** per request
- Dashboard Grafana: request rate, latency, error rate, jumlah order

---

## 13. Roadmap Pengerjaan

Dikerjakan per **fitur utuh (vertical slice)**: setiap fase menyelesaikan backend **dan** UI-nya, sehingga di akhir fase ada fitur yang bisa langsung dicoba di browser.

### 13.1 Alur Git per Fase

Setiap fase diakhiri dengan **satu commit di git lokal**, diberi **tag**, lalu **di-push ke remote**.

**Persiapan (sekali, di awal Fase 0)**
```bash
git init -b main
git config user.name "Anro128"
git config user.email "ahmadnurrohim2812@gmail.com"
git remote add origin <URL_REPO_REMOTE>    # mis. https://github.com/Anro128/javora-commerce.git
```

**Identitas commit**
- Author dan committer **hanya `Anro128`**.
- **Tidak boleh ada** trailer `Co-Authored-By`, teks "Generated with ...", atau identitas AI/tool lain di commit message, tag, kode, komentar, maupun README.
- Verifikasi sebelum push:
  ```bash
  git log --format="%an <%ae> | %cn <%ce>%n%B" -1   # harus hanya Anro128, tanpa trailer tambahan
  ```

**Di akhir setiap fase**
```bash
# 1. Pastikan semua test & build lolos
cd backend && ./mvnw verify && cd ..
cd frontend && npm run lint && npm run test -- --run && npm run build && cd ..

# 2. Commit lokal
git add -A
git status                                  # periksa: tidak ada .env, secret, target/, node_modules/
git commit -m "feat: phase N - <summary>"

# 3. Tag sebagai milestone
git tag -a phase-N -m "Phase N: <summary>"

# 4. Push ke remote
git push origin main
git push origin phase-N
```

**Aturan**
- Fase **baru boleh di-commit bila build dan semua test lolos**; jangan commit kode yang rusak.
- Commit message dalam **bahasa Inggris**, format Conventional Commits, dengan body berisi daftar perubahan utama.
- `.gitignore` sejak awal: `target/`, `node_modules/`, `dist/`, `backend/src/main/resources/static/`, `.env`, `*.log`, `.idea/`, `.vscode/`.
- Secret hanya di `.env` (tidak di-commit); sediakan `.env.example` sebagai contoh.
- Tag `phase-N` memudahkan kembali ke kondisi fase tertentu (`git checkout phase-2`).

**Rencana commit**
| Fase | Commit message | Tag |
|---|---|---|
| 0 | `chore: phase 0 - project foundation and monorepo setup` | `phase-0` |
| 1 | `feat: phase 1 - authentication and user management` | `phase-1` |
| 2 | `feat: phase 2 - product catalog and inventory` | `phase-2` |
| 3 | `feat: phase 3 - shopping cart and checkout` | `phase-3` |
| 4 | `feat: phase 4 - payment integration with Midtrans` | `phase-4` |
| 5 | `feat: phase 5 - vouchers, reviews, wishlist, and shipping` | `phase-5` |
| 6 | `feat: phase 6 - admin dashboard and sales reports` | `phase-6` |
| 7 | `chore: phase 7 - hardening and release v1.0.0` | `phase-7`, `v1.0.0` |

Contoh commit lengkap:
```
feat: phase 1 - authentication and user management

- Add register, login, and JWT refresh token rotation
- Add email verification and password reset
- Add user profile and address management
- Add login, register, and account pages in frontend
```

### Fase 0 — Fondasi (Minggu 1)
**Repo & infrastruktur**
- [x] `git init`, tambahkan remote, inisialisasi monorepo (`backend/`, `frontend/`), `.gitignore`, `.editorconfig`, `.env.example`, README
- [x] Docker Compose (PostgreSQL, Redis, MinIO, Mailpit)
- [x] Setup CI (job backend & frontend)

**Backend**
- [x] Proyek Spring Boot via Spring Initializr di `backend/`
- [x] Konfigurasi profil, Flyway, `BaseEntity` dengan auditing
- [x] `GlobalExceptionHandler`, format `ApiResponse`, OpenAPI
- [x] `frontend-maven-plugin` (profile `-Pfrontend`) + fallback SPA (`SpaWebConfig`), build menghasilkan satu JAR

**Frontend**
- [x] Proyek Vite + React + TypeScript di `frontend/`
- [x] Tailwind + shadcn/ui, oxlint + Prettier
- [x] Router, QueryClient, Axios instance, Vite proxy
- [x] Layout dasar (Header, Footer, AdminSidebar), script `gen:api`

- [x] ✅ Commit `phase-0` → tag → push ke remote

### Fase 1 — Auth & User (Minggu 2–3)
- [x] **BE**: Register, login, JWT + refresh token (cookie httpOnly), role CUSTOMER/ADMIN
- [x] **BE**: Verifikasi email & reset password; profil & alamat
- [x] **FE**: Halaman login, register, lupa/reset password, verifikasi email
- [x] **FE**: `authStore`, interceptor refresh token, `ProtectedRoute` & `AdminRoute`
- [x] **FE**: Halaman profil & kelola alamat

**Catatan implementasi Fase 1**
- JWT memakai Spring Security OAuth2 Resource Server (HS256, Nimbus), bukan `jjwt`; tidak perlu dependensi tambahan.
- Refresh token disimpan sebagai hash SHA-256, dirotasi di setiap pemakaian, dan dipakai ulang di luar jeda 10 detik akan mencabut seluruh sesi pengguna.
- Akun admin pertama dibuat otomatis lewat `app.bootstrap-admin.*` (di profil `dev`: `admin@javora.local` / `Admin12345`); di produksi isi lewat environment.
- Tipe API frontend masih ditulis manual di `features/*/types.ts`; `gen:api` akan dipakai setelah DTO diberi anotasi `required` di OpenAPI.
- Postgres Docker memakai port host **5433** karena port 5432 sering dipakai PostgreSQL lokal.

- [ ] ✅ Commit `phase-1` → tag → push ke remote

### Fase 2 — Katalog & Inventori (Minggu 3–5)
- [ ] **BE**: CRUD kategori (hierarki), produk + varian, upload gambar ke MinIO/S3
- [ ] **BE**: Listing publik (filter, sort, pagination, pencarian), stok, cache Redis
- [ ] **FE (admin)**: Tabel produk, form produk + varian + upload gambar, kelola kategori, stok
- [ ] **FE (toko)**: Beranda, katalog dengan filter, detail produk dengan pilihan varian

- [ ] ✅ Commit `phase-2` → tag → push ke remote

### Fase 3 — Keranjang & Checkout (Minggu 6)
- [ ] **BE**: Keranjang, integrasi ongkir, checkout preview, pembuatan order, reservasi stok (optimistic locking)
- [ ] **FE**: Halaman keranjang, keranjang guest (`localStorage`) + merge saat login
- [ ] **FE**: Alur checkout multi-langkah (alamat → kurir → ringkasan)

- [ ] ✅ Commit `phase-3` → tag → push ke remote

### Fase 4 — Pembayaran (Minggu 7)
- [ ] **BE**: Integrasi Midtrans Snap (sandbox), webhook idempoten + verifikasi signature
- [ ] **BE**: Job pembatalan order kedaluwarsa, email notifikasi pesanan
- [ ] **FE**: Popup Midtrans Snap, halaman hasil pembayaran
- [ ] **FE**: Riwayat & detail pesanan (timeline status, bayar ulang, batalkan)

- [ ] ✅ Commit `phase-4` → tag → push ke remote

### Fase 5 — Fitur Pelengkap (Minggu 8)
- [ ] **BE + FE**: Voucher (input kode di checkout; kelola voucher di admin)
- [ ] **BE + FE**: Ulasan & rating (hanya pembeli terverifikasi)
- [ ] **BE + FE**: Wishlist
- [ ] **BE + FE**: Pengiriman: admin input resi, customer melihat resi & konfirmasi terima

- [ ] ✅ Commit `phase-5` → tag → push ke remote

### Fase 6 — Admin & Laporan (Minggu 9)
- [ ] **BE**: Endpoint manajemen pesanan, pengguna, laporan penjualan
- [ ] **FE**: Dashboard admin (kartu ringkasan + grafik Recharts), kelola pesanan & pengguna, halaman laporan

- [ ] ✅ Commit `phase-6` → tag → push ke remote

### Fase 7 — Hardening & Rilis (Minggu 10–12)
- [ ] Rate limiting, security headers, CSP, audit dependency (Maven & npm)
- [ ] Integration & concurrency test (BE), test komponen (FE), E2E Playwright
- [ ] Polish UI: responsif mobile, loading/empty/error state, aksesibilitas dasar
- [ ] Observability (Prometheus + Grafana)
- [ ] Docker image tunggal → deploy staging → produksi

- [ ] ✅ Commit `phase-7` → tag `v1.0.0` → push ke remote

---

## 14. Pengembangan Lanjutan (Pasca-MVP)

- Migrasi ke **Next.js** bila SEO halaman produk menjadi prioritas
- Aplikasi mobile (**React Native** atau **Flutter**) memakai REST API yang sama
- Pencarian dengan Elasticsearch/OpenSearch
- Message broker (**RabbitMQ / Kafka**) untuk event antar modul
- Pecah modul berat (order, payment) menjadi microservices bila trafik menuntut
- Notifikasi WhatsApp / push notification
- Multi-vendor marketplace
- Rekomendasi produk

---

## 15. Konvensi Kode

### Bahasa: Semua dalam Bahasa Inggris
Seluruh isi kode wajib berbahasa Inggris, termasuk:
- Nama class, method, variabel, file, tabel, dan kolom database
- Komentar kode
- Pesan error & validasi API (`"Email is already registered"`), pesan log, isi template email
- Teks UI (label, tombol, notifikasi, halaman)
- Commit message, tag, README, dan nama migrasi Flyway (`V2__create_products_table.sql`)

### Aturan Komentar
- **Maksimal satu kalimat singkat dalam satu baris.**
- Tidak ada komentar multi-baris atau blok (`/* ... */`, Javadoc panjang, JSDoc panjang).
- Komentar hanya untuk menjelaskan **alasan** (*why*) atau hal yang tidak jelas dari kode; jangan mengulang apa yang sudah terbaca dari kode.
- Tidak ada kode yang dikomentari (*commented-out code*); hapus saja, karena riwayatnya ada di git.

```java
// Reserve stock first to prevent overselling under concurrent checkouts.
inventoryService.reserve(items);
```
```tsx
// Retry once after refreshing the access token.
```

### Git
- Commit mengikuti **Conventional Commits**: `feat:`, `fix:`, `refactor:`, `chore:`, `test:`, `docs:`
- Satu commit + tag per fase di branch `main` (lihat bagian 13.1), lalu push ke remote
- Satu commit boleh menyentuh backend dan frontend sekaligus; ini keuntungan monorepo

### Backend
- Penamaan paket: `com.javora.commerce.<modul>`
- DTO memakai **Java record** (`CreateProductRequest`, `ProductResponse`)
- Constructor injection (Lombok `@RequiredArgsConstructor`), hindari field injection
- Satu migrasi Flyway per perubahan skema; **jangan mengubah migrasi yang sudah dijalankan**

### Frontend
- Komponen `PascalCase.tsx`, hook `useCamelCase.ts`
- Kode dikelompokkan per fitur (`features/<domain>`), bukan per jenis file
- Jangan memanggil Axios langsung dari komponen; selalu lewat hook TanStack Query
- Jalankan `npm run gen:api` setiap kali DTO backend berubah; jangan menulis tipe API secara manual
- TypeScript `strict: true`

---

## 16. Dependensi Awal

### Backend (Spring Initializr)
```
Spring Web, Spring Security, Spring Data JPA, Validation,
PostgreSQL Driver, Flyway Migration, Spring Data Redis, Spring Cache,
Spring Boot Actuator, Java Mail Sender, Thymeleaf (template email), Lombok,
Spring Boot DevTools, Testcontainers, Docker Compose Support
```
Tambahan manual: `jjwt` (atau `spring-boot-starter-oauth2-resource-server`), `mapstruct`, `springdoc-openapi-starter-webmvc-ui`, `bucket4j`, `shedlock`, `midtrans-java`, `aws-sdk s3` / `minio`, `frontend-maven-plugin` (plugin build).

### Frontend (npm)
```
react, react-dom, react-router, @tanstack/react-query, zustand, axios,
react-hook-form, zod, @hookform/resolvers, tailwindcss, shadcn/ui (via CLI),
lucide-react, recharts, react-helmet-async, sonner (toast)

dev: typescript, vite, @vitejs/plugin-react, eslint, prettier,
     vitest, @testing-library/react, msw, @playwright/test, openapi-typescript
```
