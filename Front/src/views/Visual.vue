<template>
  <div class="dashboard">
    <div class="header">
      <div class="title">
        <div class="h1">统计分析（全量饼图）</div>
        <div class="subtitle">三大类全部指标一次性展示（仅饼图）</div>
      </div>
      <div class="ops">
        <el-button :loading="loading" @click="reload">刷新</el-button>
        <el-button @click="debugLog" :disabled="!raw.length">打印到控制台</el-button>
      </div>
    </div>

    <!-- 调试面板：确认是否真的拿到数据 -->
    <div class="debug" v-if="!loading">
      <el-alert
          :title="`接口已返回 ${raw.length} 条记录`"
          type="success"
          show-icon
          :closable="false"
      />
      <div class="debug-json" v-if="raw.length">
        <div class="debug-hint">预览前 5 条（仅展示主要字段）：</div>
        <pre>{{ previewJSON }}</pre>
      </div>
      <div class="debug-json" v-else>
        <div class="debug-hint">暂无数据，请检查接口或代理。</div>
      </div>
    </div>

    <div class="groups" v-loading="loading">
      <!-- 遍历每个类别 -->
      <div v-for="(cat, ci) in categoriesList" :key="cat.key" class="group">
        <div class="group-title">{{ cat.label }}</div>

        <div class="cards">
          <!-- 遍历该类中的每个指标，按指标生成一张饼图 -->
          <div
              v-for="mKey in cat.metrics"
              :key="`${cat.key}-${mKey}`"
              class="card"
          >
            <div class="card-title">{{ metricLabel(mKey) }}</div>
            <!-- 参考 DetailFolder：用 ref 挂载 DOM，渲染前先 dispose -->
            <div
                class="chart"
                :ref="(el) => registerEl(el, ci, String(mKey))"
            ></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
// 仿照 DetailFolder：使用普通 JS，手动管理 echarts 实例更稳
import * as echarts from 'echarts'
import { ElMessage, ElAlert } from 'element-plus'
import { getAllHealthRecords } from '@/api/HealthRecordShow.js' // 若报找不到，改成 '@/api/HealthRecordShow'

export default {
  name: 'VisualAllPies',
  components: { ElAlert },

  data() {
    return {
      loading: false,
      raw: [],

      // 指标定义（数值型给出分箱）
      metrics: [
        { key: 'totalCholesterol', label: '总胆固醇', kind: 'number', buckets: [3,4,5,6,7,8] },
        { key: 'triglyceride',     label: '甘油三酯', kind: 'number', buckets: [0.8,1.7,2.3,5.6] },
        { key: 'hdlC',             label: '高密度脂蛋白', kind: 'number', buckets: [0.8,1.0,1.3,1.6] },
        { key: 'ldlC',             label: '低密度脂蛋白', kind: 'number', buckets: [1.8,2.6,3.4,4.1] },
        { key: 'vldlC',            label: '极低密度脂蛋白', kind: 'number', buckets: [0.2,0.4,0.8] },
        { key: 'pulse',            label: '脉搏(次/分)', kind: 'number', buckets: [60,80,100,120] },
        { key: 'diastolicBp',      label: '舒张压(mmHg)', kind: 'number', buckets: [60,80,90,100] },
        { key: 'bun',              label: '尿素氮', kind: 'number', buckets: [3,7,9] },
        { key: 'uricAcid',         label: '尿酸', kind: 'number', buckets: [240,360,420,480] },
        { key: 'creatinine',       label: '肌酐', kind: 'number', buckets: [60,97,133,186] },
        { key: 'age',              label: '年龄(岁)', kind: 'number', buckets: [18,30,40,50,60,70,80] },
        { key: 'sex',              label: '性别', kind: 'enum' },
        { key: 'hypertensionHistory', label: '高血压史', kind: 'bool' },
      ],

      // 三大类：一次性全部展示
      categoriesList: [
        {
          key: 'demographic',
          label: '基本信息',
          metrics: ['sex', 'hypertensionHistory', 'age'],
        },
        {
          key: 'lipid',
          label: '血脂相关',
          metrics: ['totalCholesterol','triglyceride','hdlC','ldlC','vldlC'],
        },
        {
          key: 'renal_vitals',
          label: '生命体征 / 肾功能',
          metrics: ['pulse','diastolicBp','bun','uricAcid','creatinine'],
        },
      ],

      // —— 图表实例/DOM 缓存（参考 DetailFolder）——
      elMap: Object.create(null),   // { id: HTMLDivElement }
      chartMap: Object.create(null) // { id: EChartsInstance }
    }
  },

  computed: {
    // 调试区：仅取前 5 条常见字段便于查看
    previewJSON() {
      const keys = ['recordId','name','sex','age','totalCholesterol','triglyceride','hdlC','ldlC']
      const arr = (this.raw || []).slice(0, 5).map(r => {
        const o = {}; keys.forEach(k => o[k] = r[k]); return o
      })
      return JSON.stringify(arr, null, 2)
    }
  },

  async mounted() {
    await this.fetchAll()
    this.$nextTick(() => this.renderAll())
    window.addEventListener('resize', this.handleResize)
  },

  beforeUnmount() {
    window.removeEventListener('resize', this.handleResize)
    this.disposeAll()
  },

  methods: {
    // 生成 DOM/图表 key（类别序号 + 指标键）
    chartId(catIndex, metricKey) {
      return `c${catIndex}-${metricKey}`
    },

    // 注册 DOM（和 DetailFolder 相同风格）
    registerEl(el, catIndex, metricKey) {
      const id = this.chartId(catIndex, metricKey)
      if (el) {
        this.elMap[id] = el
      } else {
        // 节点被销毁，释放图表
        this.disposeOne(id)
        delete this.elMap[id]
      }
    },

    // API
    async fetchAll() {
      this.loading = true
      try {
        const res = await getAllHealthRecords()
        console.log(res)
        // 兼容两种返回：直接数组 或 { data: [...] }
        const data = Array.isArray(res?.data) ? res.data : (res?.data?.data ?? [])
        this.raw = data || []

        // ✅ 自动打印：总数、前10条表格、完整对象
        /* eslint-disable no-console */
        console.log('[HealthRecord] total:', this.raw.length)
        console.table((this.raw || []).slice(0, 10))
        console.log('[HealthRecord] sample:', (this.raw || []).slice(0, 5))
        /* eslint-enable no-console */
      } catch (e) {
        ElMessage.error(e?.message || '加载失败')
        this.raw = []
      } finally {
        this.loading = false
      }
    },

    async reload() {
      await this.fetchAll()
      this.$nextTick(() => this.renderAll())
    },

    // 手动再打印一次
    debugLog() {
      /* eslint-disable no-console */
      console.log('[HealthRecord] total:', this.raw.length)
      console.table((this.raw || []).slice(0, 10))
      console.log('[HealthRecord] full data:', this.raw)
      /* eslint-enable no-console */
      ElMessage.success('已在控制台打印数据')
    },

    // 工具：查指标定义/名称
    metricDef(key) {
      return this.metrics.find(m => m.key === key)
    },
    metricLabel(key) {
      const m = this.metricDef(key)
      return m?.label || key
    },

    // 分箱
    bucketize(val, cuts) {
      if (val == null || Number.isNaN(val)) return '未知'
      for (let i = 0; i < cuts.length; i++) {
        if (val < cuts[i]) {
          const left = i === 0 ? '-∞' : String(cuts[i-1])
          const right = String(cuts[i])
          return `${left}~${right}`
        }
      }
      const last = cuts[cuts.length - 1]
      return `${last}+`
    },

    // 生成饼图数据
    makePieData(metricKey) {
      const m = this.metricDef(metricKey)
      if (!m) return [{ name: '暂无数据', value: 1 }]
      const counter = new Map()

      for (const r of this.raw) {
        const v = r[m.key]
        let name = '未知'
        if (m.kind === 'number' && m.buckets) {
          name = typeof v === 'number' ? this.bucketize(v, m.buckets) : '未知'
        } else if (m.kind === 'enum') {
          name = String(v ?? '未知')
        } else if (m.kind === 'bool') {
          name = v ? '是' : '否'
        } else {
          name = String(v ?? '未知')
        }
        counter.set(name, (counter.get(name) || 0) + 1)
      }

      return Array.from(counter.entries()).map(([name, value]) => ({ name, value }))
    },

    // 渲染一个饼图（先 dispose 再 init，参考 DetailFolder）
    renderOne(catIndex, metricKey) {
      const id = this.chartId(catIndex, metricKey)
      const el = this.elMap[id]
      if (!el) return

      // 先释放旧实例
      this.disposeOne(id)

      const chart = echarts.init(el)
      this.chartMap[id] = chart

      const m = this.metricDef(metricKey)
      const data = this.raw.length ? this.makePieData(metricKey) : [{ name: '暂无数据', value: 1 }]

      chart.setOption({
        title: { text: this.metricLabel(metricKey), left: 'center', top: 6, textStyle: { fontSize: 14 } },
        tooltip: { trigger: 'item' },
        series: [{
          type: 'pie',
          radius: ['55%', '80%'],
          center: ['50%', '55%'],
          label: { show: false },
          data
        }]
      })
    },

    // 渲染全部
    renderAll() {
      this.categoriesList.forEach((cat, ci) => {
        cat.metrics.forEach(mKey => this.renderOne(ci, String(mKey)))
      })
      this.handleResize()
    },

    // 释放
    disposeOne(id) {
      const ch = this.chartMap[id]
      if (ch) {
        try { ch.dispose() } catch {}
        delete this.chartMap[id]
      }
    },
    disposeAll() {
      Object.keys(this.chartMap).forEach(id => this.disposeOne(id))
    },

    // 自适应
    handleResize() {
      Object.values(this.chartMap).forEach(ch => {
        try { ch.resize() } catch {}
      })
    }
  }
}
</script>

<style scoped>
:global(html, body, #app) { height: 100%; margin: 0; }

.dashboard {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #0b1220;
  color: #dbe2f1;
  padding: 16px 20px 20px;
  box-sizing: border-box;
}

/* 顶部 */
.header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 12px;
}
.title .h1 { font-size: 26px; font-weight: 800; }
.subtitle { margin-top: 6px; opacity: .7; font-size: 14px; }
.ops { display: flex; gap: 12px; align-items: center; }

/* 调试面板 */
.debug { margin-bottom: 12px; }
.debug-json { margin-top: 8px; background: rgba(255,255,255,.05); border: 1px solid rgba(255,255,255,.08); border-radius: 8px; padding: 8px; }
.debug-hint { font-size: 12px; opacity: .7; margin-bottom: 4px; }
pre { margin: 0; white-space: pre-wrap; word-break: break-all; }

/* 分组容器 */
.groups {
  flex: 1;
  min-height: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

/* 单个组 */
.group-title {
  font-size: 16px;
  font-weight: 700;
  margin: 4px 2px 8px;
  opacity: .9;
}

/* 卡片栅格：每组内的所有饼图 */
.cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.card {
  background: linear-gradient(180deg, rgba(255,255,255,.04), rgba(255,255,255,.02));
  border: 1px solid rgba(255,255,255,.06);
  border-radius: 14px;
  padding: 10px 8px 8px;
  display: flex;
  flex-direction: column;
  min-height: 220px;
}

.card-title {
  font-size: 13px;
  opacity: .7;
  margin-bottom: 6px;
  padding-left: 6px;
}

.chart {
  flex: 1;
  min-height: 180px;
}

@media (max-width: 1200px) {
  .cards { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 768px) {
  .cards { grid-template-columns: 1fr; }
}
</style>
