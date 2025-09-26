<template>
  <div class="home-table-container">
    <Header />

    <div class="main-content">
      <div class="content-header">
        <h2 class="page-title">操作日志（Audit Logs）</h2>
      </div>

      <div class="table-container">
        <!-- 顶部操作栏 -->
        <div class="toolbar">
          <div class="title">系统操作日志</div>
          <div class="ops">
            <el-input
                v-model="keyword"
                placeholder="筛选关键词（按操作/账号）"
                clearable
                style="width: 240px"
            />
            <el-button @click="fetchLogs" :loading="loading">刷新</el-button>
          </div>
        </div>

        <!-- 日志表格 -->
        <el-table
            v-loading="loading"
            :data="filteredList"
            border
            style="width: 100%"
            highlight-current-row
        >
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="userAccount" label="账号" width="120" />
          <el-table-column prop="action" label="操作" min-width="160" />
          <el-table-column
              prop="detail"
              label="详情"
              min-width="200"
              show-overflow-tooltip
          />
          <el-table-column prop="success" label="结果" width="100">
            <template #default="{ row }">
              <el-tag :type="row.success ? 'success' : 'danger'">
                {{ row.success ? '成功' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="time" label="时间" width="180" />
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="showDetail(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div style="margin-top: 12px; text-align: right;">
          <el-pagination
              background
              layout="prev, pager, next, jumper"
              :current-page="page"
              :page-size="size"
              :total="total"
              @current-change="onPageChange"
          />
        </div>
      </div>
    </div>

    <!-- 日志详情弹窗 -->
    <el-dialog v-model="detailVisible" title="日志详情" width="640px">
      <el-descriptions :column="1" border v-if="currentLog">
        <el-descriptions-item label="ID">{{ currentLog.id }}</el-descriptions-item>
        <el-descriptions-item label="账号">
          {{ currentLog.userAccount }} (ID={{ currentLog.userId }})
        </el-descriptions-item>
        <el-descriptions-item label="操作">{{ currentLog.action }}</el-descriptions-item>
        <el-descriptions-item label="详情">{{ currentLog.detail }}</el-descriptions-item>
        <el-descriptions-item label="结果">
          <el-tag :type="currentLog.success ? 'success' : 'danger'">
            {{ currentLog.success ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="异常信息">
          {{ currentLog.errorMsg || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="时间">{{ currentLog.time }}</el-descriptions-item>
      </el-descriptions>

      <template #footer>
        <el-button @click="detailVisible = false">关 闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import Header from '@/components/Header.vue'
import { getAuditLogs } from '@/api/AuditLogApi.js' // 👈 API

const loading = ref(false)
const logs = ref([])
const keyword = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)

const detailVisible = ref(false)
const currentLog = ref(null)

// 筛选
const filteredList = computed(() => {
  const kw = (keyword.value || '').trim().toLowerCase()
  if (!kw) return logs.value
  return logs.value.filter(
      (r) =>
          String(r.userAccount || '').toLowerCase().includes(kw) ||
          String(r.action || '').toLowerCase().includes(kw)
  )
})

const fetchLogs = async () => {
  loading.value = true
  try {
    const res = await getAuditLogs(page.value - 1, size.value) // 后端 page 从 0 开始
    logs.value = res.data?.content || []
    total.value = res.data?.totalElements || 0
  } catch (err) {
    ElMessage.error(err?.message || '加载日志失败')
  } finally {
    loading.value = false
  }
}

const onPageChange = (p) => {
  page.value = p
  fetchLogs()
}

const showDetail = (row) => {
  currentLog.value = row
  detailVisible.value = true
}

onMounted(fetchLogs)
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
