<template>
  <section class="page">
    <div class="page-header">
      <div class="page-title">
        <h1>订单详情</h1>
        <p>{{ order?.orderSn || route.params.orderSn }}</p>
      </div>
      <RouterLink class="outline-button" to="/orders">返回订单中心</RouterLink>
    </div>

    <div v-if="store.loading" class="panel"><div class="empty-state">订单详情加载中...</div></div>
    <div v-else-if="order" class="detail-grid">
      <div class="panel">
        <div class="section-title">
          <h2>商品明细</h2>
          <StatusBadge type="order" :value="order.status" />
        </div>
        <table class="data-table">
          <thead>
            <tr>
              <th>商品</th>
              <th>SKU</th>
              <th>单价</th>
              <th>数量</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in order.items" :key="`${order.orderSn}-${item.skuId}`">
              <td>
                <div class="product-cell">
                  <SafeImage :src="item.skuPic" :alt="item.skuName" />
                  <strong>{{ item.skuName }}</strong>
                </div>
              </td>
              <td>{{ item.skuId }}</td>
              <td>{{ money(item.skuPrice) }}</td>
              <td>{{ item.quantity }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <aside class="panel">
        <div class="section-title"><h2>订单信息</h2></div>
        <div class="panel-body">
          <div class="stat-line"><span>订单号</span><strong>{{ order.orderSn }}</strong></div>
          <div class="stat-line"><span>用户 ID</span><strong>{{ order.userId }}</strong></div>
          <div class="stat-line"><span>订单类型</span><strong>{{ orderTypeText(order.orderType) }}</strong></div>
          <div class="stat-line"><span>实付金额</span><strong>{{ money(order.payAmount || order.totalAmount) }}</strong></div>
          <div class="stat-line"><span>下单时间</span><strong>{{ formatTime(order.createTime) }}</strong></div>
          <div class="stat-line"><span>支付时间</span><strong>{{ formatTime(order.payTime) }}</strong></div>
          <div class="stat-line"><span>收货人</span><strong>{{ order.receiverName || '-' }}</strong></div>
          <div class="stat-line"><span>手机号</span><strong>{{ order.receiverPhone || '-' }}</strong></div>
          <div class="stat-line"><span>地址</span><strong>{{ addressText }}</strong></div>
          <div class="stat-line"><span>物流</span><strong>{{ deliveryText }}</strong></div>
        </div>
        <form v-if="canDeliverOrder && order.status === 1" class="panel-body form-grid" @submit.prevent="submitDeliver">
          <label class="field full">
            物流公司
            <input v-model.trim="deliveryCompany" required />
          </label>
          <label class="field full">
            物流单号
            <input v-model.trim="deliverySn" required />
          </label>
          <button class="primary-button field full" type="submit">确认发货</button>
        </form>
        <div v-if="message" class="message success panel-body">{{ message }}</div>
        <div v-if="error" class="message error panel-body">{{ error }}</div>
      </aside>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import SafeImage from '@/components/SafeImage.vue';
import StatusBadge from '@/components/StatusBadge.vue';
import { AdminPermissions } from '@/constants/permissions';
import { useAdminAuthStore } from '@/stores/adminAuthStore';
import { useAdminOrderStore } from '@/stores/adminOrderStore';
import { formatTime, money, orderTypeText } from '@/utils/format';

const route = useRoute();
const auth = useAdminAuthStore();
const store = useAdminOrderStore();
const order = computed(() => store.current);
const canDeliverOrder = computed(() => auth.hasPermission(AdminPermissions.ORDER_DELIVER));
const deliveryCompany = ref('');
const deliverySn = ref('');
const message = ref('');
const error = ref('');

const addressText = computed(() => {
  if (!order.value) return '-';
  return [order.value.receiverProvince, order.value.receiverCity, order.value.receiverRegion, order.value.receiverDetailAddress]
    .filter(Boolean)
    .join(' ') || '-';
});

const deliveryText = computed(() => {
  if (!order.value?.deliveryCompany) return '-';
  return `${order.value.deliveryCompany} ${order.value.deliverySn || ''}`;
});

async function load() {
  await store.loadDetail(String(route.params.orderSn));
}

async function submitDeliver() {
  if (!order.value) return;
  message.value = '';
  error.value = '';
  try {
    await store.deliver(order.value.orderSn, deliveryCompany.value, deliverySn.value);
    message.value = '订单已发货';
    await load();
  } catch (err) {
    error.value = err instanceof Error ? err.message : '发货失败';
  }
}

onMounted(load);
</script>
