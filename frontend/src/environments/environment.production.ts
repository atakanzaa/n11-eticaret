/**
 * Production environment configuration.
 *
 * `apiBaseUrl` should point at the gateway behind the public domain — when the
 * frontend is served from `smartcommerce.example` the gateway is expected at
 * `api.smartcommerce.example`. Override at deploy-time if the convention differs.
 */
export const environment = {
  production: true,
  // Empty string → http calls hit relative `/api/...` paths and are proxied
  // by nginx (see infra/deploy/nginx-smartcommerce.conf) to api-gateway:8080
  // on the same host. Override only when the gateway lives on a different
  // origin from the frontend.
  apiBaseUrl: '',
  installments: [1, 2, 3, 6, 9, 12],
  iyzicoSandbox: false,
} as const;
