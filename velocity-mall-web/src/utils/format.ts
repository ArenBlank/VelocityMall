export function money(value: number | string | null | undefined) {
  const parsed = Number(value ?? 0);
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    maximumFractionDigits: 0
  }).format(Number.isFinite(parsed) ? parsed : 0);
}

export function countdownParts(targetTime: string) {
  const diff = Math.max(new Date(targetTime).getTime() - Date.now(), 0);
  const totalSeconds = Math.floor(diff / 1000);
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  return [hours, minutes, seconds].map((item) => String(item).padStart(2, '0'));
}
