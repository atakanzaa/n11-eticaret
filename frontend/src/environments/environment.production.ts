/**
 * Production environment configuration.
 *
 * `apiBaseUrl` should point at the gateway behind the public domain — when the
 * frontend is served from `smartcommerce.example` the gateway is expected at
 * `api.smartcommerce.example`. Override at deploy-time if the convention differs.
 */
export const environment = {
  production: true,
  apiBaseUrl: 'https://api.smartcommerce.example',
  installments: [1, 2, 3, 6, 9, 12],
  iyzicoSandbox: false,
} as const;
