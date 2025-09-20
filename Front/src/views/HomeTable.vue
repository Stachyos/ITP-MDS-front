<template>
  <div class="home-table-container">
    <Header />

    <div class="main-content">
      <div class="content-header">
        <h2 class="page-title">数据管理（Data Table）</h2>
<!--        <el-breadcrumb separator="/">-->
<!--          <el-breadcrumb-item :to="{ path: '/homeTable' }">首页</el-breadcrumb-item>-->
<!--          <el-breadcrumb-item>健康数据管理</el-breadcrumb-item>-->
<!--        </el-breadcrumb>-->
      </div>

      <div class="table-container">
        <!-- 顶部操作栏 -->
        <div class="toolbar">
          <div class="title">健康数据记录</div>
          <div class="ops">
            <el-input v-model="keyword" placeholder="筛选关键词（按姓名）" clearable style="width: 220px" />
            <el-button type="primary" @click="openCreate" :loading="loading">新建</el-button>
            <el-button @click="fetchList" :loading="loading">刷新</el-button>
          </div>
        </div>

        <!-- 数据表 -->
        <el-table
            v-loading="loading"
            :data="filteredList"
            border
            style="width: 100%"
            highlight-current-row
        >
          <el-table-column prop="recordId" label="ID" width="120" />
          <el-table-column prop="name" label="名称/标签" min-width="180" />
          <el-table-column prop="sex" label="性别" width="90" />
          <el-table-column prop="age" label="年龄" width="90" />
          <el-table-column prop="totalCholesterol" label="总胆固醇" width="120" />
          <el-table-column prop="triglyceride" label="甘油三酯" width="120" />
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click.stop="openView(row)">查看</el-button>
              <el-button size="small" type="primary" plain @click.stop="openEdit(row)">编辑</el-button>
              <el-button size="small" type="danger" plain @click.stop="deleteRow(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- 查看抽屉 -->
    <el-drawer v-model="viewVisible" title="查看记录" size="520px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="ID">{{ viewData.recordId }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ viewData.name }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ viewData.sex }}</el-descriptions-item>
        <el-descriptions-item label="年龄">{{ viewData.age ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="总胆固醇">{{ viewData.totalCholesterol ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="甘油三酯">{{ viewData.triglyceride ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="HDL-C">{{ viewData.hdlC ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="LDL-C">{{ viewData.ldlC ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="VLDL-C">{{ viewData.vldlC ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="脉搏">{{ viewData.pulse ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="舒张压">{{ viewData.diastolicBp ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="高血压史">{{ viewData.hypertensionHistory ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="尿素氮">{{ viewData.bun ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="尿酸">{{ viewData.uricAcid ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="肌酐">{{ viewData.creatinine ?? '—' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>

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
  deleteHealthRecord
} from '@/api/HealthRecordShow.js'

const loading = ref(false)
const saving  = ref(false)
const list    = ref([])
const keyword = ref('')

const viewVisible = ref(false)
const viewData    = ref({})

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

const openView = (row) => {
  viewData.value = { ...row }
  viewVisible.value = true
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

onMounted(fetchList)
</script>

<style scoped>
.main-content {
  padding: 20px;
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 20px;
  color: #303133;
  margin: 0;
}

.table-container {
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  padding: 20px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.title {
  font-size: 18px;
  font-weight: 600;
}

.ops {
  display: flex;
  gap: 10px;
  align-items: center;
}
</style>
