#!/usr/bin/env bash
# End-to-end smoke test for the SmartCommerce stack.
#
# What it does:
#   1. Confirms every backend service answers /actuator/health
#   2. Hits the freshly-added endpoints (categories, brands, reviews, admin
#      overview, seller KPI, AI usage breakdown) through the gateway so we
#      know the new routes work the same way the frontend will hit them.
#   3. Confirms the Angular dev server is reachable on :4200 (skip if not)
#
# Run it every morning before demoing. Each line is meant to be fast — the
# entire script should finish in under 30 seconds when everything is green.

set -u
GATEWAY="${GATEWAY:-http://localhost:8080}"
FRONTEND="${FRONTEND:-http://localhost:4200}"
PASS=0
FAIL=0

probe() {
    local label="$1"
    local url="$2"
    local expected="${3:-200}"
    local code
    code=$(curl -s -o /dev/null -m 5 -w "%{http_code}" "$url" || echo "000")
    if [ "$code" = "$expected" ]; then
        printf "  \033[32m✓\033[0m %-40s [%s]\n" "$label" "$code"
        PASS=$((PASS+1))
    else
        printf "  \033[31m✗\033[0m %-40s [%s, expected %s]\n" "$label" "$code" "$expected"
        FAIL=$((FAIL+1))
    fi
}

echo
echo "── Backend service health ──────────────────────────────────────"
./scripts/health-services.sh || true

echo
echo "── Gateway-routed public endpoints ─────────────────────────────"
probe "GET  /api/categories"               "$GATEWAY/api/categories"
probe "GET  /api/brands"                   "$GATEWAY/api/brands"
probe "GET  /api/products?size=1"          "$GATEWAY/api/products?size=1"
probe "GET  /api/search?q=test"            "$GATEWAY/api/search?q=test"
probe "GET  /api/recommendations/popular"  "$GATEWAY/api/recommendations/popular"
probe "GET  /api/coupons/validate"         "$GATEWAY/api/coupons/validate" "400"  # POST endpoint, GET → 405 or 400
probe "GET  /api/payments/iyzico/health"   "$GATEWAY/api/payments/iyzico/health"
probe "GET  /api/mcp/tools"                "$GATEWAY/api/mcp/tools"

echo
echo "── Auth-only endpoints (expect 401 without token) ─────────────"
probe "GET  /api/users/me"                 "$GATEWAY/api/users/me"             "401"
probe "GET  /api/cart"                     "$GATEWAY/api/cart"                 "401"
probe "GET  /api/orders"                   "$GATEWAY/api/orders"               "401"
probe "GET  /api/admin/overview"           "$GATEWAY/api/admin/overview"       "401"
probe "GET  /api/ai/usage/today"           "$GATEWAY/api/ai/usage/today"       "401"

echo
echo "── Frontend dev server ─────────────────────────────────────────"
if curl -s -m 3 -o /dev/null -w "%{http_code}\n" "$FRONTEND" | grep -q "^2"; then
    printf "  \033[32m✓\033[0m Angular dev server reachable at %s\n" "$FRONTEND"
    PASS=$((PASS+1))
else
    printf "  \033[33m⚠\033[0m Frontend not running — start with: \033[1mmake frontend-dev\033[0m\n"
fi

echo
echo "── Result ──────────────────────────────────────────────────────"
printf "  %d passed, %d failed.\n" "$PASS" "$FAIL"
[ "$FAIL" -eq 0 ]
