<template>
  <div class="dashboard">
    <div class="header">
      <div class="title">
        <div class="h1">统计分析</div>
        <div class="subtitle">可切换维度与时间范围的可视化模块</div>
      </div>

      <div class="filters">
        <el-select v-model="metricKey" class="sel" size="large" @change="renderAll">
          <el-option
              v-for="m in metrics"
              :key="m.key"
              :label="m.label"
              :value="m.key"
          />
        </el-select>

        <el-select v-model="groupBy" class="sel" size="large" @change="renderAll">
          <el-option v-for="g in groupByOptions" :key="g.value" :label="g.label" :value="g.value" />
        </el-select>

        <el-select v-model="overrideType" class="sel" size="large" clearable placeholder="按推荐图型" @change="renderAll">
          <el-option label="折线图" value="line"/>
          <el-option label="柱状图" value="bar"/>
          <el-option label="饼图" value="pie"/>
        </el-select>
      </div>
    </div>

    <div class="cards">
      <div class="card"><div ref="lineRef" class="chart"></div></div>
      <div class="card"><div ref="barRef" class="chart"></div></div>
      <div class="card"><div ref="pieRef" class="chart"></div></div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from 'vue'
import * as echarts from 'echarts'
import axios from 'axios'
import { ElMessage } from 'element-plus'

type Sex = '男' | '女' | '其他' | '未知'
interface HealthRecordShowVo {
  recordId: number
  name: string
  sex: Sex
  age: number | null
  hdlC: number | null
  ldlC: number | null
  vldlC: number | null
  triglyceride: number | null
  totalCholesterol: number | null
  pulse: number | null
  diastolicBp: number | null
  hypertensionHistory: boolean
  bun: number | null
  uricAcid: number | null
  creatinine: number | null
}

export default defineComponent({
  name: 'HealthRecordDashboardOptions',

  data() {
    return {
      // 选择状态
      metricKey: 'totalCholesterol' as string,
      groupBy: 'sex' as 'none' | 'sex' | 'hypertensionHistory' | 'ageBucket',
      overrideType: '' as '' | 'line' | 'bar' | 'pie',

      // 数据
      raw: [] as HealthRecordShowVo[],

      // echarts 实例
      chartLine: null as echarts.ECharts | null,
      chartBar: null as echarts.ECharts | null,
      chartPie: null as echarts.ECharts | null,

      // DOM 引用（Options API 用 $refs）
      lineRef: null as HTMLDivElement | null,
      barRef: null as HTMLDivElement | null,
      pieRef: null as HTMLDivElement | null,

      // 配置
      metrics: [
        { key: 'totalCholesterol', label: '总胆固醇', kind: 'number', recommend: { line: true, bar: true, pie: true }, buckets: [3,4,5,6,7,8] },
        { key: 'triglyceride', label: '甘油三酯', kind: 'number', recommend: { line: true, bar: true, pie: true }, buckets: [0.8,1.7,2.3,5.6] },
        { key: 'hdlC', label: '高密度脂蛋白', kind: 'number', recommend: { line: true, bar: true, pie: true }, buckets: [0.8,1.0,1.3,1.6] },
        { key: 'ldlC', label: '低密度脂蛋白', kind: 'number', recommend: { line: true, bar: true, pie: true }, buckets: [1.8,2.6,3.4,4.1] },
        { key: 'vldlC', label: '极低密度脂蛋白', kind: 'number', recommend: { line: true, bar: true, pie: true }, buckets: [0.2,0.4,0.8] },
        { key: 'pulse', label: '脉搏(次/分)', kind: 'number', recommend: { line: true, bar: true, pie: true }, buckets: [60,80,100,120] },
        { key: 'diastolicBp', label: '舒张压(mmHg)', kind: 'number', recommend: { line: true, bar: true, pie: true }, buckets: [60,80,90,100] },
        { key: 'bun', label: '尿素氮', kind: 'number', recommend: { line: true, bar: true, pie: true }, buckets: [3,7,9] },
        { key: 'uricAcid', label: '尿酸', kind: 'number', recommend: { line: true, bar: true, pie: true }, buckets: [240,360,420,480] },
        { key: 'creatinine', label: '肌酐', kind: 'number', recommend: { line: true, bar: true, pie: true }, buckets: [60,97,133,186] },
        { key: 'age', label: '年龄(岁)', kind: 'number', recommend: { line: true, bar: true, pie: true }, buckets: [18,30,40,50,60,70,80] },
        { key: 'sex', label: '性别', kind: 'enum', recommend: { line: false, bar: true, pie: true } },
        { key: 'hypertensionHistory', label: '高血压史', kind: 'bool', recommend: { line: false, bar: true, pie: true } },
      ] as const,

      groupByOptions: [
        { label: '不分组（整体）', value: 'none' },
        { label: '按性别', value: 'sex' },
        { label: '按高血压史', value: 'hypertensionHistory' },
        { label: '按年龄段', value: 'ageBucket' },
      ],
    }
  },

  computed: {
    groupByLabel(): string {
      const f = this.groupByOptions.find(g => g.value === this.groupBy)
      return f?.label ?? ''
    },
    metricDef(): any {
      return this.metrics.find((m:any) => m.key === this.metricKey)
    }
  },

  mounted() {
    this.fetchAll().then(() => {
      // refs
      this.lineRef = this.$refs.lineRef as HTMLDivElement
      this.barRef  = this.$refs.barRef as HTMLDivElement
      this.pieRef  = this.$refs.pieRef as HTMLDivElement

      // init charts
      this.chartLine = echarts.init(this.lineRef!, 'dark')
      this.chartBar  = echarts.init(this.barRef!, 'dark')
      this.chartPie  = echarts.init(this.pieRef!, 'dark')
      this.renderAll()

      window.addEventListener('resize', this.handleResize)
    })
  },

  beforeUnmount() {
    window.removeEventListener('resize', this.handleResize)
    this.chartLine?.dispose()
    this.chartBar?.dispose()
    this.chartPie?.dispose()
  },

  methods: {
    async fetchAll() {
      try {
        const resp = await axios.get('/api/HealthRecordShow/getAll')
        const data = Array.isArray(resp.data) ? resp.data : (resp.data?.data ?? [])
        this.raw = data || []
      } catch (e:any) {
        ElMessage.error(e?.message || '加载失败')
      }
    },

    handleResize() {
      this.chartLine?.resize()
      this.chartBar?.resize()
      this.chartPie?.resize()
    },

    // —— 工具 —— //
    bucketize(val: number, cuts: number[]) {
      if (val == null || Number.isNaN(val)) return '未知'
      for (let i = 0; i < cuts.length; i++) {
        if (val < cuts[i]) {
          const left = i === 0 ? '-∞' : `${cuts[i-1]}`
          const right = `${cuts[i]}`
          return `${left}~${right}`
        }
      }
      const last = cuts[cuts.length - 1]
      return `${last}+`
    },

    ageBucket(n?: number | null) {
      if (n == null) return '未知'
      const cuts = [18,30,40,50,60,70,80]
      return this.bucketize(n, cuts)
    },

    groupKey(rec: HealthRecordShowVo, by: string) {
      switch (by) {
        case 'sex': return rec.sex ?? '未知'
        case 'hypertensionHistory': return rec.hypertensionHistory ? '有' : '无'
        case 'ageBucket': return this.ageBucket(rec.age)
        default: return '整体'
      }
    },

    groupedMean(metricKey: string, by: string) {
      const m:any = this.metrics.find((x:any) => x.key === metricKey)
      const agg = new Map<string, { sum: number, cnt: number }>()
      for (const r of this.raw) {
        const g = by === 'none' ? '整体' : this.groupKey(r, by)
        const v:any = (r as any)[m.key]
        if (m.kind === 'number' && typeof v === 'number') {
          if (!agg.has(g)) agg.set(g, { sum: 0, cnt: 0 })
          const obj = agg.get(g)!
          obj.sum += v
          obj.cnt += 1
        } else if (m.kind !== 'number') {
          const key = String(v ?? '未知')
          if (!agg.has(key)) agg.set(key, { sum: 0, cnt: 0 })
          agg.get(key)!.cnt += 1
        }
      }
      const cats:string[] = []
      const values:number[] = []
      for (const [k, v] of Array.from(agg.entries()).sort()) {
        cats.push(k)
        values.push(m.kind === 'number' ? (v.sum / Math.max(1, v.cnt)) : v.cnt)
      }
      return { cats, values }
    },

    bucketDist(metricKey: string) {
      const m:any = this.metrics.find((x:any) => x.key === metricKey)
      if (m.kind !== 'number' || !m.buckets) return []
      const counter = new Map<string, number>()
      for (const r of this.raw) {
        const v:any = (r as any)[m.key]
        const k = typeof v === 'number' ? this.bucketize(v, m.buckets) : '未知'
        counter.set(k, (counter.get(k) || 0) + 1)
      }
      return Array.from(counter.entries()).map(([name, value]) => ({ name, value }))
    },

    categoricalDist(metricKey: string) {
      const m:any = this.metrics.find((x:any) => x.key === metricKey)
      const counter = new Map<string, number>()
      for (const r of this.raw) {
        const v:any = (r as any)[m.key]
        const k = m.kind === 'bool' ? (v ? '是' : '否') : (v ?? '未知')
        counter.set(String(k), (counter.get(String(k)) || 0) + 1)
      }
      return Array.from(counter.entries()).map(([name, value]) => ({ name, value }))
    },

    seriesByIndividuals(metricKey: string) {
      const m:any = this.metrics.find((x:any) => x.key === metricKey)
      const rows = this.raw
          .filter(r => typeof (r as any)[m.key] === 'number')
          .sort((a,b)=> (a.recordId ?? 0) - (b.recordId ?? 0))
      return {
        cats: rows.map(r => r.name ?? `#${r.recordId}`),
        values: rows.map(r => (r as any)[m.key] as number)
      }
    },

    // —— 渲染 —— //
    renderAll() {
      const m:any = this.metricDef
      const metricName = m?.label || this.metricKey

      const doLine = this.overrideType ? this.overrideType === 'line' : m.recommend.line
      const doBar  = this.overrideType ? this.overrideType === 'bar'  : m.recommend.bar
      const doPie  = this.overrideType ? this.overrideType === 'pie'  : m.recommend.pie

      // 折线
      if (doLine && this.chartLine) {
        const { cats, values } = m.kind === 'number'
            ? this.seriesByIndividuals(m.key)
            : this.groupedMean(m.key, 'none')
        this.chartLine.setOption({
          grid: { left: 32, right: 16, top: 30, bottom: 30 },
          tooltip: { trigger: 'axis' },
          xAxis: { type: 'category', data: cats, boundaryGap: false },
          yAxis: { type: 'value' },
          series: [{ type: 'line', smooth: true, data: values, areaStyle: {} }],
          title: { text: `${metricName} · 个体序列` }
        })
      } else { this.chartLine?.clear() }

      // 柱状
      if (doBar && this.chartBar) {
        const { cats, values } = this.groupedMean(m.key, this.groupBy)
        this.chartBar.setOption({
          grid: { left: 36, right: 16, top: 30, bottom: 40 },
          tooltip: { trigger: 'axis' },
          xAxis: { type: 'category', data: cats, axisLabel: { rotate: 20 } },
          yAxis: { type: 'value' },
          series: [{ type: 'bar', data: values, barMaxWidth: 36 }],
          title: { text: `按「${this.groupByLabel}」分组 · ${m.kind==='number'?'均值':'计数'}` }
        })
      } else { this.chartBar?.clear() }

      // 饼图
      if (doPie && this.chartPie) {
        const pieData = m.kind === 'number' ? this.bucketDist(m.key) : this.categoricalDist(m.key)
        this.chartPie.setOption({
          tooltip: { trigger: 'item' },
          series: [{ type: 'pie', radius: ['60%','85%'], label: { show: false }, data: pieData }],
          title: { text: `${metricName} · 占比` }
        })
      } else { this.chartPie?.clear() }

      this.chartLine?.resize()
      this.chartBar?.resize()
      this.chartPie?.resize()
    },
  }
})
</script>

<style scoped>
/* 让根结点也能撑满（若已在全局写过可忽略） */
:global(html, body, #app) {
  height: 100%;
  margin: 0;
}

/* 整个页面：占满视口，高度100vh，纵向flex */
.dashboard {
  height: 100vh;               /* 关键：撑满整屏 */
  display: flex;
  flex-direction: column;
  background: #0b1220;
  color: #dbe2f1;
  padding: 16px 20px 20px;     /* 需要无边距可改为 0 */
  box-sizing: border-box;
}

/* 头部固定高度 */
.header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 12px;
}

.title .h1 { font-size: 26px; font-weight: 800; letter-spacing: 1px; }
.subtitle { margin-top: 6px; opacity: .7; font-size: 14px; }
.filters { display: flex; gap: 12px; }
.sel { width: 180px; }

/* 内容区占满除头部外的所有空间 */
.cards {
  flex: 1;                     /* 关键：占据剩余高度 */
  min-height: 0;               /* 避免子元素撑爆容器 */
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

/* 卡片和图表充满网格单元 */
.card {
  background: linear-gradient(180deg, rgba(255,255,255,.04), rgba(255,255,255,.02));
  border: 1px solid rgba(255,255,255,.06);
  border-radius: 14px;
  padding: 6px;
  display: flex;
  min-height: 0;               /* 关键：允许子元素按父容器高度收缩 */
}

.chart {
  flex: 1;
  height: 100%;                /* 关键：图表跟随卡片高度 */
  min-height: 0;
}

/* 小屏改为单列，并给每张图一个合理高度 */
@media (max-width: 1200px) {
  .cards { grid-template-columns: 1fr; }
  .chart { height: 280px; }    /* 单列时用固定高度更稳；可按需增大 */
}

</style>
