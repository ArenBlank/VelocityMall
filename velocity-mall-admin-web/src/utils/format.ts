export function money(value: number | string | null | undefined) {
  const parsed = Number(value ?? 0);
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    maximumFractionDigits: 2
  }).format(Number.isFinite(parsed) ? parsed : 0);
}

export function intText(value: number | string | null | undefined) {
  const parsed = Number(value ?? 0);
  return new Intl.NumberFormat('zh-CN').format(Number.isFinite(parsed) ? parsed : 0);
}

export function formatTime(value: string | null | undefined) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date);
}

export function toInputDateTime(value: string | null | undefined) {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value.slice(0, 16);
  const offset = date.getTimezoneOffset() * 60000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 16);
}

export function fromInputDateTime(value: string) {
  return value ? `${value}:00` : '';
}

export function orderStatusText(status: number | null | undefined) {
  const map: Record<number, string> = {
    0: '待支付',
    1: '已支付',
    2: '已发货',
    3: '已完成',
    4: '已关闭',
    5: '已退款'
  };
  return status == null ? '-' : map[status] ?? `状态 ${status}`;
}

export function orderTypeText(type: number | null | undefined) {
  return type === 1 ? '秒杀订单' : '普通订单';
}

export function publishStatusText(status: number | null | undefined) {
  return status === 1 ? '已上架' : '已下架';
}

export function activityStateText(state: string | null | undefined) {
  const map: Record<string, string> = {
    NOT_STARTED: '未开始',
    ACTIVE: '进行中',
    ENDED: '已结束',
    DISABLED: '已停用'
  };
  return state ? map[state] ?? state : '-';
}
