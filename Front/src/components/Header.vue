<template>
  <el-header class="main-header">
    <div class="header-left">
      <img src="@/assets/logo_blue.png" alt="logo" class="logo" />
      <h2 class="system-title">Health Big Data Application</h2>

      <!-- ✅ 关键：router 模式 + 以路由 path 作为 index -->
      <el-menu
          :default-active="$route.path"
          router
          mode="horizontal"
          class="nav-menu"
          :ellipsis="false"
      >
        <!-- 首页 -->
        <el-menu-item
            v-if= true
            index="/HealthRecordShow"
        >
          Home
        </el-menu-item>

        <!-- Visualization 子菜单 -->
        <el-sub-menu v-if="canVisualization" index="__vis__">
          <template #title>Visualization</template>
          <el-menu-item
              v-if="permissions.accessVisualPage"
              index="/analysis"
          >
            Analysis
          </el-menu-item>
          <el-menu-item
              v-if="permissions.accessDisplayPage"
              index="/visual"
          >
            Display
          </el-menu-item>
        </el-sub-menu>

        <!-- 权限管理 -->
        <el-menu-item
            v-if="permissions.permissionManagement"
            index="/permissionManagement"
        >
          Permission Management
        </el-menu-item>

        <!-- 审计日志（按你目前用 optionEdit 控制） -->
        <el-menu-item
            v-if="permissions.accessLogPage"
            index="/auditLogs"
        >
          Audit Logs
        </el-menu-item>
      </el-menu>
    </div>

    <div class="header-right">
      <el-dropdown trigger="click" class="user-dropdown">
        <span class="el-dropdown-link">
          <el-avatar :size="32" :src="avatarUrl" class="user-avatar" />
          <span class="username">{{ username }}</span>
          <el-icon class="dropdown-icon"><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item>
              <el-icon><User /></el-icon>
              个人中心
            </el-dropdown-item>
            <el-dropdown-item divided @click="handleLogout">
              <el-icon><SwitchButton /></el-icon>
              退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </el-header>
</template>

<script setup>
import { ref, onMounted, watch, toRaw, computed } from 'vue'
import { useRouter } from 'vue-router'
import { User, SwitchButton, ArrowDown } from '@element-plus/icons-vue'
import { getPermissionByUserId } from '@/api/Permission.js'
import { getUserId } from '@/api/User.js'

const router = useRouter()
const username = ref('管理员')
const avatarUrl = ref('https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png')

const permissions = ref({
  accessLogPage: false,
  accessVisualPage: false,
  accessDisplayPage: false,
  permissionManagement: false,
  optionEdit: false
})

const canVisualization = computed(
    () => !!(permissions.value.accessVisualPage || permissions.value.accessDisplayPage)
)

const printPerms = () => {
  console.group('[Permission] Current user permissions')
  console.table(permissions.value)
  console.log('Raw JSON:\n', JSON.stringify(permissions.value, null, 2))
  console.groupEnd()
  window.__PERMS__ = toRaw(permissions.value)
}

onMounted(async () => {
  try {
    // 兼容：getUserId 可能直接返回字符串或 { data: "1" }
    const resp1 = await getUserId()
    const uidStr = (typeof resp1 === 'string') ? resp1 : (resp1?.data ?? resp1)
    const userId = Number(uidStr)
    if (!Number.isFinite(userId)) {
      console.error('[getUserId] invalid id:', resp1)
      return
    }

    // 兼容：权限可能直接对象或 { data: vo }
    const respPerm = await getPermissionByUserId(userId)
    const vo = (respPerm && respPerm.data !== undefined) ? respPerm.data : respPerm

    permissions.value = {
      accessLogPage: !!vo?.accessLogPage,
      accessVisualPage: !!vo?.accessVisualPage,
      accessDisplayPage: !!vo?.accessDisplayPage,
      permissionManagement: !!vo?.permissionManagement,
      optionEdit: !!vo?.optionEdit
    }

    printPerms()
  } catch (e) {
    console.error('[Header] load permissions failed:', e)
  }
})

watch(permissions, () => {
  console.info('[Permission] permissions updated')
  printPerms()
}, { deep: true })

const handleLogout = () => {
  localStorage.removeItem('auth_token')
  sessionStorage.removeItem('auth_token')
  router.push('/login').catch(() => {})
}
</script>

<style scoped>
.main-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  padding: 0 20px;
  height: 60px;
  border-bottom: 1px solid #e6e6e6;
}

.header-left {
  display: flex;
  align-items: center;
}

.system-title {
  display: flex;
  align-items: center;
  margin-left: 10px;
  margin-right: 30px;
  color: #409EFF;
  font-size: 20px;
}

.logo { width: 36px; height: 36px; }

.nav-menu { border-bottom: none; }

:deep(.nav-menu .el-sub-menu__title),
:deep(.nav-menu .el-menu-item) {
  border-bottom: none !important;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.user-dropdown { cursor: pointer; }

.el-dropdown-link {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-avatar { background-color: #409EFF; }

.username { font-weight: 500; color: #606266; }

.dropdown-icon { color: #909399; }
</style>
