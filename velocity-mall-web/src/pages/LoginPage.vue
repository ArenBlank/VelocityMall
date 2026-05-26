<script setup lang="ts">
import { Bell, Eye, EyeOff, LockKeyhole, ShieldCheck, UserRound, Zap } from 'lucide-vue-next';
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { fallbackCoverImages } from '@/config/media';
import { useAuthStore } from '@/stores/authStore';

type AuthMode = 'login' | 'register';

const props = withDefaults(
  defineProps<{
    initialMode?: AuthMode;
  }>(),
  {
    initialMode: 'login'
  }
);

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const mode = ref<AuthMode>(props.initialMode);
const username = ref('');
const password = ref('');
const error = ref('');
const success = ref('');
const showPassword = ref(false);

const title = computed(() => (mode.value === 'login' ? '登录 VelocityMall' : '注册 VelocityMall'));
const submitText = computed(() => (mode.value === 'login' ? '登录' : '注册'));
const loadingText = computed(() => (mode.value === 'login' ? '登录中' : '注册中'));
const switchText = computed(() => (mode.value === 'login' ? '还没有账号？' : '已有账号？'));
const switchActionText = computed(() => (mode.value === 'login' ? '去注册' : '去登录'));

function switchMode(nextMode: AuthMode) {
  if (mode.value === nextMode) {
    return;
  }
  mode.value = nextMode;
  error.value = '';
  success.value = '';
  void router.replace({
    name: nextMode === 'login' ? 'login' : 'register',
    query: route.query
  });
}

function redirectTarget() {
  const redirect = route.query.redirect;
  return typeof redirect === 'string' && redirect.startsWith('/') ? redirect : '/home';
}

async function submit() {
  error.value = '';
  success.value = '';

  try {
    if (mode.value === 'login') {
      await authStore.login(username.value, password.value);
      await router.push(redirectTarget());
      return;
    }

    await authStore.register(username.value, password.value);
    success.value = '注册成功，请使用当前账号登录';
    mode.value = 'login';
    password.value = '';
    void router.replace({ name: 'login', query: route.query });
  } catch (err) {
    error.value = err instanceof Error ? err.message : `${submitText.value}失败`;
  }
}

watch(
  () => props.initialMode,
  (nextMode) => {
    mode.value = nextMode;
    error.value = '';
    success.value = '';
  }
);
</script>

<template>
  <main class="auth-entry-page">
    <section class="auth-campaign">
      <div class="campaign-copy">
        <span class="campaign-badge"><Zap :size="24" fill="currentColor" /> 买家入口</span>
        <h1>VelocityMall</h1>
        <p>登录后参与秒杀、查看订单、模拟支付</p>
        <div class="campaign-facts">
          <span>用户登录</span>
          <i />
          <span>用户注册</span>
          <i />
          <span>订单状态同步</span>
        </div>
        <div class="campaign-divider" />
        <p class="campaign-note">
          账号登录后即可参与活动，并在订单中心查看支付状态。
        </p>

        <div class="campaign-steps" aria-label="买家使用流程">
          <div>
            <b>01</b>
            <span>登录或注册买家账号</span>
          </div>
          <div>
            <b>02</b>
            <span>进入秒杀专区提交抢购</span>
          </div>
          <div>
            <b>03</b>
            <span>订单生成后完成模拟支付</span>
          </div>
        </div>
      </div>

      <div class="campaign-visual">
        <img class="campaign-phone" :src="fallbackCoverImages[0]" alt="VelocityMall 商品封面" />
      </div>
    </section>

    <section class="auth-panel-modern" aria-label="VelocityMall 用户登录注册">
      <div class="auth-tabs-modern" role="tablist" aria-label="登录注册切换">
        <button :class="{ active: mode === 'login' }" type="button" @click="switchMode('login')">登录</button>
        <button :class="{ active: mode === 'register' }" type="button" @click="switchMode('register')">注册</button>
      </div>

      <form class="auth-form-modern" @submit.prevent="submit">
        <h2>{{ title }}</h2>
        <p>登录后参与秒杀、查看订单、模拟支付</p>

        <label>
          用户名
          <span class="auth-input">
            <UserRound :size="22" />
            <input v-model.trim="username" required placeholder="请输入用户名…" autocomplete="username" />
          </span>
        </label>

        <label>
          密码
          <span class="auth-input">
            <LockKeyhole :size="21" />
            <input
              v-model="password"
              required
              :type="showPassword ? 'text' : 'password'"
              placeholder="请输入密码…"
              :autocomplete="mode === 'login' ? 'current-password' : 'new-password'"
            />
            <button type="button" aria-label="切换密码可见性" @click="showPassword = !showPassword">
              <EyeOff v-if="showPassword" :size="21" />
              <Eye v-else :size="21" />
            </button>
          </span>
        </label>

        <button class="auth-submit" type="submit" :disabled="authStore.loading">
          {{ authStore.loading ? loadingText : submitText }}
        </button>

        <p class="auth-switch">
          {{ switchText }}
          <button type="button" @click="switchMode(mode === 'login' ? 'register' : 'login')">
            {{ switchActionText }}
          </button>
        </p>

        <div class="secure-note">
          <ShieldCheck :size="18" />
          登录状态会保存在当前浏览器
        </div>

        <div class="auth-message" :class="{ error, success }" aria-live="polite">
          <Bell :size="18" />
          <span>{{ error || success || '提示信息将在此处显示' }}</span>
        </div>
      </form>
    </section>
  </main>
</template>
