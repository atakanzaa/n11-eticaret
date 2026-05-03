#!/usr/bin/env bash
# One-time bootstrap for an Ubuntu 22.04/24.04 GCE VM (e2-standard-8 önerilen).
# Çalıştırma: ssh ubuntu@VM_IP, sonra: bash bootstrap-vm.sh
# Idempotent — birden fazla kez çalıştırmak güvenli.
set -euo pipefail

REPO_URL="${REPO_URL:-https://github.com/atakanzaa/n11-eticaret.git}"
REPO_DIR="${HOME}/n11-eticaret"
NGINX_SITE="/etc/nginx/sites-available/smartcommerce"
WWW_DIR="/var/www/smartcommerce"

echo "==> apt update + base tools"
sudo apt-get update -y
sudo apt-get install -y ca-certificates curl gnupg git unzip nginx ufw jq

echo "==> Java 21 (Temurin)"
if ! java -version 2>&1 | grep -q '21\.'; then
    sudo apt-get install -y wget apt-transport-https
    sudo mkdir -p /etc/apt/keyrings
    wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo gpg --dearmor -o /etc/apt/keyrings/adoptium.gpg
    echo "deb [signed-by=/etc/apt/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb $(. /etc/os-release; echo $VERSION_CODENAME) main" | sudo tee /etc/apt/sources.list.d/adoptium.list >/dev/null
    sudo apt-get update -y
    sudo apt-get install -y temurin-21-jdk
fi

echo "==> Maven"
sudo apt-get install -y maven

echo "==> Node 20 (NodeSource)"
if ! node -v 2>/dev/null | grep -q '^v20\.'; then
    curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
    sudo apt-get install -y nodejs
fi

echo "==> Docker Engine + Compose plugin"
if ! command -v docker >/dev/null 2>&1; then
    sudo install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    sudo chmod a+r /etc/apt/keyrings/docker.gpg
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release; echo $VERSION_CODENAME) stable" | sudo tee /etc/apt/sources.list.d/docker.list >/dev/null
    sudo apt-get update -y
    sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
    sudo usermod -aG docker "$USER"
fi

echo "==> Repo clone"
if [[ ! -d "$REPO_DIR/.git" ]]; then
    git clone "$REPO_URL" "$REPO_DIR"
fi

echo "==> .env (skeleton — gerçek değerleri elle düzenle)"
if [[ ! -f "$REPO_DIR/.env" ]]; then
    cat > "$REPO_DIR/.env" <<'EOF'
# Production secrets — bu dosya git'e gitmesin!
JWT_SECRET=change-me-please-256-bit-min-32-chars
POSTGRES_PASSWORD=postgres
GEMINI_API_KEY=
TRACING_SAMPLE=0.1
EOF
    chmod 600 "$REPO_DIR/.env"
    echo "    >>> $REPO_DIR/.env oluşturuldu — JWT_SECRET ve diğer değerleri düzenle!"
fi

echo "==> Nginx site config"
sudo cp "$REPO_DIR/infra/deploy/nginx-smartcommerce.conf" "$NGINX_SITE"
sudo ln -sf "$NGINX_SITE" /etc/nginx/sites-enabled/smartcommerce
sudo rm -f /etc/nginx/sites-enabled/default
sudo mkdir -p "$WWW_DIR"
sudo chown -R "$USER:$USER" "$WWW_DIR"
sudo nginx -t
sudo systemctl enable --now nginx
sudo systemctl reload nginx

echo "==> Sudoers — deploy user can reload nginx without password"
SUDOERS_LINE="$USER ALL=(ALL) NOPASSWD: /bin/systemctl reload nginx, /bin/systemctl restart nginx, /usr/bin/cp, /usr/bin/rm, /usr/bin/mkdir"
echo "$SUDOERS_LINE" | sudo tee /etc/sudoers.d/smartcommerce-deploy >/dev/null
sudo chmod 440 /etc/sudoers.d/smartcommerce-deploy

echo "==> UFW (firewall) — 22, 80, 8080, 3000, 8091, 15672, 5050, 8025"
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 8080/tcp   # api-gateway
sudo ufw allow 3000/tcp   # grafana
sudo ufw allow 8091/tcp   # kafka-ui
sudo ufw allow 15672/tcp  # rabbitmq mgmt
sudo ufw allow 5050/tcp   # pgadmin
sudo ufw allow 8025/tcp   # mailpit ui
sudo ufw --force enable

echo "==> Done."
echo
echo "Sıra:"
echo "  1) cd $REPO_DIR && nano .env       (gerçek secret'leri yaz)"
echo "  2) Docker grubu için: newgrp docker  ya da yeniden SSH"
echo "  3) GitHub'a SSH public key ekle:    cat ~/.ssh/id_rsa.pub"
echo "  4) GitHub deploy workflow'u manuel tetikle (Actions → Deploy Demo → Run workflow)"
