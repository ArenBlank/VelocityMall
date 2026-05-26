<template>
  <section class="page">
    <div class="page-header">
      <div class="page-title">
        <h1>订单中心</h1>
        <p>查看买家订单，已支付订单可填写物流信息并发货。</p>
      </div>
      <button class="outline-button" type="button" @click="applyFilters(currentPage)">
        <RefreshCw :size="17" /> 刷新
      </button>
    </div>

    <form class="filters panel" @submit.prevent="applyFilters(1)">
      <label>
        订单号
        <input v-model.trim="filters.orderSn" placeholder="精确或模糊订单号" />
      </label>
      <label>
        用户 ID
        <input v-model.number="filters.userId" min="1" type="number" />
      </label>
      <label>
        状态
        <select v-model="filters.status">
          <option value="">全部</option>
          <option value="0">待支付</option>
          <option value="1">已支付</option>
          <option value="2">已发货</option>
          <option value="3">已完成</option>
          <option value="4">已关闭</option>
          <option value="5">已退款</option>
        </select>
      </label>
      <label>
        类型
        <select v-model="filters.orderType">
          <option value="">全部</option>
          <option value="1">秒杀订单</option>
          <option value="0">普通订单</option>
        </select>
      </label>
      <button class="primary-button" type="submit"><Search :size="17" /> 搜索</button>
      <button class="outline-button" type="button" @click="reset">重置</button>
    </form>

    <div v-if="feedback" class="message success">{{ feedback }}</div>
    <div v-if="localError || store.error" class="message error">{{ localError || store.error }}</div>

    <div class="panel">
      <table class="data-table">
        <thead>
          <tr>
            <th>订单信息</th>
            <th>金额</th>
            <th>类型</th>
            <th>状态</th>
            <th>下单时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="order in records" :key="order.orderSn">
            <td>
              <div class="order-product">
                <SafeImage class-name="small" :src="primaryItem(order)?.skuPic" :alt="primaryItem(order)?.skuName" />
                <div>
                  <strong>{{ order.orderSn }}</strong>
                  <span class="subtext">{{ primaryItem(order)?.skuName || '订单商品' }} · 用户 {{ order.userId }}</span>
                </div>
              </div>
            </td>
            <td>{{ money(order.payAmount || order.totalAmount) }}</td>
            <td>{{ orderTypeText(order.orderType) }}</td>
            <td><StatusBadge type="order" :value="order.status" /></td>
            <td>{{ formatTime(order.createTime) }}</td>
            <td>
              <div class="row-actions">
                <RouterLink class="ghost-button compact" :to="`/orders/${order.orderSn}`">详情</RouterLink>
                <button v-if="canDeliverOrder && order.status === 1" class="primary-button compact" type="button" @click="openDeliver(order.orderSn)">
                  发货
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

    <div v-if="canDeliverOrder && deliverForm.orderSn" class="panel panel-body deliver-panel">
      <h2>订单发货</h2>
      <p class="muted">订单号：{{ deliverForm.orderSn }}</p>
      <form class="form-grid" @submit.prevent="submitDeliver">
        <label class="field">
          物流公司
          <input v-model.trim="deliverForm.deliveryCompany" required placeholder="例如：顺丰速运…" />
        </label>
        <label class="field">
          物流单号
          <input v-model.trim="deliverForm.deliverySn" required placeholder="请输入真实物流单号…" />
        </label>
        <div class="form-actions field full">
          <button class="primary-button" type="submit">确认发货</button>
          <button class="outline-button" type="button" @click="closeDeliver">取消</button>
        </div>
      </form>
      <div v-if="localError" class="message error">{{ localError }}</div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { RefreshCw, Search } from 'lucide-vue-next';
import EmptyState from '@/components/EmptyState.vue';
import Pager from '@/components/Pager.vue';
import SafeImage from '@/components/SafeImage.vue';
import StatusBadge from '@/components/StatusBadge.vue';
import { AdminPermissions } from '@/constants/permissions';
import type { AdminOrderVO } from '@/api/types';
import { useAdminAuthStore } from '@/stores/adminAuthStore';
import { useAdminOrderStore } from '@/stores/adminOrderStore';
import { formatTime, money, orderTypeText } from '@/utils/format';

const store = useAdminOrderStore();
const auth = useAdminAuthStore();
const route = useRoute();
const router = useRouter();
const currentPage = computed(() => Number(route.query.page || 1));
const records = computed(() => store.page?.records || []);
const canDeliverOrder = computed(() => auth.hasPermission(AdminPermissions.ORDER_DELIVER));
const feedback = ref('');
const localError = ref('');

const filters = reactive({
  orderSn: String(route.query.orderSn || ''),
  userId: route.query.userId ? Number(route.query.userId) : null as number | null,
  status: route.query.status == null ? '' : String(route.query.status),
  orderType: route.query.orderType == null ? '' : String(route.query.orderType)
});

const deliverForm = reactive({ orderSn: '', deliveryCompany: '', deliverySn: '' });
function nullableNumber(value: string | number | null) {
  if (value === '' || value == null) return null;
  return Number(value);
}

function primaryItem(order: AdminOrderVO) {
  return order.items?.[0];
}

async function applyFilters(page = 1) {
  localError.value = '';
  await router.replace({
    path: '/orders',
    query: {
      page,
      orderSn: filters.orderSn || undefined,
      userId: filters.userId || undefined,
      status: filters.status || undefined,
      orderType: filters.orderType || undefined
    }
  });
  await store.load({
    page,
    size: 10,
    orderSn: filters.orderSn,
    userId: filters.userId,
    status: nullableNumber(filters.status),
    orderType: nullableNumber(filters.orderType)
  });
}

function reset() {
  filters.orderSn = '';
  filters.userId = null;
  filters.status = '';
  filters.orderType = '';
  void applyFilters(1);
}

function openDeliver(orderSn: string) {
  localError.value = '';
  feedback.value = '';
  Object.assign(deliverForm, { orderSn, deliveryCompany: '', deliverySn: '' });
}

function closeDeliver() {
  Object.assign(deliverForm, { orderSn: '', deliveryCompany: '', deliverySn: '' });
}

async function submitDeliver() {
  localError.value = '';
  feedback.value = '';
  try {
    const orderSn = deliverForm.orderSn;
    await store.deliver(orderSn, deliverForm.deliveryCompany, deliverForm.deliverySn);
    closeDeliver();
    feedback.value = `订单 ${orderSn} 已发货`;
    await applyFilters(currentPage.value);
  } catch (error) {
    localError.value = error instanceof Error ? error.message : '发货失败';
  }
}

onMounted(() => {
  void applyFilters(currentPage.value);
});
</script>
