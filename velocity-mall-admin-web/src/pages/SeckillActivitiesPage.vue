<template>
  <section class="page">
    <div class="page-header">
      <div class="page-title">
        <h1>秒杀活动</h1>
        <p>配置 SKU 秒杀价、活动时间、秒杀库存，并可手动预热 Redis 库存。</p>
      </div>
      <button v-if="canWriteSeckill" class="primary-button" type="button" @click="newActivity"><Plus :size="18" /> 新建活动</button>
    </div>

    <form class="filters panel" @submit.prevent="load(1)">
      <label>
        SKU ID
        <input v-model.number="filters.skuId" min="1" type="number" />
      </label>
      <label>
        活动状态
        <select v-model="filters.state">
          <option value="">全部</option>
          <option value="NOT_STARTED">未开始</option>
          <option value="ACTIVE">进行中</option>
          <option value="ENDED">已结束</option>
          <option value="DISABLED">已停用</option>
        </select>
      </label>
      <button class="primary-button" type="submit"><Search :size="17" /> 搜索</button>
      <button class="outline-button" type="button" @click="reset">重置</button>
    </form>

    <div class="drawer-grid" :class="{ 'single-column': !canWriteSeckill }">
      <div class="panel">
        <table class="data-table">
          <thead>
            <tr>
              <th>活动</th>
              <th>SKU</th>
              <th>价格</th>
              <th>库存</th>
              <th>时间</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="activity in records" :key="activity.id">
              <td><strong>{{ activity.activityName }}</strong></td>
              <td>SKU {{ activity.skuId }}<br /><span class="subtext">SPU {{ activity.spuId }}</span></td>
              <td>{{ money(activity.seckillPrice) }}<br /><span class="subtext">原价 {{ money(activity.originalPrice) }}</span></td>
              <td>{{ activity.remainingStock ?? '-' }} / {{ activity.seckillStock }}</td>
              <td>{{ formatTime(activity.startTime) }}<br /><span class="subtext">{{ formatTime(activity.endTime) }}</span></td>
              <td><StatusBadge type="activity" :value="activity.state" /></td>
              <td>
                <div class="row-actions">
                  <button v-if="canWriteSeckill" class="ghost-button compact" type="button" @click="edit(activity)">编辑</button>
                  <button v-if="canPreheatSeckill" class="ghost-button compact" type="button" @click="preheat(activity.id)">预热</button>
                  <button v-if="canWriteSeckill" class="danger-button compact" type="button" @click="toggle(activity)">
                    {{ activity.status === 1 ? '停用' : '启用' }}
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <EmptyState v-if="!store.loading && records.length === 0" />
        <Pager v-if="store.page" :page="store.page.current" :pages="store.page.pages" :total="store.page.total" @change="load" />
      </div>

      <aside v-if="canWriteSeckill" class="panel">
        <div class="section-title"><h2>{{ editingId ? '编辑活动' : '新建活动' }}</h2></div>
        <form class="panel-body form-grid" @submit.prevent="save">
          <label class="field">
            SKU ID
            <input v-model.number="form.skuId" min="1" required type="number" />
          </label>
          <label class="field">
            SPU ID
            <input v-model.number="form.spuId" min="1" required type="number" />
          </label>
          <label class="field full">
            活动名称
            <input v-model.trim="form.activityName" required />
          </label>
          <label class="field">
            秒杀价
            <input v-model.number="form.seckillPrice" min="0" step="0.01" required type="number" />
          </label>
          <label class="field">
            原价
            <input v-model.number="form.originalPrice" min="0" step="0.01" required type="number" />
          </label>
          <label class="field">
            秒杀库存
            <input v-model.number="form.seckillStock" min="0" required type="number" />
          </label>
          <label class="field">
            状态
            <select v-model.number="form.status">
              <option :value="1">启用</option>
              <option :value="0">停用</option>
            </select>
          </label>
          <label class="field">
            开始时间
            <input v-model="form.startTime" required type="datetime-local" />
          </label>
          <label class="field">
            结束时间
            <input v-model="form.endTime" required type="datetime-local" />
          </label>
          <div class="form-actions field full">
            <button class="primary-button" type="submit" :disabled="store.saving">
              <LoaderCircle v-if="store.saving" :size="17" class="spin" />
              保存
            </button>
            <button class="outline-button" type="button" @click="newActivity">清空</button>
          </div>
          <div class="message field full" :class="{ success: Boolean(message), error: Boolean(error) }">
            {{ error || message || '活动保存后，可按需执行预热同步库存。' }}
          </div>
        </form>
      </aside>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { LoaderCircle, Plus, Search } from 'lucide-vue-next';
import EmptyState from '@/components/EmptyState.vue';
import Pager from '@/components/Pager.vue';
import StatusBadge from '@/components/StatusBadge.vue';
import type { AdminSeckillActivityVO } from '@/api/types';
import { AdminPermissions } from '@/constants/permissions';
import { useAdminAuthStore } from '@/stores/adminAuthStore';
import { useAdminSeckillStore } from '@/stores/adminSeckillStore';
import { formatTime, fromInputDateTime, money, toInputDateTime } from '@/utils/format';

const auth = useAdminAuthStore();
const store = useAdminSeckillStore();
const records = computed(() => store.page?.records || []);
const canWriteSeckill = computed(() => auth.hasPermission(AdminPermissions.SECKILL_WRITE));
const canPreheatSeckill = computed(() => auth.hasPermission(AdminPermissions.SECKILL_PREHEAT));
const filters = reactive({ skuId: null as number | null, state: '' });
const editingId = ref<number | null>(null);
const message = ref('');
const error = ref('');
const form = reactive({
  skuId: 0,
  spuId: 0,
  activityName: '',
  seckillPrice: 0,
  originalPrice: 0,
  seckillStock: 0,
  startTime: '',
  endTime: '',
  status: 1
});

async function load(page = 1) {
  await store.load({ page, size: 10, state: filters.state, skuId: filters.skuId });
}

function reset() {
  filters.skuId = null;
  filters.state = '';
  void load(1);
}

function newActivity() {
  editingId.value = null;
  Object.assign(form, {
    skuId: 0,
    spuId: 0,
    activityName: '',
    seckillPrice: 0,
    originalPrice: 0,
    seckillStock: 0,
    startTime: '',
    endTime: '',
    status: 1
  });
  message.value = '';
  error.value = '';
}

function edit(activity: AdminSeckillActivityVO) {
  editingId.value = activity.id;
  Object.assign(form, {
    skuId: activity.skuId,
    spuId: activity.spuId,
    activityName: activity.activityName,
    seckillPrice: Number(activity.seckillPrice),
    originalPrice: Number(activity.originalPrice),
    seckillStock: activity.seckillStock,
    startTime: toInputDateTime(activity.startTime),
    endTime: toInputDateTime(activity.endTime),
    status: activity.status
  });
}

async function save() {
  message.value = '';
  error.value = '';
  if (new Date(form.startTime).getTime() >= new Date(form.endTime).getTime()) {
    error.value = '活动开始时间必须早于结束时间';
    return;
  }
  try {
    const saved = await store.save(
      {
        ...form,
        startTime: fromInputDateTime(form.startTime),
        endTime: fromInputDateTime(form.endTime)
      },
      editingId.value || undefined
    );
    editingId.value = saved.id;
    message.value = '活动已保存';
    await load(store.page?.current || 1);
  } catch (err) {
    error.value = err instanceof Error ? err.message : '保存失败';
  }
}

async function toggle(activity: AdminSeckillActivityVO) {
  const next = activity.status === 1 ? 0 : 1;
  if (!window.confirm(`确认${next === 1 ? '启用' : '停用'}活动「${activity.activityName}」？`)) return;
  await store.updateStatus(activity.id, next);
  await load(store.page?.current || 1);
}

async function preheat(id: number) {
  if (!window.confirm('确认预热该活动库存？')) return;
  await store.preheat(id);
  message.value = '库存已预热';
  await load(store.page?.current || 1);
}

onMounted(() => {
  void load(1);
});
</script>
