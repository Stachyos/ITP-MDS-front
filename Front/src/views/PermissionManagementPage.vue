<template>
  <div class="permission-page">
    <Header :key="headerKey" />

    <div class="main-content">
      <div class="content-header">
        <h2 class="page-title">Permission Management</h2>
        <div class="ops">
          <el-button type="primary" @click="fetchPermissions" :loading="loading">Refresh</el-button>
        </div>
      </div>

      <el-table
          v-loading="loading"
          :data="permissionList"
          border
          style="width: 100%"
          highlight-current-row
      >
        <el-table-column prop="userId" label="User ID" width="100" />
        <el-table-column prop="account" label="Account" width="220" />
        <el-table-column prop="email" label="Email" min-width="160" />

        <el-table-column prop="optionEdit" label="Table Actions" width="150">
          <template #default="{ row }">
            <el-switch v-model="row.optionEdit" />
          </template>
        </el-table-column>

        <el-table-column prop="accessLogPage" label="Access Logs" width="150">
          <template #default="{ row }">
            <el-switch v-model="row.accessLogPage" />
          </template>
        </el-table-column>

        <el-table-column prop="accessVisualPage" label="Access Analysis" width="150">
          <template #default="{ row }">
            <el-switch v-model="row.accessVisualPage" />
          </template>
        </el-table-column>

        <el-table-column prop="accessDisplayPage" label="Access Display" width="150">
          <template #default="{ row }">
            <el-switch v-model="row.accessDisplayPage" />
          </template>
        </el-table-column>

        <el-table-column prop="permissionManagement" label="Permission Management" width="180">
          <template #default="{ row }">
            <el-switch v-model="row.permissionManagement" />
          </template>
        </el-table-column>

        <el-table-column label="Actions" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
                type="primary"
                size="small"
                plain
                @click="savePermissionRow(row)"
                :loading="savingId === row.userId"
            >
              Save
            </el-button>
            <el-button
                type="danger"
                size="small"
                plain
                @click="deletePermissionRow(row)"
                :loading="deletingId === row.userId"
            >
              Delete
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

const headerKey = ref(0)

const loading = ref(false)
const savingId = ref(null)
const deletingId = ref(null)
const permissionList = ref([])

/** Fetch permission list */
const fetchPermissions = async () => {
  loading.value = true
  try {
    const res = await getAllPermissions()
    // TODO: for test
    console.log('Response:', res)

    // permissionList.value = res.data || []
    permissionList.value = res || []
  } catch (err) {
    ElMessage.error(err?.response?.data?.message || err?.message || 'Failed to load')
  } finally {
    loading.value = false
  }
}

/** Save a single user's permissions */
const savePermissionRow = async (row) => {
  savingId.value = row.userId
  try {
    await savePermission(row)
    ElMessage.success(`Permissions updated for ${row.account} (${row.email})`)
    await fetchPermissions()
    headerKey.value++
  } catch (err) {
    ElMessage.error(err?.response?.data?.message || err?.message || 'Save failed')
  } finally {
    savingId.value = null
  }
}

/** Delete a single user's permissions */
const deletePermissionRow = async (row) => {
  try {
    await ElMessageBox.confirm(
        `Delete permissions for user ${row.account} (${row.email})?`,
        'Confirm',
        { type: 'warning', confirmButtonText: 'Delete', cancelButtonText: 'Cancel' }
    )
    deletingId.value = row.userId
    await deletePermission(row.permissionId)
    ElMessage.success('Deleted successfully')
    await fetchPermissions()
    headerKey.value++
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error(err?.response?.data?.message || err?.message || 'Delete failed')
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
