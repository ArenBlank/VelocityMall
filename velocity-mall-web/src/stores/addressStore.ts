import { defineStore } from 'pinia';

import {
  createAddress,
  deleteAddress,
  listAddresses,
  updateAddress,
  type AddressPayload
} from '@/api/addressApi';
import type { AddressVO } from '@/api/types';

interface AddressState {
  items: AddressVO[];
  loading: boolean;
  saving: boolean;
  error: string;
}

export const useAddressStore = defineStore('address', {
  state: (): AddressState => ({
    items: [],
    loading: false,
    saving: false,
    error: ''
  }),
  getters: {
    defaultAddress(state) {
      return state.items.find((item) => item.isDefault === true || item.isDefault === 1) || state.items[0] || null;
    }
  },
  actions: {
    async loadAddresses() {
      this.loading = true;
      this.error = '';
      try {
        this.items = await listAddresses();
      } catch (error) {
        this.error = error instanceof Error ? error.message : '地址加载失败';
      } finally {
        this.loading = false;
      }
    },
    async saveAddress(payload: AddressPayload, id?: number) {
      this.saving = true;
      this.error = '';
      try {
        if (id) {
          await updateAddress(id, payload);
        } else {
          await createAddress(payload);
        }
        await this.loadAddresses();
      } catch (error) {
        this.error = error instanceof Error ? error.message : '地址保存失败';
        throw error;
      } finally {
        this.saving = false;
      }
    },
    async removeAddress(id: number) {
      this.saving = true;
      this.error = '';
      try {
        await deleteAddress(id);
        await this.loadAddresses();
      } catch (error) {
        this.error = error instanceof Error ? error.message : '地址删除失败';
        throw error;
      } finally {
        this.saving = false;
      }
    }
  }
});
