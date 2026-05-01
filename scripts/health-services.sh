#!/usr/bin/env bash
# Curl /actuator/health on every service. Returns non-zero if any are down.
set -u

services=(
  "api-gateway:8080"
  "auth-service:8081"
  "user-service:8082"
  "seller-service:8083"
  "product-service:8084"
  "inventory-service:8085"
  "catalog-service:8086"
  "recommendation-service:8087"
  "cart-service:8088"
  "order-service:8089"
  "payment-service:8090"
  "shipment-service:8092"
  "promotion-service:8093"
  "notification-service:8094"
  "fraud-detection-service:8095"
  "return-service:8096"
  "ai-orchestrator:8098"
)

failed=0
for entry in "${services[@]}"; do
  name="${entry%%:*}"
  port="${entry##*:}"
  status=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${port}/actuator/health" 2>/dev/null || echo "000")
  if [ "$status" = "200" ]; then
    printf "  ✓  %-30s :%s\n" "$name" "$port"
  else
    printf "  ✗  %-30s :%s  (HTTP %s)\n" "$name" "$port" "$status"
    failed=$((failed + 1))
  fi
done

echo ""
if [ "$failed" -eq 0 ]; then
  echo "All 18 services healthy."
else
  echo "$failed service(s) NOT healthy."
  exit 1
fi
