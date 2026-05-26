<script setup lang="ts">
import { Check, LoaderCircle, MapPin, Pencil, Plus, Trash2 } from 'lucide-vue-next';
import { computed, onMounted, reactive, ref } from 'vue';

import type { AddressPayload } from '@/api/addressApi';
import type { AddressVO } from '@/api/types';
import { useAddressStore } from '@/stores/addressStore';

const addressStore = useAddressStore();
const editingId = ref<number | null>(null);
const message = ref('');
const form = reactive<AddressPayload>({
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  region: '',
  detailAddress: '',
  isDefault: false
});

const submitLabel = computed(() => (editingId.value ? '保存地址' : '新增地址'));

function resetForm() {
  editingId.value = null;
  Object.assign(form, {
    receiverName: '',
    receiverPhone: '',
    province: '',
    city: '',
    region: '',
    detailAddress: '',
    isDefault: false
  });
}

function editAddress(address: AddressVO) {
  editingId.value = address.id;
  Object.assign(form, {
    receiverName: address.receiverName,
    receiverPhone: address.receiverPhone,
    province: address.province,
    city: address.city,
    region: address.region,
    detailAddress: address.detailAddress,
    isDefault: address.isDefault === true || address.isDefault === 1
  });
}

async function saveAddress() {
  message.value = '';
  await addressStore.saveAddress({ ...form, isDefault: form.isDefault ? 1 : 0 }, editingId.value || undefined);
  message.value = editingId.value ? '地址已更新' : '地址已新增';
  resetForm();
}

async function removeAddress(id: number) {
  message.value = '';
  await addressStore.removeAddress(id);
  message.value = '地址已删除';
  if (editingId.value === id) {
    resetForm();
  }
}

function fullAddress(address: AddressVO) {
  return `${address.province}${address.city}${address.region}${address.detailAddress}`;
}

onMounted(() => {
  void addressStore.loadAddresses();
});
</script>

<template>
  <main class="buyer-page address-page">
    <section class="buyer-page-head">
      <div>
        <span class="eyebrow"><MapPin :size="18" /> 收货地址</span>
        <h1>地址管理</h1>
        <p>下普通订单时会从这里选择收货地址，数据来自当前登录账号。</p>
      </div>
      <button type="button" class="outline-action" @click="resetForm">
        <Plus :size="18" />
        新增
      </button>
    </section>

    <section class="two-column-workspace">
      <form class="buyer-form" @submit.prevent="saveAddress">
        <h2>{{ submitLabel }}</h2>
        <label>
          收货人
          <input v-model.trim="form.receiverName" required placeholder="请输入收货人姓名…" />
        </label>
        <label>
          手机号
          <input v-model.trim="form.receiverPhone" type="tel" autocomplete="tel" inputmode="numeric" required placeholder="请输入手机号…" />
        </label>
        <div class="form-grid-3">
          <label>
            省份
            <input v-model.trim="form.province" required placeholder="省份" />
          </label>
          <label>
            城市
            <input v-model.trim="form.city" required placeholder="城市" />
          </label>
          <label>
            区县
            <input v-model.trim="form.region" required placeholder="区县" />
          </label>
        </div>
        <label>
          详细地址
          <textarea v-model.trim="form.detailAddress" required placeholder="街道、门牌号等详细信息…" />
        </label>
        <label class="checkbox-line">
          <input v-model="form.isDefault" type="checkbox" />
          设为默认地址
        </label>
        <div class="form-actions">
          <button type="submit" class="primary-action" :disabled="addressStore.saving">
            <LoaderCircle v-if="addressStore.saving" :size="18" class="spin" />
            {{ submitLabel }}
          </button>
          <button type="button" class="ghost-action" @click="resetForm">清空</button>
        </div>
        <p v-if="message" class="form-message success">{{ message }}</p>
        <p v-if="addressStore.error" class="form-message error">{{ addressStore.error }}</p>
      </form>

      <section class="address-list-panel">
        <header>
          <h2>我的地址</h2>
          <span>{{ addressStore.items.length }} 条</span>
        </header>
        <div v-if="addressStore.loading" class="inline-loading">
          <LoaderCircle :size="20" class="spin" />
          正在加载地址…
        </div>
        <div v-else-if="addressStore.items.length === 0" class="empty-inline">暂无地址，请先新增一个收货地址。</div>
        <article v-for="address in addressStore.items" v-else :key="address.id" class="address-item">
          <div>
            <strong>{{ address.receiverName }}</strong>
            <span>{{ address.receiverPhone }}</span>
            <em v-if="address.isDefault === true || address.isDefault === 1"><Check :size="14" /> 默认</em>
          </div>
          <p>{{ fullAddress(address) }}</p>
          <footer>
            <button type="button" @click="editAddress(address)">
              <Pencil :size="16" />
              编辑
            </button>
            <button type="button" class="danger-text" @click="removeAddress(address.id)">
              <Trash2 :size="16" />
              删除
            </button>
          </footer>
        </article>
      </section>
    </section>
  </main>
</template>
