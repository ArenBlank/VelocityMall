import { defineStore } from 'pinia';

import { listCategoryTree } from '@/api/categoryApi';
import type { CategoryTreeVO } from '@/api/types';

interface CategoryState {
  tree: CategoryTreeVO[];
  loading: boolean;
  error: string;
}

function flattenCategories(nodes: CategoryTreeVO[]): CategoryTreeVO[] {
  return nodes.flatMap((node) => [node, ...flattenCategories(node.children || [])]);
}

export const useCategoryStore = defineStore('category', {
  state: (): CategoryState => ({
    tree: [],
    loading: false,
    error: ''
  }),
  getters: {
    flatCategories(state) {
      return flattenCategories(state.tree);
    }
  },
  actions: {
    async loadCategories() {
      if (this.tree.length > 0) {
        return;
      }
      this.loading = true;
      this.error = '';
      try {
        this.tree = await listCategoryTree();
      } catch (error) {
        this.error = error instanceof Error ? error.message : '分类加载失败';
      } finally {
        this.loading = false;
      }
    }
  }
});
