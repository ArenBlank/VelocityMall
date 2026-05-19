const baseUrl = import.meta.env.VITE_API_BASE_URL || '';
const minioProductBaseUrl =
  import.meta.env.VITE_MINIO_PRODUCT_BASE_URL || `${baseUrl}/minio/velocity-mall-product`;

export const fallbackCoverImages = [
  `${minioProductBaseUrl}/products/default-covers/phone-1.png`,
  `${minioProductBaseUrl}/products/default-covers/phone-2.png`,
  `${minioProductBaseUrl}/products/default-covers/phone-3.png`
];

export function pickFallbackCover(seed?: number) {
  const index = Math.abs(seed || 0) % fallbackCoverImages.length;
  return fallbackCoverImages[index];
}
