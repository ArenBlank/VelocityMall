<template>
  <section class="page">
    <div class="page-header">
      <div class="page-title">
        <h1>{{ spu?.name || '商品详情' }}</h1>
        <p>维护 SPU 基础信息、SKU 价格库存和 C 端封面图。</p>
      </div>
      <RouterLink class="outline-button" to="/products">返回商品列表</RouterLink>
    </div>

    <div v-if="store.loading" class="panel"><div class="empty-state">商品详情加载中…</div></div>
    <div v-else-if="spu" class="detail-grid" :class="{ 'single-column': !canWriteProducts }">
      <div class="panel">
        <div class="section-title">
          <h2>SKU 列表</h2>
          <button v-if="canWriteProducts" class="primary-button" type="button" @click="newSku"><Plus :size="17" /> 新建 SKU</button>
        </div>
        <table class="data-table">
          <thead>
            <tr>
              <th>SKU</th>
              <th>价格</th>
              <th>库存</th>
              <th>销量</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="sku in spu.skuList" :key="sku.skuId">
              <td>
                <div class="product-cell">
                  <SafeImage :src="sku.coverImg" :alt="sku.skuName" />
                  <div>
                    <strong>{{ sku.skuName }}</strong>
                    <span class="subtext">SKU {{ sku.skuId }} · {{ sku.skuCode }}</span>
                  </div>
                </div>
              </td>
              <td>{{ money(sku.price) }}</td>
              <td>可售 {{ sku.availableStock }} / 总 {{ sku.stock }}</td>
              <td>{{ sku.saleCount }}</td>
              <td>
                <div class="row-actions">
                  <button v-if="canWriteProducts" class="ghost-button compact" type="button" @click="editSku(sku)">编辑</button>
                  <label v-if="canWriteProducts" class="ghost-button compact file-button">
                    上传封面
                    <input type="file" accept="image/*" @change="uploadCover(sku.skuId, $event)" />
                  </label>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <EmptyState v-if="spu.skuList.length === 0" title="暂无 SKU" description="请在右侧创建第一个 SKU。" />
      </div>

      <aside v-if="canWriteProducts" class="panel">
        <div class="section-title">
          <h2>SPU 信息</h2>
          <StatusBadge type="publish" :value="spu.publishStatus" />
        </div>
        <form class="panel-body form-grid" @submit.prevent="saveSpu">
          <label class="field full">
            商品名称
            <input v-model.trim="spuForm.name" required />
          </label>
          <label class="field">
            类目 ID
            <input v-model.number="spuForm.categoryId" min="1" required type="number" />
          </label>
          <label class="field">
            状态
            <select v-model.number="spuForm.publishStatus">
              <option :value="1">已上架</option>
              <option :value="0">已下架</option>
            </select>
          </label>
          <label class="field full">
            描述
            <textarea v-model.trim="spuForm.description"></textarea>
          </label>
          <button class="primary-button field full" type="submit">保存 SPU</button>
        </form>

        <div class="section-title">
          <h2>{{ skuEditingId ? '编辑 SKU' : '新建 SKU' }}</h2>
        </div>
        <form class="panel-body form-grid" @submit.prevent="saveSku">
          <label class="field full">
            SKU 名称
            <input v-model.trim="skuForm.skuName" required />
          </label>
          <label class="field full">
            SKU 编码
            <input v-model.trim="skuForm.skuCode" required />
          </label>
          <label class="field">
            价格
            <input v-model.number="skuForm.price" min="0" step="0.01" required type="number" />
          </label>
          <label class="field">
            库存
            <input v-model.number="skuForm.stock" min="0" required type="number" />
          </label>
          <label class="field full">
            封面 URL
            <input v-model.trim="skuForm.coverImg" placeholder="可由上传封面自动生成" />
          </label>
          <div class="form-actions field full">
            <button class="primary-button" type="submit" :disabled="store.saving">
              <LoaderCircle v-if="store.saving" :size="17" class="spin" />
              保存 SKU
            </button>
            <button class="outline-button" type="button" @click="newSku">清空</button>
          </div>
          <div class="message field full" :class="{ success: Boolean(message), error: Boolean(error) }">
            {{ error || message || 'SKU 保存后可被搜索索引重建同步到 C 端。' }}
          </div>
        </form>
      </aside>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';
import { LoaderCircle, Plus } from 'lucide-vue-next';
import EmptyState from '@/components/EmptyState.vue';
import SafeImage from '@/components/SafeImage.vue';
import StatusBadge from '@/components/StatusBadge.vue';
import { AdminPermissions } from '@/constants/permissions';
import type { AdminSkuVO } from '@/api/types';
import { useAdminProductStore } from '@/stores/adminProductStore';
import { useAdminAuthStore } from '@/stores/adminAuthStore';
import { money } from '@/utils/format';

const route = useRoute();
const store = useAdminProductStore();
const auth = useAdminAuthStore();
const message = ref('');
const error = ref('');
const skuEditingId = ref<number | null>(null);
const spu = computed(() => store.current);
const canWriteProducts = computed(() => auth.hasPermission(AdminPermissions.PRODUCT_WRITE));

const spuForm = reactive({ name: '', categoryId: 1, description: '', publishStatus: 1 });
const skuForm = reactive({ spuId: 0, skuName: '', skuCode: '', price: 0, stock: 0, coverImg: '' });

function fillSpuForm() {
  if (!spu.value) return;
  Object.assign(spuForm, {
    name: spu.value.name,
    categoryId: spu.value.categoryId,
    description: spu.value.description || '',
    publishStatus: spu.value.publishStatus
  });
}

async function load() {
  const spuId = Number(route.params.spuId);
  await store.loadDetail(spuId);
  fillSpuForm();
  newSku();
}

function newSku() {
  skuEditingId.value = null;
  Object.assign(skuForm, {
    spuId: Number(route.params.spuId),
    skuName: '',
    skuCode: '',
    price: 0,
    stock: 0,
    coverImg: ''
  });
}

function editSku(sku: AdminSkuVO) {
  skuEditingId.value = sku.skuId;
  Object.assign(skuForm, {
    spuId: sku.spuId,
    skuName: sku.skuName,
    skuCode: sku.skuCode,
    price: Number(sku.price),
    stock: Number(sku.stock),
    coverImg: sku.coverImg || ''
  });
}

async function saveSpu() {
  error.value = '';
  message.value = '';
  try {
    await store.saveSpu({ ...spuForm }, Number(route.params.spuId));
    message.value = 'SPU 已保存';
    await load();
  } catch (err) {
    error.value = err instanceof Error ? err.message : '保存失败';
  }
}

async function saveSku() {
  error.value = '';
  message.value = '';
  try {
    await store.saveSku({ ...skuForm }, skuEditingId.value || undefined);
    message.value = 'SKU 已保存';
    await load();
  } catch (err) {
    error.value = err instanceof Error ? err.message : '保存失败';
  }
}

async function uploadCover(skuId: number, event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0];
  if (!file) return;
  error.value = '';
  message.value = '';
  try {
    await store.uploadCover(skuId, file);
    message.value = '封面已上传并同步';
    await load();
  } catch (err) {
    error.value = err instanceof Error ? err.message : '上传失败';
  } finally {
    (event.target as HTMLInputElement).value = '';
  }
}

onMounted(load);
</script>

<style scoped>
.file-button {
  position: relative;
  overflow: hidden;
}

.file-button input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}
</style>
