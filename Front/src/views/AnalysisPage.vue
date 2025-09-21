<template>
  <div class="dashboard">
    <Header />

    <!-- Top: Title + Global Filters -->
    <div class="header">
      <div class="title">
        <div class="h1">Deep Analysis（深度分析）</div>
        <div class="subtitle">
          Correlation, Chi-square, t-test, Correlation Matrix, Age Trends, Boxplot, Bootstrap CI
          （相关性、卡方、t检验、相关矩阵、年龄趋势、箱线图、Bootstrap 置信区间）
        </div>
      </div>

      <div class="ops">
        <el-select
            v-model="globalSex"
            size="large"
            class="ctrl"
            placeholder="Sex（性别）"
            @change="renderAll"
        >
          <el-option v-for="s in sexOptions" :key="s" :label="s" :value="s" />
        </el-select>

        <el-select
            v-model="globalAgeBucket"
            size="large"
            class="ctrl"
            placeholder="Age Range（年龄区间）"
            @change="renderAll"
        >
          <el-option v-for="ab in ageBucketOptions" :key="ab" :label="ab" :value="ab" />
        </el-select>

        <el-button :loading="loading" @click="reload">Refresh（刷新）</el-button>
      </div>
    </div>

    <div class="groups" v-loading="loading">
      <!-- 1. A/B Variable Tests -->
      <div class="group">
        <div class="group-title">A/B Variable Tests（A/B 变量选择与检验）</div>

        <div class="selectors">
          <el-select v-model="varA" class="ctrl" placeholder="Select Variable A（选择变量 A）" @change="renderVarPair">
            <el-option v-for="m in allMetricOptions" :key="m.key" :label="m.label" :value="m.key" />
          </el-select>
          <el-select v-model="varB" class="ctrl" placeholder="Select Variable B（选择变量 B）" @change="renderVarPair">
            <el-option v-for="m in allMetricOptions" :key="m.key" :label="m.label" :value="m.key" />
          </el-select>
          <div class="tips">Auto-select suitable test and chart by variable types（根据变量类型自动选择检验与图表）</div>
        </div>

        <div class="cards">
          <div class="card stat-card">
            <div class="card-header"><div class="card-title">Statistics（统计结果）</div></div>
            <div class="stat-content">
              <div v-if="pairType === 'num-num'" class="stat-grid">
                <div class="stat-item"><span>Pearson r（皮尔逊 r）：</span>{{ fmt(statsPair.pearson?.r) }}</div>
                <div class="stat-item"><span>Sample Size n（样本量）：</span>{{ statsPair.commonN }}</div>
                <div class="stat-item"><span>Spearman ρ（斯皮尔曼 ρ）：</span>{{ fmt(statsPair.spearman?.rho) }}</div>
                <div class="stat-item"><span>Slope（斜率）：</span>{{ fmt(statsPair.reg?.slope) }}</div>
                <div class="stat-item"><span>Intercept（截距）：</span>{{ fmt(statsPair.reg?.intercept) }}</div>
                <div class="stat-item"><span>R²：</span>{{ fmt(statsPair.reg?.r2) }}</div>
              </div>
              <div v-else-if="pairType === 'cat-cat'" class="stat-grid">
                <div class="stat-item"><span>Chi-square χ²（卡方）：</span>{{ fmt(statsPair.chi2?.chi2) }}</div>
                <div class="stat-item"><span>df（自由度）：</span>{{ statsPair.chi2?.df }}</div>
                <div class="stat-item"><span>p-value（近似）：</span>{{ fmt(statsPair.chi2?.p) }}</div>
              </div>
              <div v-else-if="pairType === 'num-cat'" class="stat-grid">
                <div class="stat-item"><span>#Groups（组数）：</span>{{ statsPair.group?.k }}</div>
                <div class="stat-item" v-if="statsPair.group?.type === 'welch'"><span>Welch t：</span>{{ fmt(statsPair.group?.t) }}</div>
                <div class="stat-item" v-if="statsPair.group?.type === 'welch'"><span>df（自由度 近似）：</span>{{ fmt(statsPair.group?.df) }}</div>
                <div class="stat-item" v-if="statsPair.group?.type === 'welch'"><span>p-value（近似）：</span>{{ fmt(statsPair.group?.p) }}</div>
                <div class="stat-note" v-if="statsPair.group?.type === 'anova'">
                  Multi-group mean comparison shown; for strict ANOVA please verify server-side（多组场景展示均值对比图；严格 ANOVA 建议后端复核）
                </div>
              </div>
              <div v-else class="stat-note">Please pick two variables（请选择两个变量）</div>
            </div>
          </div>

          <div class="card">
            <div class="card-header"><div class="card-title">Visualization（可视化）</div></div>
            <div class="chart" ref="pairChartRef"></div>
          </div>

          <div class="card" v-if="pairType === 'cat-cat'">
            <div class="card-header"><div class="card-title">Contingency Heatmap（列联表热力图）</div></div>
            <div class="chart" ref="contingencyHeatRef"></div>
          </div>
        </div>
      </div>

      <!-- 2. Correlation Matrix -->
      <div class="group">
        <div class="group-title">Correlation Matrix (Pearson)（数值属性相关矩阵（皮尔逊））</div>
        <div class="cards">
          <div class="card">
            <div class="card-header"><div class="card-title">Heatmap（热力图）</div></div>
            <div class="chart" ref="corrHeatRef"></div>
          </div>
        </div>
      </div>

      <!-- 3. Age Trends -->
      <div class="group">
        <div class="group-title">Means by Age Bucket（属性随年龄区间的均值变化）</div>

        <div class="selectors">
          <el-select
              v-model="ageTrendKeys"
              class="ctrl wide"
              multiple
              collapse-tags
              :max-collapse-tags="3"
              placeholder="Select metrics（选择多个数值属性）"
              @change="renderAgeTrend"
          >
            <el-option v-for="m in numericMetricOptions" :key="m.key" :label="m.label" :value="m.key" />
          </el-select>
          <div class="tips">Buckets: -∞~18, 18~30, …, 80+（按年龄分箱计算均值并绘制折线）</div>
        </div>

        <div class="cards">
          <div class="card">
            <div class="card-header"><div class="card-title">Line Chart（折线图）</div></div>
            <div class="chart" ref="ageTrendRef"></div>
          </div>
        </div>
      </div>

      <!-- 4. Boxplot -->
      <div class="group">
        <div class="group-title">Boxplot by Sex / Hypertension（箱线图：按性别或是否高血压分组）</div>

        <div class="selectors">
          <el-select v-model="boxMetricKey" class="ctrl" placeholder="Select numeric metric（选择数值指标）" @change="renderBoxplot">
            <el-option v-for="m in numericMetricOptions" :key="m.key" :label="m.label" :value="m.key" />
          </el-select>
          <el-select v-model="boxGroupKey" class="ctrl" placeholder="Group by（分组方式）" @change="renderBoxplot">
            <el-option label="Sex（性别）" value="sex" />
            <el-option label="Hypertension History（高血压史）" value="hypertensionHistory" />
          </el-select>
          <div class="tips">Show Q1/Median/Q3, whiskers (1.5×IQR), and outliers（显示四分位、须与异常点）</div>
        </div>

        <div class="cards">
          <div class="card">
            <div class="card-header"><div class="card-title">{{ metricLabel(boxMetricKey) }} — Boxplot（箱线图）</div></div>
            <div class="chart" ref="boxplotRef"></div>
          </div>
        </div>
      </div>

      <!-- 5. Bootstrap CI -->
      <div class="group">
        <div class="group-title">Mean & 95% CI (Bootstrap)（均值与 95% 置信区间：Bootstrap）</div>

        <div class="selectors">
          <el-select v-model="ciMetricKey" class="ctrl" placeholder="Select numeric metric（选择数值指标）" @change="renderBootstrapCI">
            <el-option v-for="m in numericMetricOptions" :key="m.key" :label="m.label" :value="m.key" />
          </el-select>
          <el-select v-model="ciGroupKey" class="ctrl" placeholder="Group by（分组方式）" @change="renderBootstrapCI">
            <el-option label="Sex（性别）" value="sex" />
            <el-option label="Hypertension History（高血压史）" value="hypertensionHistory" />
          </el-select>
          <div class="tips">Per-group bootstrap (default 1000) — 2.5%~97.5% quantiles as 95% CI（每组自助抽样取分位构造 95% CI）</div>
        </div>

        <div class="cards">
          <div class="card">
            <div class="card-header"><div class="card-title">{{ metricLabel(ciMetricKey) }} — Mean & 95% CI（均值与置信区间）</div></div>
            <div class="chart" ref="ciRef"></div>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { getAllHealthRecords } from '@/api/HealthRecordShow.js'
import Header from '@/components/Header.vue'

export default {
  name: 'Analysis',
  components: { Header },

  data() {
    return {
      loading: false,
      raw: [],

      // Global filters
      globalSex: 'All（全部）',
      globalAgeBucket: 'All（全部）',

      // A/B selections
      varA: 'totalCholesterol',
      varB: 'triglyceride',

      // Age trend selections
      ageTrendKeys: ['totalCholesterol', 'ldlC', 'hdlC'],

      // Boxplot / CI selections
      boxMetricKey: 'totalCholesterol',
      boxGroupKey: 'sex',
      ciMetricKey: 'totalCholesterol',
      ciGroupKey: 'sex',

      // Metric definitions (labels bilingual)
      metrics: [
        { key: 'totalCholesterol', label: 'Total Cholesterol（总胆固醇）', kind: 'number', buckets: [3,4,5,6,7,8] },
        { key: 'triglyceride',     label: 'Triglyceride（甘油三酯）', kind: 'number', buckets: [0.8,1.7,2.3,5.6] },
        { key: 'hdlC',             label: 'HDL-C（高密度脂蛋白）', kind: 'number', buckets: [0.8,1.0,1.3,1.6] },
        { key: 'ldlC',             label: 'LDL-C（低密度脂蛋白）', kind: 'number', buckets: [1.8,2.6,3.4,4.1] },
        { key: 'vldlC',            label: 'VLDL-C（极低密度脂蛋白）', kind: 'number', buckets: [0.2,0.4,0.8] },
        { key: 'pulse',            label: 'Pulse (bpm)（脉搏）', kind: 'number', buckets: [60,80,100,120] },
        { key: 'diastolicBp',      label: 'Diastolic BP (mmHg)（舒张压）', kind: 'number', buckets: [60,80,90,100] },
        { key: 'bun',              label: 'BUN（尿素氮）', kind: 'number', buckets: [3,7,9] },
        { key: 'uricAcid',         label: 'Uric Acid（尿酸）', kind: 'number', buckets: [240,360,420,480] },
        { key: 'creatinine',       label: 'Creatinine（肌酐）', kind: 'number', buckets: [60,97,133,186] },
        { key: 'age',              label: 'Age（年龄）', kind: 'number', buckets: [18,30,40,50,60,70,80] },
        { key: 'sex',              label: 'Sex（性别）', kind: 'enum' },
        { key: 'hypertensionHistory', label: 'Hypertension History（高血压史）', kind: 'bool' },
      ],

      // Refs
      pairChartRef: null,
      contingencyHeatRef: null,
      corrHeatRef: null,
      ageTrendRef: null,
      boxplotRef: null,
      ciRef: null,

      // ECharts instances
      pairChart: null,
      contingencyHeat: null,
      corrHeat: null,
      ageTrendChart: null,
      boxplotChart: null,
      ciChart: null,

      // Stats results
      statsPair: {},
    }
  },

  computed: {
    sexOptions() {
      return ['All（全部）', 'Male（男）', 'Female（女）', 'Other（其他）', 'Unknown（未知）']
    },
    ageBucketOptions() {
      const labels = this.bucketLabels([18,30,40,50,60,70,80])
      return ['All（全部）', ...labels]
    },
    allMetricOptions() {
      return this.metrics.map(m => ({ key: m.key, label: m.label, kind: m.kind }))
    },
    numericMetricOptions() {
      return this.metrics.filter(m => m.kind==='number').map(m=>({key:m.key,label:m.label}))
    },
    pairType() {
      const a = this.metrics.find(x => x.key === this.varA)
      const b = this.metrics.find(x => x.key === this.varB)
      if (!a || !b) return ''
      const na = a.kind === 'number', nb = b.kind === 'number'
      if (na && nb) return 'num-num'
      const ca = a.kind !== 'number', cb = b.kind !== 'number'
      if (ca && cb) return 'cat-cat'
      return 'num-cat'
    }
  },

  async mounted() {
    await this.fetchAll()
    this.initCharts()
    this.renderAll()
    window.addEventListener('resize', this.handleResize)
  },

  beforeUnmount() {
    window.removeEventListener('resize', this.handleResize)
    this.disposeAll()
  },

  methods: {
    // ---- API ----
    async fetchAll() {
      this.loading = true
      try {
        const res = await getAllHealthRecords()
        const data = Array.isArray(res?.data) ? res.data : (res?.data?.data ?? [])
        this.raw = data || []
      } catch (e) {
        ElMessage.error(e?.message || 'Load failed（加载失败）')
        this.raw = []
      } finally { this.loading = false }
    },
    async reload() {
      await this.fetchAll()
      this.renderAll()
    },

    // ---- Init / Dispose ----
    initCharts() {
      this.pairChart = echarts.init(this.$refs.pairChartRef)
      this.corrHeat  = echarts.init(this.$refs.corrHeatRef)
      this.ageTrendChart = echarts.init(this.$refs.ageTrendRef)
      this.boxplotChart  = echarts.init(this.$refs.boxplotRef)
      this.ciChart       = echarts.init(this.$refs.ciRef)
    },
    disposeAll() {
      ;[this.pairChart, this.corrHeat, this.ageTrendChart, this.contingencyHeat, this.boxplotChart, this.ciChart]
          .forEach(ch => { if (ch) try { ch.dispose() } catch {} })
    },

    // ---- Utils ----
    metricDef(key){ return this.metrics.find(m=>m.key===key) },
    metricLabel(key){ return this.metricDef(key)?.label || key },
    bucketize(val, cuts){ if (val==null||Number.isNaN(val)) return 'Unknown（未知）'; for(let i=0;i<cuts.length;i++){ if(val<cuts[i]) return `${i===0?'-∞':cuts[i-1]}~${cuts[i]}` } return `${cuts[cuts.length-1]}+` },
    bucketLabels(cuts){
      const a=[]
      for(let i=0;i<cuts.length;i++){ a.push(i===0?`-∞~${cuts[i]}`:`${cuts[i-1]}~${cuts[i]}`) }
      a.push(`${cuts[cuts.length-1]}+`)
      return a
    },
    ageBucket(n){ if(n==null) return 'Unknown（未知）'; return this.bucketize(n,[18,30,40,50,60,70,80]) },
    applyGlobalFilters(records){
      let arr = records || []
      if (this.globalSex !== 'All（全部）') arr = arr.filter(r => (this.sexLabel(r?.sex ?? '未知') === this.globalSex))
      if (this.globalAgeBucket !== 'All（全部）') arr = arr.filter(r => this.ageBucket(r?.age ?? null) === this.globalAgeBucket)
      return arr
    },
    sexLabel(v){
      const map = { '男':'Male（男）', '女':'Female（女）', '其他':'Other（其他）', '未知':'Unknown（未知）' }
      return map[v] ?? String(v)
    },
    fmt(v){ if(v==null||Number.isNaN(v)) return '—'; if(typeof v!=='number') return v; const a=Math.abs(v)<1?v.toFixed(4):v.toFixed(3); return String(Number(a)) },

    // ---- Stats helpers ----
    mean(arr){ const n=arr.length; return n?arr.reduce((a,b)=>a+b,0)/n:NaN },
    variance(arr){ const m=this.mean(arr),n=arr.length; if(n<2) return NaN; return arr.reduce((s,x)=>s+(x-m)*(x-m),0)/(n-1) },
    std(arr){ const v=this.variance(arr); return isNaN(v)?NaN:Math.sqrt(v) },
    pearson(x,y){ const n=Math.min(x.length,y.length); if(n<2) return {r:NaN,n:0}; const mx=this.mean(x),my=this.mean(y); let num=0,dx=0,dy=0; for(let i=0;i<n;i++){const a=x[i]-mx,b=y[i]-my; num+=a*b; dx+=a*a; dy+=b*b} const r=(dx===0||dy===0)?NaN:(num/Math.sqrt(dx*dy)); return {r,n} },
    rank(arr){ const p=arr.map((v,i)=>({v,i})).sort((a,b)=>a.v-b.v); const r=Array(arr.length).fill(0); for(let i=0;i<p.length;){let j=i+1; while(j<p.length&&p[j].v===p[i].v) j++; const avg=(i+1+j)/2; for(let k=i;k<j;k++) r[p[k].i]=avg; i=j} return r },
    spearman(x,y){ const rx=this.rank(x),ry=this.rank(y); const {r,n}=this.pearson(rx,ry); return {rho:r,n} },
    linreg(x,y){ const n=Math.min(x.length,y.length); if(n<2) return {slope:NaN,intercept:NaN,r2:NaN}; const mx=this.mean(x),my=this.mean(y); let sxy=0,sxx=0,syy=0; for(let i=0;i<n;i++){const dx=x[i]-mx,dy=y[i]-my; sxy+=dx*dy; sxx+=dx*dx; syy+=dy*dy} const slope=sxx===0?NaN:sxy/sxx; const intercept=my-slope*mx; const r2=(sxx===0||syy===0)?NaN:(sxy*sxy)/(sxx*syy); return {slope,intercept,r2} },
    phi(z){ return 0.5*(1+Math.erf(z/Math.SQRT2)) },
    welchT(x,y){ const nx=x.length,ny=y.length; if(nx<2||ny<2) return {t:NaN,df:NaN,p:NaN}; const mx=this.mean(x),my=this.mean(y),vx=this.variance(x),vy=this.variance(y); const t=(mx-my)/Math.sqrt(vx/nx+vy/ny); const df=(vx/nx+vy/ny)**2/((vx*vx)/((nx*nx)*(nx-1))+(vy*vy)/((ny*ny)*(ny-1))); const p=2*(1-this.phi(Math.abs(t))); return {t,df,p} },
    chiSquare(calc){ const {table,rows,cols}=calc; const rS=rows.map((_,i)=>table[i].reduce((a,b)=>a+b,0)); const cS=cols.map((_,j)=>rows.reduce((a,_,i)=>a+table[i][j],0)); const N=rS.reduce((a,b)=>a+b,0); let chi2=0; for(let i=0;i<rows.length;i++){ for(let j=0;j<cols.length;j++){ const exp=(rS[i]*cS[j])/N||0; const obs=table[i][j]; if(exp>0) chi2+=(obs-exp)*(obs-exp)/exp } } const df=(rows.length-1)*(cols.length-1); const z=(chi2-df)/Math.sqrt(2*df||1); const p=1-this.phi(z); return {chi2,df,p} },

    // ---- Data extraction ----
    filtered(){ return this.applyGlobalFilters(this.raw) },
    extractNumPair(aKey,bKey){
      const arr=[]
      for(const r of this.filtered()){
        const a=r[aKey],b=r[bKey]
        if(typeof a==='number'&&typeof b==='number'&&!Number.isNaN(a)&&!Number.isNaN(b)) arr.push([a,b])
      }
      return arr
    },
    extractNumCat(numKey,catKey){
      const map=new Map()
      for(const r of this.filtered()){
        const v=r[numKey]; let g=r[catKey]
        if(catKey==='hypertensionHistory') g = r[catKey] ? 'Yes（是）':'No（否）'
        if(catKey==='sex') g = this.sexLabel(r[catKey] ?? '未知')
        if(typeof v==='number'&&!Number.isNaN(v)){
          const key=String(g??'Unknown（未知）')
          if(!map.has(key)) map.set(key,[])
          map.get(key).push(v)
        }
      }
      return map
    },
    contingency(aKey,bKey){
      const rowsSet=new Set(), colsSet=new Set(), pairs=[]
      for(const r of this.filtered()){
        let a=r[aKey], b=r[bKey]
        if(aKey==='hypertensionHistory') a=r[aKey]?'Yes（是）':'No（否）'
        if(bKey==='hypertensionHistory') b=r[bKey]?'Yes（是）':'No（否）'
        if(aKey==='sex') a=this.sexLabel(a ?? '未知')
        if(bKey==='sex') b=this.sexLabel(b ?? '未知')
        a=String(a??'Unknown（未知）'); b=String(b??'Unknown（未知）')
        rowsSet.add(a); colsSet.add(b); pairs.push([a,b])
      }
      const rows=Array.from(rowsSet).sort(), cols=Array.from(colsSet).sort()
      const idxR=Object.fromEntries(rows.map((v,i)=>[v,i])), idxC=Object.fromEntries(cols.map((v,i)=>[v,i]))
      const table=Array(rows.length).fill(0).map(()=>Array(cols.length).fill(0))
      for(const [ra,cb] of pairs){ table[idxR[ra]][idxC[cb]]++ }
      return { table, rows, cols }
    },

    // ---- Rendering ----
    renderAll(){
      this.renderVarPair()
      this.renderCorrMatrix()
      this.renderAgeTrend()
      this.renderBoxplot()
      this.renderBootstrapCI()
      this.handleResize()
    },
    renderVarPair(){
      if(!this.pairChart) this.pairChart=echarts.init(this.$refs.pairChartRef)
      const aDef=this.metricDef(this.varA), bDef=this.metricDef(this.varB)
      if(!aDef||!bDef) return
      const type=this.pairType
      this.statsPair = {}

      if(type==='num-num'){
        const data=this.extractNumPair(this.varA,this.varB)
        const xs=data.map(d=>d[0]), ys=data.map(d=>d[1])
        const pear=this.pearson(xs,ys), spea=this.spearman(xs,ys), reg=this.linreg(xs,ys)
        this.statsPair={pearson:pear, spearman:spea, reg, commonN:data.length}
        const [minX,maxX]=[Math.min(...xs),Math.max(...xs)]
        const fit=[[minX,reg.intercept+reg.slope*minX],[maxX,reg.intercept+reg.slope*maxX]]
        this.pairChart.setOption({
          title:{ text:`${this.metricLabel(this.varA)} vs ${this.metricLabel(this.varB)}（散点与回归）`, left:'center', top:8 },
          tooltip:{ trigger:'item' },
          grid:{ left:50,right:30,top:50,bottom:50,containLabel:true },
          xAxis:{ type:'value', name:this.metricLabel(this.varA), splitLine:{ lineStyle:{ color:'#F3F4F6' } } },
          yAxis:{ type:'value', name:this.metricLabel(this.varB), splitLine:{ lineStyle:{ color:'#F3F4F6' } } },
          legend:{ right:10, bottom:10, data:['Samples（样本）','Fit Line（拟合线）'] },
          series:[
            { name:'Samples（样本）', type:'scatter', data },
            { name:'Fit Line（拟合线）', type:'line', data:fit, showSymbol:false }
          ]
        })
        if(this.contingencyHeat){ this.contingencyHeat.dispose(); this.contingencyHeat=null }
      } else if (type==='cat-cat'){
        const ct=this.contingency(this.varA,this.varB)
        const chi2=this.chiSquare(ct)
        this.statsPair={ chi2 }
        const categories=ct.cols
        const series=ct.rows.map((rName,i)=>({
          name:rName, type:'bar', stack:'total', emphasis:{ focus:'series' }, data:ct.cols.map((_,j)=>ct.table[i][j])
        }))
        this.pairChart.setOption({
          title:{ text:`${this.metricLabel(this.varA)} × ${this.metricLabel(this.varB)} — Stacked Bars（堆叠柱）`, left:'center', top:8 },
          tooltip:{ trigger:'axis', axisPointer:{ type:'shadow' } },
          legend:{ right:10, bottom:10 },
          grid:{ left:50,right:30,top:60,bottom:60,containLabel:true },
          xAxis:{ type:'category', data:categories, axisLabel:{ interval:0, rotate:20 } },
          yAxis:{ type:'value', name:'Count（计数）', splitLine:{ lineStyle:{ color:'#F3F4F6' } } },
          series
        })
        if(!this.contingencyHeat) this.contingencyHeat=echarts.init(this.$refs.contingencyHeatRef)
        const heatData=[]
        for(let i=0;i<ct.rows.length;i++){ for(let j=0;j<ct.cols.length;j++){ heatData.push([j,i,ct.table[i][j]]) } }
        this.contingencyHeat.setOption({
          title:{ text:'Contingency Heatmap（列联表热力图）', left:'center', top:6 },
          tooltip:{ position:'top' },
          grid:{ left:70,right:30,top:40,bottom:60,containLabel:true },
          xAxis:{ type:'category', data:ct.cols, splitArea:{ show:true } },
          yAxis:{ type:'category', data:ct.rows, splitArea:{ show:true } },
          visualMap:{ min:0, max:Math.max(1,...heatData.map(d=>d[2])), calculable:true, orient:'horizontal', left:'center', bottom:10 },
          series:[{ type:'heatmap', data:heatData, label:{ show:true } }]
        })
      } else {
        const numKey = this.metricDef(this.varA).kind==='number' ? this.varA : this.varB
        const catKey = numKey===this.varA ? this.varB : this.varA
        const groups=this.extractNumCat(numKey,catKey)
        const cats=Array.from(groups.keys())
        const means=cats.map(k=>this.mean(groups.get(k)))
        if(cats.length===2){
          const {t,df,p}=this.welchT(groups.get(cats[0]), groups.get(cats[1]))
          this.statsPair={ group:{ type:'welch', t, df, p, k:2 } }
        } else {
          this.statsPair={ group:{ type:'anova', k:cats.length } }
        }
        this.pairChart.setOption({
          title:{ text:`Group Means of ${this.metricLabel(numKey)} by ${this.metricLabel(catKey)}（组均值）`, left:'center', top:8 },
          tooltip:{ trigger:'axis' },
          legend:{ right:10, bottom:10, data:[`Mean（均值）`] },
          grid:{ left:50,right:30,top:60,bottom:60,containLabel:true },
          xAxis:{ type:'category', data:cats, axisLabel:{ interval:0, rotate:20 } },
          yAxis:{ type:'value', name:'Mean（均值）', splitLine:{ lineStyle:{ color:'#F3F4F6' } } },
          series:[{ name:'Mean（均值）', type:'bar', data:means, barMaxWidth:40, label:{ show:true, position:'top' } }]
        })
        if(this.contingencyHeat){ this.contingencyHeat.dispose(); this.contingencyHeat=null }
      }
    },
    renderCorrMatrix(){
      const numKeys=this.metrics.filter(m=>m.kind==='number').map(m=>m.key)
      const data=this.applyGlobalFilters(this.raw)
      const mat=[]
      for(let i=0;i<numKeys.length;i++){
        for(let j=0;j<numKeys.length;j++){
          const xi=data.map(r=>r[numKeys[i]]).filter(v=>typeof v==='number'&&!Number.isNaN(v))
          const yj=data.map(r=>r[numKeys[j]]).filter(v=>typeof v==='number'&&!Number.isNaN(v))
          const n=Math.min(xi.length,yj.length)
          const x=xi.slice(0,n), y=yj.slice(0,n)
          const { r }=this.pearson(x,y)
          mat.push([i,j,isNaN(r)?0:r])
        }
      }
      const labels=numKeys.map(k=>this.metricLabel(k))
      this.corrHeat.setOption({
        title:{ text:'Correlation Matrix（相关矩阵）', left:'center', top:6 },
        tooltip:{ position:'top', formatter: p=>`${labels[p.value[1]]} × ${labels[p.value[0]]}: r = ${this.fmt(p.value[2])}` },
        grid:{ left:100,right:40,top:40,bottom:70,containLabel:true },
        xAxis:{ type:'category', data:labels, splitArea:{ show:true }, axisLabel:{ rotate:30 } },
        yAxis:{ type:'category', data:labels, splitArea:{ show:true } },
        visualMap:{ min:-1, max:1, calculable:true, orient:'horizontal', left:'center', bottom:10 },
        series:[{ type:'heatmap', data:mat, label:{ show:true, formatter:({value})=>this.fmt(value[2]) } }]
      })
    },
    renderAgeTrend(){
      const filtered=this.applyGlobalFilters(this.raw)
      const cuts=[18,30,40,50,60,70,80], labels=this.bucketLabels(cuts)
      const idx=(n)=>{ if(n==null||Number.isNaN(n)) return -1; for(let i=0;i<cuts.length;i++) if(n<cuts[i]) return i; return cuts.length }
      const series=[]
      for(const key of this.ageTrendKeys){
        const sums=Array(labels.length).fill(0), cnts=Array(labels.length).fill(0)
        for(const r of filtered){ const v=r[key], i=idx(r.age); if(i>=0&&typeof v==='number'&&!Number.isNaN(v)){ sums[i]+=v; cnts[i]++ } }
        const values=sums.map((s,i)=> cnts[i]?s/cnts[i]:null)
        series.push({ name:this.metricLabel(key), type:'line', data:values, connectNulls:true, smooth:true })
      }
      this.ageTrendChart.setOption({
        title:{ text:'Means by Age Bucket（按年龄区间的均值变化）', left:'center', top:8 },
        tooltip:{ trigger:'axis' },
        legend:{ right:10, bottom:10 },
        grid:{ left:50,right:30,top:60,bottom:60,containLabel:true },
        xAxis:{ type:'category', data:labels },
        yAxis:{ type:'value', name:'Mean（均值）', splitLine:{ lineStyle:{ color:'#F3F4F6' } } },
        series
      })
    },
    renderBoxplot(){
      const key=this.boxMetricKey, groupKey=this.boxGroupKey
      if(!key||!groupKey) return
      const groups = this.extractNumCat(key, groupKey) // Map<group, number[]>
      const cats = Array.from(groups.keys())
      // five-number summary & outliers
      const five = [], outliers=[]
      const q = (arr, p) => {
        if(!arr.length) return NaN
        const a=[...arr].sort((x,y)=>x-y)
        const pos=(a.length-1)*p, i=Math.floor(pos), d=pos-i
        return i+1<a.length ? a[i]*(1-d)+a[i+1]*d : a[i]
      }
      cats.forEach((c, ci)=>{
        const a=groups.get(c)||[]
        if(a.length){
          const Q1=q(a,0.25), Q2=q(a,0.5), Q3=q(a,0.75)
          const IQR=Q3-Q1
          const lo=Q1-1.5*IQR, hi=Q3+1.5*IQR
          const min=Math.min(...a.filter(v=>v>=lo))
          const max=Math.max(...a.filter(v=>v<=hi))
          five.push([min,Q1,Q2,Q3,max])
          a.forEach(v=>{ if(v<lo||v>hi) outliers.push([ci, v]) })
        }else{
          five.push([NaN,NaN,NaN,NaN,NaN])
        }
      })
      this.boxplotChart.setOption({
        title:{ text:`${this.metricLabel(key)} — Boxplot（箱线图）`, left:'center', top:8 },
        tooltip:{ trigger:'item' },
        grid:{ left:60,right:30,top:60,bottom:60,containLabel:true },
        xAxis:{ type:'category', data:cats, axisLabel:{ interval:0, rotate:20 } },
        yAxis:{ type:'value', name:this.metricLabel(key), splitLine:{ lineStyle:{ color:'#F3F4F6' } } },
        legend:{ right:10, bottom:10, data:['Box（箱线）','Outliers（异常点）'] },
        series:[
          { name:'Box（箱线）', type:'boxplot', data:five, itemStyle:{ borderColor:'#4B5563' } },
          { name:'Outliers（异常点）', type:'scatter', data:outliers.map(([i,v])=>[cats[i],v]) }
        ]
      })
    },
    renderBootstrapCI(B=1000){
      const key=this.ciMetricKey, groupKey=this.ciGroupKey
      if(!key||!groupKey) return
      const groups=this.extractNumCat(key, groupKey)
      const cats=Array.from(groups.keys())
      const randInt=(n)=>Math.floor(Math.random()*n)
      const quantile=(arr,p)=>{ const a=[...arr].sort((x,y)=>x-y); const pos=(a.length-1)*p, i=Math.floor(pos), d=pos-i; return i+1<a.length ? a[i]*(1-d)+a[i+1]*d : a[i] }
      const results=[]
      for(const c of cats){
        const arr=groups.get(c)||[]
        const n=arr.length
        if(n===0){ results.push({cat:c, mean:NaN, lo:NaN, hi:NaN}); continue }
        const means=[]
        for(let b=0;b<B;b++){
          let s=0
          for(let i=0;i<n;i++){ s += arr[randInt(n)] }
          means.push(s/n)
        }
        const m=this.mean(arr), lo=quantile(means,0.025), hi=quantile(means,0.975)
        results.push({ cat:c, mean:m, lo, hi })
      }
      const categories=results.map(r=>r.cat)
      const means=results.map(r=>r.mean)
      const errData=results.map((r,i)=>[i, r.lo, r.hi])
      const renderItem=(params, api)=>{
        const x=api.coord([api.value(0), 0])[0]
        const yLo=api.coord([0, api.value(1)])[1]
        const yHi=api.coord([0, api.value(2)])[1]
        const w=8
        return {
          type:'group',
          children:[
            { type:'line', shape:{ x1:x, y1:yLo, x2:x, y2:yHi }, style:{ stroke:'#111827', lineWidth:1.5 } },
            { type:'line', shape:{ x1:x-w, y1:yLo, x2:x+w, y2:yLo }, style:{ stroke:'#111827', lineWidth:1.5 } },
            { type:'line', shape:{ x1:x-w, y1:yHi, x2:x+w, y2:yHi }, style:{ stroke:'#111827', lineWidth:1.5 } },
          ]
        }
      }
      this.ciChart.setOption({
        title:{ text:`${this.metricLabel(key)} — Mean & 95% CI（均值与置信区间）`, left:'center', top:8 },
        tooltip:{ trigger:'axis' },
        legend:{ right:10, bottom:10, data:['Mean（均值）','95% CI（置信区间）'] },
        grid:{ left:60,right:40,top:60,bottom:60,containLabel:true },
        xAxis:{ type:'category', data:categories, axisLabel:{ interval:0, rotate:20 } },
        yAxis:{ type:'value', name:'Mean（均值）', splitLine:{ lineStyle:{ color:'#F3F4F6' } } },
        series:[
          { name:'Mean（均值）', type:'bar', data:means, barMaxWidth:40, label:{ show:true, position:'top', formatter:({value})=>this.fmt(value) } },
          { name:'95% CI（置信区间）', type:'custom', encode:{ x:0, y:[1,2] }, data: errData, renderItem, z: 10 }
        ]
      })
    },

    // ---- Others ----
    handleResize(){
      ;[this.pairChart, this.corrHeat, this.ageTrendChart, this.contingencyHeat, this.boxplotChart, this.ciChart]
          .forEach(ch => { if (ch) try { ch.resize() } catch {} })
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
  background: #ffffff;
  color: #111827;
  padding: 16px 20px 20px;
  box-sizing: border-box;
}

/* Header */
.header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 14px;
  border-bottom: 1px solid #E5E7EB;
  padding-bottom: 10px;
}
.title .h1 { font-size: 26px; font-weight: 800; color: #0f172a; }
.subtitle { margin-top: 6px; color: #6B7280; font-size: 14px; }
.ops { display: flex; gap: 12px; align-items: center; }
.ctrl { width: 180px; }
.ctrl.wide { width: 420px; }

/* Groups */
.groups {
  flex: 1;
  min-height: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 22px;
}
.group-title {
  font-size: 16px;
  font-weight: 700;
  margin: 6px 2px 10px;
  color: #111827;
}

/* Selectors */
.selectors {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 10px;
}
.tips { color: #6B7280; font-size: 12px; }

/* Cards */
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
.stat-card { min-height: 180px; }

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}
.card-title { font-size: 14px; font-weight: 600; color: #111827; }

/* Chart container */
.chart { flex: 1; min-height: 300px; }

/* Responsive */
@media (max-width: 1200px) {
  .cards { grid-template-columns: 1fr; }
  .ctrl.wide { width: 100%; }
}
</style>
