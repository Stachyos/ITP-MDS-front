<template>
  <div class="home-table-container">
    <Header />

    <div class="main-content">
      <div class="content-header">
        <h2 class="page-title">Data Management (Data Table)</h2>
      </div>

      <div class="table-container">
        <!-- Toolbar -->
        <div class="toolbar">
          <div class="title">Health Data Records</div>
          <div class="ops">
            <el-input v-model="keyword" placeholder="Filter by name" clearable style="width: 220px" />
            <el-button @click="fetchList" :loading="loading">Refresh</el-button>

            <!-- Import button -->
            <el-button v-if="canEditAll" @click="onPickFile" :loading="importing">Import Data</el-button>

            <!-- Hidden file input -->
            <input
                ref="fileInput"
                type="file"
                accept=".csv,.xlsx,.xls,.json"
                style="display:none"
                @change="onFileChange"
            />

            <el-button @click="downloadTemplate" :loading="downloading">Download Template</el-button>
            <el-button v-if="canEditAll" type="primary" @click="openCreate" :loading="loading">Create</el-button>
          </div>
        </div>

        <!-- Table (client-side pagination; render current page only) -->
        <el-table
            v-loading="loading"
            :data="pagedList"
            border
            style="width: 100%"
            highlight-current-row
        >
          <el-table-column prop="recordId" label="ID" width="60" />
          <el-table-column prop="name" label="Name/Label" min-width="100" />
          <el-table-column prop="sex" label="Sex" width="80">
            <template #default="{ row }">
              {{ mapSex(row.sex) }}
            </template>
          </el-table-column>
          <el-table-column prop="age" label="Age" width="80" />

          <el-table-column prop="totalCholesterol" label="Total Cholesterol" width="110" />
          <el-table-column prop="triglyceride" label="Triglycerides" width="110" />
          <el-table-column prop="hdlC" label="HDL-C" width="100" />
          <el-table-column prop="ldlC" label="LDL-C" width="100" />
          <el-table-column prop="vldlC" label="VLDL-C" width="110" />

          <el-table-column prop="pulse" label="Pulse" width="90" />
          <el-table-column prop="diastolicBp" label="Diastolic BP" width="90" />
          <el-table-column label="Hypertension History" width="120">
            <template #default="{ row }">
              {{ row.hypertensionHistory ? 'Yes' : 'No' }}
            </template>
          </el-table-column>

          <el-table-column prop="bun" label="Blood Urea Nitrogen (BUN)" width="160" />
          <el-table-column prop="uricAcid" label="Uric Acid" width="100" />
          <el-table-column prop="creatinine" label="Creatinine" width="100" />

          <el-table-column label="Actions" width="240" fixed="right">
            <template #default="{ row }">
              <div class="action-buttons">
                <el-button v-if="canEditAll" size="small" type="primary" plain @click.stop="openEdit(row)">Edit</el-button>
                <el-button v-if="canEditAll" size="small" type="danger" plain @click.stop="deleteRow(row)">Delete</el-button>
                <el-button size="small" plain @click.stop="exportOnePDF(row)">PDF</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <!-- Pagination -->
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

    <!-- Create/Edit dialog -->
    <el-dialog v-model="formVisible" :title="isEdit ? 'Edit Record' : 'Create Record'" width="640px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="Name" prop="name">
          <el-input v-model="form.name" placeholder="Please enter name" />
        </el-form-item>

        <el-form-item label="Sex" prop="sex">
          <el-select v-model="form.sex" placeholder="Please select">
            <!-- Keep values as original for backend compatibility -->
            <el-option label="Male" value="男" />
            <el-option label="Female" value="女" />
            <el-option label="Other" value="其他" />
            <el-option label="Unknown" value="未知" />
          </el-select>
        </el-form-item>

        <el-form-item label="Age">
          <el-input-number v-model="form.age" :min="0" :max="150" />
        </el-form-item>

        <el-form-item label="Total Cholesterol">
          <el-input-number v-model="form.totalCholesterol" :step="0.1" :min="0" />
        </el-form-item>

        <el-form-item label="Triglycerides">
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

        <el-form-item label="Pulse">
          <el-input-number v-model="form.pulse" :min="0" />
        </el-form-item>

        <el-form-item label="Diastolic BP">
          <el-input-number v-model="form.diastolicBp" :min="0" />
        </el-form-item>

        <el-form-item label="Hypertension History">
          <el-switch v-model="form.hypertensionHistory" />
        </el-form-item>

        <el-form-item label="BUN">
          <el-input-number v-model="form.bun" :step="0.1" :min="0" />
        </el-form-item>

        <el-form-item label="Uric Acid">
          <el-input-number v-model="form.uricAcid" :min="0" />
        </el-form-item>

        <el-form-item label="Creatinine">
          <el-input-number v-model="form.creatinine" :min="0" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="formVisible = false">Cancel</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">Save</el-button>
      </template>
    </el-dialog>
  </div>

  <!-- Bottom-right: export report (floating action button) -->
  <div class="export-fab" :class="{ disabled: loading }" @click="onExportClick">
    Export Report
  </div>

  <!-- Choose export format -->
  <el-dialog v-model="chooseExportVisible" title="Choose Export Format" width="420px">
    <div style="display:flex; gap:12px; flex-wrap:wrap; justify-content:center; margin-top:8px;">
      <el-button size="large" @click="onExportCommand('csv')">CSV (general)</el-button>
      <el-button size="large" @click="onExportCommand('xlsx')">Excel (.xlsx)</el-button>
      <el-button size="large" @click="onExportCommand('pdf')">PDF (table)</el-button>
    </div>
    <template #footer>
      <el-button @click="chooseExportVisible=false">Cancel</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'        // Add watch
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
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus'

import { getUserId } from '@/api/User.js'
import { getPermissionByUserId } from '@/api/Permission.js'

const fetchPermissions = async () => {
  try {
    const r1 = await getUserId()
    const uidStr = typeof r1 === 'string' ? r1 : (r1?.data ?? r1)
    const userId = Number(uidStr)
    if (!Number.isFinite(userId)) return
    const r2 = await getPermissionByUserId(userId)
    const vo = (r2 && r2.data !== undefined) ? r2.data : r2
    permissions.value.optionEdit = !!vo?.optionEdit
  } catch {}
}

const permissions = ref({ optionEdit: false })
const canEditAll = computed(() => !!permissions.value.optionEdit)
const loading = ref(false)
const saving  = ref(false)
const downloading = ref(false)

const list    = ref([])
const keyword = ref('')

// Filter first so pagination depends on it safely
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
  sex: '未知', // keep internal code for compatibility
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
  name: [{ required: true, message: 'Please enter name', trigger: 'blur' }],
  sex:  [{ required: true, message: 'Please select sex', trigger: 'change' }]
}

const importing = ref(false)
const fileInput = ref(null)

const onPickFile = () => { fileInput.value && fileInput.value.click() }
const inferFormatByExt = (filename = '') => {
  const f = filename.toLowerCase()
  if (f.endsWith('.csv')) return 'csv'
  if (f.endsWith('.xlsx') || f.endsWith('.xls')) return 'excel'
  if (f.endsWith('.json')) return 'json'
  return ''
}

const fetchList = async (opts = {}) => {
  const { skipCache = false } = opts

  // A. Session cache first (can skip after import)
  if (!skipCache) {
    const cached = sessionStorage.getItem(CACHE_KEY)
    if (cached) {
      try { list.value = JSON.parse(cached) } catch {}
    }
  }

  loading.value = true
  try {
    // B. Timestamp to avoid GET caching in some environments
    const res = await getAllHealthRecords({ _t: Date.now() })

    // C. Compatible with both array and { code, data }
    const data = Array.isArray(res) ? res : (res?.data ?? [])
    list.value = data
    sessionStorage.setItem(CACHE_KEY, JSON.stringify(list.value))
  } catch (err) {
    ElMessage.error(err?.message || 'Failed to load')
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  form.value = {
    recordId: null,
    name: '',
    sex: '未知', // keep internal code for compatibility
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
        ElMessage.success('Updated successfully')
      } else {
        await createHealthRecord(form.value)
        ElMessage.success('Created successfully')
      }
      formVisible.value = false
      fetchList()
    } catch (err) {
      ElMessage.error(err?.response?.data?.message || err?.message || 'Operation failed')
    } finally {
      saving.value = false
    }
  })
}

const deleteRow = async (row) => {
  try {
    await ElMessageBox.confirm(
        `Delete record ID=${row.recordId} (${row.name})?`,
        'Confirm',
        {
          type: 'warning',
          confirmButtonText: 'Delete',
          cancelButtonText: 'Cancel'
        }
    )
    await deleteHealthRecord(row.recordId)
    ElMessage.success('Deleted successfully')
    fetchList()
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error(err?.response?.data?.message || err?.message || 'Delete failed')
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
        ElMessage.error(err.message || 'Download failed')
      } catch {
        ElMessage.error('Download failed: ' + text.slice(0, 120))
      }
      return
    }

    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'HealthDataTemplate.xlsx'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error(e?.message || 'Download failed')
  } finally {
    downloading.value = false
  }
}

// After selecting a file, upload & import
const onFileChange = async (e) => {
  const file = e.target.files && e.target.files[0]
  e.target.value = ''
  if (!file) return

  const format = inferFormatByExt(file.name)

  let loadingService
  let refreshTimer = null
  let lastCount = list.value.length        // current visible count
  let quietStreak = 0                      // consecutive "no growth" polls
  const INTERVAL = 10000                   // poll every 10s
  const QUIET_LIMIT = 6                    // stop after ~1 min without growth

  const startRefresh = () => {
    if (refreshTimer) return
    refreshTimer = setInterval(async () => {
      const before = list.value.length
      await fetchList({ skipCache: true })
      const after = list.value.length
      if (after > before) {
        lastCount = after
        quietStreak = 0
      } else {
        quietStreak++
        if (quietStreak >= QUIET_LIMIT) stopRefresh()
      }
    }, INTERVAL)
  }
  const stopRefresh = () => {
    if (refreshTimer) {
      clearInterval(refreshTimer)
      refreshTimer = null
    }
  }

  try {
    importing.value = true
    loadingService = ElLoading.service({
      lock: true,
      text: 'Importing, please wait…',
      background: 'rgba(0, 0, 0, 0.35)'
    })

    // Poll lightly while backend is processing so users see rows increasing
    startRefresh()

    // Disable timeout (keep same API)
    const resp = await importHealthRecords(file, format, { timeout: 0 })
    const d = resp?.data || {}

    const total = d.totalRows ?? d.total ?? 0
    const saved = d.saved ?? 0
    const dedup = d.deduplicated ?? d.dedup ?? 0
    const miss  = d.skippedMissingKey ?? 0
    const abn   = d.skippedAbnormal ?? 0

    const msg = [
      `Import finished: total ${total}`,
      `Saved ${saved}`,
      `Deduplicated ${dedup}`,
      miss ? `Skipped (missing key) ${miss}` : null,
      abn  ? `Skipped (abnormal) ${abn}`     : null
    ].filter(Boolean).join(', ')
    ElMessage.success(msg)

    // Hard refresh after success
    await fetchList({ skipCache: true })
    setTimeout(() => fetchList({ skipCache: true }), 400)
  } catch (err) {
    // For timeout/network errors: warn but keep polling to reflect progress
    const isTimeout = err?.code === 'ECONNABORTED' || /timeout|Network Error/i.test(err?.message || '')
    if (isTimeout) {
      ElMessage.warning('Connection interrupted; import may still be running. The table will auto-refresh to show progress.')
      if (loadingService) loadingService.close()
      loadingService = null
      // Let polling stop naturally after QUIET_LIMIT
    } else {
      ElMessage.error(err?.response?.data?.message || err?.message || 'Import failed, please try again later')
      stopRefresh()
    }
  } finally {
    importing.value = false
    if (loadingService) loadingService.close()
    // You can stop refresh immediately if desired:
    // stopRefresh()
  }
}

// — Export: main FAB opens format picker
const chooseExportVisible = ref(false)
const onExportClick = () => {
  if (loading.value) return
  chooseExportVisible.value = true
}

const onExportCommand = async (cmd) => {
  chooseExportVisible.value = false
  if (!filteredList.value?.length) {
    ElMessage.warning('No data to export')
    return
  }
  try {
    if (cmd === 'csv') await exportCSV()
    else if (cmd === 'xlsx') await exportXLSX()
    else if (cmd === 'pdf') await exportPDF()
  } catch (e) {
    ElMessage.error(e?.message || 'Export failed')
  }
}

// — Export helpers —
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
  ElMessage.success('CSV exported')
}

const exportXLSX = async () => {
  try {
    const xlsx = await import('xlsx')
    const rows = buildRows()
    const ws = xlsx.utils.json_to_sheet(rows)
    const wb = xlsx.utils.book_new()
    xlsx.utils.book_append_sheet(wb, ws, 'Records')
    xlsx.writeFile(wb, `HealthRecords_${ts()}.xlsx`)
    ElMessage.success('Excel exported')
  } catch (e) {
    ElMessage.error('Dependency "xlsx" not found. Please install: npm i xlsx')
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
    doc.text('Health Data Records Export', 40, 40)

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
    ElMessage.success('PDF exported')
  } catch (e) {
    ElMessage.error('PDF export failed. For table layout, please install: npm i jspdf jspdf-autotable')
    throw e
  }
}

// — Single-record export: map one row to export object (reuse EXPORT_COLUMNS + format)
const buildOneRow = (row) => {
  const obj = {}
  for (const c of EXPORT_COLUMNS) {
    const raw = row?.[c.key]
    const val = typeof c.format === 'function' ? c.format(raw, row) : (raw ?? '')
    obj[c.title] = val
  }
  return obj
}

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

    const genderText = (typeof mapSex === 'function') ? mapSex(row.sex) : safe(row.sex)

    doc.text(`Name: ${safe(row.name)}`,       leftX,  infoY)
    doc.text(`Gender: ${genderText}`,         leftX,  infoY + 18)
    doc.text(`Age: ${safe(row.age)}`,         leftX,  infoY + 36)
    doc.text(`Record ID: ${safe(row.recordId)}`, rightX, infoY)
    doc.text(`Exported at: ${new Date().toLocaleString()}`, rightX, infoY + 18)

    // ========= Section 1: Health Assessment =========
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
        // Color the Status column based on assessment color
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
          doc.setTextColor(0,0,0)
        },
      })
      y = doc.lastAutoTable ? doc.lastAutoTable.finalY + 24 : (y + 24)
    } else {
      // Fallback without autotable
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

    // ========= Section 2: Raw Values =========
    doc.setFontSize(14)
    doc.text('Raw Values', margin.left, y)
    y += 8
    doc.setDrawColor(240)
    doc.line(margin.left, y, pageWidth - margin.right, y)
    y += 10

    const r = buildOneRow(row) // Map to {Title: value}
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

    const base = `HealthRecord_${(row.name ?? 'one')}_${row.recordId || 'one'}_${ts()}`
    doc.save(`${base}.pdf`)
    ElMessage.success('Single-record PDF exported')
  } catch (e) {
    ElMessage.error('PDF export failed. For table layout, please install: npm i jspdf jspdf-autotable')
    throw e
  }
}

onMounted(async () => {
  await fetchPermissions()
  await fetchList()
})
</script>

<style scoped>
.main-content { padding: 20px; }
.content-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-title { font-size: 20px; color: #303133; margin: 0; }
.table-container { background: #fff; border-radius: 4px; box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1); padding: 20px; }
.toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.title { font-size: 18px; font-weight: 600; }
.ops { display: flex; gap: 10px; align-items: center; }

/* Main button (bottom-right) */
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
  gap: 6px;               /* gap between buttons */
  justify-content: center; /* center */
}

/* Small-screen adjustments */
@media (max-width: 768px) {
  .export-fab {
    right: calc(16px + env(safe-area-inset-right));
    bottom: calc(16px + env(safe-area-inset-bottom));
  }
}
</style>
