function baseUrl(): string {
  return import.meta.env.VITE_API_BASE_URL || '';
}

export function normalizeImageUrl(url: string | null | undefined) {
  if (!url) return '';
  const base = baseUrl();
  if (url.startsWith('http://127.0.0.1:9000')) {
    return base + url.replace('http://127.0.0.1:9000', '/minio');
  }
  if (url.startsWith('http://localhost:9000')) {
    return base + url.replace('http://localhost:9000', '/minio');
  }
  if (base && url.startsWith('/minio')) {
    return base + url;
  }
  return url;
}
