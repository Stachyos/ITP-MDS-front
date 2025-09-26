<template>
  <div class="permission-page">
    <Header />

    <div class="main-content">
      <div class="content-header">
        <h2 class="page-title">权限管理</h2>
        <div class="ops">
          <el-button type="primary" @click="fetchPermissions" :loading="loading">刷新</el-button>
        </div>
      </div>

      <el-table
          v-loading="loading"
          :data="permissionList"
          border
          style="width: 100%"
          highlight-current-row
      >
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="account" label="账号" width="220" />
        <el-table-column prop="email" label="邮箱" min-width="160" />


        <el-table-column prop="accessLogPage" label="查看日志" width="120">
          <template #default="{ row }">
            <el-switch v-model="row.accessPatientInfo" />
          </template>
        </el-table-column>

        <el-table-column prop="accessResearchData" label="科研数据" width="120">
          <template #default="{ row }">
            <el-switch v-model="row.accessResearchData" />
          </template>
        </el-table-column>

        <el-table-column prop="moduleReportGeneration" label="报告生成" width="120">
          <template #default="{ row }">
            <el-switch v-model="row.moduleReportGeneration" />
          </template>
        </el-table-column>

        <el-table-column prop="moduleStatsAnalysis" label="统计分析" width="120">
          <template #default="{ row }">
            <el-switch v-model="row.moduleStatsAnalysis" />
          </template>
        </el-table-column>

        <el-table-column prop="optionEdit" label="编辑表格" width="120">
          <template #default="{ row }">
            <el-switch v-model="row.moduleStatsAnalysis" />
          </template>
        </el-table-column>

        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
                type="primary"
                size="small"
                plain
                @click="savePermissionRow(row)"
                :loading="savingId === row.userId"
            >
              保存
            </el-button>
            <el-button
                type="danger"
                size="small"
                plain
                @click="deletePermissionRow(row)"
                :loading="deletingId === row.userId"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import Header from '@/components/Header.vue'
import {
  getAllPermissions,
  savePermission,
  deletePermission
} from '@/api/Permission.js'

const loading = ref(false)
const savingId = ref(null)
const deletingId = ref(null)
const permissionList = ref([])

// 获取权限列表
const fetchPermissions = async () => {
  loading.value = true
  try {
    const res = await getAllPermissions()
    //TODO: for test
    console.log('返回结果:', res)

    // permissionList.value = res.data || []
    permissionList.value = res || []

  } catch (err) {
    ElMessage.error(err?.response?.data?.message || err?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

// 保存单个用户的权限
const savePermissionRow = async (row) => {
  savingId.value = row.userId
  try {
    await savePermission(row)
    ElMessage.success(`用户 ${row.account} (${row.email}) 权限已更新`)
  } catch (err) {
    ElMessage.error(err?.response?.data?.message || err?.message || '保存失败')
  } finally {
    savingId.value = null
  }
}

// 删除单个用户权限
const deletePermissionRow = async (row) => {
  try {
    await ElMessageBox.confirm(
        `确定删除用户 ${row.account} (${row.email}) 的权限吗？`,
        '提示',
        { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
    deletingId.value = row.userId
    await deletePermission(row.permissionId)
    ElMessage.success('删除成功')
    fetchPermissions()
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error(err?.response?.data?.message || err?.message || '删除失败')
    }
  } finally {
    deletingId.value = null
  }
}

onMounted(fetchPermissions)
</script>

<style scoped>
.permission-page {
  min-height: 100vh;
  background: #f5f7fa;
}
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
  font-weight: 600;
  color: #303133;
}
.ops {
  display: flex;
  gap: 10px;
}
</style>
