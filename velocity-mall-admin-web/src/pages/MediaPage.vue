<template>
  <section class="page">
    <div class="page-header">
      <div class="page-title">
        <h1>媒体资源</h1>
        <p>按 SKU 上传商品封面，文件会写入 MinIO 并同步商品服务。</p>
      </div>
      <button class="outline-button" type="button" @click="loadSkus">刷新 SKU</button>
    </div>

    <div class="detail-grid">
      <div class="stack">
        <form class="panel panel-body form-grid" @submit.prevent="upload">
          <label class="field full">
            选择 SKU
            <select v-model.number="skuId" required>
              <option :value="0">请选择真实商品 SKU</option>
              <option v-for="option in skuOptions" :key="option.skuId" :value="option.skuId">
                SKU {{ option.skuId }} · {{ option.skuName }}
              </option>
            </select>
          </label>
          <label class="field full">
            图片文件
            <input :key="fileInputKey" accept="image/*" required type="file" @change="selectFile" />
          </label>
          <div class="form-actions field full">
            <button class="primary-button" type="submit" :disabled="store.loading || !file || !skuId">
              <LoaderCircle v-if="store.loading" :size="17" class="spin" />
              上传 SKU 封面
            </button>
          </div>
          <div class="message field full" :class="{ success: Boolean(store.lastUpload), error: Boolean(store.error) }">
            {{ store.error || (store.lastUpload ? '封面已上传并返回可访问 URL' : '请选择真实 SKU 和图片文件。') }}
          </div>
        </form>

        <section class="panel">
          <div class="section-title">
            <div>
              <h2>新增 SKU</h2>
              <p>选择真实 SPU 后创建 SKU，保存后会进入商品 SKU 列表和媒体资源下拉。</p>
            </div>
          </div>
          <form class="panel-body form-grid" @submit.prevent="createSku">
            <label class="field full">
              所属 SPU
              <select v-model.number="skuForm.spuId" required>
                <option :value="0">请选择真实 SPU</option>
                <option v-for="spu in productOptions" :key="spu.spuId" :value="spu.spuId">
                  SPU {{ spu.spuId }} · {{ spu.name }}
                </option>
              </select>
            </label>
            <label class="field full">
              SKU 名称
              <input v-model.trim="skuForm.skuName" required placeholder="例如 Velocity Phone Pro 16GB+1TB 曜石黑" />
            </label>
            <label class="field">
              SKU 编码
              <input v-model.trim="skuForm.skuCode" required placeholder="唯一编码" />
            </label>
            <label class="field">
              售价
              <input v-model.number="skuForm.price" min="0" required step="0.01" type="number" />
            </label>
            <label class="field">
              初始库存
              <input v-model.number="skuForm.stock" min="0" required type="number" />
            </label>
            <label class="field full">
              封面 URL
              <input v-model.trim="skuForm.coverImg" placeholder="可先留空，创建后在上方上传封面" />
            </label>
            <div class="form-actions field full">
              <button class="primary-button" type="submit" :disabled="productStore.saving || !skuForm.spuId">
                <LoaderCircle v-if="productStore.saving" :size="17" class="spin" />
                保存 SKU
              </button>
              <button class="outline-button" type="button" @click="resetSkuForm">清空</button>
            </div>
            <div class="message field full" :class="{ success: Boolean(skuMessage), error: Boolean(skuError) }">
              {{ skuError || skuMessage || 'SKU 来自真实后端接口，不再依赖手工写死数据库。' }}
            </div>
          </form>
        </section>
      </div>

      <aside class="panel panel-body">
        <h2>{{ preview ? '本次上传预览' : '当前 SKU 封面' }}</h2>
        <div class="media-preview">
          <img v-if="preview" :src="preview" alt="本地预览" />
          <SafeImage v-else class-name="large" :src="selectedSku?.coverImg" :alt="selectedSku?.skuName" label="请选择 SKU" />
        </div>
        <div v-if="selectedSku" class="stat-line">
          <span>当前 SKU</span>
          <strong>{{ selectedSku.skuName }}</strong>
        </div>
        <div v-if="store.lastUpload" class="stat-line">
          <span>访问 URL</span>
          <strong>{{ store.lastUpload.url }}</strong>
        </div>
      </aside>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { LoaderCircle } from 'lucide-vue-next';
import SafeImage from '@/components/SafeImage.vue';
import { useAdminMediaStore } from '@/stores/adminMediaStore';
import { useAdminProductStore } from '@/stores/adminProductStore';
import type { AdminSkuRequest } from '@/api/types';

const store = useAdminMediaStore();
const productStore = useAdminProductStore();
const skuId = ref(0);
const file = ref<File | null>(null);
const preview = ref('');
const fileInputKey = ref(0);
const skuMessage = ref('');
const skuError = ref('');
const skuForm = reactive<AdminSkuRequest>({
  spuId: 0,
  skuName: '',
  skuCode: '',
  price: 0,
  stock: 0,
  coverImg: ''
});

const productOptions = computed(() => productStore.page?.records || []);

const skuOptions = computed(() =>
  (productStore.page?.records || []).flatMap((spu) =>
    (spu.skuList || []).map((sku) => ({
      ...sku,
      spuName: spu.name
    }))
  )
);

const selectedSku = computed(() => skuOptions.value.find((sku) => sku.skuId === skuId.value));

async function loadSkus() {
  await productStore.load({ page: 1, size: 50, status: null });
}

function resetSkuForm() {
  skuMessage.value = '';
  skuError.value = '';
  Object.assign(skuForm, {
    spuId: 0,
    skuName: '',
    skuCode: '',
    price: 0,
    stock: 0,
    coverImg: ''
  });
}

function selectFile(event: Event) {
  const selected = (event.target as HTMLInputElement).files?.[0] || null;
  file.value = selected;
  preview.value = selected ? URL.createObjectURL(selected) : '';
}

async function upload() {
  if (!skuId.value || !file.value) return;
  await store.uploadSkuCover(skuId.value, file.value);
  preview.value = '';
  file.value = null;
  fileInputKey.value += 1;
  await loadSkus();
}

async function createSku() {
  skuMessage.value = '';
  skuError.value = '';
  try {
    const saved = await productStore.saveSku({
      ...skuForm,
      coverImg: skuForm.coverImg?.trim() || undefined
    });
    await loadSkus();
    skuId.value = saved.skuId;
    skuMessage.value = `SKU ${saved.skuId} 已创建，可继续上传封面`;
    Object.assign(skuForm, {
      spuId: saved.spuId,
      skuName: '',
      skuCode: '',
      price: 0,
      stock: 0,
      coverImg: ''
    });
  } catch (error) {
    skuError.value = error instanceof Error ? error.message : 'SKU 创建失败';
  }
}

onMounted(() => {
  void loadSkus();
});
</script>
