<template>
  <div class="permission-page">
    <Header />

    <div class="main-content">
      <div class="content-header">
        <h2 class="page-title">权限管理</h2>
      </div>

      <el-table
          :data="permissionList"
          border
          style="width: 100%"
          v-loading="loading"
      >
        <el-table-column prop="user_id" label="用户ID" />
        <el-table-column prop="account" label="账号" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="access_patient_info" label="病人信息">
          <template #default="{ row }">
            <el-switch v-model="row.access_patient_info" />
          </template>
        </el-table-column>
        <el-table-column prop="access_research_data" label="科研数据">
          <template #default="{ row }">
            <el-switch v-model="row.access_research_data" />
          </template>
        </el-table-column>
        <el-table-column prop="module_report_generation" label="报告生成">
          <template #default="{ row }">
            <el-switch v-model="row.module_report_generation" />
          </template>
        </el-table-column>
        <el-table-column prop="module_stats_analysis" label="统计分析">
          <template #default="{ row }">
            <el-switch v-model="row.module_stats_analysis" />
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="savePermission(row)">
              保存
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import Header from '@/components/Header.vue'

// 模拟静态数据（关联了 users 和 permissions）
const permissionList = ref([
  {
    permission_id: 1,
    user_id: 101,
    account: 'zhangsan',
    email: 'zhangsan@example.com',
    access_patient_info: true,
    access_research_data: false,
    module_report_generation: true,
    module_stats_analysis: false
  },
  {
    permission_id: 2,
    user_id: 102,
    account: 'lisi',
    email: 'lisi@example.com',
    access_patient_info: false,
    access_research_data: true,
    module_report_generation: false,
    module_stats_analysis: true
  },
  {
    permission_id: 3,
    user_id: 103,
    account: 'wangwu',
    email: 'wangwu@example.com',
    access_patient_info: true,
    access_research_data: true,
    module_report_generation: true,
    module_stats_analysis: true
  }
])

const loading = ref(false)

const fetchPermissions = async () => {
  loading.value = true
  setTimeout(() => {
    loading.value = false
  }, 500)
}

const savePermission = async (row) => {
  ElMessage.success(`用户 ${row.account} (${row.email}) 权限已更新 (模拟保存)`)
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
  margin-bottom: 20px;
}
.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}
</style>
