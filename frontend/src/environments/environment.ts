/**
 * Development environment.
 * API and WebSocket URLs point to local backend.
 */
export const environment = {
  production: false,
  /** Base URL for REST API (e.g. http://localhost:8080/api). */
  apiUrl: 'http://localhost:8080/api',
  // apiUrl: 'https://db8ldswv-8080.uks1.devtunnels.ms/api',
  /** WebSocket URL (e.g. http://localhost:8080/ws). Use path like /ws for same-origin in production. */
  wsUrl: 'http://localhost:8080/ws',
  // wsUrl: 'https://db8ldswv-8080.uks1.devtunnels.ms/ws',
};
