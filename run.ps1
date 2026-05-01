<#
.SYNOPSIS
    SmartCommerce projesi icin Windows PowerShell yonetim scripti.
    Makefile'in tum islevselligini PowerShell'de karsilar.

.EXAMPLE
    .\run.ps1 help
    .\run.ps1 up-all
    .\run.ps1 rebuild-service order-service
#>

param(
    [Parameter(Position = 0)]
    [string]$Command = "help",

    [Parameter(Position = 1)]
    [string]$ServiceName
)

$ErrorActionPreference = "Stop"

# ── Otomatik PATH ayarlama (Java 21 + Maven) ─────────────────────
# Her yeni PowerShell oturumunda PATH'e eklenmesi gereken dizinleri otomatik ekler.
$jdkDir = Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory -Filter "jdk-21*" -ErrorAction SilentlyContinue | Select-Object -First 1
if ($jdkDir) {
    if (-not $env:JAVA_HOME -or $env:JAVA_HOME -notmatch "jdk-21") {
        $env:JAVA_HOME = $jdkDir.FullName
    }
    if ($env:PATH -notmatch [regex]::Escape($jdkDir.FullName)) {
        $env:PATH = "$($jdkDir.FullName)\bin;$env:PATH"
    }
}

$mvnDir = Get-ChildItem "C:\ProgramData\chocolatey\lib\maven" -Directory -Filter "apache-maven-*" -ErrorAction SilentlyContinue | Select-Object -First 1
if ($mvnDir) {
    $mvnBin = Join-Path $mvnDir.FullName "bin"
    if ($env:PATH -notmatch [regex]::Escape($mvnBin)) {
        $env:PATH += ";$mvnBin"
    }
}

# ── Renkli cikti yardimcilari ──────────────────────────────────────
function Write-OK    { param([string]$msg) Write-Host "  [OK]  $msg" -ForegroundColor Green }
function Write-Fail  { param([string]$msg) Write-Host "  [X]   $msg" -ForegroundColor Red }
function Write-Info  { param([string]$msg) Write-Host "  [*]   $msg" -ForegroundColor Cyan }
function Write-Title { param([string]$msg) Write-Host "`n=== $msg ===" -ForegroundColor Yellow }

# ── Onkosul kontrolleri ────────────────────────────────────────────
function Assert-Docker {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        Write-Fail "docker bulunamadi. Docker Desktop kurulu ve calisir durumda olmali."; exit 1
    }
    docker info *>$null
    if ($LASTEXITCODE -ne 0) { Write-Fail "Docker daemon calismiyorsa Docker Desktop'i baslatiniz."; exit 1 }
}

function Assert-Maven {
    if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
        Write-Fail "mvn (Maven) bulunamadi."
        Write-Host "  Kurmak icin:  choco install maven -y" -ForegroundColor Yellow
        exit 1
    }
}

# ── Altyapi (Infrastructure) ──────────────────────────────────────
function Start-Infra {
    Write-Title "Altyapi baslatiliyor (postgres, redis, kafka, rabbitmq, opensearch)"
    Assert-Docker
    docker-compose up -d
    if ($LASTEXITCODE -ne 0) { Write-Fail "docker-compose up basarisiz"; exit 1 }
    Write-Info "Servislerin ayaga kalkmasi bekleniyor (15 sn)..."
    Start-Sleep -Seconds 15
    Test-InfraHealth
}

function Stop-Infra {
    Write-Title "Altyapi durduruluyor"
    Assert-Docker
    docker-compose down
}

# ── Build ──────────────────────────────────────────────────────────
function Invoke-Build {
    Write-Title "Maven Build (JAR'lar uretiliyor)"
    Assert-Maven
    mvn clean install --% -DskipTests
    if ($LASTEXITCODE -ne 0) { Write-Fail "Maven build basarisiz"; exit 1 }
    Write-OK "Build tamamlandi"
}

function Invoke-BuildServices {
    Write-Title "Servisler derleniyor + Docker image'lari olusturuluyor"
    Assert-Maven
    Assert-Docker
    mvn clean package --% -Dmaven.test.skip=true
    if ($LASTEXITCODE -ne 0) { Write-Fail "Maven package basarisiz"; exit 1 }
    docker-compose -f docker-compose.services.yml build
    if ($LASTEXITCODE -ne 0) { Write-Fail "Docker build basarisiz"; exit 1 }
    Write-OK "Servis image'lari hazir"
}

function Invoke-Test {
    Write-Title "Testler calistiriliyor"
    Assert-Maven
    mvn verify
}

function Invoke-Clean {
    Write-Title "Temizlik yapiliyor"
    Assert-Maven
    Assert-Docker
    mvn clean
    docker-compose down -v
    docker-compose -f docker-compose.services.yml down -v 2>$null
    docker-compose -f docker-compose.observability.yml down -v 2>$null
    Write-OK "Temizlik tamamlandi"
}

# ── Uygulama Servisleri (18 servis) ───────────────────────────────
function Start-Services {
    Write-Title "18 uygulama servisi baslatiliyor"
    Assert-Docker
    docker-compose -f docker-compose.services.yml up -d
    if ($LASTEXITCODE -ne 0) { Write-Fail "Servisler baslatilamadi"; exit 1 }
    Write-OK "Tum 18 servis baslatildi. Log icin: .\run.ps1 logs-services"
}

function Stop-Services {
    Write-Title "Uygulama servisleri durduruluyor"
    Assert-Docker
    docker-compose -f docker-compose.services.yml down
}

function Show-ServiceLogs {
    Assert-Docker
    docker-compose -f docker-compose.services.yml logs -f
}

function Invoke-RebuildService {
    param([string]$Svc)
    if (-not $Svc) {
        Write-Fail "Servis adi belirtilmedi. Kullanim: .\run.ps1 rebuild-service order-service"
        exit 1
    }
    Write-Title "$Svc yeniden derleniyor"
    Assert-Maven
    Assert-Docker
    & mvn "-pl" "services/$Svc" "-am" "package" "-DskipTests"
    if ($LASTEXITCODE -ne 0) { Write-Fail "Maven build basarisiz: $Svc"; exit 1 }
    docker-compose -f docker-compose.services.yml up -d --build --no-deps $Svc
    if ($LASTEXITCODE -ne 0) { Write-Fail "Docker rebuild basarisiz: $Svc"; exit 1 }
    Write-OK "$Svc yeniden baslatildi"
}

# ── Observability ─────────────────────────────────────────────────
function Start-Observability {
    Write-Title "Observability stack baslatiliyor (prometheus, grafana, loki, tempo)"
    Assert-Docker
    docker-compose -f docker-compose.observability.yml up -d
    if ($LASTEXITCODE -ne 0) { Write-Fail "Observability baslatma basarisiz"; exit 1 }
    Write-OK "Observability stack hazir"
}

function Stop-Observability {
    Write-Title "Observability stack durduruluyor"
    Assert-Docker
    docker-compose -f docker-compose.observability.yml down
}

# ── All-in-one ─────────────────────────────────────────────────────
function Start-All {
    Write-Title "TUM STACK BASLATILIYOR"
    Start-Infra
    Invoke-BuildServices
    Start-Services
    Start-Observability

    Write-Host ""
    Write-Host "================================================" -ForegroundColor Green
    Write-Host "  Her sey hazir! Servislerin register olmasi"     -ForegroundColor Green
    Write-Host "  icin ~60 saniye bekleyin."                      -ForegroundColor Green
    Write-Host "================================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "  API Gateway:  http://localhost:8080"
    Write-Host "  Grafana:      http://localhost:3000  (admin/admin)"
    Write-Host "  Prometheus:   http://localhost:9090"
    Write-Host "  Kafka UI:     http://localhost:8091"
    Write-Host "  RabbitMQ UI:  http://localhost:15672 (smartcommerce/smartcommerce)"
    Write-Host "  pgAdmin:      http://localhost:5050  (admin@smartcommerce.local/admin)"
    Write-Host ""
}

function Stop-All {
    Write-Title "TUM STACK DURDURULUYOR"
    Assert-Docker
    docker-compose -f docker-compose.services.yml down 2>$null
    docker-compose -f docker-compose.observability.yml down 2>$null
    docker-compose down
    Write-OK "Her sey durduruldu"
}

# ── Kafka Topic Olusturma ─────────────────────────────────────────
function New-KafkaTopics {
    Write-Title "Kafka topic'leri olusturuluyor (43 topic + DLQ'lar)"
    Assert-Docker

    $kafkaContainer = "smartcommerce-kafka"
    $broker         = "kafka:29092"

    $topics = @(
        "user.registered.v1",
        "user.profile-updated.v1",
        "user.deleted.v1",
        "seller.registered.v1",
        "seller.status-changed.v1",
        "product.created.v1",
        "product.updated.v1",
        "product.deleted.v1",
        "offer.created.v1",
        "offer.price-changed.v1",
        "offer.stock-changed.v1",
        "offer.status-changed.v1",
        "inventory.reserved.v1",
        "inventory.reservation-failed.v1",
        "inventory.released.v1",
        "inventory.confirmed.v1",
        "inventory.low-stock.v1",
        "order.created.v1",
        "order.confirmed.v1",
        "order.cancelled.v1",
        "order.expired.v1",
        "order.fraud-flagged.v1",
        "payment.initiated.v1",
        "payment.succeeded.v1",
        "payment.failed.v1",
        "payment.refunded.v1",
        "shipment.created.v1",
        "shipment.dispatched.v1",
        "shipment.delivered.v1",
        "shipment.failed.v1",
        "return.requested.v1",
        "return.approved.v1",
        "return.rejected.v1",
        "return.refunded.v1",
        "return.completed.v1",
        "cart.item-added.v1",
        "cart.checkout-started.v1",
        "cart.abandoned.v1",
        "review.created.v1",
        "review.updated.v1",
        "review.deleted.v1",
        "review.approved.v1",
        "review.rejected.v1"
    )

    foreach ($topic in $topics) {
        docker exec $kafkaContainer kafka-topics `
            --bootstrap-server $broker `
            --create --if-not-exists `
            --topic $topic `
            --partitions 3 `
            --replication-factor 1 2>$null

        docker exec $kafkaContainer kafka-topics `
            --bootstrap-server $broker `
            --create --if-not-exists `
            --topic "$topic.dlq" `
            --partitions 1 `
            --replication-factor 1 2>$null

        Write-OK "$topic  (+dlq)"
    }

    Write-Host ""
    Write-Info "Mevcut topic listesi:"
    docker exec $kafkaContainer kafka-topics --bootstrap-server $broker --list
}

# ── Health Check: Altyapi ─────────────────────────────────────────
function Test-InfraHealth {
    Write-Title "Altyapi saglik kontrolu"

    $checks = @(
        @{ Name = "PostgreSQL"; Cmd = "docker exec smartcommerce-postgres pg_isready -U smartcommerce" },
        @{ Name = "Redis";      Cmd = "docker exec smartcommerce-redis redis-cli ping" },
        @{ Name = "Kafka";      Cmd = "docker exec smartcommerce-kafka kafka-topics --bootstrap-server localhost:9092 --list" },
        @{ Name = "RabbitMQ";   Cmd = "docker exec smartcommerce-rabbitmq rabbitmq-diagnostics ping" }
    )

    foreach ($c in $checks) {
        Invoke-Expression $c.Cmd *>$null
        if ($LASTEXITCODE -eq 0) { Write-OK $c.Name }
        else                     { Write-Fail "$($c.Name) (FAILED)" }
    }

    # OpenSearch - HTTP kontrolu
    try {
        $resp = Invoke-WebRequest -Uri "http://localhost:9200/_cluster/health" -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
        if ($resp.StatusCode -eq 200) { Write-OK "OpenSearch" }
        else { Write-Fail "OpenSearch (HTTP $($resp.StatusCode))" }
    }
    catch {
        Write-Fail "OpenSearch (UNREACHABLE)"
    }

    Write-Host ""
}

# ── Health Check: 18 Servis ───────────────────────────────────────
function Test-ServiceHealth {
    Write-Title "Servis saglik kontrolu (18 servis)"

    $services = @(
        @{ Name = "api-gateway";             Port = 8080 },
        @{ Name = "auth-service";            Port = 8081 },
        @{ Name = "user-service";            Port = 8082 },
        @{ Name = "seller-service";          Port = 8083 },
        @{ Name = "product-service";         Port = 8084 },
        @{ Name = "inventory-service";       Port = 8085 },
        @{ Name = "catalog-service";         Port = 8086 },
        @{ Name = "recommendation-service";  Port = 8087 },
        @{ Name = "cart-service";            Port = 8088 },
        @{ Name = "order-service";           Port = 8089 },
        @{ Name = "payment-service";         Port = 8090 },
        @{ Name = "shipment-service";        Port = 8092 },
        @{ Name = "promotion-service";       Port = 8093 },
        @{ Name = "notification-service";    Port = 8094 },
        @{ Name = "fraud-detection-service"; Port = 8095 },
        @{ Name = "return-service";          Port = 8096 },
        @{ Name = "ai-orchestrator";         Port = 8098 }
    )

    $failed = 0
    foreach ($svc in $services) {
        $url = "http://localhost:$($svc.Port)/actuator/health"
        try {
            $resp = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 3 -ErrorAction Stop
            if ($resp.StatusCode -eq 200) {
                Write-OK  ("{0,-30} :{1}" -f $svc.Name, $svc.Port)
            }
            else {
                Write-Fail ("{0,-30} :{1}  (HTTP {2})" -f $svc.Name, $svc.Port, $resp.StatusCode)
                $failed++
            }
        }
        catch {
            Write-Fail ("{0,-30} :{1}  (UNREACHABLE)" -f $svc.Name, $svc.Port)
            $failed++
        }
    }

    Write-Host ""
    if ($failed -eq 0) {
        Write-OK "Tum 18 servis saglikli."
    }
    else {
        Write-Fail "$failed servis(ler) saglikli degil."
    }
}

# ── Loglar ─────────────────────────────────────────────────────────
function Show-InfraLogs {
    Assert-Docker
    docker-compose logs -f
}

# ── Frontend ───────────────────────────────────────────────────────
function Install-Frontend {
    Write-Title "Frontend bagimliliklari yukleniyor"
    Push-Location frontend
    npm install
    Pop-Location
}

function Start-FrontendDev {
    Write-Title "Frontend dev server baslatiliyor (http://localhost:4200)"
    Push-Location frontend
    npm start
    Pop-Location
}

function Invoke-FrontendBuild {
    Write-Title "Frontend production build"
    Push-Location frontend
    npm run build:prod
    Pop-Location
}

# ── Yardim ─────────────────────────────────────────────────────────
function Show-Help {
    Write-Host ""
    Write-Host "SmartCommerce - Windows Yonetim Scripti" -ForegroundColor Cyan
    Write-Host "=======================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "-- Altyapi (postgres, redis, kafka, rabbitmq, opensearch) --" -ForegroundColor Yellow
    Write-Host "  .\run.ps1 up                - Altyapiyi baslat"
    Write-Host "  .\run.ps1 down              - Altyapiyi durdur"
    Write-Host "  .\run.ps1 health            - Altyapi saglik kontrolu"
    Write-Host "  .\run.ps1 create-topics     - Kafka topic'lerini olustur"
    Write-Host "  .\run.ps1 logs              - Altyapi loglarini izle"
    Write-Host ""
    Write-Host "-- Uygulama Servisleri (18 servis) --" -ForegroundColor Yellow
    Write-Host "  .\run.ps1 build             - Maven build (JAR)"
    Write-Host "  .\run.ps1 build-services    - Maven package + Docker build"
    Write-Host "  .\run.ps1 up-services       - 18 servisi baslat"
    Write-Host "  .\run.ps1 down-services     - 18 servisi durdur"
    Write-Host "  .\run.ps1 logs-services     - Servis loglarini izle"
    Write-Host "  .\run.ps1 rebuild-service <isim>  - Tek servis rebuild"
    Write-Host "  .\run.ps1 health-services   - 18 servis saglik kontrolu"
    Write-Host ""
    Write-Host "-- Observability (prometheus, grafana, loki, tempo) --" -ForegroundColor Yellow
    Write-Host "  .\run.ps1 up-observability    - Monitoring baslat"
    Write-Host "  .\run.ps1 down-observability  - Monitoring durdur"
    Write-Host ""
    Write-Host "-- Hepsi Bir Arada --" -ForegroundColor Yellow
    Write-Host "  .\run.ps1 up-all            - Altyapi + build + servisler + monitoring"
    Write-Host "  .\run.ps1 down-all          - Her seyi durdur"
    Write-Host ""
    Write-Host "-- Frontend --" -ForegroundColor Yellow
    Write-Host "  .\run.ps1 frontend-install  - npm install"
    Write-Host "  .\run.ps1 frontend-dev      - Dev server (localhost:4200)"
    Write-Host "  .\run.ps1 frontend-build    - Production build"
    Write-Host ""
    Write-Host "-- Diger --" -ForegroundColor Yellow
    Write-Host "  .\run.ps1 test              - Tum testleri calistir"
    Write-Host "  .\run.ps1 clean             - Build temizle + volume sil"
    Write-Host ""
}

# ── Ana switch ─────────────────────────────────────────────────────
switch ($Command.ToLower()) {
    "up"                 { Start-Infra }
    "down"               { Stop-Infra }
    "restart"            { Stop-Infra; Start-Infra }
    "build"              { Invoke-Build }
    "build-services"     { Invoke-BuildServices }
    "up-services"        { Start-Services }
    "down-services"      { Stop-Services }
    "logs-services"      { Show-ServiceLogs }
    "rebuild-service"    { Invoke-RebuildService -Svc $ServiceName }
    "health-services"    { Test-ServiceHealth }
    "up-observability"   { Start-Observability }
    "down-observability" { Stop-Observability }
    "up-all"             { Start-All }
    "down-all"           { Stop-All }
    "create-topics"      { New-KafkaTopics }
    "health"             { Test-InfraHealth }
    "logs"               { Show-InfraLogs }
    "test"               { Invoke-Test }
    "clean"              { Invoke-Clean }
    "frontend-install"   { Install-Frontend }
    "frontend-dev"       { Start-FrontendDev }
    "frontend-build"     { Invoke-FrontendBuild }
    "help"               { Show-Help }
    default {
        Write-Fail "Bilinmeyen komut: '$Command'"
        Write-Host "  Kullanim icin: .\run.ps1 help" -ForegroundColor Yellow
        exit 1
    }
}
