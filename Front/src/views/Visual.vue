<template>
  <div class="dashboard">
    <Header />

    <div class="header">
      <div class="title">
        <div class="h1">统计分析（全量图表）</div>
        <div class="subtitle">全局性别/年龄筛选 + 每图表独立切换饼图/柱状图</div>
      </div>

      <!-- 顶部右侧：全局筛选 + 刷新 -->
      <div class="ops">
        <el-select
            v-model="globalSex"
            size="large"
            class="ctrl"
            placeholder="性别"
            @change="renderAll"
        >
          <el-option v-for="s in sexOptions" :key="s" :label="s" :value="s" />
        </el-select>

        <el-select
            v-model="globalAgeBucket"
            size="large"
            class="ctrl"
            placeholder="年龄区间"
            @change="renderAll"
        >
          <el-option v-for="ab in ageBucketOptions" :key="ab" :label="ab" :value="ab" />
        </el-select>

        <el-button :loading="loading" @click="reload">刷新</el-button>
      </div>
    </div>

    <div class="groups" v-loading="loading">
      <div v-for="(cat, ci) in categoriesList" :key="cat.key" class="group">
        <div class="group-title">{{ cat.label }}</div>

        <div class="cards">
          <div
              v-for="mKey in cat.metrics"
              :key="`${cat.key}-${mKey}`"
              class="card"
          >
            <div class="card-header">
              <div class="card-title">{{ metricLabel(mKey) }}</div>
              <!-- 每个图自己的饼/柱切换按钮 -->
              <div class="card-controls">
                <el-button
                    size="small"
                    type="primary"
                    plain
                    @click="toggleChartTypeFor(chartId(ci, String(mKey)))"
                >
                  {{ getChartType(chartId(ci, String(mKey))) === 'pie' ? '切换为柱状图' : '切回饼图' }}
                </el-button>
              </div>
            </div>

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
// 手动管理 echarts 实例
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { getAllHealthRecords } from '@/api/HealthRecordShow.js' // 若报错可改为 '@/api/HealthRecordShow'
import Header from '@/components/Header.vue'

export default {
  name: 'VisualAllChartsWithGlobalFilter',

  components: { Header },

  data() {
    return {
      loading: false,
      raw: [],

      // 全局筛选
      globalSex: '全部',
      globalAgeBucket: '全部',

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
        { key: 'demographic',  label: '基本信息',         metrics: ['sex', 'hypertensionHistory', 'age'] },
        { key: 'lipid',        label: '血脂相关',         metrics: ['totalCholesterol','triglyceride','hdlC','ldlC','vldlC'] },
        { key: 'renal_vitals', label: '生命体征 / 肾功能', metrics: ['pulse','diastolicBp','bun','uricAcid','creatinine'] },
      ],

      // DOM 与 ECharts 实例缓存
      elMap: Object.create(null),    // { id: HTMLDivElement }
      chartMap: Object.create(null), // { id: EChartsInstance }

      // 每图的类型：pie/bar
      chartTypeMap: Object.create(null), // { id: 'pie' | 'bar' }

      // 维度选项
      sexOptions: ['全部', '男', '女', '其他', '未知'],
    }
  },

  computed: {
    ageBucketOptions() {
      const labels = this.bucketLabels([18,30,40,50,60,70,80])
      return ['全部', ...labels]
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
    // --- 公共 ---
    chartId(catIndex, metricKey) {
      return `c${catIndex}-${metricKey}`
    },

    registerEl(el, catIndex, metricKey) {
      const id = this.chartId(catIndex, metricKey)
      if (el) {
        this.elMap[id] = el
        // 初始化图表类型（默认饼图）
        if (!this.chartTypeMap[id]) this.chartTypeMap[id] = 'pie'
      } else {
        this.disposeOne(id)
        delete this.elMap[id]
      }
    },

    async fetchAll() {
      this.loading = true
      try {
        const res = await getAllHealthRecords()
        const data = Array.isArray(res?.data) ? res.data : (res?.data?.data ?? [])
        this.raw = data || []
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

    // --- 图类型（每图独立）---
    getChartType(id) {
      return this.chartTypeMap[id] || 'pie'
    },
    toggleChartTypeFor(id) {
      this.chartTypeMap[id] = this.getChartType(id) === 'pie' ? 'bar' : 'pie'
      // 只重绘这个图
      const [ci, metricKey] = id.replace(/^c/, '').split('-') // "c{ci}-{metric}"
      this.renderOne(Number(ci), metricKey)
    },

    // --- 工具 ---
    metricDef(key) {
      return this.metrics.find(m => m.key === key)
    },
    metricLabel(key) {
      const m = this.metricDef(key)
      return m?.label || key
    },
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
    bucketLabels(cuts) {
      const labels = []
      for (let i = 0; i < cuts.length; i++) {
        if (i === 0) labels.push(`-∞~${cuts[i]}`)
        else labels.push(`${cuts[i-1]}~${cuts[i]}`)
      }
      labels.push(`${cuts[cuts.length - 1]}+`)
      return labels
    },
    ageBucket(n) {
      if (n == null) return '未知'
      const cuts = [18,30,40,50,60,70,80]
      return this.bucketize(n, cuts)
    },

    // 应用全局筛选
    applyGlobalFilters(records) {
      let arr = records || []
      if (this.globalSex && this.globalSex !== '全部') {
        arr = arr.filter(r => (r?.sex ?? '未知') === this.globalSex)
      }
      if (this.globalAgeBucket && this.globalAgeBucket !== '全部') {
        arr = arr.filter(r => this.ageBucket(r?.age ?? null) === this.globalAgeBucket)
      }
      return arr
    },

    // 统一统计：返回 [{name, value}]
    makeItems(metricKey, filtered) {
      const m = this.metricDef(metricKey)
      if (!m) return [{ name: '暂无数据', value: 1 }]
      const counter = new Map()

      for (const r of filtered) {
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

    // --- 渲染 ---
    renderOne(catIndex, metricKey) {
      const id = this.chartId(catIndex, metricKey)
      const el = this.elMap[id]
      if (!el) return

      // 清理旧实例
      this.disposeOne(id)
      const chart = echarts.init(el)
      this.chartMap[id] = chart

      const filtered = this.applyGlobalFilters(this.raw)
      const titleText = this.metricLabel(metricKey)
      const items = filtered.length ? this.makeItems(metricKey, filtered) : [{ name: '暂无数据', value: 1 }]
      const chartType = this.getChartType(id)

      const legendCommon = {
        right: 10,
        bottom: 10,
        type: 'scroll',
        orient: 'vertical'
      }

      if (chartType === 'pie') {
        chart.setOption({
          title: {
            text: titleText,
            left: 'center',
            top: 8,
            textStyle: { fontSize: 15, color: '#111827' }
          },
          tooltip: { trigger: 'item' },
          legend: { ...legendCommon, data: items.map(d => d.name), textStyle: { color: '#374151' } },
          series: [{
            type: 'pie',
            radius: ['55%','78%'],
            center: ['50%','56%'],
            label: { show: false },
            data: items
          }]
        })
      } else {
        const categories = items.map(d => d.name)
        const values = items.map(d => d.value)
        chart.setOption({
          title: {
            text: titleText,
            left: 'center',
            top: 8,
            textStyle: { fontSize: 15, color: '#111827' }
          },
          tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
          legend: { ...legendCommon, data: [titleText], textStyle: { color: '#374151' } },
          grid: { left: 50, right: 120, top: 60, bottom: 60, containLabel: true },
          xAxis: { type: 'category', data: categories, axisLabel: { interval: 0, rotate: 20, color: '#374151' }, axisLine: { lineStyle: { color: '#E5E7EB' } } },
          yAxis: { type: 'value', name: '数量', axisLabel: { color: '#374151' }, splitLine: { lineStyle: { color: '#F3F4F6' } }, axisLine: { lineStyle: { color: '#E5E7EB' } } },
          series: [{
            name: titleText,
            type: 'bar',
            data: values,
            barMaxWidth: 40,
            label: { show: true, position: 'top', color: '#374151' },
            itemStyle: { borderRadius: [4,4,0,0] }
          }]
        })
      }
    },

    renderAll() {
      this.categoriesList.forEach((cat, ci) => {
        cat.metrics.forEach(mKey => this.renderOne(ci, String(mKey)))
      })
      this.handleResize()
    },

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
  /* 浅色背景 */
  background: #ffffff;
  color: #111827;              /* 灰黑文字 */
  padding: 16px 20px 20px;
  box-sizing: border-box;
}

/* 顶部 */
.header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 14px;
  border-bottom: 1px solid #E5E7EB; /* 浅灰分割线 */
  padding-bottom: 10px;
}
.title .h1 { font-size: 26px; font-weight: 800; color: #0f172a; }
.subtitle { margin-top: 6px; color: #6B7280; font-size: 14px; }
.ops { display: flex; gap: 12px; align-items: center; }
.ctrl { width: 160px; }

/* 容器 */
.groups {
  flex: 1;
  min-height: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 22px;
}

/* 组标题 */
.group-title {
  font-size: 16px;
  font-weight: 700;
  margin: 6px 2px 10px;
  color: #111827;
}

/* 卡片：白底 + 阴影 + 浅灰边框；两列更大 */
.cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 18px;
}

.card {
  background: #ffffff;
  border: 1px solid #E5E7EB;
  border-radius: 14px;
  padding: 10px 12px 12px;
  display: flex;
  flex-direction: column;
  min-height: 380px;
  box-shadow: 0 1px 2px rgba(16,24,40,0.06), 0 1px 3px rgba(16,24,40,0.10);
}

/* 卡片头：左标题、右“切换图形”按钮 */
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}
.card-title {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}
.card-controls { display: flex; gap: 8px; align-items: center; }

/* 图表容器 */
.chart {
  flex: 1;
  min-height: 300px;
}

/* 小屏：单列 */
@media (max-width: 1200px) {
  .cards { grid-template-columns: 1fr; }
}
</style>
