# SmartCommerce

> Türkiye pazarına yönelik, çok satıcılı, gerçek **3D Secure ödeme** ve **AI alışveriş asistanı** içeren e-ticaret platformu.
>
> 17 mikroservis · Spring Boot 3 · Angular 17 · Kafka · PostgreSQL · Iyzico · GCE üzerinde canlı.

**Canlı demo:** http://34.76.197.240/
**Repo:** https://github.com/atakanzaa/n11-eticaret

---

## İçindekiler

- [Öne çıkan özellikler](#öne-çıkan-özellikler)
- [Mimari](#mimari)
- [Teknoloji yığını](#teknoloji-yığını)
- [Mikroservisler](#mikroservisler)
- [Hızlı başlangıç (lokal)](#hızlı-başlangıç-lokal)
- [Geliştirme komutları](#geliştirme-komutları)
- [Deploy (GCE)](#deploy-gce)
- [Gözlemlenebilirlik](#gözlemlenebilirlik)
- [Test kartları](#test-kartları)
- [Proje yapısı](#proje-yapısı)

---

## Öne çıkan özellikler

### Alıcı (Buyer)
- Anasayfa, kategori bazlı vitrin, OpenSearch tabanlı tam-metin arama
- Ürün detay: çoklu satıcı paneli, kampanyalar, marka, attribute, **yıldızlı yorumlar**
- 3 adımlı **checkout**: adres → kargo → ödeme
- **Iyzico 3D Secure** sandbox entegrasyonu (HMAC-SHA256 doğrulamalı callback)
- Sipariş takibi (timeline), iade akışı, "Bu ürünü değerlendir" otomatik form açma
- **AI alışveriş asistanı** (Anthropic / OpenAI / Gemini arasında geçişli)

### Satıcı (Seller)
- Dashboard: gelir, sipariş sayısı, en çok satan ürünler
- Ürün ekleme (marka + kategori + attribute + görsel + ilk stok), teklif yönetimi
- Sipariş yaşam döngüsü (PENDING → CONFIRMED → SHIPPED → DELIVERED)
- Mağazaya özel **zaman sınırlı, kullanım sınırlı kuponlar**
- Yorum yanıtlama

### Admin
- Genel bakış metrikleri
- Kategori ağacı yönetimi
- **Fraud Detection** kararları (4 kural: HighAmount, Velocity, NewUserHighAmount, Blacklist)
- Kampanya yönetimi
- Yorum moderasyonu, raporlanan yorumlar
- AI provider kullanım & maliyet takibi (günlük bütçe limiti ile)

### Platform / DevOps
- Tek komutla ayağa kalkan **Docker Compose** ortamı
- **GitHub Actions → SSH → GCE VM** auto-deploy
- **Prometheus + Grafana + Tempo + Loki** (Promtail ile log shipping)
- JWT tabanlı kimlik (`api-gateway` global filter ile)
- Kafka event bus (sipariş, ödeme, kupon, fraud event'leri için)

---

## Mimari

```
                    ┌──────────────────────────────┐
   Browser ─────────▶  Nginx :80 (GCE VM)          │
   (Angular SPA)    │  - SPA static dosyaları      │
                    │  - /api/* → 127.0.0.1:8080   │
                    └──────────────┬───────────────┘
                                   │
                    ┌──────────────▼───────────────┐
                    │  api-gateway :8080            │
                    │  - JWT global filter          │
                    │  - Public path allowlist      │
                    │  - Path-aware CORS            │
                    └──────────────┬───────────────┘
                                   │ routes
        ┌─────────┬─────────┬──────┴──────┬─────────┬─────────┐
        ▼         ▼         ▼             ▼         ▼         ▼
   auth-svc  user-svc  product-svc  order-svc  payment-svc  fraud-svc  ...
        │         │         │             │         │
        └─────────┴────┬────┴─────────────┴─────────┘
                       │
        ┌──────────────┼──────────────┬──────────────┬──────────────┐
        ▼              ▼              ▼              ▼              ▼
   PostgreSQL       Redis           Kafka       OpenSearch      RabbitMQ
   (per-svc DB)   (cache,           (event       (search       (notification
                   sessions)         bus)         index)        queue)
```

### Önemli akışlar

- **Checkout & 3DS:** order-service → payment-service → Iyzico API → 3DS HTML → kullanıcı bankası → `POST /api/payments/iyzico/callback` → HMAC doğrula → 303 redirect → frontend sonuç sayfası
- **Fraud:** order-service `pending` durumunda fraud-service'e senkron çağrı yapıyor → 4 kural çalışıyor → APPROVED / REVIEW / DECLINED kararı dönüyor
- **AI:** ai-orchestrator provider-agnostic adapter pattern; `AI_PROVIDER` env var ile sağlayıcı seçilir, kullanım & maliyet `ai_usage` tablosunda tutulur

---

## Teknoloji yığını

| Katman | Stack |
| --- | --- |
| Frontend | Angular 17 (standalone, signals, `@if`/`@for`), SCSS |
| Backend | Java 21, Spring Boot 3, Spring Cloud Gateway, JPA |
| Veri | PostgreSQL 15 (her servise kendi şema), Redis 7 |
| Mesajlaşma | Apache Kafka, RabbitMQ |
| Arama | OpenSearch |
| Mail | Mailpit (dev) |
| Ödeme | Iyzico Sandbox (3D Secure + HMAC-SHA256 callback) |
| AI | Anthropic Claude / OpenAI / Google Gemini (provider-pluggable) |
| Observability | Prometheus, Grafana, Tempo (tracing), Loki + Promtail (logs) |
| CI/CD | GitHub Actions + SSH deploy |
| Container | Docker Compose |

---

## Mikroservisler

| Servis | Port | Sorumluluk |
| --- | --- | --- |
| `api-gateway` | 8080 | Tek giriş noktası, JWT, CORS, rate limit |
| `auth-service` | 8081 | Register, login, refresh token, password change |
| `user-service` | 8082 | Profil, adres, favoriler |
| `seller-service` | 8083 | Satıcı CRUD, mağaza |
| `product-service` | 8084 | Ürün, marka, kategori, **yorum lifecycle** (CRUD + moderasyon + replies + reports) |
| `inventory-service` | 8085 | Stok, rezervasyon |
| `catalog-service` | 8086 | OpenSearch arama indeksi |
| `recommendation-service` | 8087 | Popüler & kişisel öneriler |
| `cart-service` | 8088 | Sepet (Redis) |
| `order-service` | 8089 | Sipariş, checkout |
| `payment-service` | 8090 | Iyzico 3DS, webhook event log, payment retry |
| `shipment-service` | 8092 | Kargo takibi |
| `promotion-service` | 8093 | Zaman sınırlı kampanyalar, kuponlar |
| `notification-service` | 8094 | E-posta + uygulama içi bildirim |
| `fraud-detection-service` | 8095 | Kural tabanlı fraud (4 rule) |
| `return-service` | 8096 | İade akışı |
| `ai-orchestrator` | 8098 | Multi-provider AI proxy + token/maliyet logging |

---

## Hızlı başlangıç (lokal)

### Önkoşullar
- **Java 21** (Temurin önerilir)
- **Maven 3.9+**
- **Docker Desktop** (Compose v2 dahili)
- **Node.js 20+** + **npm**

### 1. Repo'yu klonla
```bash
git clone https://github.com/atakanzaa/n11-eticaret.git
cd n11-eticaret
```

### 2. Ortam değişkenlerini ayarla
```bash
cp .env.example .env
# .env dosyasını aç:
#   IYZICO_API_KEY ve IYZICO_SECRET_KEY → Iyzico sandbox merchant'tan al
#   AI_API_KEY     → Anthropic / OpenAI / Gemini birinden al
#   JWT_SECRET     → openssl rand -base64 64 ile yeni üret
```

### 3. Tek komutla her şeyi ayağa kaldır
```bash
make up-all
```
Bu komut sırasıyla şunları yapar:
- Postgres + Redis + Kafka + RabbitMQ + OpenSearch (`docker-compose.yml`)
- Tüm 17 servisi build edip başlatır (`docker-compose.services.yml`)
- Prometheus + Grafana + Tempo + Loki (`docker-compose.observability.yml`)

### 4. Frontend dev server'ını başlat (ayrı terminalde)
```bash
make frontend-dev
# http://localhost:4200
```

### 5. Smoke test
```bash
make health-services    # tüm servislerin /actuator/health'lerini ping'le
./scripts/smoke-fullstack.sh
```

> **İpucu:** Backend + frontend'i tek komutta ayağa kaldırmak için `make up-fullstack`.

---

## Geliştirme komutları

```bash
# Sadece infra (Postgres + Kafka + Redis...)
make up                       # başlat
make down                     # durdur
make restart                  # restart

# Servisler
make build-services           # mvn package + docker compose build
make up-services              # 17 servisi başlat
make rebuild-service SVC=order-service   # tek servisi yeniden build & restart
make logs-services            # tail logs

# Test
make test                     # mvn verify (unit testler)

# Frontend
make frontend-install
make frontend-dev             # http://localhost:4200
make frontend-build           # production build → dist/

# Temizlik
make clean                    # build + tüm volume'leri sil
```

---

## Deploy (GCE)

`main` branch'ine her push, `.github/workflows/deploy-demo.yml` workflow'unu tetikler. Workflow şunları yapar:

1. SSH ile GCE VM'e bağlanır (`secrets.GCE_HOST`, `GCE_USER`, `GCE_SSH_KEY`)
2. `git fetch && git reset --hard origin/main`
3. **VM `.env` dosyasındaki `FRONTEND_URL` ve `APP_ALLOWED_ORIGINS`'i public host ile günceller** (idempotent)
4. `mvn -DskipTests clean package`
5. `docker compose ... up -d --build --force-recreate` (env propagation için zorunlu recreate)
6. Frontend'i `npm run build:prod` ile derler, `/var/www/smartcommerce/`'ye kopyalar
7. nginx reload

VM'de servisleri görmek için:
```bash
ssh ubuntu@34.76.197.240 'docker compose -f ~/n11-eticaret/docker-compose.services.yml ps'
```

---

## Gözlemlenebilirlik

| Tool | URL (lokal) | Amaç |
| --- | --- | --- |
| **Grafana** | http://localhost:3000 (admin/admin) | Dashboard'lar, log arama, trace görüntüleme |
| **Prometheus** | http://localhost:9090 | Metrik scrape & alert |
| **Tempo** | (Grafana datasource) | Distributed tracing (OTLP) |
| **Loki** | (Grafana datasource) | Log aggregation (Promtail ile shipping) |
| **Kafka UI** | http://localhost:8091 | Topic & consumer takibi |
| **RabbitMQ UI** | http://localhost:15672 (smartcommerce/smartcommerce) | Queue takibi |
| **pgAdmin** | http://localhost:5050 (admin@smartcommerce.local/admin) | DB inceleme |
| **Mailpit** | http://localhost:8025 | Gönderilen e-postaları görüntüle |

Tüm servisler `/actuator/prometheus` ile metric expose eder; tracing için OTLP endpoint `OTLP_ENDPOINT` env'inde tanımlı.

---

## Test kartları

Iyzico sandbox akışını denerken kullanılabilecek hazır kartlar:

| Kart No | Sonuç |
| --- | --- |
| `5528 7900 0000 0008` | Başarılı 3DS doğrulama |
| `4543 6000 0000 0006` | Başarılı 3DS doğrulama |
| `5400 0100 0000 0008` | 3DS başarısız (negatif test) |

- **CVV:** `123`
- **Son Kullanma:** `12/30` (gelecekte herhangi bir tarih)
- **3DS şifresi:** `123456`

---

## Proje yapısı

```
n11-eticaret/
├── frontend/                    # Angular 17 monorepo
│   └── src/app/features/
│       ├── buyer/               # Anasayfa, ürün, sepet, checkout, ödeme, hesap
│       ├── seller/              # Dashboard, ürünler, siparişler, kuponlar
│       └── admin/               # Genel bakış, fraud, moderasyon, AI usage
│
├── services/                    # 17 mikroservis
│   ├── api-gateway/
│   ├── auth-service/
│   ├── ...
│   └── ai-orchestrator/
│
├── shared/                      # Servisler arası paylaşılan kod
│   ├── common-security/         # JWT filter, SecurityConfig
│   ├── common-web/              # GlobalExceptionHandler, CorrelationIdFilter
│   └── common-errors/
│
├── infra/
│   ├── deploy/                  # nginx config + seed scripts
│   ├── kafka/                   # Topic bootstrap script
│   ├── observability/           # Prometheus, Grafana, Tempo, Loki yapılandırmaları
│   └── postgres/                # Init SQL'leri
│
├── docker-compose.yml           # Infra (Postgres, Redis, Kafka, RabbitMQ, OpenSearch)
├── docker-compose.services.yml  # 17 application servisi
├── docker-compose.observability.yml
├── Makefile                     # Geliştirme komutları
└── .github/workflows/
    └── deploy-demo.yml          # GCE auto-deploy
```

---

## Lisans

Akademik / portföy projesi olarak geliştirildi. Iyzico API anahtarları ve diğer secret'ler `.env` üzerinden yönetilir; **`.env` dosyası asla commit'lenmez** (`.gitignore`'da).
