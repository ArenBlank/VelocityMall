<template>
  <main class="login-shell admin-login-shell">
    <div class="admin-login-topline">
      <div class="admin-login-logo">
        <strong>Velocity<span>Mall</span></strong>
        <small>Admin Console</small>
      </div>
      <span>真实后端运营工作台</span>
    </div>

    <section class="login-card admin-login-card">
      <div class="login-brand-panel admin-login-brand">
        <span class="login-badge"><ShieldCheck :size="18" /> 管理员入口</span>
        <h1>VelocityMall<br />Admin</h1>
        <p>集中维护商品、订单、秒杀活动、优惠券与评价，让 C 端体验保持实时一致。</p>

        <div class="admin-login-flow">
          <div>
            <PackageSearch :size="20" />
            <strong>商品运营</strong>
            <span>SPU/SKU、封面、上下架</span>
          </div>
          <div>
            <Zap :size="20" />
            <strong>秒杀配置</strong>
            <span>活动时间、库存与预热</span>
          </div>
          <div>
            <ReceiptText :size="20" />
            <strong>订单处理</strong>
            <span>发货、状态同步、售后查看</span>
          </div>
        </div>
      </div>

      <div class="login-form-panel admin-login-form">
        <div class="admin-login-form-heading">
          <span>安全登录</span>
          <h2>登录管理端</h2>
          <p class="muted">使用管理员账号进入运营工作台。</p>
        </div>
        <form @submit.prevent="submit">
          <label class="field">
            用户名
            <input v-model.trim="username" autocomplete="username" placeholder="请输入管理员用户名…" />
          </label>
          <label class="field">
            密码
            <input v-model="password" autocomplete="current-password" placeholder="请输入密码…" type="password" />
          </label>
          <button class="primary-button" type="submit" :disabled="auth.loading">
            <LoaderCircle v-if="auth.loading" :size="18" class="spin" />
            登录
          </button>
        </form>
        <div class="message" :class="{ error: Boolean(message) }">
          {{ message || '本地开发默认管理员账号：admin / 123456' }}
        </div>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { LoaderCircle, PackageSearch, ReceiptText, ShieldCheck, Zap } from 'lucide-vue-next';
import { useAdminAuthStore } from '@/stores/adminAuthStore';

const auth = useAdminAuthStore();
const router = useRouter();
const route = useRoute();
const username = ref('admin');
const password = ref('123456');
const localError = ref('');
const message = computed(() => localError.value || auth.error);

async function submit() {
  localError.value = '';
  if (!username.value || !password.value) {
    localError.value = '请输入用户名和密码';
    return;
  }
  try {
    await auth.login(username.value, password.value);
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/products';
    await router.push(redirect);
  } catch (error) {
    localError.value = error instanceof Error ? error.message : '登录失败';
  }
}
</script>
