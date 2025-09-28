<template>
  <div class="home-table-container">
    <Header />

    <div class="main-content">
      <div class="content-header">
        <h2 class="page-title">Audit Logs</h2>
      </div>

      <div class="table-container">
        <!-- Toolbar -->
        <div class="toolbar">
          <div class="title">System Audit Logs</div>
          <div class="ops">
            <el-input
                v-model="keyword"
                placeholder="Filter keyword (action/account)"
                clearable
                style="width: 240px"
            />
            <el-button @click="refreshAll" :loading="loading">Refresh</el-button>
          </div>
        </div>

        <!-- Table (client-side pagination) -->
        <el-table
            v-loading="loading"
            :data="pagedList"
            border
            style="width: 100%"
            highlight-current-row
            empty-text="No logs"
        >
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="userAccount" label="Account" min-width="140" />
          <el-table-column prop="action" label="Action" min-width="160" />
          <el-table-column
              prop="detail"
              label="Details"
              min-width="240"
              show-overflow-tooltip
          />
          <el-table-column prop="success" label="Result" width="100">
            <template #default="{ row }">
              <el-tag :type="row.success ? 'success' : 'danger'">
                {{ row.success ? 'Success' : 'Failed' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="time" label="Time" width="180" />
          <el-table-column label="Actions" width="120" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="showDetail(row)">Details</el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- Pagination (client-side) -->
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

    <!-- Log details dialog -->
    <el-dialog v-model="detailVisible" title="Log Details" width="640px">
      <el-descriptions :column="1" border v-if="currentLog">
        <el-descriptions-item label="ID">{{ currentLog.id }}</el-descriptions-item>
        <el-descriptions-item label="Account">
          {{ currentLog.userAccount }} (ID={{ currentLog.userId }})
        </el-descriptions-item>
        <el-descriptions-item label="Action">{{ currentLog.action }}</el-descriptions-item>
        <el-descriptions-item label="Details">{{ currentLog.detail }}</el-descriptions-item>
        <el-descriptions-item label="Result">
          <el-tag :type="currentLog.success ? 'success' : 'danger'">
            {{ currentLog.success ? 'Success' : 'Failed' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="Error Message">
          {{ currentLog.errorMsg || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="Time">{{ currentLog.time }}</el-descriptions-item>
      </el-descriptions>

      <template #footer>
        <el-button @click="detailVisible = false">Close</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import Header from '@/components/Header.vue'
import { getAuditLogs } from '@/api/AuditLogApi.js' // Backend pagination API (page starts at 0)

const loading = ref(false)
const logs = ref([])              // Full log list (merged)
const keyword = ref('')

const page = ref(1)               // Client-side pagination: current page
const pageSize = ref(20)          // Client-side pagination: page size
const detailVisible = ref(false)
const currentLog = ref(null)

const CACHE_KEY = 'audit_logs_cache_v1'
// Fetching strategy
const FETCH_PAGE_SIZE = 1000      // Page size per backend request
const MAX_ROWS = 20000            // Upper bound of total rows to fetch

// Unified: client-side filtering + client-side pagination
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

// ✅ Pagination handlers (required; otherwise clicking pages won't work)
const onPageChange = (p) => {
  if (Number.isFinite(p)) page.value = p
}
const onPageSizeChange = (s) => {
  if (Number.isFinite(s) && s > 0) {
    pageSize.value = s
    page.value = 1  // Reset to first page after page size changes
  }
}

// Reset to first page when keyword changes
watch(keyword, () => { page.value = 1 })

// ✅ Bounds safety: when total or pageSize changes and max page shrinks, clamp current page
watch([filteredTotal, pageSize], ([total, size]) => {
  const maxPage = Math.max(1, Math.ceil((total || 0) / (size || 1)))
  if (page.value > maxPage) page.value = maxPage
})

// Refresh (re-fetch)
const refreshAll = async () => {
  await fetchAllLogs(true)
}

const fetchAllLogs = async (force = false) => {
  // 1) Session cache first for fast initial render
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
    // 2) Fetch in chunks until limit reached or all data loaded
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
    // After fetching, go back to the first page to avoid out-of-range page index
    page.value = 1
  } catch (err) {
    ElMessage.error(err?.message || 'Failed to load logs')
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
