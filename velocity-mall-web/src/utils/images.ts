import { fallbackCoverImages } from '@/config/media';

const LOCAL_MINIO_HOSTS = new Set(['127.0.0.1:9000', 'localhost:9000']);
const baseUrl = import.meta.env.VITE_API_BASE_URL || '';

export function normalizeProductImage(src?: string | null, fallback = fallbackCoverImages[0]) {
  if (!src || src.includes('static.velocitymall.local')) {
    return baseUrl ? baseUrl + fallback : fallback;
  }

  try {
    const url = new URL(src);
    if (LOCAL_MINIO_HOSTS.has(url.host)) {
      return `${baseUrl}/minio${url.pathname}`;
    }
  } catch {
    if (baseUrl && src.startsWith('/minio')) {
      return baseUrl + src;
    }
  }

  return src;
}
