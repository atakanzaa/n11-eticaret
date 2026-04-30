# SmartCommerce — Angular Source Bundle

Türkiye odaklı çok satıcılı pazaryeri prototipinin tam Angular 17 kaynak kodu.

## Hızlı başlangıç

```bash
cd smartcommerce
npm install
npm start
# http://localhost:4200
```

## Mimari

- **Angular 17** (standalone components, signals, new control flow `@if` / `@for`)
- **State management:** Signals + computed (NgRx olmadan, fine-grained reaktivite)
- **Styling:** SCSS + CSS variables (tek temada tüm tasarım tokenleri `src/styles.scss`)
- **Component dosya yapısı:** Her component standart Angular üçlüsü — `*.component.ts` + `*.component.html` + `*.component.scss` (ayrı dosyalar)
- **Routing:** Lazy-loaded standalone routes (`/`, `/satici`, `/admin`)
- **HTTP:** `provideHttpClient(withFetch())` hazır — backend bağlamak için servis methodlarını değiştir

## Klasör yapısı

```
src/
├── index.html, main.ts, styles.scss
└── app/
    ├── app.component.ts          # kök <sc-root> + toast host
    ├── app.config.ts             # provideRouter, provideHttpClient
    ├── app.routes.ts             # lazy route ağacı
    ├── core/
    │   ├── models.ts             # tüm domain interface'leri
    │   ├── mock-data.ts          # ürün/sipariş/satıcı/admin verileri
    │   ├── cart.service.ts       # signals tabanlı sepet (localStorage)
    │   ├── orders.service.ts     # sipariş + iade
    │   ├── ai-assistant.service.ts  # backend AI orchestrator için arayüz
    │   └── toast.service.ts
    ├── shared/
    │   ├── try.pipe.ts           # ₺ formatlama
    │   └── ui/                   # header, footer, product-card, ai-chat, toast-host
    └── features/
        ├── buyer/                # home, search, product, cart, checkout, success, account/*
        ├── seller/               # dashboard, products
        └── admin/                # overview, fraud
```

## Backend entegrasyonu

Tüm mock veri `core/mock-data.ts` içinde. Gerçek API'ye bağlamak için:

1. `core/` altına `api/` klasörü oluştur (örn. `products.api.ts`).
2. `HttpClient` ile gerçek endpoint'i çağır.
3. Servisleri (`cart.service.ts`, `orders.service.ts`) inject ettiğin api'yi kullanacak şekilde güncelle.
4. `ai-assistant.service.ts` zaten `/api/ai/chat` POST için hazır şablona sahip.

## Ekranlar

**Alıcı:** Anasayfa · Arama/Kategori · Ürün detay · Sepet · 3 adımlı Ödeme (3DS modal) · Sipariş başarılı · Profil · Siparişlerim · Sipariş detay (timeline) · İade akışı

**Satıcı (`/satici`):** Anasayfa (4 KPI + 30g ciro grafiği + son siparişler) · Ürünler (filtre + arama + 3 adımlı yeni ürün modali)

**Admin (`/admin`):** Genel Bakış (5 KPI + GMV grafik + kategori dağılımı) · Fraud (kurallar + risk-skorlu sipariş tablosu, onayla/reddet)

## Notlar

- Tüm metin Türkçe, ₺ TRY formatlama `tr-TR` locale ile.
- AI chat (sağ alt baloncuk) — `ai-assistant.service.ts` lokal scripted, prod'da backend'e POST atacak.
- Ürün görselleri için monospace etiketli placeholder (gerçek resim yok).
- Mobile responsive; 900px breakpoint.
- `localStorage` ile sepet kalıcı.

## Build

```bash
npm run build
# dist/smartcommerce/ → statik host'a deploy
```
