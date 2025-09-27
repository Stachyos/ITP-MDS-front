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
            <el-button @click="refreshAll" :loading="loading">刷新</el-button>
          </div>
        </div>

        <!-- 表格（前端分页） -->
        <el-table
            v-loading="loading"
            :data="pagedList"
            border
            style="width: 100%"
            highlight-current-row
            empty-text="暂无日志"
        >
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="userAccount" label="账号" min-width="140" />
          <el-table-column prop="action" label="操作" min-width="160" />
          <el-table-column
              prop="detail"
              label="详情"
              min-width="240"
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

        <!-- 分页（前端） -->
        <div style="margin-top: 12px; text-align: right;">
          <el-pagination
              background
              layout="total, sizes, prev, pager, next, jumper"
              :current-page="page"
              :page-size="pageSize"
              :total="filteredTotal"
              :page-sizes="[10, 20, 50, 100]"
              @current-change="onPageChange"
              @size-change="onPageSizeChange"
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
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import Header from '@/components/Header.vue'
import { getAuditLogs } from '@/api/AuditLogApi.js' // 后端分页API（page从0开始）

const loading = ref(false)
const logs = ref([])              // 全量日志（合并后）
const keyword = ref('')

const page = ref(1)               // 前端分页：当前页
const pageSize = ref(20)          // 前端分页：每页大小
const detailVisible = ref(false)
const currentLog = ref(null)

const CACHE_KEY = 'audit_logs_cache_v1'
// 拉取策略
const FETCH_PAGE_SIZE = 1000      // 每次向后端请求的页大小
const MAX_ROWS = 20000            // 最大抓取条数上限

// 统一：前端筛选 + 前端分页
const filteredList = computed(() => {
  const kw = (keyword.value || '').trim().toLowerCase()
  if (!kw) return logs.value
  return logs.value.filter(r =>
      String(r.userAccount ?? '').toLowerCase().includes(kw) ||
      String(r.action ?? '').toLowerCase().includes(kw)
  )
})
const filteredTotal = computed(() => filteredList.value.length)
const pagedList = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredList.value.slice(start, start + pageSize.value)
})

// ✅ 分页事件（缺这个会导致点击分页无效）
const onPageChange = (p) => {
  if (Number.isFinite(p)) page.value = p
}
const onPageSizeChange = (s) => {
  if (Number.isFinite(s) && s > 0) {
    pageSize.value = s
    page.value = 1  // 切换每页条数后回到第一页
  }
}

// 搜索变更时回到首页
watch(keyword, () => { page.value = 1 })

// ✅ 防越界：当总数或 pageSize 改变导致最大页变小，自动把 page 调回有效范围
watch([filteredTotal, pageSize], ([total, size]) => {
  const maxPage = Math.max(1, Math.ceil((total || 0) / (size || 1)))
  if (page.value > maxPage) page.value = maxPage
})

// 刷新（重抓）
const refreshAll = async () => {
  await fetchAllLogs(true)
}

const fetchAllLogs = async (force = false) => {
  // 1) 会话缓存优先，首屏秒开
  if (!force) {
    const cached = sessionStorage.getItem(CACHE_KEY)
    if (cached) {
      try {
        const arr = JSON.parse(cached)
        if (Array.isArray(arr)) logs.value = arr
      } catch {}
    }
  }

  loading.value = true
  try {
    // 2) 分段抓取到上限或抓完
    const merged = []
    let pageIdx = 0
    let totalElements = Infinity

    while (merged.length < totalElements && merged.length < MAX_ROWS) {
      const res = await getAuditLogs(pageIdx, FETCH_PAGE_SIZE)
      const pageData = res?.data?.content ?? []
      totalElements = typeof res?.data?.totalElements === 'number'
          ? res.data.totalElements
          : totalElements

      if (!pageData.length) break
      merged.push(...pageData)
      pageIdx += 1

      if (merged.length >= totalElements) break
    }

    logs.value = merged
    sessionStorage.setItem(CACHE_KEY, JSON.stringify(merged))
    // 拉完回到第一页，避免当前页越界
    page.value = 1
  } catch (err) {
    ElMessage.error(err?.message || '加载日志失败')
  } finally {
    loading.value = false
  }
}

const showDetail = (row) => {
  currentLog.value = row
  detailVisible.value = true
}

onMounted(() => fetchAllLogs(false))
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
