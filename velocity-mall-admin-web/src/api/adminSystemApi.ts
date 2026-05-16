import { http } from './http';
import type { AdminRebuildIndexVO } from './types';

export function rebuildAdminSkuIndex() {
  return http.post<AdminRebuildIndexVO, AdminRebuildIndexVO>('/api/v1/admin/search/skus/rebuild-index');
}
