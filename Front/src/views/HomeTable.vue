<template>
  <div class="home-table-container">
    <Header />

    <div class="main-content">
      <div class="content-header">
        <h2 class="page-title">数据列表</h2>
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: '/homeTable' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item>数据管理</el-breadcrumb-item>
        </el-breadcrumb>
      </div>


    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh, Edit, Delete } from '@element-plus/icons-vue'
import Header from '@/components/Header.vue'

const router = useRouter()
const tableData = ref([])
const loading = ref(true)
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 模拟数据加载
const loadData = () => {
  loading.value = true
  // 模拟API请求延迟
  setTimeout(() => {
    const mockData = []
    for (let i = 1; i <= 50; i++) {
      mockData.push({
        id: i,
        name: `用户${i}`,
        email: `user${i}@example.com`,
        phone: `1380013800${i % 10}`,
        status: i % 3 === 0 ? '禁用' : '激活',
        createTime: `2023-06-${10 + (i % 20)} 08:30:45`,
        address: `北京市海淀区中关村大街${i}号科技大厦`
      })
    }
    tableData.value = mockData
    total.value = mockData.length
    loading.value = false
  }, 800)
}

onMounted(() => {
  loadData()
})

const handleAdd = () => {
  ElMessage.success('点击了新增数据按钮')
}

const handleEdit = (index, row) => {
  ElMessage.info(`编辑第${index + 1}行数据，ID: ${row.id}`)
}

const handleDelete = (index, row) => {
  ElMessageBox.confirm('此操作将永久删除该数据, 是否继续?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    ElMessage.success('删除成功')
  }).catch(() => {
    ElMessage.info('已取消删除')
  })
}

const handleSearch = () => {
  ElMessage.info(`搜索关键词: ${searchKeyword.value}`)
}

const refreshTable = () => {
  loadData()
}

const handleSizeChange = (val) => {
  ElMessage.info(`每页 ${val} 条`)
}

const handleCurrentChange = (val) => {
  ElMessage.info(`当前页: ${val}`)
}
</script>

<style scoped>
.home-table-container {
  min-height: 100vh;
  background-color: #f5f7fa;
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
  color: #303133;
  margin: 0;
}

</style>