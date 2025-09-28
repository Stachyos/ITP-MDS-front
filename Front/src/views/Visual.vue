<template>
  <div class="dashboard">
    <Header />

    <div class="header">
      <div class="header-content">
        <div class="title-section">
          <h1 class="main-title">All Charts Visualization（全量图表）</h1>
          <p class="subtitle">Global Filters by Gender/Age + Switch Pie/Bar Chart for Each（全局性别/年龄筛选 + 每图表独立切换饼图/柱状图）</p>
        </div>

        <!-- Top right: Global filters + Refresh -->
        <div class="controls-section">

          <div class="filter-controls">
            <el-select
                v-model="globalSex"
                size="large"
                class="filter-select"
                placeholder="Gender（性别）"
                @change="renderAll"
            >
              <el-option v-for="s in sexOptions" :key="s" :label="s" :value="s" />
            </el-select>

            <el-select
                v-model="globalAgeBucket"
                size="large"
                class="filter-select"
                placeholder="Age Range（年龄区间）"
                @change="renderAll"
            >
              <el-option v-for="ab in ageBucketOptions" :key="ab" :label="ab" :value="ab" />
            </el-select>

          </div>

          <el-button
              :loading="loading"
              @click="reload"
              class="refresh-btn"
              type="primary"
          >
            <i class="refresh-icon"></i>
            Refresh（刷新）
          </el-button>
        </div>
      </div>
    </div>

    <div class="content" v-loading="loading">
      <div class="groups" v-loading="loading">
        <div v-for="(cat, ci) in categoriesList" :key="cat.key" class="group-section">
          <div class="group-header">
            <h2 class="group-title">{{ cat.label }}</h2>
            <div class="group-divider"></div>
          </div>

          <div class="cards-grid">
            <div
                v-for="mKey in cat.metrics"
                :key="`${cat.key}-${mKey}`"
                class="chart-card"
                :class="{
            'active': activeChartId === chartId(ci, String(mKey)),
            'inactive': activeChartId && activeChartId !== chartId(ci, String(mKey))
          }"
                @mouseenter="handleChartHover(ci, String(mKey))"
                @mouseleave="handleChartLeave"
            >
              <div class="card-header">
                <h3 class="card-title">{{ metricLabel(mKey) }}</h3>
                <div class="card-controls">
                  <el-button
                      size="small"
                      :type="getChartType(chartId(ci, String(mKey))) === 'pie' ? 'primary' : 'success'"
                      @click="toggleChartTypeFor(chartId(ci, String(mKey)))"
                      class="toggle-btn"
                  >
                    <i :class="getChartType(chartId(ci, String(mKey))) === 'pie' ? 'bar-chart-icon' : 'pie-chart-icon'"></i>
                    {{ getChartType(chartId(ci, String(mKey))) === 'pie' ? 'Bar Chart' : 'Pie Chart' }}
                  </el-button>
                </div>
              </div>

              <div
                  class="chart-container"
                  :ref="(el) => registerEl(el, ci, String(mKey))"
              ></div>

              <!-- 悬停时显示的详细信息 -->
              <div v-if="activeChartId === chartId(ci, String(mKey))" class="chart-details">
                <div class="details-content">
                  <h4>详细分析</h4>
                  <p>{{ getChartDescription(mKey) }}</p>
                  <div class="stats-info">
                    <div class="stat-item">
                      <span class="stat-label">数据总量:</span>
                      <span class="stat-value">{{ getFilteredCount() }} 条记录</span>
                    </div>
                    <div class="stat-item">
                      <span class="stat-label">当前视图:</span>
                      <span class="stat-value">{{ getChartType(chartId(ci, String(mKey))) === 'pie' ? '饼状图' : '柱状图' }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
// Manually manage echarts instances
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { getAllHealthRecords } from '@/api/HealthRecordShow.js' // If error, change to '@/api/HealthRecordShow'
import Header from '@/components/Header.vue'

export default {
  name: 'VisualAllChartsWithGlobalFilter',

  components: { Header },

  data() {
    return {
      activeChartId: null, // 当前激活的图表ID
      hoverTimer: null, // 悬停计时器
      hoverDelay: 900, // 悬停延迟时间（毫秒）
      leaveDelay: 300,  // 离开延迟时间
      loading: false,
      raw: [],
      // 板块可见性控制
      categoryVisibility: {
        'demographic': true,
        'lipid': true,
        'renal_vitals': true
      },

      // 单个图表可见性控制 { [chartId]: boolean }
      chartVisibility: {},

      // 控制面板展开状态
      controlPanelExpanded: false,

      // Global filters
      globalSex: '全部 (All)',
      globalAgeBucket: '全部 (All)',

      // Metric definitions (numeric with buckets)
      metrics: [
        { key: 'totalCholesterol', label: '总胆固醇 (Total Cholesterol)', kind: 'number', buckets: [3,4,5,6,7,8] },
        { key: 'triglyceride',     label: '甘油三酯 (Triglyceride)', kind: 'number', buckets: [0.8,1.7,2.3,5.6] },
        { key: 'hdlC',             label: '高密度脂蛋白 (HDL-C)', kind: 'number', buckets: [0.8,1.0,1.3,1.6] },
        { key: 'ldlC',             label: '低密度脂蛋白 (LDL-C)', kind: 'number', buckets: [1.8,2.6,3.4,4.1] },
        { key: 'vldlC',            label: '极低密度脂蛋白 (VLDL-C)', kind: 'number', buckets: [0.2,0.4,0.8] },
        { key: 'pulse',            label: '脉搏 (Pulse)', kind: 'number', buckets: [60,80,100,120] },
        { key: 'diastolicBp',      label: '舒张压 (Diastolic BP)', kind: 'number', buckets: [60,80,90,100] },
        { key: 'bun',              label: '尿素氮 (BUN)', kind: 'number', buckets: [3,7,9] },
        { key: 'uricAcid',         label: '尿酸 (Uric Acid)', kind: 'number', buckets: [240,360,420,480] },
        { key: 'creatinine',       label: '肌酐 (Creatinine)', kind: 'number', buckets: [60,97,133,186] },
        { key: 'age',              label: '年龄 (Age)', kind: 'number', buckets: [18,30,40,50,60,70,80] },
        { key: 'sex',              label: '性别 (Sex)', kind: 'enum' },
        { key: 'hypertensionHistory', label: '高血压史 (Hypertension History)', kind: 'bool' },
      ],

      // Three major categories: Display all at once
      categoriesList: [
        { key: 'demographic',  label: '基本信息 (Demographics)',         metrics: ['sex', 'hypertensionHistory', 'age'] },
        { key: 'lipid',        label: '血脂相关 (Lipid Related)',         metrics: ['totalCholesterol','triglyceride','hdlC','ldlC','vldlC'] },
        { key: 'renal_vitals', label: '生命体征 / 肾功能 (Vitals / Renal Function)', metrics: ['pulse','diastolicBp','bun','uricAcid','creatinine'] },
      ],

      // DOM and ECharts instance cache
      elMap: Object.create(null),    // { id: HTMLDivElement }
      chartMap: Object.create(null), // { id: EChartsInstance }

      // Each chart's type: pie/bar
      chartTypeMap: Object.create(null), // { id: 'pie' | 'bar' }

      // Dimension options
      sexOptions: ['全部 (All)', '男 (Male)', '女 (Female)', '其他 (Other)', '未知 (Unknown)'],
    }
  },

  computed: {
    ageBucketOptions() {
      const labels = this.bucketLabels([18,30,40,50,60,70,80])
      return ['全部 (All)', ...labels]
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
    // 处理图表悬停
    handleChartHover(catIndex, metricKey) {
      const chartId = this.chartId(catIndex, metricKey)

      // 清除之前的计时器
      if (this.hoverTimer) {
        clearTimeout(this.hoverTimer)
      }

      // 设置新的计时器，实现平滑过渡
      this.hoverTimer = setTimeout(() => {
        this.activeChartId = chartId
        this.$nextTick(() => {
          if (this.getChartType(chartId) === 'pie') {
            this.renderBarPreview(chartId)
          }
        })
      }, 200)
    },

    // 处理鼠标离开
    handleChartLeave() {
      if (this.hoverTimer) {
        clearTimeout(this.hoverTimer)
      }
      this.hoverTimer = setTimeout(() => {
        this.activeChartId = null
      }, 300)
    },

    // 渲染柱状图预览
    renderBarPreview(chartId) {
      if (!this.$refs.previewChart) return

      // 销毁之前的预览图表
      const existingChart = echarts.getInstanceByDom(this.$refs.previewChart)
      if (existingChart) {
        existingChart.dispose()
      }

      const chart = echarts.init(this.$refs.previewChart)
      const [ci, metricKey] = chartId.replace(/^c/, '').split('-')
      const filtered = this.applyGlobalFilters(this.raw)
      const titleText = this.metricLabel(metricKey)
      const items = filtered.length ? this.makeItems(metricKey, filtered) : [{ name: '暂无数据 (No data)', value: 1 }]

      const categories = items.map(d => d.name)
      const values = items.map(d => d.value)

      chart.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: 60, right: 30, top: 20, bottom: 40, containLabel: true },
        xAxis: {
          type: 'category',
          data: categories,
          axisLabel: {
            interval: 0,
            rotate: 45,
            color: '#374151',
            fontSize: 12
          },
          axisLine: { lineStyle: { color: '#E5E7EB' } }
        },
        yAxis: {
          type: 'value',
          name: '数量',
          axisLabel: { color: '#374151', fontSize: 12 },
          splitLine: { lineStyle: { color: '#F3F4F6' } },
          axisLine: { lineStyle: { color: '#E5E7EB' } }
        },
        series: [{
          name: titleText,
          type: 'bar',
          data: values,
          barMaxWidth: 30,
          label: { show: true, position: 'top', color: '#374151', fontSize: 11 },
          itemStyle: {
            borderRadius: [4,4,0,0],
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#3b82f6' },
              { offset: 1, color: '#1d4ed8' }
            ])
          }
        }]
      })
    },

    // 切换到柱状图
    switchToBar(chartId) {
      this.toggleChartTypeFor(chartId)
      this.activeChartId = null
    },
// 计算正常值比例
    calculateNormalRatio(metricKey, filteredData) {
      if (filteredData.length === 0) return 0;

      const m = this.metricDef(metricKey);
      if (!m) return 0;

      let normalCount = 0;

      switch (metricKey) {
        case 'totalCholesterol':
          normalCount = filteredData.filter(r => r[m.key] < 5.2).length;
          break;
        case 'triglyceride':
          normalCount = filteredData.filter(r => r[m.key] < 1.7).length;
          break;
        case 'ldlC':
          normalCount = filteredData.filter(r => r[m.key] < 2.6).length;
          break;
        case 'hdlC':
          // 高密度脂蛋白：男性>1.0，女性>1.3
          normalCount = filteredData.filter(r => {
            const sex = r.sex;
            const value = r[m.key];
            if (sex === '男 (Male)' || sex === '男') return value > 1.0;
            if (sex === '女 (Female)' || sex === '女') return value > 1.3;
            return value > 1.0; // 默认使用男性标准
          }).length;
          break;
        case 'pulse':
          normalCount = filteredData.filter(r => r[m.key] >= 60 && r[m.key] <= 100).length;
          break;
        case 'diastolicBp':
          normalCount = filteredData.filter(r => r[m.key] < 80).length;
          break;
        case 'bun':
          normalCount = filteredData.filter(r => r[m.key] >= 3 && r[m.key] <= 7).length;
          break;
        case 'uricAcid':
          // 尿酸：男性<420，女性<360
          normalCount = filteredData.filter(r => {
            const sex = r.sex;
            const value = r[m.key];
            if (sex === '男 (Male)' || sex === '男') return value < 420;
            if (sex === '女 (Female)' || sex === '女') return value < 360;
            return value < 420; // 默认使用男性标准
          }).length;
          break;
        case 'creatinine':
          // 肌酐：男性<97，女性<133
          normalCount = filteredData.filter(r => {
            const sex = r.sex;
            const value = r[m.key];
            if (sex === '男 (Male)' || sex === '男') return value < 97;
            if (sex === '女 (Female)' || sex === '女') return value < 133;
            return value < 97; // 默认使用男性标准
          }).length;
          break;
        case 'hypertensionHistory':
          normalCount = filteredData.filter(r => !r[m.key]).length; // 无高血压史为正常
          break;
        case 'age':
          // 年龄：18-80岁视为正常
          normalCount = filteredData.filter(r => r[m.key] >= 18 && r[m.key] <= 80).length;
          break;
        case 'sex':
        case 'vldlC':
          // 对于分类数据或没有明确正常范围的数据，返回50%作为默认值
          return 0.5;
        default:
          return 0.5;
      }

      return normalCount / filteredData.length;
    },
    // 获取图表描述和计算正常值比例
    getChartDescription(metricKey) {
      const filteredData = this.applyGlobalFilters(this.raw);
      const totalCount = filteredData.length;

      if (totalCount === 0) {
        return '暂无数据可供分析。';
      }

      // 计算正常值比例
      const normalRatio = this.calculateNormalRatio(metricKey, filteredData);
      const normalPercentage = (normalRatio * 100).toFixed(1);

      const descriptions = {
        'totalCholesterol': `总胆固醇是血液中所有脂蛋白所含胆固醇的总和。在当前筛选条件下，正常值（<5.2mmol/L）占比 ${normalPercentage}%。`,
        'triglyceride': `甘油三酯是血液中的脂肪成分。在当前筛选条件下，理想水平（<1.7mmol/L）占比 ${normalPercentage}%。`,
        'hdlC': `高密度脂蛋白被称为"好胆固醇"，有助于清除血管中的胆固醇。在当前筛选条件下，正常水平占比 ${normalPercentage}%。`,
        'ldlC': `低密度脂蛋白被称为"坏胆固醇"，是动脉粥样硬化的主要风险因素。在当前筛选条件下，正常水平（<2.6mmol/L）占比 ${normalPercentage}%。`,
        'vldlC': `极低密度脂蛋白主要运输内源性甘油三酯。在当前筛选条件下，正常水平占比 ${normalPercentage}%。`,
        'pulse': `脉搏反映心脏跳动频率。在当前筛选条件下，正常静息心率（60-100次/分钟）占比 ${normalPercentage}%。`,
        'diastolicBp': `舒张压是心脏舒张时动脉血管的最低压力。在当前筛选条件下，正常水平（<80mmHg）占比 ${normalPercentage}%。`,
        'bun': `尿素氮是蛋白质代谢的产物，反映肾功能状况。在当前筛选条件下，正常水平占比 ${normalPercentage}%。`,
        'uricAcid': `尿酸是嘌呤代谢的终产物。在当前筛选条件下，正常水平占比 ${normalPercentage}%。`,
        'creatinine': `肌酐是肌肉代谢产物，是评估肾功能的重要指标。在当前筛选条件下，正常水平占比 ${normalPercentage}%。`,
        'age': `年龄是健康评估的基础因素。在当前筛选条件下，数据分布正常占比 ${normalPercentage}%。`,
        'sex': `性别差异影响疾病风险和生理指标正常范围。当前筛选条件下的性别分布如上所示。`,
        'hypertensionHistory': `高血压史是心血管疾病的重要风险因素。在当前筛选条件下，无高血压史占比 ${normalPercentage}%。`
      }

      return descriptions[metricKey] || `该指标反映了相关的健康信息。在当前筛选条件下，正常值占比 ${normalPercentage}%。`;
    },

    // 获取过滤后的数据数量
    getFilteredCount() {
      return this.applyGlobalFilters(this.raw).length
    },

    // 获取激活图表的标签
    getActiveChartLabel() {
      if (!this.activeChartId) return ''
      const [ci, metricKey] = this.activeChartId.replace(/^c/, '').split('-')
      return this.metricLabel(metricKey)
    },
    // --- Common ---
    chartId(catIndex, metricKey) {
      return `c${catIndex}-${metricKey}`
    },

    registerEl(el, catIndex, metricKey) {
      const id = this.chartId(catIndex, metricKey)
      if (el) {
        this.elMap[id] = el
        // Initialize chart type (default pie chart)
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
        ElMessage.error(e?.message || 'Load failed（加载失败）')
        this.raw = []
      } finally {
        this.loading = false
      }
    },

    async reload() {
      await this.fetchAll()
      this.$nextTick(() => this.renderAll())
    },

    // --- Chart type (independent for each chart) ---
    getChartType(id) {
      return this.chartTypeMap[id] || 'pie'
    },
    toggleChartTypeFor(id) {
      this.chartTypeMap[id] = this.getChartType(id) === 'pie' ? 'bar' : 'pie'
      // Re-render only this chart
      const [ci, metricKey] = id.replace(/^c/, '').split('-') // "c{ci}-{metric}"
      this.renderOne(Number(ci), metricKey)
    },

    // --- Utility ---
    metricDef(key) {
      return this.metrics.find(m => m.key === key)
    },
    metricLabel(key) {
      const m = this.metricDef(key)
      return m?.label || key
    },
    bucketize(val, cuts) {
      if (val == null || Number.isNaN(val)) return '未知 (Unknown)'
      for (let i = 0; i < cuts.length; i++) {
        if (val < cuts[i]) {
          const left = i === 0 ? 0 : String(cuts[i-1])
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
        if (i === 0) labels.push(`0~${cuts[i]}`)
        else labels.push(`${cuts[i-1]}~${cuts[i]}`)
      }
      labels.push(`${cuts[cuts.length - 1]}+`)
      return labels
    },
    // 确保年龄区间计算正确
    ageBucket(n) {
      // 更严格的空值检查
      if (n == null || n === '' || n === undefined) return '未知 (Unknown)'

      const age = Number(n)
      // 更全面的有效性检查
      if (isNaN(age) || !isFinite(age) || age < 0) return '未知 (Unknown)'

      const cuts = [18, 30, 40, 50, 60, 70, 80]
      return this.bucketize(age, cuts)
    },

// 添加性别标准化方法
    normalizeSex(sex) {
      if (!sex) return '未知 (Unknown)'

      const sexStr = String(sex).toLowerCase().trim()

      if (sexStr.includes('男') || sexStr.includes('male')) return '男 (Male)'
      if (sexStr.includes('女') || sexStr.includes('female')) return '女 (Female)'
      if (sexStr.includes('其他') || sexStr.includes('other')) return '其他 (Other)'

      return '未知 (Unknown)'
    },
    // Apply global filters
    applyGlobalFilters(records) {
      let arr = records || []

      // 性别筛选 - 添加更灵活的匹配
      if (this.globalSex && this.globalSex !== '全部 (All)') {
        arr = arr.filter(r => {
          const recordSex = r?.sex ?? '未知 (Unknown)'
          // 支持多种格式的性别匹配
          return this.normalizeSex(recordSex) === this.normalizeSex(this.globalSex)
        })
      }

      // 年龄区间筛选 - 修正逻辑
      if (this.globalAgeBucket && this.globalAgeBucket !== '全部 (All)') {
        arr = arr.filter(r => {
          const age = r?.age ?? null
          const recordAgeBucket = this.ageBucket(age)
          return recordAgeBucket === this.globalAgeBucket
        })
      }

      console.log('筛选后数据量:', arr.length, '筛选条件:', {
        sex: this.globalSex,
        ageBucket: this.globalAgeBucket
      })

      return arr
    },

    // Uniform statistics: return [{name, value}]
    makeItems(metricKey, filtered) {
      const m = this.metricDef(metricKey)
      if (!m) return [{ name: '暂无数据 (No data)', value: 1 }]
      const counter = new Map()

      for (const r of filtered) {
        const v = r[m.key]
        let name = '未知 (Unknown)'
        if (m.kind === 'number' && m.buckets) {
          name = typeof v === 'number' ? this.bucketize(v, m.buckets) : '未知 (Unknown)'
        } else if (m.kind === 'enum') {
          name = String(v ?? '未知 (Unknown)')
        } else if (m.kind === 'bool') {
          name = v ? '是 (Yes)' : '否 (No)'
        } else {
          name = String(v ?? '未知 (Unknown)')
        }
        counter.set(name, (counter.get(name) || 0) + 1)
      }

      return Array.from(counter.entries()).map(([name, value]) => ({ name, value }))
    },

    // --- Rendering ---
    renderOne(catIndex, metricKey) {
      const id = this.chartId(catIndex, metricKey)
      const el = this.elMap[id]
      if (!el) return

      // Clear old instances
      this.disposeOne(id)
      const chart = echarts.init(el)
      this.chartMap[id] = chart

      const filtered = this.applyGlobalFilters(this.raw)
      const titleText = this.metricLabel(metricKey)
      const items = filtered.length ? this.makeItems(metricKey, filtered) : [{ name: '暂无数据 (No data)', value: 1 }]
      const chartType = this.getChartType(id)

      const legendCommon = {
        right: 10,
        bottom: 10,
        type: 'scroll',
        orient: 'vertical'
      }

      // 特殊处理基本信息组和血脂相关组的饼图大小
      const isDemographics = catIndex === 0; // 第一个分组是基本信息
      const isLipidRelated = catIndex === 1; // 第二个分组是血脂相关
      const isSmallChart = isDemographics || isLipidRelated;

      const pieRadius = isSmallChart ? ['45%', '65%'] : ['55%', '78%'];
      const pieCenter = isSmallChart ? ['50%', '52%'] : ['50%', '56%'];

      if (chartType === 'pie') {
        chart.setOption({
          title: {
            text: titleText,
            left: 'center',
            top: 8,
            textStyle: { fontSize: isSmallChart ? 14 : 15, color: '#111827' }
          },
          tooltip: { trigger: 'item' },
          legend: {
            ...legendCommon,
            data: items.map(d => d.name),
            textStyle: { color: '#374151' },
            // 小尺寸图表图例位置调整
            ...(isSmallChart && { right: 5, bottom: 5 })
          },
          series: [{
            type: 'pie',
            radius: pieRadius,
            center: pieCenter,
            label: { show: false },
            data: items
          }]
        })
      } else {
        // 柱状图配置保持不变
        const categories = items.map(d => d.name)
        const values = items.map(d => d.value)
        chart.setOption({
          title: {
            text: titleText,
            left: 'center',
            top: 8,
            textStyle: { fontSize: isDemographics ? 14 : 15, color: '#111827' }
          },
          tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
          legend: { ...legendCommon, data: [titleText], textStyle: { color: '#374151' } },
          grid: { left: 50, right: 120, top: 60, bottom: 60, containLabel: true },
          xAxis: { type: 'category', data: categories, axisLabel: { interval: 0, rotate: 20, color: '#374151' }, axisLine: { lineStyle: { color: '#E5E7EB' } } },
          yAxis: { type: 'value', name: '数量 (Count)', axisLabel: { color: '#374151' }, splitLine: { lineStyle: { color: '#F3F4F6' } }, axisLine: { lineStyle: { color: '#E5E7EB' } } },
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

    // 获取正常值比例（百分比形式）
    getNormalRatio(metricKey) {
      const filteredData = this.applyGlobalFilters(this.raw);
      const ratio = this.calculateNormalRatio(metricKey, filteredData);
      return (ratio * 100).toFixed(1);
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
:global(html, body, #app) {
  height: 100%;
  margin: 0;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.dashboard {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  color: #1e293b;
  padding: 0;
  box-sizing: border-box;
  overflow: hidden;
}

/* Header Section */
.header {
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  border-bottom: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.08);
  padding: 24px 32px;
}

.header-content {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  max-width: 100%;
  gap: 32px;
}

.title-section {
  flex: 1;
  min-width: 0;
}

.main-title {
  font-size: 28px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 8px 0;
  background: linear-gradient(135deg, #0f172a 0%, #334155 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.subtitle {
  margin: 0;
  color: #64748b;
  font-size: 15px;
  line-height: 1.5;
  font-weight: 500;
}

.controls-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
  align-items: flex-end;
}

.filter-controls {
  display: flex;
  gap: 12px;
  align-items: center;
}

.filter-select {
  width: 180px;
}

.filter-select :deep(.el-input__inner) {
  border-radius: 12px;
  border: 1.5px solid #e2e8f0;
  transition: all 0.3s ease;
}

.filter-select :deep(.el-input__inner:hover),
.filter-select :deep(.el-input__inner:focus) {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.refresh-btn {
  border-radius: 12px;
  padding: 10px 20px;
  font-weight: 600;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(59, 130, 246, 0.2);
}

.refresh-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(59, 130, 246, 0.3);
}

.refresh-icon {
  display: inline-block;
  width: 16px;
  height: 16px;
  margin-right: 6px;
  background: currentColor;
  mask: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath fill='currentColor' d='M17.65 6.35A7.958 7.958 0 0 0 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08A5.99 5.99 0 0 1 12 18c-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z'/%3E%3C/svg%3E");
}

/* Content Section */
.content {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 24px 32px;
  background: #f8fafc;
}

.groups {
  display: flex;
  flex-direction: column;
  gap: 32px;
  max-width: 100%;
}

/* Group Section */
.group-section {
  background: transparent;
}

.group-header {
  margin-bottom: 20px;
}

.group-title {
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 12px 0;
  position: relative;
  padding-left: 16px;
}

.group-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 20px;
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  border-radius: 2px;
}

.group-divider {
  height: 1px;
  background: linear-gradient(90deg, transparent 0%, #e2e8f0 50%, transparent 100%);
}

/* Cards Grid */
.cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(480px, 1fr));
  gap: 24px;
}

/* Chart Card */
.chart-card {
  background: #ffffff;
  border: 1px solid #f1f5f9;
  border-radius: 16px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  min-height: 380px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04);
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.chart-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #3b82f6, #8b5cf6, #ec4899);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.chart-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(15, 23, 42, 0.1);
  border-color: #e2e8f0;
}

.chart-card:hover::before {
  opacity: 1;
}

/* Card Header */
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f1f5f9;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
  line-height: 1.4;
}

.card-controls {
  display: flex;
  gap: 8px;
  align-items: center;
}

.toggle-btn {
  border-radius: 8px;
  font-weight: 600;
  transition: all 0.3s ease;
  padding: 6px 12px;
}

.toggle-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.15);
}

.bar-chart-icon,
.pie-chart-icon {
  display: inline-block;
  width: 14px;
  height: 14px;
  margin-right: 4px;
  background: currentColor;
  vertical-align: -2px;
}

.bar-chart-icon {
  mask: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath fill='currentColor' d='M5 9.2h3V19H5zM10.6 5h2.8v14h-2.8zm5.6 8H19v6h-2.8z'/%3E%3C/svg%3E");
}

.pie-chart-icon {
  mask: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath fill='currentColor' d='M11 2v20c-5.1-.5-9-4.8-9-10s3.9-9.5 9-10zm2.03 0v8.99H22c-.47-4.74-4.24-8.52-8.97-8.99zm0 11.01V22c4.74-.47 8.5-4.25 8.97-8.99h-8.97z'/%3E%3C/svg%3E");
}

/* Chart Container */
.chart-container {
  flex: 1;
  min-height: 300px;
  border-radius: 8px;
  background: #fafbfc;
}

/* Responsive Design */
@media (max-width: 1200px) {
  .cards-grid {
    grid-template-columns: 1fr;
  }

  .header-content {
    flex-direction: column;
    gap: 20px;
  }

  .controls-section {
    align-items: stretch;
    width: 100%;
  }

  .filter-controls {
    justify-content: space-between;
  }
}

@media (max-width: 768px) {
  .header {
    padding: 20px 24px;
  }

  .content {
    padding: 20px 24px;
  }

  .main-title {
    font-size: 24px;
  }

  .cards-grid {
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .chart-card {
    min-height: 340px;
    padding: 16px;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .card-controls {
    align-self: flex-end;
  }
}

@media (max-width: 480px) {
  .header {
    padding: 16px 20px;
  }

  .content {
    padding: 16px 20px;
  }

  .filter-controls {
    flex-direction: column;
    gap: 12px;
  }

  .filter-select {
    width: 100%;
  }
}
/* 特殊处理基本信息组的卡片布局 */
.group-section:first-child .cards-grid {
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.group-section:first-child .chart-card {
  min-height: 320px;
}


/* 特殊处理血脂相关组的卡片布局 - 三个一行 */
.group-section:nth-child(2) .cards-grid {
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.group-section:nth-child(2) .chart-card {
  min-height: 320px;
}

/* 图表卡片悬停效果 */
.chart-card {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
}

.chart-card.active {
  transform: scale(1.05);
  z-index: 10;
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.15);
  border-color: #3b82f6;
}

.chart-card.inactive {
  transform: scale(0.9);
  opacity: 0.6;
  filter: blur(1px);
}



/* 响应式调整 */
@media (max-width: 1200px) {
  .group-section:first-child .cards-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .group-section:first-child .cards-grid {
    grid-template-columns: 1fr;
  }
}
</style>