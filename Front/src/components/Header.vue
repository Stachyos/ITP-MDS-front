<template>
  <el-header class="main-header">
    <div class="header-left">
      <img src="@/assets/logo_blue.png" alt="logo" class="logo" />
      <h2 class="system-title">Health Big Data Application</h2>
      <el-menu
          :default-active="activeIndex"
          mode="horizontal"
          class="nav-menu"
          @select="handleSelect"
          :ellipsis="false"
      >
        <el-menu-item index="1">Home</el-menu-item>
        <el-sub-menu index="2">
          <template #title>Visualization</template>
          <el-menu-item index="2-1">Analysis</el-menu-item>
          <el-menu-item index="2-2">Display</el-menu-item>
        </el-sub-menu>
        <el-menu-item index="3">Permission Management</el-menu-item>
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
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Bell,
  User,
  Message,
  SwitchButton,
  ArrowDown
} from '@element-plus/icons-vue'

const router = useRouter()
const activeIndex = ref('1')
const username = ref('管理员')
const avatarUrl = ref('https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png')

// 菜单和路由的映射关系
const menuRoutes = {
  '1': '/HealthRecordShow',
  '2-1': '/analysis',
  '2-2': '/visual',
  '3': '/permissionManagement'
}

const handleSelect = (key) => {
  console.log('Selected menu:', key)
  const path = menuRoutes[key]
  if (path) {
    router.push(path)
  }
}

const handleLogout = () => {
  // 清除登录状态
  localStorage.removeItem('auth_token')
  sessionStorage.removeItem('auth_token')
  // 跳转到登录页
  router.push('/login')
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

.logo {
  width: 36px;   /* 你可以根据需求调整大小 */
  height: 36px;
}

.nav-menu {
  border-bottom: none;
}

:deep(.nav-menu .el-sub-menu__title) {
  border-bottom: none !important;
}

:deep(.nav-menu .el-menu-item) {
  border-bottom: none !important;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.notification-badge {
  cursor: pointer;
}

.user-dropdown {
  cursor: pointer;
}

.el-dropdown-link {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-avatar {
  background-color: #409EFF;
}

.username {
  font-weight: 500;
  color: #606266;
}

.dropdown-icon {
  color: #909399;
}
</style>
