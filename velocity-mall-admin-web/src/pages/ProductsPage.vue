<template>
  <section class="page">
    <div class="page-header">
      <div class="page-title">
        <h1>商品运营</h1>
        <p>管理 SPU 与 SKU，商品上下架和封面上传会影响 C 端展示。</p>
      </div>
      <button class="primary-button" type="button" @click="openCreate">
        <Plus :size="18" /> 新建 SPU
      </button>
    </div>

    <form class="filters panel" @submit.prevent="applyFilters(1)">
      <label>
        关键词
        <input v-model.trim="filters.keyword" placeholder="商品名或描述" />
      </label>
      <label>
        上架状态
        <select v-model="filters.status">
          <option value="">全部</option>
          <option value="1">已上架</option>
          <option value="0">已下架</option>
        </select>
      </label>
      <button class="primary-button" type="submit"><Search :size="17" /> 搜索</button>
      <button class="outline-button" type="button" @click="reset">重置</button>
    </form>

    <div class="drawer-grid">
      <div class="panel">
        <table class="data-table">
          <thead>
            <tr>
              <th>商品信息</th>
              <th>状态</th>
              <th>SKU</th>
              <th>更新时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="spu in records" :key="spu.spuId">
              <td>
                <div class="product-cell">
                  <SafeImage :src="coverOf(spu)" :alt="spu.name" />
                  <div>
                    <strong>{{ spu.name }}</strong>
                    <span class="subtext">SPU {{ spu.spuId }} · 类目 {{ spu.categoryId }}</span>
                  </div>
                </div>
              </td>
              <td><StatusBadge type="publish" :value="spu.publishStatus" /></td>
              <td>{{ spu.skuList?.length || 0 }} 个</td>
              <td>{{ formatTime(spu.updateTime || spu.createTime) }}</td>
              <td>
                <div class="row-actions">
                  <RouterLink class="ghost-button compact" :to="`/products/${spu.spuId}`">详情</RouterLink>
                  <button class="ghost-button compact" type="button" @click="edit(spu)">编辑</button>
                  <button
                    class="danger-button compact"
                    type="button"
                    @click="toggleStatus(spu)"
                  >
                    {{ spu.publishStatus === 1 ? '下架' : '上架' }}
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <EmptyState v-if="!store.loading && records.length === 0" />
        <Pager
          v-if="store.page"
          :page="store.page.current"
          :pages="store.page.pages"
          :total="store.page.total"
          @change="applyFilters"
        />
      </div>

      <aside class="panel">
        <div class="section-title">
          <h2>{{ editingSpuId ? '编辑 SPU' : '新建 SPU' }}</h2>
        </div>
        <form class="panel-body form-grid" @submit.prevent="saveSpu">
          <label class="field">
            商品名称
            <input v-model.trim="form.name" required />
          </label>
          <label class="field">
            类目 ID
            <input v-model.number="form.categoryId" min="1" required type="number" />
          </label>
          <label class="field">
            状态
            <select v-model.number="form.publishStatus">
              <option :value="1">已上架</option>
              <option :value="0">已下架</option>
            </select>
          </label>
          <label class="field full">
            描述
            <textarea v-model.trim="form.description" placeholder="商品运营描述"></textarea>
          </label>
          <div class="form-actions field full">
            <button class="primary-button" type="submit" :disabled="store.saving">
              <LoaderCircle v-if="store.saving" :size="17" class="spin" />
              保存
            </button>
            <button class="outline-button" type="button" @click="openCreate">清空</button>
          </div>
          <div class="message field full" :class="{ success: Boolean(actionMessage), error: Boolean(store.error) }">
            {{ store.error || actionMessage || '保存后可进入商品详情维护 SKU。' }}
          </div>
        </form>
      </aside>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { LoaderCircle, Plus, Search } from 'lucide-vue-next';
import EmptyState from '@/components/EmptyState.vue';
import Pager from '@/components/Pager.vue';
import SafeImage from '@/components/SafeImage.vue';
import StatusBadge from '@/components/StatusBadge.vue';
import type { AdminSpuVO } from '@/api/types';
import { useAdminProductStore } from '@/stores/adminProductStore';
import { formatTime } from '@/utils/format';

const store = useAdminProductStore();
const route = useRoute();
const router = useRouter();
const actionMessage = ref('');
const editingSpuId = ref<number | null>(null);

const filters = reactive({
  keyword: String(route.query.keyword || ''),
  status: route.query.status == null ? '' : String(route.query.status)
});

const form = reactive({
  name: '',
  categoryId: 1,
  description: '',
  publishStatus: 1
});

const records = computed(() => store.page?.records || []);

function coverOf(spu: AdminSpuVO) {
  return spu.skuList?.find((sku) => sku.coverImg)?.coverImg || '';
}

function statusValue() {
  return filters.status === '' ? null : Number(filters.status);
}

async function applyFilters(page = 1) {
  actionMessage.value = '';
  await router.replace({
    path: '/products',
    query: {
      page,
      keyword: filters.keyword || undefined,
      status: filters.status || undefined
    }
  });
  await store.load({ page, size: 10, keyword: filters.keyword, status: statusValue() });
}

function reset() {
  filters.keyword = '';
  filters.status = '';
  void applyFilters(1);
}

function openCreate() {
  editingSpuId.value = null;
  Object.assign(form, { name: '', categoryId: 1, description: '', publishStatus: 1 });
  actionMessage.value = '';
}

function edit(spu: AdminSpuVO) {
  editingSpuId.value = spu.spuId;
  Object.assign(form, {
    name: spu.name,
    categoryId: spu.categoryId,
    description: spu.description || '',
    publishStatus: spu.publishStatus
  });
}

async function saveSpu() {
  const saved = await store.saveSpu({ ...form }, editingSpuId.value || undefined);
  editingSpuId.value = saved.spuId;
  actionMessage.value = 'SPU 已保存';
  await applyFilters(Number(route.query.page || 1));
}

async function toggleStatus(spu: AdminSpuVO) {
  const publish = spu.publishStatus !== 1;
  const confirmed = window.confirm(`确认${publish ? '上架' : '下架'}「${spu.name}」？`);
  if (!confirmed) return;
  await store.setSpuStatus(spu.spuId, publish);
  actionMessage.value = publish ? '商品已上架' : '商品已下架';
  await applyFilters(Number(route.query.page || 1));
}

onMounted(() => {
  void applyFilters(Number(route.query.page || 1));
});
</script>
