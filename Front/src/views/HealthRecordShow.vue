<template>
  <div class="home-table-container">
    <Header />

    <div class="main-content">
      <div class="content-header">
        <h2 class="page-title">数据管理（Data Table）</h2>
      </div>

      <div class="table-container">
        <!-- 顶部操作栏 -->
        <div class="toolbar">
          <div class="title">健康数据记录</div>
          <div class="ops">
            <el-input v-model="keyword" placeholder="筛选关键词（按姓名）" clearable style="width: 220px" />
            <el-button @click="fetchList" :loading="loading">刷新</el-button>
            <!-- 新增：导入按钮 -->
            <el-button @click="onPickFile" :loading="importing">导入数据</el-button>
            <!-- 隐藏的文件选择框 -->
            <input
                ref="fileInput"
                type="file"
                accept=".csv,.xlsx,.xls,.json"
                style="display:none"
                @change="onFileChange"
            />

            <el-button @click="downloadTemplate" :loading="downloading">下载模板</el-button>
            <el-button type="primary" @click="openCreate" :loading="loading">新建</el-button>
          </div>
        </div>

        <!-- 导入进度条 -->
        <el-progress
            v-if="showProgress"
            :percentage="importProgress"
            :stroke-width="18"
            status="success"
            style="margin-bottom: 10px;"
        />

        <!-- 数据表（前端分页后仅渲染当前页） -->
        <el-table
            v-loading="loading"
            :data="pagedList"
            border
            style="width: 100%"
            highlight-current-row
        >
          <el-table-column prop="recordId" label="ID" width="60" />
          <el-table-column prop="name" label="名称/标签" min-width="100" />
          <el-table-column prop="sex" label="性别" width="80" />
          <el-table-column prop="age" label="年龄" width="80" />

          <el-table-column prop="totalCholesterol" label="总胆固醇" width="110" />
          <el-table-column prop="triglyceride" label="甘油三酯" width="110" />
          <el-table-column prop="hdlC" label="HDL-C" width="100" />
          <el-table-column prop="ldlC" label="LDL-C" width="100" />
          <el-table-column prop="vldlC" label="VLDL-C" width="110" />

          <el-table-column prop="pulse" label="脉搏" width="90" />
          <el-table-column prop="diastolicBp" label="舒张压" width="90" />
          <el-table-column label="高血压史" width="100">
            <template #default="{ row }">
              {{ row.hypertensionHistory ? '是' : '否' }}
            </template>
          </el-table-column>

          <el-table-column prop="bun" label="尿素氮" width="100" />
          <el-table-column prop="uricAcid" label="尿酸" width="100" />
          <el-table-column prop="creatinine" label="肌酐" width="100" />

          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <div class="action-buttons">
                <el-button size="small" type="primary" plain @click.stop="openEdit(row)">编辑</el-button>
                <el-button size="small" type="danger" plain @click.stop="deleteRow(row)">删除</el-button>
                <el-button size="small" plain @click.stop="exportOnePDF(row)">生成</el-button>
              </div>
            </template>
          </el-table-column>

        </el-table>

        <!-- 分页 -->
        <div style="margin-top: 12px; text-align: right;">
          <el-pagination
              background
              layout="total, sizes, prev, pager, next, jumper"
              :total="filteredTotal"
              :current-page="page"
              :page-size="pageSize"
              :page-sizes="[10, 20, 50, 100]"
              @current-change="onPageChange"
              @size-change="onPageSizeChange"
          />
        </div>
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="formVisible" :title="isEdit ? '编辑记录' : '新建记录'" width="640px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>

        <el-form-item label="性别" prop="sex">
          <el-select v-model="form.sex" placeholder="请选择">
            <el-option label="男" value="男" />
            <el-option label="女" value="女" />
            <el-option label="其他" value="其他" />
            <el-option label="未知" value="未知" />
          </el-select>
        </el-form-item>

        <el-form-item label="年龄">
          <el-input-number v-model="form.age" :min="0" :max="150" />
        </el-form-item>

        <el-form-item label="总胆固醇">
          <el-input-number v-model="form.totalCholesterol" :step="0.1" :min="0" />
        </el-form-item>

        <el-form-item label="甘油三酯">
          <el-input-number v-model="form.triglyceride" :step="0.1" :min="0" />
        </el-form-item>

        <el-form-item label="HDL-C">
          <el-input-number v-model="form.hdlC" :step="0.1" :min="0" />
        </el-form-item>

        <el-form-item label="LDL-C">
          <el-input-number v-model="form.ldlC" :step="0.1" :min="0" />
        </el-form-item>

        <el-form-item label="VLDL-C">
          <el-input-number v-model="form.vldlC" :step="0.1" :min="0" />
        </el-form-item>

        <el-form-item label="脉搏">
          <el-input-number v-model="form.pulse" :min="0" />
        </el-form-item>

        <el-form-item label="舒张压">
          <el-input-number v-model="form.diastolicBp" :min="0" />
        </el-form-item>

        <el-form-item label="高血压史">
          <el-switch v-model="form.hypertensionHistory" />
        </el-form-item>

        <el-form-item label="尿素氮 (BUN)">
          <el-input-number v-model="form.bun" :step="0.1" :min="0" />
        </el-form-item>

        <el-form-item label="尿酸">
          <el-input-number v-model="form.uricAcid" :min="0" />
        </el-form-item>

        <el-form-item label="肌酐">
          <el-input-number v-model="form.creatinine" :min="0" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="formVisible = false">取 消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保 存</el-button>
      </template>
    </el-dialog>

    <!-- 导入调试信息 -->
    <el-dialog v-model="showImportDetail" title="导入调试信息" width="680px">
      <el-descriptions :column="2" border v-if="importSummary">
        <el-descriptions-item label="totalRows">{{ importSummary.totalRows }}</el-descriptions-item>
        <el-descriptions-item label="saved">{{ importSummary.saved }}</el-descriptions-item>
        <el-descriptions-item label="deduplicated">{{ importSummary.deduplicated }}</el-descriptions-item>
        <el-descriptions-item label="skippedMissingKey">{{ importSummary.skippedMissingKey }}</el-descriptions-item>
        <el-descriptions-item label="skippedAbnormal">{{ importSummary.skippedAbnormal }}</el-descriptions-item>
        <el-descriptions-item label="message">{{ importSummary.message }}</el-descriptions-item>
      </el-descriptions>

      <h4 style="margin-top:12px;">原始响应(JSON)</h4>
      <pre style="max-height:260px;overflow:auto;background:#f7f7f7;padding:10px;border-radius:6px;">{{ rawImportResp }}</pre>

      <template #footer>
        <el-button @click="showImportDetail=false">关 闭</el-button>
        <el-button type="primary" @click="copyRaw">复制原始响应</el-button>
      </template>
    </el-dialog>
  </div>
  <!-- 右下角：导出报告（悬浮） -->
  <div class="export-fab" :class="{ disabled: loading }" @click="onExportClick">
    导出报告
  </div>

  <!-- 选择导出格式 -->
  <el-dialog v-model="chooseExportVisible" title="选择导出格式" width="420px">
    <div style="display:flex; gap:12px; flex-wrap:wrap; justify-content:center; margin-top:8px;">
      <el-button size="large" @click="onExportCommand('csv')">CSV（通用）</el-button>
      <el-button size="large" @click="onExportCommand('xlsx')">Excel（.xlsx）</el-button>
      <el-button size="large" @click="onExportCommand('pdf')">PDF（表格）</el-button>
    </div>
    <template #footer>
      <el-button @click="chooseExportVisible=false">取 消</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'        // 👈 补上 watch
import axios from 'axios'                                    // 👈 补上 axios
import { ElMessage, ElMessageBox } from 'element-plus'
import Header from '@/components/Header.vue'
import {
  getAllHealthRecords,
  createHealthRecord,
  updateHealthRecord,
  deleteHealthRecord,
  downloadHealthRecordTemplate,
  importHealthRecords
} from '@/api/HealthRecordShow.js'
import { assessMetrics } from '@/utils/health/assess.js'



const loading = ref(false)
const importProgress = ref(0)        // 进度百分比
const showProgress   = ref(false)    // 控制进度条显示
const saving  = ref(false)
const downloading = ref(false)

const list    = ref([])
const keyword = ref('')

// 先定义 filteredList，后面分页计算才不会引用未定义
const filteredList = computed(() => {
  const kw = (keyword.value || '').trim().toLowerCase()
  if (!kw) return list.value
  return list.value.filter(r => String(r.name || '').toLowerCase().includes(kw))
})

const page = ref(1)
const pageSize = ref(20)
const filteredTotal = computed(() => filteredList.value.length)
const pagedList = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredList.value.slice(start, start + pageSize.value)
})
const onPageChange = (p) => { page.value = p }
const onPageSizeChange = (s) => { page.value = 1; pageSize.value = s }
watch(keyword, () => { page.value = 1 })

const CACHE_KEY = 'health_records_cache_v1'

const formVisible = ref(false)
const isEdit      = ref(false)
const formRef     = ref(null)
const form = ref({
  recordId: null,
  name: '',
  sex: '未知',
  age: null,
  hdlC: null,
  ldlC: null,
  vldlC: null,
  triglyceride: null,
  totalCholesterol: null,
  pulse: null,
  diastolicBp: null,
  hypertensionHistory: false,
  bun: null,
  uricAcid: null,
  creatinine: null
})
const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  sex:  [{ required: true, message: '请选择性别', trigger: 'change' }]
}

const importing = ref(false)
const fileInput = ref(null)
const showImportDetail = ref(false)
const rawImportResp = ref('')
const importSummary = ref(null)

const onPickFile = () => { fileInput.value && fileInput.value.click() }
const inferFormatByExt = (filename = '') => {
  const f = filename.toLowerCase()
  if (f.endsWith('.csv')) return 'csv'
  if (f.endsWith('.xlsx') || f.endsWith('.xls')) return 'excel'
  if (f.endsWith('.json')) return 'json'
  return ''
}
const copyRaw = async () => {
  try { await navigator.clipboard.writeText(rawImportResp.value); ElMessage.success('已复制'); }
  catch { ElMessage.error('复制失败') }
}

const fetchList = async () => {
  // 先尝试从缓存拿，提升首屏
  const cached = sessionStorage.getItem(CACHE_KEY)
  if (cached) {
    try {
      list.value = JSON.parse(cached)
    } catch {}
  }

  loading.value = true
  try {
    const res = await getAllHealthRecords()
    list.value = res.data || []
    sessionStorage.setItem(CACHE_KEY, JSON.stringify(list.value))
  } catch (err) {
    ElMessage.error(err?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  form.value = {
    recordId: null,
    name: '',
    sex: '未知',
    age: null,
    hdlC: null, ldlC: null, vldlC: null,
    triglyceride: null, totalCholesterol: null,
    pulse: null, diastolicBp: null, hypertensionHistory: false,
    bun: null, uricAcid: null, creatinine: null
  }
}

const openCreate = () => {
  isEdit.value = false
  resetForm()
  formVisible.value = true
}

const openEdit = (row) => {
  isEdit.value = true
  form.value = { ...row }
  formVisible.value = true
}

const submitForm = () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      if (isEdit.value && form.value.recordId != null) {
        await updateHealthRecord(form.value.recordId, form.value)
        ElMessage.success('更新成功')
      } else {
        await createHealthRecord(form.value)
        ElMessage.success('创建成功')
      }
      formVisible.value = false
      fetchList()
    } catch (err) {
      ElMessage.error(err?.response?.data?.message || err?.message || '操作失败')
    } finally {
      saving.value = false
    }
  })
}

const deleteRow = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除记录 ID=${row.recordId}（${row.name}）吗？`, '提示', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await deleteHealthRecord(row.recordId)
    ElMessage.success('删除成功')
    fetchList()
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error(err?.response?.data?.message || err?.message || '删除失败')
    }
  }
}

const downloadTemplate = async () => {
  try {
    downloading.value = true
    const blob = await downloadHealthRecordTemplate()

    if (blob && blob.type && blob.type.includes('json')) {
      const text = await blob.text()
      try {
        const err = JSON.parse(text)
        ElMessage.error(err.message || '下载失败')
      } catch {
        ElMessage.error('下载失败：' + text.slice(0, 120))
      }
      return
    }

    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '健康数据模板.xlsx'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error(e?.message || '下载失败')
  } finally {
    downloading.value = false
  }
}

// 选择文件后上传导入
const onFileChange = async (e) => {
  const file = e.target.files && e.target.files[0]
  e.target.value = ''
  if (!file) return

  const format = inferFormatByExt(file.name)

  try {
    importing.value = true
    importProgress.value = 0
    showProgress.value = true

    const formData = new FormData()
    formData.append('file', file)
    if (format) formData.append('format', format)

    const resp = await axios.post('/api/HealthRecordShow/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (progressEvent) => {
        if (progressEvent.total) {
          importProgress.value = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        }
      }
    })

    const body = resp.data || {}
    rawImportResp.value = JSON.stringify(body, null, 2)
    importSummary.value = body?.data || null
    showImportDetail.value = true

    ElMessage.success('导入完成')
    fetchList()
  } catch (err) {
    rawImportResp.value = JSON.stringify(err?.response?.data || err, null, 2)
    showImportDetail.value = true
    ElMessage.error(err?.message || '导入失败')
  } finally {
    importing.value = false
    setTimeout(() => { showProgress.value = false }, 800)
  }
}

// === 导出：点击主按钮先询问 ===
const chooseExportVisible = ref(false)
const onExportClick = () => {
  if (loading.value) return
  chooseExportVisible.value = true
}

const onExportCommand = async (cmd) => {
  chooseExportVisible.value = false
  if (!filteredList.value?.length) {
    ElMessage.warning('当前没有可导出的数据')
    return
  }
  try {
    if (cmd === 'csv') await exportCSV()
    else if (cmd === 'xlsx') await exportXLSX()
    else if (cmd === 'pdf') await exportPDF()
  } catch (e) {
    ElMessage.error(e?.message || '导出失败')
  }
}

// === 导出实现 ===
const ts = () => {
  const d = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}${pad(d.getMonth()+1)}${pad(d.getDate())}_${pad(d.getHours())}${pad(d.getMinutes())}${pad(d.getSeconds())}`
}

const EXPORT_COLUMNS = [
  { key: 'recordId',         title: 'ID' },
  { key: 'name',             title: 'Name/Label' },
  { key: 'sex',              title: 'Gender', format: mapSex },
  { key: 'age',              title: 'Age' },
  { key: 'totalCholesterol', title: 'Total Cholesterol' },
  { key: 'triglyceride',     title: 'Triglycerides' },
  { key: 'hdlC',             title: 'HDL-C' },
  { key: 'ldlC',             title: 'LDL-C' },
  { key: 'vldlC',            title: 'VLDL-C' },
  { key: 'pulse',            title: 'Pulse' },
  { key: 'diastolicBp',      title: 'Diastolic BP' },
  { key: 'hypertensionHistory', title: 'Hypertension History', format: mapYesNo },
  { key: 'bun',              title: 'Blood Urea Nitrogen (BUN)' },
  { key: 'uricAcid',         title: 'Uric Acid' },
  { key: 'creatinine',       title: 'Creatinine' },
]

function mapSex(v) {
  const s = String(v ?? '').trim().toLowerCase()
  if (['男','m','male','man','1'].includes(s)) return 'Male'
  if (['女','f','female','woman','0'].includes(s)) return 'Female'
  if (['其他','other'].includes(s)) return 'Other'
  if (['未知','unknown','unk',''].includes(s)) return 'Unknown'
  return v ?? ''
}

function mapYesNo(v) {
  if (typeof v === 'boolean') return v ? 'Yes' : 'No'
  const s = String(v ?? '').trim().toLowerCase()
  if (['是','yes','y','true','1'].includes(s)) return 'Yes'
  if (['否','no','n','false','0'].includes(s)) return 'No'
  return v ?? ''
}

const buildRows = () => {
  return filteredList.value.map(row => {
    const obj = {}
    for (const c of EXPORT_COLUMNS) {
      const raw = row?.[c.key]
      const val = typeof c.format === 'function' ? c.format(raw, row) : (raw ?? '')
      obj[c.title] = val
    }
    return obj
  })
}

const exportCSV = async () => {
  const rows = buildRows()
  const headers = Object.keys(rows[0] || EXPORT_COLUMNS.reduce((acc, c) => (acc[c.title]='', acc), {}))
  const esc = (s) => {
    const str = String(s ?? '')
    if (/[",\n]/.test(str)) return `"${str.replace(/"/g, '""')}"`
    return str
  }
  const lines = [
    headers.join(','),
    ...rows.map(row => headers.map(h => esc(row[h])).join(','))
  ]
  const blob = new Blob([lines.join('\n')], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `HealthRecords_${ts()}.csv`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
  ElMessage.success('CSV 导出完成')
}

const exportXLSX = async () => {
  try {
    const xlsx = await import('xlsx')
    const rows = buildRows()
    const ws = xlsx.utils.json_to_sheet(rows)
    const wb = xlsx.utils.book_new()
    xlsx.utils.book_append_sheet(wb, ws, 'Records')
    xlsx.writeFile(wb, `HealthRecords_${ts()}.xlsx`)
    ElMessage.success('Excel 导出完成')
  } catch (e) {
    ElMessage.error('未检测到 xlsx 依赖，请先安装：npm i xlsx')
    throw e
  }
}

const exportPDF = async () => {
  try {
    const jsPDF = (await import('jspdf')).default
    let autoTable
    try {
      autoTable = (await import('jspdf-autotable')).default
    } catch {}
    const doc = new jsPDF({ orientation: 'landscape', unit: 'pt', format: 'a4' })
    doc.setFontSize(16)
    doc.text('健康数据记录导出', 40, 40)

    const rows = buildRows()
    const headers = Object.keys(rows[0] || EXPORT_COLUMNS.reduce((acc, c) => (acc[c.title]='', acc), {}))

    if (autoTable) {
      autoTable(doc, {
        head: [headers],
        body: rows.map(r => headers.map(h => r[h])),
        startY: 60,
        styles: { fontSize: 9, cellPadding: 4 },
        headStyles: { fontStyle: 'bold' }
      })
    } else {
      doc.setFontSize(10)
      let y = 60
      doc.text(headers.join(' | '), 40, y)
      y += 16
      rows.forEach(row => {
        doc.text(headers.map(h => String(row[h])).join(' | '), 40, y)
        y += 14
        if (y > 560) { doc.addPage(); y = 40 }
      })
    }
    doc.save(`HealthRecords_${ts()}.pdf`)
    ElMessage.success('PDF 导出完成')
  } catch (e) {
    ElMessage.error('PDF 导出失败，如需表格排版请安装：npm i jspdf jspdf-autotable')
    throw e
  }
}

// —— 单条导出：把一条 row 映射为导出行（复用 EXPORT_COLUMNS + format）
const buildOneRow = (row) => {
  const obj = {}
  for (const c of EXPORT_COLUMNS) {
    const raw = row?.[c.key]
    const val = typeof c.format === 'function' ? c.format(raw, row) : (raw ?? '')
    obj[c.title] = val
  }
  return obj
}
// — Polished PDF (info card + metrics table + footer page numbers)
// — Polished PDF (info card + assessment table + raw table + footer page numbers)
const exportOnePDF = async (row) => {
  try {
    const jsPDF = (await import('jspdf')).default
    let autoTable
    try { autoTable = (await import('jspdf-autotable')).default } catch {}

    const doc = new jsPDF({ orientation: 'portrait', unit: 'pt', format: 'a4' })
    const margin = { left: 56, right: 56, top: 68, bottom: 56 }
    const pageWidth  = doc.internal.pageSize.getWidth()
    const pageHeight = doc.internal.pageSize.getHeight()

    // Title
    doc.setFontSize(18)
    doc.text('Personal Health Record', margin.left, margin.top)

    // Divider
    const lineY = margin.top + 14
    doc.setDrawColor(230)
    doc.line(margin.left, lineY, pageWidth - margin.right, lineY)

    // Info card
    const infoY = lineY + 18
    doc.setFontSize(12)
    const safe = (v) => (v ?? '-') + ''
    const leftX  = margin.left
    const rightX = pageWidth / 2

    // 若你已定义了 mapSex，可替换为统一显示；否则仍用原值
    const genderText = (typeof mapSex === 'function') ? mapSex(row.sex) : safe(row.sex)

    doc.text(`Name: ${safe(row.name)}`,       leftX,  infoY)
    doc.text(`Gender: ${genderText}`,         leftX,  infoY + 18)
    doc.text(`Age: ${safe(row.age)}`,         leftX,  infoY + 36)
    doc.text(`Record ID: ${safe(row.recordId)}`, rightX, infoY)
    doc.text(`Exported at: ${new Date().toLocaleString()}`, rightX, infoY + 18)

    // ========= Section 1: Health Assessment（评估表）=========
    let y = infoY + 56
    doc.setFontSize(14)
    doc.text('Health Assessment', margin.left, y)
    y += 8
    doc.setDrawColor(240)
    doc.line(margin.left, y, pageWidth - margin.right, y)
    y += 10

    const metrics = assessMetrics(row) // [{ item, valueStr, statusText, statusColor }...]
    const assessHead = ['Item', 'Value', 'Status']

    if (autoTable) {
      autoTable(doc, {
        head: [assessHead],
        body: metrics.map(m => [m.item, m.valueStr, m.statusText]),
        startY: y,
        margin,
        styles: { fontSize: 11, cellPadding: 6, overflow: 'linebreak', lineColor: 230, lineWidth: 0.5 },
        headStyles: { fillColor: [64, 158, 255], textColor: 255, fontStyle: 'bold' },
        alternateRowStyles: { fillColor: [248, 250, 252] },
        columnStyles: {
          0: { cellWidth: 180 },   // Item
          1: { cellWidth: 120 },   // Value
          2: { cellWidth: 'auto' } // Status
        },
        // 给 Status 列按评估颜色上色
        didParseCell: (data) => {
          const { section, column, row, cell } = data
          if (section === 'body' && column.index === 2) {
            const m = metrics[row.index]
            if (m?.statusColor?.length === 3) {
              cell.textColor = m.statusColor
              cell.styles.fontStyle = 'bold'
            }
          }
        },
        didDrawPage: () => {
          const page  = doc.internal.getCurrentPageInfo().pageNumber
          const total = doc.getNumberOfPages()
          doc.setFontSize(10); doc.setTextColor(130)
          doc.text(`Page ${page} of ${total}`, pageWidth - margin.right, pageHeight - 26, { align: 'right' })
          doc.setTextColor(0,0,0) // 恢复默认颜色
        },
      })
      y = doc.lastAutoTable ? doc.lastAutoTable.finalY + 24 : (y + 24)
    } else {
      // 无 autotable 的兜底渲染
      doc.setFontSize(12)
      let yy = y
      doc.text(assessHead.join(' | '), margin.left, yy); yy += 18
      doc.setFontSize(11)
      metrics.forEach((m) => {
        const left = `${m.item} | ${m.valueStr} | `
        doc.setTextColor(0,0,0)
        doc.text(left, margin.left, yy)
        const sx = margin.left + doc.getTextWidth(left)
        const [r,g,b] = m.statusColor || [0,0,0]
        doc.setTextColor(r,g,b)
        doc.text(String(m.statusText || ''), sx, yy)
        yy += 16
        if (yy > pageHeight - margin.bottom) { doc.addPage(); yy = margin.top }
      })
      doc.setTextColor(0,0,0)
      y = yy + 24
    }

    // ========= Section 2: Raw Values（原始值表）=========
    doc.setFontSize(14)
    doc.text('Raw Values', margin.left, y)
    y += 8
    doc.setDrawColor(240)
    doc.line(margin.left, y, pageWidth - margin.right, y)
    y += 10

    const r = buildOneRow(row) // 复用你已有的函数：将该条数据映射为 {Title: value}
    const rawBody = Object.entries(r).map(([k, v]) => [k, String(v ?? '')])
    const rawHead = ['Item', 'Value']

    if (autoTable) {
      autoTable(doc, {
        head: [rawHead],
        body: rawBody,
        startY: y,
        margin,
        styles: { fontSize: 11, cellPadding: 6, overflow: 'linebreak', lineColor: 230, lineWidth: 0.5 },
        headStyles: { fillColor: [99, 102, 241], textColor: 255, fontStyle: 'bold' },
        alternateRowStyles: { fillColor: [248, 250, 252] },
        columnStyles: { 0: { cellWidth: 220 }, 1: { cellWidth: 'auto' } },
        didDrawPage: () => {
          const page  = doc.internal.getCurrentPageInfo().pageNumber
          const total = doc.getNumberOfPages()
          doc.setFontSize(10); doc.setTextColor(130)
          doc.text(`Page ${page} of ${total}`, pageWidth - margin.right, pageHeight - 26, { align: 'right' })
          doc.setTextColor(0,0,0)
        },
      })
    } else {
      doc.setFontSize(11)
      let yy = y
      doc.text(rawHead.join(' | '), margin.left, yy); yy += 16
      rawBody.forEach((row2) => {
        doc.setTextColor(0,0,0)
        doc.text(row2.join(' : '), margin.left, yy)
        yy += 14
        if (yy > pageHeight - margin.bottom) { doc.addPage(); yy = margin.top }
      })
      doc.setTextColor(0,0,0)
    }

    // Save
    const base = `HealthRecord_${(row.name ?? 'one')}_${row.recordId || 'one'}_${ts()}`
    doc.save(`${base}.pdf`)
    ElMessage.success('Single-record PDF exported')
  } catch (e) {
    ElMessage.error('PDF export failed. For table layout, please install: npm i jspdf jspdf-autotable')
    throw e
  }
}


onMounted(fetchList)
</script>

<style scoped>
.main-content { padding: 20px; }
.content-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-title { font-size: 20px; color: #303133; margin: 0; }
.table-container { background: #fff; border-radius: 4px; box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1); padding: 20px; }
.toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.title { font-size: 18px; font-weight: 600; }
.ops { display: flex; gap: 10px; align-items: center; }

/* 右下角主按钮 */
.export-fab {
  position: fixed;
  right: calc(24px + env(safe-area-inset-right));
  bottom: calc(24px + env(safe-area-inset-bottom));
  z-index: 4000;
  background: #409eff;
  color: #fff;
  padding: 12px 16px;
  border-radius: 999px;
  box-shadow: 0 6px 18px rgba(64,158,255,.35);
  cursor: pointer;
  user-select: none;
  transition: transform .12s ease, box-shadow .12s ease, opacity .2s ease;
}
.export-fab:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 24px rgba(64,158,255,.45);
}
.export-fab.disabled {
  opacity: .5;
  cursor: not-allowed;
  transform: none;
  box-shadow: 0 6px 18px rgba(0,0,0,.12);
}

.action-buttons {
  display: flex;
  gap: 6px;         /* 按钮间距 */
  justify-content: center; /* 居中 */
}


/* 小屏适配 */
@media (max-width: 768px) {
  .export-fab {
    right: calc(16px + env(safe-area-inset-right));
    bottom: calc(16px + env(safe-area-inset-bottom));
  }
}
</style>
