/**
 * Production environment.
 * Use relative paths when frontend and backend are served from the same origin (e.g. reverse proxy).
 * Override at build time via file replacement or set absolute URLs for a separate API host.
 */
export const environment = {
  production: true,
  apiUrl: '/api',
  wsUrl: '/ws',
};
