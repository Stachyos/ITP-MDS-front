<template>
  <div class="dashboard">
    <Header />

    <!-- Top: Title + Global Filters -->
    <div class="header">
      <div class="title">
        <div class="h1">Deep Analysis</div>
        <div class="subtitle">
          Correlation, Chi-square, t-test, Correlation Matrix, Age Trends, Boxplot, Bootstrap CI
        </div>
      </div>

      <div class="ops">
        <el-select
            v-model="globalSex"
            size="large"
            class="ctrl"
            placeholder="Sex"
            @change="renderAll"
        >
          <el-option v-for="s in sexOptions" :key="s" :label="s" :value="s" />
        </el-select>

        <el-select
            v-model="globalAgeBucket"
            size="large"
            class="ctrl"
            placeholder="Age Range"
            @change="renderAll"
        >
          <el-option v-for="ab in ageBucketOptions" :key="ab" :label="ab" :value="ab" />
        </el-select>

        <el-button :loading="loading" @click="reload">Refresh</el-button>
      </div>
    </div>

    <div class="groups" v-loading="loading">
      <!-- 1. A/B Variable Tests -->
      <div class="group">
        <div class="group-title">A/B Variable Tests</div>

        <div class="selectors">
          <el-select v-model="varA" class="ctrl" placeholder="Select Variable A" @change="renderVarPair">
            <el-option v-for="m in allMetricOptions" :key="m.key" :label="m.label" :value="m.key" />
          </el-select>
          <el-select v-model="varB" class="ctrl" placeholder="Select Variable B" @change="renderVarPair">
            <el-option v-for="m in allMetricOptions" :key="m.key" :label="m.label" :value="m.key" />
          </el-select>
          <div class="tips">Auto-selects suitable test and chart based on variable types</div>
        </div>

        <div class="cards ab-test-cards">
          <!-- Statistics card -->
          <div class="card stat-card">
            <div class="card-header">
              <div class="card-title">Statistics</div>
            </div>
            <div class="stat-content">
              <div v-if="pairType === 'num-num'" class="stat-grid">
                <div class="stat-item"><span>Pearson r:</span>{{ fmt(statsPair.pearson?.r) }}</div>
                <div class="stat-item"><span>Sample Size n:</span>{{ statsPair.commonN }}</div>
                <div class="stat-item"><span>Spearman ρ:</span>{{ fmt(statsPair.spearman?.rho) }}</div>
                <div class="stat-item"><span>Slope:</span>{{ fmt(statsPair.reg?.slope) }}</div>
                <div class="stat-item"><span>Intercept:</span>{{ fmt(statsPair.reg?.intercept) }}</div>
                <div class="stat-item"><span>R²:</span>{{ fmt(statsPair.reg?.r2) }}</div>
              </div>
              <div v-else-if="pairType === 'cat-cat'" class="stat-grid">
                <div class="stat-item"><span>Chi-square χ²:</span>{{ fmt(statsPair.chi2?.chi2) }}</div>
                <div class="stat-item"><span>df:</span>{{ statsPair.chi2?.df }}</div>
                <div class="stat-item"><span>p-value (approx.):</span>{{ fmt(statsPair.chi2?.p) }}</div>
              </div>
              <div v-else-if="pairType === 'num-cat'" class="stat-grid">
                <div class="stat-item"><span>#Groups:</span>{{ statsPair.group?.k }}</div>
                <div class="stat-item" v-if="statsPair.group?.type === 'welch'"><span>Welch t:</span>{{ fmt(statsPair.group?.t) }}</div>
                <div class="stat-item" v-if="statsPair.group?.type === 'welch'"><span>df (approx.):</span>{{ fmt(statsPair.group?.df) }}</div>
                <div class="stat-item" v-if="statsPair.group?.type === 'welch'"><span>p-value (approx.):</span>{{ fmt(statsPair.group?.p) }}</div>
                <div class="stat-note" v-if="statsPair.group?.type === 'anova'">
                  Multi-group mean comparison shown; for strict ANOVA please verify on the server side.
                </div>
              </div>
              <div v-else class="stat-note">Please pick two variables</div>
            </div>
          </div>

          <!-- Visualization card -->
          <div class="card chart-card">
            <div class="card-header">
              <div class="card-title">Visualization</div>
            </div>
            <div class="chart" ref="pairChartRef"></div>
          </div>
        </div>

        <!-- Contingency heatmap (only for cat-cat, on a second row) -->
        <div class="cards contingency-cards" v-if="pairType === 'cat-cat'">
          <div class="card chart-card full-width">
            <div class="card-header">
              <div class="card-title">Contingency Heatmap</div>
            </div>
            <div class="chart" ref="contingencyHeatRef"></div>
          </div>
        </div>
      </div>

      <!-- 2. Correlation Matrix -->
      <div class="group">
        <div class="group-title">Correlation Matrix (Pearson)</div>
        <div class="cards single-card">
          <div class="card chart-card">
            <div class="card-header">
              <div class="card-title">Heatmap</div>
            </div>
            <div class="chart" ref="corrHeatRef"></div>
          </div>
        </div>
      </div>

      <!-- 3. Age Trends -->
      <div class="group">
        <div class="group-title">Means by Age Bucket</div>

        <div class="selectors">
          <el-select
              v-model="ageTrendKeys"
              class="ctrl wide"
              multiple
              collapse-tags
              :max-collapse-tags="3"
              placeholder="Select numeric metrics"
              @change="renderAgeTrend"
          >
            <el-option v-for="m in numericMetricOptions" :key="m.key" :label="m.label" :value="m.key" />
          </el-select>
          <div class="tips">Buckets: -∞~18, 18~30, …, 80+ (mean per bucket)</div>
        </div>

        <div class="cards single-card">
          <div class="card chart-card">
            <div class="card-header">
              <div class="card-title">Line Chart</div>
            </div>
            <div class="chart" ref="ageTrendRef"></div>
          </div>
        </div>
      </div>

      <!-- 4. Boxplot -->
      <div class="group">
        <div class="group-title">Boxplot by Sex / Hypertension</div>

        <div class="selectors">
          <el-select v-model="boxMetricKey" class="ctrl" placeholder="Select numeric metric" @change="renderBoxplot">
            <el-option v-for="m in numericMetricOptions" :key="m.key" :label="m.label" :value="m.key" />
          </el-select>
          <el-select v-model="boxGroupKey" class="ctrl" placeholder="Group by" @change="renderBoxplot">
            <el-option label="Sex" value="sex" />
            <el-option label="Hypertension History" value="hypertensionHistory" />
          </el-select>
          <div class="tips">Show Q1/Median/Q3, whiskers (1.5×IQR), and outliers</div>
        </div>

        <div class="cards single-card">
          <div class="card chart-card">
            <div class="card-header">
              <div class="card-title">{{ metricLabel(boxMetricKey) }} — Boxplot</div>
            </div>
            <div class="chart" ref="boxplotRef"></div>
          </div>
        </div>
      </div>

      <!-- 5. Bootstrap CI -->
      <div class="group">
        <div class="group-title">Mean & 95% CI (Bootstrap)</div>

        <div class="selectors">
          <el-select v-model="ciMetricKey" class="ctrl" placeholder="Select numeric metric" @change="renderBootstrapCI">
            <el-option v-for="m in numericMetricOptions" :key="m.key" :label="m.label" :value="m.key" />
          </el-select>
          <el-select v-model="ciGroupKey" class="ctrl" placeholder="Group by" @change="renderBootstrapCI">
            <el-option label="Sex" value="sex" />
            <el-option label="Hypertension History" value="hypertensionHistory" />
          </el-select>
          <div class="tips">Per-group bootstrap (default 1000) — 2.5%~97.5% quantiles as 95% CI</div>
        </div>

        <div class="cards single-card">
          <div class="card chart-card">
            <div class="card-header">
              <div class="card-title">{{ metricLabel(ciMetricKey) }} — Mean & 95% CI</div>
            </div>
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
      globalSex: 'All',
      globalAgeBucket: 'All',

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

      // Metric definitions (labels in English)
      metrics: [
        { key: 'totalCholesterol', label: 'Total Cholesterol', kind: 'number', buckets: [3,4,5,6,7,8] },
        { key: 'triglyceride',     label: 'Triglyceride',      kind: 'number', buckets: [0.8,1.7,2.3,5.6] },
        { key: 'hdlC',             label: 'HDL-C',             kind: 'number', buckets: [0.8,1.0,1.3,1.6] },
        { key: 'ldlC',             label: 'LDL-C',             kind: 'number', buckets: [1.8,2.6,3.4,4.1] },
        { key: 'vldlC',            label: 'VLDL-C',            kind: 'number', buckets: [0.2,0.4,0.8] },
        { key: 'pulse',            label: 'Pulse (bpm)',       kind: 'number', buckets: [60,80,100,120] },
        { key: 'diastolicBp',      label: 'Diastolic BP (mmHg)', kind: 'number', buckets: [60,80,90,100] },
        { key: 'bun',              label: 'BUN',               kind: 'number', buckets: [3,7,9] },
        { key: 'uricAcid',         label: 'Uric Acid',         kind: 'number', buckets: [240,360,420,480] },
        { key: 'creatinine',       label: 'Creatinine',        kind: 'number', buckets: [60,97,133,186] },
        { key: 'age',              label: 'Age',               kind: 'number', buckets: [18,30,40,50,60,70,80] },
        { key: 'sex',              label: 'Sex',               kind: 'enum' },
        { key: 'hypertensionHistory', label: 'Hypertension History', kind: 'bool' },
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
      return ['All', 'Male', 'Female', 'Other', 'Unknown']
    },
    ageBucketOptions() {
      const labels = this.bucketLabels([18,30,40,50,60,70,80])
      return ['All', ...labels]
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
        ElMessage.error(e?.message || 'Load failed')
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
    ageBucket(n){ if(n==null) return 'Unknown'; return this.bucketize(n,[18,30,40,50,60,70,80]) },
    applyGlobalFilters(records){
      let arr = records || []
      if (this.globalSex !== 'All') arr = arr.filter(r => (this.sexLabel(r?.sex ?? 'Unknown') === this.globalSex))
      if (this.globalAgeBucket !== 'All') arr = arr.filter(r => this.ageBucket(r?.age ?? null) === this.globalAgeBucket)
      return arr
    },
    // Map possible Chinese backend values to English display labels
    sexLabel(v){
      const map = { '男':'Male', '女':'Female', '其他':'Other', '未知':'Unknown' }
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
        if(catKey==='hypertensionHistory') g = r[catKey] ? 'Yes' : 'No'
        if(catKey==='sex') g = this.sexLabel(r[catKey] ?? 'Unknown')
        if(typeof v==='number'&&!Number.isNaN(v)){
          const key=String(g??'Unknown')
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
        if(aKey==='hypertensionHistory') a=r[aKey]?'Yes':'No'
        if(bKey==='hypertensionHistory') b=r[bKey]?'Yes':'No'
        if(aKey==='sex') a=this.sexLabel(a ?? 'Unknown')
        if(bKey==='sex') b=this.sexLabel(b ?? 'Unknown')
        a=String(a??'Unknown'); b=String(b??'Unknown')
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
          title:{
            text: `${this.metricLabel(this.varA)} vs ${this.metricLabel(this.varB)} — Scatter & Regression`,
            left: 'center',
            top: 8,
            textStyle: {
              fontSize: 14,
              fontWeight: 'normal'
            }
          },
          tooltip:{ trigger:'item' },
          grid:{
            left: 20,
            right: 50,
            top: 40,
            bottom: 50,
            containLabel: true
          },
          xAxis:{
            type:'value',
            name:this.metricLabel(this.varA),
            splitLine:{ lineStyle:{ color:'#F3F4F6' } }
          },
          yAxis:{
            type:'value',
            name:this.metricLabel(this.varB),
            splitLine:{ lineStyle:{ color:'#F3F4F6' } }
          },
          legend:{
            right: 10,
            bottom: 10,
            data:['Samples','Fit Line'],
            textStyle: { fontSize: 12 }
          },
          series:[
            { name:'Samples', type:'scatter', data, symbolSize: 6 },
            { name:'Fit Line', type:'line', data:fit, showSymbol:false, lineStyle: { width: 2 } }
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
          title:{
            text: `${this.metricLabel(this.varA)} × ${this.metricLabel(this.varB)} — Stacked Bars`,
            left: 'center',
            top: 8,
            textStyle: { fontSize: 14, fontWeight: 'normal' }
          },
          tooltip:{ trigger:'axis', axisPointer:{ type:'shadow' } },
          legend:{ right:10, bottom:10, textStyle: { fontSize: 12 } },
          grid:{ left:50, right:30, top:40, bottom:60, containLabel:true },
          xAxis:{
            type:'category',
            data:categories,
            axisLabel:{ interval:0, rotate:20, fontSize: 11 }
          },
          yAxis:{
            type:'value',
            name:'Count',
            splitLine:{ lineStyle:{ color:'#F3F4F6' } },
            nameTextStyle: { fontSize: 11 }
          },
          series
        })
        if(!this.contingencyHeat) this.contingencyHeat=echarts.init(this.$refs.contingencyHeatRef)
        const heatData=[]
        for(let i=0;i<ct.rows.length;i++){ for(let j=0;j<ct.cols.length;j++){ heatData.push([j,i,ct.table[i][j]]) } }
        this.contingencyHeat.setOption({
          title:{ text:'Contingency Heatmap', left:'center', top:6, textStyle: { fontSize: 14, fontWeight: 'normal' } },
          tooltip:{ position:'top' },
          grid:{ left:70, right:30, top:35, bottom:60, containLabel:true },
          xAxis:{ type:'category', data:ct.cols, splitArea:{ show:true }, axisLabel: { fontSize: 11 } },
          yAxis:{ type:'category', data:ct.rows, splitArea:{ show:true }, axisLabel: { fontSize: 11 } },
          visualMap:{
            min:0,
            max:Math.max(1,...heatData.map(d=>d[2])),
            calculable:true,
            orient:'horizontal',
            left:'center',
            bottom:10,
            textStyle: { fontSize: 11 }
          },
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
          title:{
            text: `Group Means of ${this.metricLabel(numKey)} by ${this.metricLabel(catKey)}`,
            left: 'center',
            top: 8,
            textStyle: { fontSize: 14, fontWeight: 'normal' }
          },
          tooltip:{ trigger:'axis' },
          legend:{ right:10, bottom:10, data:['Mean'], textStyle: { fontSize: 12 } },
          grid:{ left:50, right:30, top:40, bottom:60, containLabel:true },
          xAxis:{ type:'category', data:cats, axisLabel:{ interval:0, rotate:20, fontSize: 11 } },
          yAxis:{
            type:'value',
            name:'Mean',
            splitLine:{ lineStyle:{ color:'#F3F4F6' } },
            nameTextStyle: { fontSize: 11 }
          },
          series:[{
            name:'Mean',
            type:'bar',
            data:means,
            barMaxWidth:40,
            label:{ show:true, position:'top', fontSize: 11 }
          }]
        })
        if(this.contingencyHeat){ this.contingencyHeat.dispose(); this.contingencyHeat=null }
      }
    },

    // Other renderers with adjusted titles
    renderCorrMatrix(){
      const numKeys = this.metrics.filter(m => m.kind === 'number').map(m => m.key)
      const data = this.applyGlobalFilters(this.raw)
      const mat = []

      // Compute correlation matrix
      for(let i = 0; i < numKeys.length; i++){
        for(let j = 0; j < numKeys.length; j++){
          const xi = data.map(r => r[numKeys[i]]).filter(v => typeof v === 'number' && !Number.isNaN(v))
          const yj = data.map(r => r[numKeys[j]]).filter(v => typeof v === 'number' && !Number.isNaN(v))
          const n = Math.min(xi.length, yj.length)
          const x = xi.slice(0, n), y = yj.slice(0, n)
          const { r } = this.pearson(x, y)
          mat.push([i, j, isNaN(r) ? 0 : r])
        }
      }

      const labels = numKeys.map(k => this.metricLabel(k))

      this.corrHeat.setOption({
        title: {
          text: 'Correlation Matrix',
          left: 'center',
          top: 10,
          textStyle: { fontSize: 16, fontWeight: 'bold', color: '#333' }
        },
        tooltip: {
          position: 'top',
          backgroundColor: 'rgba(0,0,0,0.8)',
          borderColor: '#333',
          textStyle: { color: '#fff' },
          formatter: p => {
            const value = p.value[2]
            const abs = Math.abs(value)
            const strength = abs > 0.7 ? 'Strong' : abs > 0.3 ? 'Moderate' : 'Weak'
            return `
              <div style="text-align:center;">
                <div><strong>${labels[p.value[1]]} × ${labels[p.value[0]]}</strong></div>
                <div>Correlation: <strong>${this.fmt(value)}</strong></div>
                <div style="font-size:12px;color:#ccc">${strength}</div>
              </div>
            `
          }
        },
        grid: { left: 120, right: 40, top: 50, bottom: 100, height: numKeys.length * 30, width: 'auto', containLabel: true },
        xAxis: {
          type: 'category',
          data: labels,
          splitArea: { show: true },
          axisLabel: { rotate: 45, fontSize: 11, interval: 0, margin: 15 },
          axisLine: { lineStyle: { color: '#ccc' } }
        },
        yAxis: {
          type: 'category',
          data: labels,
          splitArea: { show: true },
          axisLabel: { fontSize: 11, margin: 10 },
          axisLine: { lineStyle: { color: '#ccc' } }
        },
        visualMap: {
          min: -1,
          max: 1,
          calculable: true,
          orient: 'horizontal',
          left: 'center',
          bottom: 30,
          itemWidth: 12,
          itemHeight: 80,
          textStyle: { fontSize: 10 },
          // Simple gradient palette
          inRange: { color: ['#1a3670', '#597ef7', '#adc6ff', '#f0f0f0', '#ffccc7', '#ff4d4f', '#a8071a'] }
        },
        series: [{
          type: 'heatmap',
          data: mat,
          emphasis: { itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0, 0, 0, 0.5)' } },
          label: {
            show: true,
            formatter: ({ value }) => {
              const num = value[2]
              if (Math.abs(num) > 0.3) return this.fmt(num)
              return ''
            },
            color: '#333',
            fontSize: 10,
            fontWeight: 'bold'
          },
          itemStyle: { borderColor: '#fff', borderWidth: 1 }
        }]
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
        title:{ text:'Means by Age Bucket', left:'center', top:8, textStyle: { fontSize: 14, fontWeight: 'normal' } },
        tooltip:{ trigger:'axis' },
        legend:{ right:10, bottom:10, textStyle: { fontSize: 12 } },
        grid:{ left:50, right:30, top:40, bottom:60, containLabel:true },
        xAxis:{ type:'category', data:labels, axisLabel: { fontSize: 11 } },
        yAxis:{
          type:'value',
          name:'Mean',
          splitLine:{ lineStyle:{ color:'#F3F4F6' } },
          nameTextStyle: { fontSize: 11 }
        },
        series
      })
    },

    renderBoxplot(){
      const key=this.boxMetricKey, groupKey=this.boxGroupKey
      if(!key||!groupKey) return
      const groups = this.extractNumCat(key, groupKey)
      const cats = Array.from(groups.keys())
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
        title:{ text:`${this.metricLabel(key)} — Boxplot`, left:'center', top:8, textStyle: { fontSize: 14, fontWeight: 'normal' } },
        tooltip:{ trigger:'item' },
        grid:{ left:60, right:30, top:40, bottom:60, containLabel:true },
        xAxis:{ type:'category', data:cats, axisLabel:{ interval:0, rotate:20, fontSize: 11 } },
        yAxis:{
          type:'value',
          name:this.metricLabel(key),
          splitLine:{ lineStyle:{ color:'#F3F4F6' } },
          nameTextStyle: { fontSize: 11 }
        },
        legend:{ right:10, bottom:10, data:['Box','Outliers'], textStyle: { fontSize: 12 } },
        series:[
          { name:'Box', type:'boxplot', data:five, itemStyle:{ borderColor:'#4B5563' } },
          { name:'Outliers', type:'scatter', data:outliers.map(([i,v])=>[cats[i],v]) }
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
        title:{ text:`${this.metricLabel(key)} — Mean & 95% CI`, left:'center', top:8, textStyle: { fontSize: 14, fontWeight: 'normal' } },
        tooltip:{ trigger:'axis' },
        legend:{ right:10, bottom:10, data:['Mean','95% CI'], textStyle: { fontSize: 12 } },
        grid:{ left:60, right:40, top:40, bottom:60, containLabel:true },
        xAxis:{ type:'category', data:categories, axisLabel:{ interval:0, rotate:20, fontSize: 11 } },
        yAxis:{
          type:'value',
          name:'Mean',
          splitLine:{ lineStyle:{ color:'#F3F4F6' } },
          nameTextStyle: { fontSize: 11 }
        },
        series:[
          { name:'Mean', type:'bar', data:means, barMaxWidth:40, label:{ show:true, position:'top', formatter:({value})=>this.fmt(value) } },
          { name:'95% CI', type:'custom', encode:{ x:0, y:[1,2] }, data: errData, renderItem, z: 10 }
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
:global(html, body, #app) {
  height: 100%;
  margin: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

.dashboard {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f8fafc;
  color: #111827;
  padding: 16px 20px 20px;
  box-sizing: border-box;
  overflow: hidden;
}

/* Header */
.header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e2e8f0;
  flex-wrap: wrap;
  gap: 16px;
}

.title .h1 {
  font-size: 24px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.subtitle {
  margin-top: 6px;
  color: #64748b;
  font-size: 14px;
  line-height: 1.4;
  max-width: 600px;
}

.ops {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.ctrl {
  width: 180px;
  min-width: 160px;
}

.ctrl.wide {
  width: 420px;
  min-width: 300px;
}

/* Groups container */
.groups {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding-right: 4px;
}

.groups::-webkit-scrollbar { width: 6px; }
.groups::-webkit-scrollbar-track { background: #f1f5f9; border-radius: 3px; }
.groups::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 3px; }
.groups::-webkit-scrollbar-thumb:hover { background: #94a3b8; }

/* Group section */
.group {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.08);
  border: 1px solid #e2e8f0;
}

.group-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 16px 0;
  color: #1e293b;
  padding-bottom: 8px;
  border-bottom: 2px solid #f1f5f9;
}

/* Selectors */
.selectors {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 20px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.tips {
  color: #64748b;
  font-size: 13px;
  line-height: 1.4;
  flex: 1;
  min-width: 200px;
}

/* Cards layout - A/B Test layout */
.ab-test-cards {
  display: grid;
  grid-template-columns: 1fr 2fr; /* stats:1, chart:2 */
  gap: 20px;
  margin-bottom: 20px;
}

.contingency-cards {
  display: grid;
  grid-template-columns: 1fr;
  gap: 20px;
}

.cards.single-card { grid-template-columns: 1fr; }

/* Cards */
.card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 0;
  display: flex;
  flex-direction: column;
  min-height: 400px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
  transition: box-shadow 0.2s ease, border-color 0.2s ease;
}

.card:hover {
  box-shadow: 0 4px 6px rgba(15, 23, 42, 0.1);
  border-color: #cbd5e1;
}

.stat-card { min-height: 400px; }
.chart-card { min-height: 400px; }

.card.full-width { grid-column: 1 / -1; }

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #f1f5f9;
  background: #f8fafc;
  border-radius: 12px 12px 0 0;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #334155;
  margin: 0;
}

/* Stat content */
.stat-content {
  padding: 20px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.stat-grid { display: flex; flex-direction: column; gap: 12px; }

.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  background: #f8fafc;
  border-radius: 8px;
  border-left: 3px solid #3b82f6;
  transition: background-color 0.2s ease;
}

.stat-item:hover { background: #f1f5f9; }

.stat-item span {
  font-weight: 500;
  color: #475569;
  font-size: 14px;
}

.stat-item:not(span) {
  font-family: 'Courier New', monospace;
  font-weight: 600;
  color: #1e293b;
  font-size: 14px;
}

.stat-note {
  color: #64748b;
  font-size: 14px;
  text-align: center;
  padding: 20px;
  font-style: italic;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* Chart container */
.chart { flex: 1; min-height: 320px; padding: 8px; }

/* Responsive */
@media (max-width: 1200px) {
  .ab-test-cards { grid-template-columns: 1fr; }
  .cards.single-card { grid-template-columns: 1fr; }
}

@media (max-width: 968px) {
  .ab-test-cards { grid-template-columns: 1fr; gap: 16px; }
  .stat-card, .chart-card { min-height: 350px; }
}

@media (max-width: 768px) {
  .dashboard { padding: 12px 16px; height: 100vh; }

  .header { flex-direction: column; align-items: stretch; gap: 12px; }

  .title .h1 { font-size: 20px; }
  .subtitle { font-size: 13px; }

  .ops { justify-content: flex-start; }

  .ctrl, .ctrl.wide { width: 100%; min-width: unset; }

  .group { padding: 16px; }
  .selectors { flex-direction: column; align-items: stretch; gap: 8px; }
  .cards { gap: 16px; }
  .card { min-height: 320px; }
  .chart-card { min-height: 320px; }

  .stat-item { flex-direction: column; align-items: flex-start; gap: 4px; }
  .stat-item span { font-size: 13px; }
  .stat-item:not(span) { font-size: 13px; }
}

@media (max-width: 480px) {
  .dashboard { padding: 8px 12px; }
  .group { padding: 12px; }
  .group-title { font-size: 16px; }
  .card-header { padding: 12px 16px; }
  .stat-content { padding: 16px; }
  .chart { min-height: 280px; }
  .stat-card, .chart-card { min-height: 300px; }
}

/* Loading state */
:deep(.el-loading-mask) {
  border-radius: 12px;
  background-color: rgba(248, 250, 252, 0.8);
}

/* Element Plus select styling */
:deep(.el-select .el-input__inner) { border-radius: 8px; }
:deep(.el-select .el-input__wrapper) { border-radius: 8px; box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04); }
:deep(.el-select .el-input__wrapper:hover) { box-shadow: 0 1px 3px rgba(15, 23, 42, 0.1); }

/* Button styling */
:deep(.el-button) { border-radius: 8px; font-weight: 500; }
</style>
