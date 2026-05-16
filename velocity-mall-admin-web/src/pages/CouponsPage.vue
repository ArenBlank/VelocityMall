<template>
  <section class="page">
    <div class="page-header">
      <div class="page-title">
        <h1>优惠券管理</h1>
        <p>维护可领取优惠券，C 端领券入口使用同一批数据。</p>
      </div>
      <button class="primary-button" type="button" @click="newCoupon"><Plus :size="18" /> 新建优惠券</button>
    </div>
    <form class="filters panel" @submit.prevent="load(1)">
      <label>
        状态
        <select v-model="status">
          <option value="">全部</option>
          <option value="1">启用</option>
          <option value="0">停用</option>
        </select>
      </label>
      <button class="primary-button" type="submit">筛选</button>
    </form>
    <div class="drawer-grid">
      <div class="panel">
        <table class="data-table">
          <thead>
            <tr>
              <th>名称</th>
              <th>面额</th>
              <th>门槛</th>
              <th>库存</th>
              <th>时间</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="coupon in records" :key="coupon.id">
              <td><strong>{{ coupon.name }}</strong></td>
              <td>{{ money(coupon.amount) }}</td>
              <td>{{ money(coupon.minPoint) }}</td>
              <td>{{ coupon.stock }} · 每人 {{ coupon.limitPerUser }}</td>
              <td>{{ formatTime(coupon.startTime) }}<br /><span class="subtext">{{ formatTime(coupon.endTime) }}</span></td>
              <td><StatusBadge type="coupon" :value="coupon.status" /></td>
              <td>
                <div class="row-actions">
                  <button class="ghost-button compact" type="button" @click="edit(coupon)">编辑</button>
                  <button class="danger-button compact" type="button" @click="toggle(coupon)">
                    {{ coupon.status === 1 ? '停用' : '启用' }}
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <EmptyState v-if="!store.loading && records.length === 0" />
        <Pager v-if="store.page" :page="store.page.current" :pages="store.page.pages" :total="store.page.total" @change="load" />
      </div>
      <aside class="panel">
        <div class="section-title"><h2>{{ editingId ? '编辑优惠券' : '新建优惠券' }}</h2></div>
        <form class="panel-body form-grid" @submit.prevent="save">
          <label class="field full">名称<input v-model.trim="form.name" required /></label>
          <label class="field">面额<input v-model.number="form.amount" min="0" step="0.01" required type="number" /></label>
          <label class="field">使用门槛<input v-model.number="form.minPoint" min="0" step="0.01" required type="number" /></label>
          <label class="field">库存<input v-model.number="form.stock" min="0" required type="number" /></label>
          <label class="field">每人限领<input v-model.number="form.limitPerUser" min="1" required type="number" /></label>
          <label class="field">开始时间<input v-model="form.startTime" required type="datetime-local" /></label>
          <label class="field">结束时间<input v-model="form.endTime" required type="datetime-local" /></label>
          <label class="field full">状态<select v-model.number="form.status"><option :value="1">启用</option><option :value="0">停用</option></select></label>
          <div class="form-actions field full">
            <button class="primary-button" type="submit" :disabled="store.saving">保存</button>
            <button class="outline-button" type="button" @click="newCoupon">清空</button>
          </div>
          <div class="message field full" :class="{ success: Boolean(message), error: Boolean(error) }">
            {{ error || message || '优惠券保存后会进入 C 端领券列表。' }}
          </div>
        </form>
      </aside>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { Plus } from 'lucide-vue-next';
import EmptyState from '@/components/EmptyState.vue';
import Pager from '@/components/Pager.vue';
import StatusBadge from '@/components/StatusBadge.vue';
import type { AdminCouponVO } from '@/api/types';
import { useAdminCouponStore } from '@/stores/adminCouponStore';
import { formatTime, fromInputDateTime, money, toInputDateTime } from '@/utils/format';

const store = useAdminCouponStore();
const records = computed(() => store.page?.records || []);
const status = ref('');
const editingId = ref<number | null>(null);
const message = ref('');
const error = ref('');
const form = reactive({ name: '', amount: 0, minPoint: 0, stock: 0, limitPerUser: 1, startTime: '', endTime: '', status: 1 });

async function load(page = 1) {
  await store.load({ page, size: 10, status: status.value === '' ? null : Number(status.value) });
}

function newCoupon() {
  editingId.value = null;
  Object.assign(form, { name: '', amount: 0, minPoint: 0, stock: 0, limitPerUser: 1, startTime: '', endTime: '', status: 1 });
}

function edit(coupon: AdminCouponVO) {
  editingId.value = coupon.id;
  Object.assign(form, { ...coupon, startTime: toInputDateTime(coupon.startTime), endTime: toInputDateTime(coupon.endTime) });
}

async function save() {
  message.value = '';
  error.value = '';
  try {
    await store.save({ ...form, startTime: fromInputDateTime(form.startTime), endTime: fromInputDateTime(form.endTime) }, editingId.value || undefined);
    message.value = '优惠券已保存';
    await load(store.page?.current || 1);
  } catch (err) {
    error.value = err instanceof Error ? err.message : '保存失败';
  }
}

async function toggle(coupon: AdminCouponVO) {
  await store.updateStatus(coupon.id, coupon.status === 1 ? 0 : 1);
  await load(store.page?.current || 1);
}

onMounted(() => {
  void load(1);
});
</script>
