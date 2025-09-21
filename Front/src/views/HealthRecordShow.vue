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
  e.target.value = '' // 允许同一文件反复选择
  if (!file) return

  const format = inferFormatByExt(file.name)
  console.groupCollapsed('[导入] 请求参数')
  console.log('file:', file)
  console.log('infer format:', format)
  console.groupEnd()

  try {
    importing.value = true

    const resp = await importHealthRecords(file, format)
    const body = resp.data || resp || {} // 你的 request 可能做了解包

    // 调试：完整打印
    console.groupCollapsed('[导入] 原始响应')
    console.log('resp:', resp)
    console.log('body:', body)
    console.groupEnd()

    // 调试面板赋值
    rawImportResp.value = JSON.stringify(body, null, 2)
    importSummary.value = body?.data || null
    showImportDetail.value = true

    // 业务提示
    if (body.reply) {
      const s = body.data || {}
      const msg = `导入完成：共${s.totalRows ?? 0}，入库${s.saved ?? 0}，去重${s.deduplicated ?? 0}，缺关键${s.skippedMissingKey ?? 0}，异常${s.skippedAbnormal ?? 0}`
      ElMessage.success(msg)
      fetchList()
    } else {
      ElMessage.error(body.message || '导入失败')
    }
  } catch (err) {
    // 把错误也放到调试弹窗
    const errObj = {
      message: err?.message,
      response: err?.response?.data || null,
      stack: err?.stack?.split('\n').slice(0,3).join('\n')
    }
    rawImportResp.value = JSON.stringify(errObj, null, 2)
    importSummary.value = null
    showImportDetail.value = true

    ElMessage.error(err?.response?.data?.message || err?.message || '导入失败')
  } finally {
    importing.value = false
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
</style>
