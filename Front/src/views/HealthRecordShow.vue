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

        <!-- 数据表（直接展示主要字段） -->
        <el-table
            v-loading="loading"
            :data="filteredList"
            border
            style="width: 100%"
            highlight-current-row
        >
          <el-table-column prop="recordId" label="ID" width="120" />
          <el-table-column prop="name" label="名称/标签" min-width="160" />
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

          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="primary" plain @click.stop="openEdit(row)">编辑</el-button>
              <el-button size="small" type="danger"  plain @click.stop="deleteRow(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
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
import { ref, computed, onMounted } from 'vue'
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

const loading = ref(false)
const importProgress = ref(0)        // 进度百分比
const showProgress   = ref(false)    // 控制进度条显示
const saving  = ref(false)
const downloading = ref(false)

const list    = ref([])
const keyword = ref('')

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

const filteredList = computed(() => {
  const kw = (keyword.value || '').trim().toLowerCase()
  if (!kw) return list.value
  return list.value.filter(r => String(r.name || '').toLowerCase().includes(kw))
})

const fetchList = async () => {
  loading.value = true
  try {
    const res = await getAllHealthRecords()
    list.value = res.data || []
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
    const blob = await downloadHealthRecordTemplate() // 这里就是 Blob

    // 如果后端返的是 JSON 错误，这里 type 往往是 application/json
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
          importProgress.value = Math.round(
              (progressEvent.loaded * 100) / progressEvent.total
          )
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
    setTimeout(() => { showProgress.value = false }, 800) // 收起进度条
  }
}
// === 导出：点击主按钮先询问 ===
const chooseExportVisible = ref(false)

const onExportClick = () => {
  if (loading.value) return
  // 打开“选择导出格式”的弹窗
  chooseExportVisible.value = true
}

// 也复用下拉命令的处理（弹窗和下拉共用同一入口）
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
  { key: 'sex',              title: 'Gender', format: mapSex },              // ← 加上 format
  { key: 'age',              title: 'Age' },
  { key: 'totalCholesterol', title: 'Total Cholesterol' },
  { key: 'triglyceride',     title: 'Triglycerides' },
  { key: 'hdlC',             title: 'HDL-C' },
  { key: 'ldlC',             title: 'LDL-C' },
  { key: 'vldlC',            title: 'VLDL-C' },
  { key: 'pulse',            title: 'Pulse' },
  { key: 'diastolicBp',      title: 'Diastolic BP' },
  { key: 'hypertensionHistory', title: 'Hypertension History', format: mapYesNo }, // ← 加上 format
  { key: 'bun',              title: 'Blood Urea Nitrogen (BUN)' },
  { key: 'uricAcid',         title: 'Uric Acid' },
  { key: 'creatinine',       title: 'Creatinine' },
]

// 2) 映射函数（兼容中英文/布尔/数字）
function mapSex(v) {
  const s = String(v ?? '').trim().toLowerCase()
  if (['男','m','male','man','1'].includes(s)) return 'Male'
  if (['女','f','female','woman','0'].includes(s)) return 'Female'
  if (['其他','other'].includes(s)) return 'Other'
  if (['未知','unknown','unk',''].includes(s)) return 'Unknown'
  // 兜底：原样返回（避免误伤）
  return v ?? ''
}

function mapYesNo(v) {
  if (typeof v === 'boolean') return v ? 'Yes' : 'No'
  const s = String(v ?? '').trim().toLowerCase()
  if (['是','yes','y','true','1'].includes(s)) return 'Yes'
  if (['否','no','n','false','0'].includes(s)) return 'No'
  // 兜底：原样返回
  return v ?? ''
}

// 3) 导出用的数据行构建（调用 format）
const buildRows = () => {
  return filteredList.value.map(row => {
    const obj = {}
    for (const c of EXPORT_COLUMNS) {
      const raw = row?.[c.key]
      // 先处理你之前的高血压史布尔 -> 中英文，此处改为统一走 format
      const val = typeof c.format === 'function' ? c.format(raw, row) : (raw ?? '')
      obj[c.title] = val
    }
    return obj
  })
}

// CSV 导出（原生，无需第三方库）
const exportCSV = async () => {
  const rows = buildRows()
  const headers = Object.keys(rows[0] || EXPORT_COLUMNS.reduce((acc, c) => (acc[c.title]='', acc), {}))
  const esc = (s) => {
    const str = String(s ?? '')
    // 若包含逗号/引号/换行，按 CSV 规范转义
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

// Excel 导出（需要 xlsx 依赖）
const exportXLSX = async () => {
  try {
    const xlsx = await import('xlsx')  // 动态导入
    const rows = buildRows()
    const ws = xlsx.utils.json_to_sheet(rows)
    const wb = xlsx.utils.book_new()
    xlsx.utils.book_append_sheet(wb, ws, 'Records')
    xlsx.writeFile(wb, `HealthRecords_${ts()}.xlsx`)
    ElMessage.success('Excel 导出完成')
  } catch (e) {
    // 未安装时给出指引
    ElMessage.error('未检测到 xlsx 依赖，请先安装：npm i xlsx')
    throw e
  }
}

// PDF 导出（推荐 jspdf + jspdf-autotable）
const exportPDF = async () => {
  try {
    const jsPDF = (await import('jspdf')).default
    let autoTable
    try {
      autoTable = (await import('jspdf-autotable')).default
    } catch {
      // 允许缺少 autotable，但体验会差
    }

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
      // 简单兜底：逐行打印（列多时可能溢出）
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
  z-index: 4000;               /* 高于大多数 Element Plus 弹层 */
  background: #409eff;         /* Element Plus 主色 */
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

/* 三点菜单的悬浮定位（可选） */
.export-fab-menu {
  position: fixed;
  right: calc(24px + env(safe-area-inset-right));
  bottom: calc(84px + env(safe-area-inset-bottom)); /* 与主按钮竖直错开 */
  z-index: 4000;
}
.export-fab-menu__trigger {
  width: 42px;
  height: 42px;
  line-height: 42px;
  display: inline-block;
  text-align: center;
  border-radius: 50%;
  background: #fff;
  border: 1px solid #ebeef5;
  box-shadow: 0 6px 18px rgba(0,0,0,.08);
  cursor: pointer;
  font-weight: 700;
  font-size: 18px;
}
.export-fab-menu__trigger:hover {
  box-shadow: 0 10px 24px rgba(0,0,0,.12);
}

/* 小屏适配：离边距更近一点 */
@media (max-width: 768px) {
  .export-fab {
    right: calc(16px + env(safe-area-inset-right));
    bottom: calc(16px + env(safe-area-inset-bottom));
  }
  .export-fab-menu {
    right: calc(16px + env(safe-area-inset-right));
    bottom: calc(72px + env(safe-area-inset-bottom));
  }
}

</style>
