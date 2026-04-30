/**
 * Development environment configuration.
 *
 * `apiBaseUrl` is left empty so HttpClient calls go through the same origin —
 * `proxy.conf.json` rewrites `/api/**` to the gateway at `localhost:8080`,
 * sidestepping CORS preflight entirely during dev.
 *
 * For production, see `environment.production.ts` (chosen via fileReplacements
 * in angular.json's `production` configuration).
 */
export const environment = {
  production: false,
  apiBaseUrl: '',
  installments: [1, 2, 3, 6, 9, 12],
  iyzicoSandbox: true,
} as const;
