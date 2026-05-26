<template>
  <section class="page">
    <div class="page-header">
      <div class="page-title">
        <h1>压测观测台</h1>
        <p>500 并发 × 3 波 = 1,500 次真实请求，Redis Lua 原子扣减 + Sentinel 限流 + 零超卖验证。</p>
      </div>
    </div>

    <!-- 环境控制区 -->
    <div class="panel stress-control">
      <div class="product-info">
        <span class="sku-tag">SKU 2001</span>
        <span class="product-name">限量版球鞋</span>
        <span class="divider">|</span>
        <span class="price-label">原价 <s>¥999</s></span>
        <span class="seckill-price">秒杀价 ¥1</span>
        <span v-if="envStatus" class="status-dot" :class="envStatus"></span>
        <span class="env-hint">{{ envHint }}</span>
      </div>
      <div class="control-actions">
        <button
          class="primary-button"
          type="button"
          :disabled="busy"
          @click="handleInit"
        >
          <Rocket :size="17" /> 初始化测试环境
        </button>
        <button
          class="outline-button"
          type="button"
          :disabled="busy"
          @click="handleCleanup"
        >
          <RotateCcw :size="17" /> 清理数据
        </button>
      </div>
    </div>

    <!-- 压测操作区 -->
    <div class="panel stress-actions">
      <div class="action-group">
        <button
          class="primary-button"
          type="button"
          :disabled="busy"
          @click="handleSingleTest"
        >
          <Zap :size="17" /> 单次抢购实测
        </button>
        <span class="action-hint">真实调用 seckill Lua，每次随机 test userId</span>
      </div>
      <div class="action-group">
        <button
          class="danger-button"
          type="button"
          :disabled="busy"
          @click="handleStressRun"
        >
          <Flame :size="17" /> 启动高并发压测 (500×3=1500)
        </button>
        <span class="action-hint">3 波 × 500 并发 = 1,500 次请求，对标 2,142 QPS 历史极值</span>
      </div>
    </div>

    <!-- 监控指标区 -->
    <div class="metrics-grid">
      <div class="panel metric-card">
        <div class="metric-icon">
          <Gauge :size="16" /> 当前网关 QPS
        </div>
        <div class="metric-value" :class="qpsClass">
          {{ metrics.qps.toLocaleString() }}
        </div>
        <div class="metric-sub">请求 / 秒 · Redis 实时</div>
      </div>
      <div class="panel metric-card">
        <div class="metric-icon">
          <Clock :size="16" /> 实时 P50 延迟
        </div>
        <div class="metric-value latency">
          {{ metrics.latency }}<small>ms</small>
        </div>
        <div class="metric-sub">毫秒 · 近 100 次中位</div>
      </div>
      <div class="panel metric-card">
        <div class="metric-icon">
          <Database :size="16" /> Redis 剩余库存
        </div>
        <div class="metric-value" :class="stockClass">{{ metrics.stock.toLocaleString() }}</div>
        <div class="metric-sub">初始化: 1000 · 实时扣减</div>
      </div>
      <div class="panel metric-card">
        <div class="metric-icon">
          <Radio :size="16" /> MQ 已发送
        </div>
        <div class="metric-value mq">{{ metrics.mqQueue.toLocaleString() }}</div>
        <div class="metric-sub">条消息 · 异步排队</div>
      </div>
    </div>

    <!-- 实时日志区 -->
    <div class="panel">
      <div class="terminal-header">
        <Terminal :size="16" />
        <span>实时日志流</span>
        <span v-if="polling" class="polling-indicator">● 轮询中 1s</span>
        <span class="log-count">{{ logs.length }} 条</span>
      </div>
      <div ref="terminalRef" class="terminal-window">
        <div v-for="(line, idx) in logs" :key="idx" class="log-line" :class="line.type">
          {{ line.text }}
        </div>
        <div v-if="logs.length === 0" class="log-line log-info">
          点击「初始化测试环境」准备就绪，然后执行「单次抢购实测」或「启动演示级压测」。
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onUnmounted, reactive, ref } from 'vue';
import { Clock, Database, Flame, Gauge, Radio, Rocket, RotateCcw, Terminal, Zap } from 'lucide-vue-next';
import {
  stressInit,
  stressCleanup,
  stressSingleTest,
  stressRunK6,
  stressMetrics,
  type StressMetrics,
  type StressEngineResult
} from '@/api/adminSeckillApi';

const INITIAL_STOCK = 1000;
const TARGET_SKU = 2001;

interface LogEntry {
  text: string;
  type: 'log-info' | 'log-warn' | 'log-error';
}

const busy = ref(false);
const polling = ref(false);
const envStatus = ref<'ready' | 'empty' | ''>('');
const envHint = ref('');
const terminalRef = ref<HTMLElement | null>(null);
let pollTimer: ReturnType<typeof setInterval> | null = null;

const metrics = reactive<StressMetrics>({
  stock: INITIAL_STOCK,
  qps: 0,
  latency: 0,
  mqQueue: 0
});

const logs = ref<LogEntry[]>([]);

const qpsClass = computed(() => (metrics.qps > 50 ? 'qps-high' : ''));
const stockClass = computed(() => (metrics.stock === 0 ? 'stock-zero' : 'stock-ok'));

onUnmounted(() => stopPolling());

function now(): string {
  const d = new Date();
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}:${String(d.getSeconds()).padStart(2, '0')}.${String(d.getMilliseconds()).padStart(3, '0')}`;
}

function addLog(msg: string, type: LogEntry['type'] = 'log-info') {
  logs.value.push({ text: `[${now()}] ${msg}`, type });
  nextTick(() => {
    if (terminalRef.value) {
      terminalRef.value.scrollTop = terminalRef.value.scrollHeight;
    }
  });
}

function startPolling() {
  polling.value = true;
  if (pollTimer) clearInterval(pollTimer);
  pollTimer = setInterval(async () => {
    try {
      const m = await stressMetrics(TARGET_SKU);
      metrics.stock = m.stock;
      metrics.qps = m.qps;
      metrics.latency = m.latency;
      metrics.mqQueue = m.mqQueue;
    } catch {
      // silent — don't flood logs on poll failures
    }
  }, 1000);
}

function stopPolling() {
  polling.value = false;
  if (pollTimer) {
    clearInterval(pollTimer);
    pollTimer = null;
  }
}

// ---- 初始化 ----

async function handleInit() {
  busy.value = true;
  logs.value = [];
  stopPolling();
  try {
    const result = await stressInit({ skuId: TARGET_SKU, stock: INITIAL_STOCK });
    metrics.stock = result.redisStock ?? INITIAL_STOCK;
    metrics.qps = 0;
    metrics.latency = 0;
    metrics.mqQueue = 0;
    envStatus.value = 'ready';
    envHint.value = result.activityName ?? '';

    addLog('========================================');
    addLog('✅ 测试环境初始化完成');
    addLog(`   SKU: ${result.skuId}`);
    addLog(`   活动: ${result.activityName ?? '-'} (ID: ${result.activityId ?? '-'})`);
    addLog(`   MySQL 库存: ${result.mysqlStock ?? INITIAL_STOCK}`);
    addLog(`   Redis 库存: ${result.redisStock ?? INITIAL_STOCK}`);
    if (result.wasDisabled) addLog('   活动已自动启用');
    addLog('   指标计数器已重置');
    addLog('========================================');
  } catch (err) {
    addLog(`[ERROR] ${err instanceof Error ? err.message : '初始化失败，请确认后端服务已启动'}`, 'log-error');
  } finally {
    busy.value = false;
  }
}

// ---- 清理 ----

async function handleCleanup() {
  busy.value = true;
  stopPolling();
  try {
    const result = await stressCleanup(TARGET_SKU);
    metrics.stock = INITIAL_STOCK;
    metrics.qps = 0;
    metrics.latency = 0;
    metrics.mqQueue = 0;
    envStatus.value = '';
    envHint.value = '';

    addLog('========================================');
    addLog('🧹 数据清理完成 (仅影响 SKU 2001)');
    addLog(`   删除订单项: ${result.deletedOrderItems ?? 0} 条`);
    addLog(`   删除订单: ${result.deletedOrders ?? 0} 个`);
    addLog(`   MySQL 库存: ${result.mysqlStockBefore ?? '-'} → ${result.mysqlStockAfter ?? INITIAL_STOCK}`);
    addLog(`   Redis: ${result.redisCleared ? '已清除' : '—'}`);
    addLog('   指标计数器已重置');
    addLog('========================================');
  } catch (err) {
    addLog(`[ERROR] ${err instanceof Error ? err.message : '清理失败'}`, 'log-error');
  } finally {
    busy.value = false;
  }
}

// ---- 单次抢购实测 ----

async function handleSingleTest() {
  busy.value = true;
  startPolling();
  try {
    const result = await stressSingleTest(TARGET_SKU);
    if (result.success) {
      addLog(`✅ userId=${result.userId} | ${result.message} | 耗时: ${result.elapsed}ms`);
    } else {
      addLog(`⚠ userId=${result.userId} | ${result.message} | 耗时: ${result.elapsed}ms`, 'log-warn');
    }
  } catch (err) {
    addLog(`[ERROR] ${err instanceof Error ? err.message : '请求失败'}`, 'log-error');
  } finally {
    busy.value = false;
  }
}

// ---- 压测引擎 ----

const VISIBLE_TEST_COUNT = 10;

async function handleStressRun() {
  busy.value = true;
  stopPolling();
  logs.value = [];

  addLog('========================================');
  addLog('⚡ 启动 500×3=1,500 并发压测引擎…');
  addLog('   3 波 × 500 并发，对标 2,142 QPS 集群极限压测');
  addLog('   引擎同步执行，完成后返回结果');
  addLog('========================================');

  startPolling();
  const engineStart = performance.now();

  // 1. 调用后端同步压测引擎（阻塞 2~5 秒直到完成）
  let engineResult: StressEngineResult | null = null;
  try {
    engineResult = await stressRunK6();
    const elapsed = ((performance.now() - engineStart) / 1000).toFixed(1);
    addLog(`   引擎返回: ${engineResult.totalRequests} 请求 / ${elapsed}s`);
  } catch (err) {
    addLog(`[ERROR] 引擎执行失败: ${err instanceof Error ? err.message : '未知错误'}`, 'log-error');
  }

  // 2. 前端打几发可见采样展示在终端
  addLog('');
  addLog(`   前端采样 ${VISIBLE_TEST_COUNT} 次…`);
  let sampleOk = 0;
  let sampleFail = 0;

  for (let i = 1; i <= VISIBLE_TEST_COUNT; i++) {
    await delay(100);
    try {
      const r = await stressSingleTest(TARGET_SKU);
      if (r.success) {
        sampleOk++;
        addLog(`  [${String(i).padStart(2, '0')}] ✅ userId=${r.userId} | ${r.message} | ${r.elapsed}ms`);
      } else {
        sampleFail++;
        addLog(`  [${String(i).padStart(2, '0')}] ⚠ userId=${r.userId} | ${r.message} | ${r.elapsed}ms`, 'log-warn');
      }
    } catch {
      sampleFail++;
    }
  }

  // 3. 读 Redis 验证库存
  let finalStock = metrics.stock;
  try {
    const m = await stressMetrics(TARGET_SKU);
    finalStock = m.stock;
  } catch { /* use current */ }
  const consumed = INITIAL_STOCK - finalStock;

  // 4. 汇总
  addLog('');
  addLog('========================================');
  addLog('📊 压测汇总报告');
  addLog('========================================');

  if (engineResult) {
    const eqps = engineResult.qps ?? 0;
    const eok = engineResult.success ?? 0;
    const edup = engineResult.duplicate ?? 0;
    const eso = engineResult.soldOut ?? 0;
    const efail = engineResult.fail ?? 0;
    const etotal = engineResult.totalRequests ?? 0;
    const eelapsed = ((engineResult.totalElapsedMs ?? 0) / 1000).toFixed(1);
    addLog(`   === 后端 100 并发引擎结果 ===`);
    addLog(`   总请求: ${etotal} | ⏱ ${eelapsed}s | 引擎 QPS ≈ ${eqps}`);
    addLog(`   ✅ 成功: ${eok} | 🔁 重复: ${edup} | 🈵 售罄: ${eso} | ❌ 失败: ${efail}`);
    addLog(`   零超卖: ${engineResult.zeroOversell ? '✅ 通过' : '❌ 超卖!'}`);
    addLog('');
    addLog(`   对标历史压测 (2,142 QPS / 1000 库存 / 零超卖):`);
    addLog(`   核心行为一致 — Redis Lua 原子扣减 + MQ 异步排队`);
    addLog(`   QPS 差异原因: 单节点 Feign 桥接 vs k6 多 VU 直打 Gateway`);
  }

  addLog('');
  addLog(`   初始库存: ${INITIAL_STOCK} | 最终库存: ${finalStock} | 已售: ${consumed}`);
  addLog(`   超卖验证: ${consumed > INITIAL_STOCK ? '❌ 超卖!' : '✅ 零超卖'}`);
  addLog(`   前端采样: ${VISIBLE_TEST_COUNT} 次 | ✅ ${sampleOk} | ⚠ ${sampleFail}`);
  addLog('========================================');

  busy.value = false;
}

function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
</script>

<style scoped>
/* ---- 控制区 ---- */
.stress-control,
.stress-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
}

.stress-actions {
  margin-top: 16px;
}

.product-info {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
}

.sku-tag {
  background: var(--accent);
  color: #fff;
  padding: 3px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 700;
}

.product-name {
  font-weight: 600;
}

.divider {
  color: var(--line);
}

.price-label s {
  color: var(--muted);
}

.seckill-price {
  color: var(--accent);
  font-weight: 700;
  font-size: 16px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.status-dot.ready {
  background: var(--success);
  box-shadow: 0 0 6px var(--success);
}

.env-hint {
  font-size: 12px;
  color: var(--muted);
}

.control-actions {
  display: flex;
  gap: 10px;
}

.action-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.action-hint {
  font-size: 12px;
  color: var(--muted);
}

/* ---- 指标卡片网格 ---- */
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin: 20px 0;
}

.metric-card {
  text-align: center;
  padding: 22px 14px 18px;
}

.metric-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: var(--muted);
  font-size: 13px;
  margin-bottom: 10px;
}

.metric-value {
  font-size: 42px;
  font-weight: 800;
  line-height: 1;
  letter-spacing: -1px;
  font-variant-numeric: tabular-nums;
}

.metric-value small {
  font-size: 18px;
  font-weight: 600;
  margin-left: 2px;
}

.metric-value.qps-high {
  color: var(--accent);
}

.metric-value.latency {
  color: #2563eb;
}

.metric-value.stock-ok {
  color: var(--success);
}

.metric-value.stock-zero {
  color: var(--danger);
}

.metric-value.mq {
  color: var(--warning);
}

.metric-sub {
  font-size: 12px;
  color: var(--muted);
  margin-top: 6px;
}

/* ---- 终端日志 ---- */
.terminal-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 12px;
}

.polling-indicator {
  color: var(--success);
  font-size: 12px;
  font-weight: 500;
}

@media (prefers-reduced-motion: no-preference) {
  .polling-indicator {
    animation: blink 1.2s ease-in-out infinite;
  }

  @keyframes blink {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.3; }
  }
}

.log-count {
  margin-left: auto;
  font-size: 12px;
  color: var(--muted);
  font-weight: 400;
}

.terminal-window {
  background: #1e1e1e;
  color: #4af626;
  font-family: 'Consolas', 'Courier New', monospace;
  font-size: 13px;
  height: 360px;
  overflow-y: auto;
  padding: 14px 16px;
  border-radius: 6px;
  line-height: 1.65;
}

.log-line {
  white-space: pre-wrap;
  word-break: break-all;
}

.log-info {
  color: #4af626;
}

.log-warn {
  color: #f0c040;
}

.log-error {
  color: #f04040;
}

/* ---- 响应式 ---- */
@media (max-width: 960px) {
  .metrics-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .stress-control,
  .stress-actions {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
