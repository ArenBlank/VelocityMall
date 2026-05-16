export function normalizeImageUrl(url: string | null | undefined) {
  if (!url) return '';
  if (url.startsWith('http://127.0.0.1:9000')) {
    return url.replace('http://127.0.0.1:9000', '/minio');
  }
  if (url.startsWith('http://localhost:9000')) {
    return url.replace('http://localhost:9000', '/minio');
  }
  return url;
}
