import { fallbackCoverImages } from '@/config/media';

const LOCAL_MINIO_HOSTS = new Set(['127.0.0.1:9000', 'localhost:9000']);

export function normalizeProductImage(src?: string | null, fallback = fallbackCoverImages[0]) {
  if (!src || src.includes('static.velocitymall.local')) {
    return fallback;
  }

  try {
    const url = new URL(src);
    if (LOCAL_MINIO_HOSTS.has(url.host)) {
      return `/minio${url.pathname}`;
    }
  } catch {
    // Relative paths are already suitable for the current deployment origin.
  }

  return src;
}
