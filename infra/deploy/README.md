# Demo Deploy — GCE + GitHub Actions

1 haftalık demo için Google Cloud Compute Engine üzerinde tek VM, GitHub Actions ile push-to-deploy.

## Mimari

```
GitHub (push to main)
        │
        ▼
┌──────────────────────┐        ┌────────────────────────────────────────┐
│  GitHub Actions      │  SSH   │  GCE VM (e2-standard-8, Ubuntu 22.04)  │
│  • ci.yml (build)    │ ─────▶ │  • git pull + mvn package              │
│  • deploy-demo.yml   │        │  • docker compose up -d (3 stack)      │
└──────────────────────┘        │  • Angular build → /var/www/...        │
                                │  • nginx reload                        │
                                └────────────────────────────────────────┘
                                  ▲
                                  │ HTTP
                            kullanıcı (browser)
```

Erişim adresleri (VM external IP):

| URL | Servis |
|-----|--------|
| `http://VM_IP/` | Frontend (nginx) |
| `http://VM_IP/api/...` | API (nginx → api-gateway:8080) |
| `http://VM_IP:8080` | API Gateway direkt |
| `http://VM_IP:3000` | Grafana |
| `http://VM_IP:8091` | Kafka UI |
| `http://VM_IP:15672` | RabbitMQ Management |
| `http://VM_IP:5050` | pgAdmin |
| `http://VM_IP:8025` | Mailpit |

## 1. GCE VM oluştur

```bash
gcloud compute instances create smartcommerce-demo \
  --zone=europe-west1-b \
  --machine-type=e2-standard-8 \
  --image-family=ubuntu-2204-lts \
  --image-project=ubuntu-os-cloud \
  --boot-disk-size=100GB \
  --boot-disk-type=pd-balanced \
  --tags=http-server,smartcommerce \
  --metadata=enable-oslogin=FALSE
```

Firewall (gcloud CLI veya Console):

```bash
gcloud compute firewall-rules create allow-smartcommerce \
  --allow=tcp:22,tcp:80,tcp:8080,tcp:3000,tcp:8091,tcp:15672,tcp:5050,tcp:8025 \
  --target-tags=smartcommerce \
  --source-ranges=0.0.0.0/0
```

## 2. SSH key üret (lokal makinen)

```bash
ssh-keygen -t ed25519 -C "smartcommerce-deploy" -f ~/.ssh/smartcommerce_demo -N ""
```

Public key'i VM'e ekle:

```bash
gcloud compute ssh smartcommerce-demo --zone=europe-west1-b
# VM içinde:
mkdir -p ~/.ssh && chmod 700 ~/.ssh
cat >> ~/.ssh/authorized_keys <<EOF
<smartcommerce_demo.pub içeriği>
EOF
chmod 600 ~/.ssh/authorized_keys
```

Alternatif: `gcloud compute instances add-metadata smartcommerce-demo --metadata-from-file ssh-keys=...`

## 3. VM bootstrap (bir kerelik)

```bash
ssh -i ~/.ssh/smartcommerce_demo ubuntu@VM_IP
git clone https://github.com/atakanzaa/n11-eticaret.git
cd n11-eticaret
bash infra/deploy/bootstrap-vm.sh
# script biter — newgrp docker, sonra .env düzenle
nano .env   # JWT_SECRET, GEMINI_API_KEY vs.
```

`bootstrap-vm.sh` ne yapıyor:
- Java 21 + Maven + Node 20 + Docker + Compose plugin kur
- nginx config'i deploy et (SPA fallback + `/api/` proxy)
- UFW firewall aç
- Deploy user'a sudoers izinleri (nginx reload, www dizinine yazma)

## 4. GitHub Secrets

Repo → Settings → Secrets and variables → Actions → **New repository secret**:

| Secret | Değer |
|--------|-------|
| `GCE_HOST` | VM external IP |
| `GCE_USER` | `ubuntu` |
| `GCE_SSH_KEY` | `cat ~/.ssh/smartcommerce_demo` (private key, tüm içerik dahil) |

## 5. Deploy

İki yol:

**Otomatik**: `main` branch'e push at → `deploy-demo.yml` tetiklenir.

**Manuel**: GitHub → Actions → "Deploy Demo (GCE)" → Run workflow.

İlk deploy ~10-15 dk (Maven dependency download + 17 servis Docker build). Sonraki deploy'lar ~3-5 dk.

## 6. Smoke test

```bash
# Frontend
curl -I http://VM_IP/

# API gateway sağlık
curl http://VM_IP/api/actuator/health

# Servisler
ssh ubuntu@VM_IP 'docker compose -f ~/n11-eticaret/docker-compose.services.yml ps'
```

## 7. Demo bittikten sonra

```bash
gcloud compute instances delete smartcommerce-demo --zone=europe-west1-b
gcloud compute firewall-rules delete allow-smartcommerce
```

VM silinince ücret kesilmez, free credit korunur.

## Sorun giderme

- **`docker compose up` OOM**: VM e2-standard-4 ise yetmez, e2-standard-8'e geç. Postgres + Kafka + OpenSearch + 17 JVM = ~24-28 GB RAM.
- **GitHub Actions SSH timeout**: VM external IP değişti mi (stop/start sonrası değişir) — `GCE_HOST` secret'ini güncelle ya da static IP rezerve et (`gcloud compute addresses create`).
- **Kafka topic'leri yok**: `bash infra/kafka/create-topics.sh` elle koştur.
- **Frontend 404**: `ls /var/www/smartcommerce/` boşsa `npm run build:prod` adımı bozulmuş — Actions log'unu oku.
- **`502 Bad Gateway`**: api-gateway henüz ayağa kalkmamış — `docker compose -f docker-compose.services.yml logs api-gateway` ile kontrol.
