import { defineStore } from 'pinia';

import { getSku, getSpu } from '@/api/productApi';
import { searchSkus } from '@/api/searchApi';
import type { PageVO, SearchSkuVO, SkuVO, SpuDetailVO } from '@/api/types';

export type SearchSort = 'sale_desc' | 'price_asc' | 'price_desc';

interface ProductState {
  sku: SkuVO | null;
  spu: SpuDetailVO | null;
  searchPage: PageVO<SearchSkuVO> | null;
  loading: boolean;
  error: string;
}

export const useProductStore = defineStore('product', {
  state: (): ProductState => ({
    sku: null,
    spu: null,
    searchPage: null,
    loading: false,
    error: ''
  }),
  actions: {
    async loadSearchCatalog(keyword = '', sort: SearchSort = 'sale_desc', page = 1, size = 10) {
      this.loading = true;
      this.error = '';
      try {
        this.searchPage = await searchSkus({
          keyword: keyword || undefined,
          page,
          size,
          sort
        });
      } catch (error) {
        this.error = error instanceof Error ? error.message : '商品搜索列表加载失败';
      } finally {
        this.loading = false;
      }
    },
    async loadProduct(skuId: number, fallbackSpuId: number) {
      this.loading = true;
      this.error = '';
      this.sku = null;
      this.spu = null;
      try {
        const [sku, searchPage] = await Promise.all([
          getSku(skuId),
          searchSkus({ page: 1, size: 20, sort: 'sale_desc' }).catch(() => null)
        ]);
        this.sku = sku;
        this.searchPage = searchPage;
        this.spu = await getSpu(sku.spuId || fallbackSpuId);
      } catch (error) {
        this.error = error instanceof Error ? error.message : '商品信息加载失败';
      } finally {
        this.loading = false;
      }
    }
  }
});
