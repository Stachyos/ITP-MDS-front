<template>
  <div class="register-page">
    <el-card class="register-card" shadow="never">

      <!-- Header: Logo & Title — consistent with login page -->
      <div class="header">
        <img src="@/assets/logo.png" alt="Logo" class="logo" />
        <h1 class="main-title">Health Big Data Application</h1>
      </div>

      <div class="title">Create Account</div>

      <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          @keyup.enter="onSubmit"
      >
        <el-form-item label="Account" prop="account">
          <el-input
              v-model.trim="form.account"
              placeholder="Enter account"
              autocomplete="username"
              clearable
              size="large"
          />
        </el-form-item>

        <el-form-item label="Email" prop="email">
          <el-input
              v-model.trim="form.email"
              placeholder="you@example.com"
              autocomplete="email"
              clearable
              size="large"
          />
        </el-form-item>

        <el-form-item label="Role" prop="role">
          <el-select
              v-model="form.role"
              placeholder="Select role"
              class="full"
              size="large"
          >
            <el-option label="Administrators" value="administrators" />
            <el-option label="Researchers" value="researchers" />
            <el-option label="Analysts" value="analysts" />
            <el-option label="Auditors" value="auditors" />
          </el-select>
        </el-form-item>

        <el-form-item label="Password" prop="password">
          <el-input
              v-model="form.password"
              type="password"
              show-password
              placeholder="Enter password"
              autocomplete="new-password"
              size="large"
          />
          <template #extra>
            <div class="pwd-hint">
              <span class="hint-label">Password strength:</span>
              <el-progress
                  :percentage="strength.percent"
                  :status="strength.status"
                  :stroke-width="8"
                  class="pwd-progress"
              />
              <div class="hint" :class="strength.status">{{ strength.hint }}</div>
            </div>
          </template>
        </el-form-item>

        <el-form-item label="Confirm Password" prop="confirmPassword">
          <el-input
              v-model="form.confirmPassword"
              type="password"
              show-password
              placeholder="Re-enter password"
              autocomplete="new-password"
              size="large"
          />
        </el-form-item>

        <el-form-item>
          <div class="button-group">
            <el-button
                type="primary"
                class="submit-btn"
                :loading="loading"
                @click="onSubmit"
            >
              Create Account
            </el-button>
            <el-button class="back-btn" @click="goLogin">
              Back to Login
            </el-button>
          </div>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { apiRegister } from '@/api/User'

const formRef = ref()
const loading = ref(false)

const form = reactive({
  account: '',
  email: '',
  role: 'auditors',
  password: '',
  confirmPassword: '',
})

const strength = computed(() => {
  const v = form.password || ''
  let score = 0
  if (v.length >= 6) score++
  if (/[A-Z]/.test(v)) score++
  if (/[a-z]/.test(v)) score++
  if (/\d/.test(v)) score++
  if (/[^A-Za-z0-9]/.test(v)) score++
  const percent = Math.min(100, score * 20)
  const status = percent < 40 ? 'exception' : percent < 80 ? 'warning' : 'success'
  const hint = percent < 40 ? 'Weak' : percent < 80 ? 'Medium' : 'Strong'
  return { percent, status, hint }
})

const rules = {
  account: [
    { required: true, message: 'Account is required', trigger: 'blur' },
    { min: 3, max: 60, message: 'Account length must be 3–60', trigger: 'blur' },
  ],
  email: [
    { required: true, message: 'Email is required', trigger: 'blur' },
    { type: 'email', message: 'Invalid email format', trigger: 'blur' },
  ],
  role: [{ required: true, message: 'Role is required', trigger: 'change' }],
  password: [
    { required: true, message: 'Password is required', trigger: 'blur' },
    { min: 6, message: 'Password must be at least 6 characters', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: 'Please confirm your password', trigger: 'blur' },
    {
      validator: (_r, v, cb) => (v !== form.password ? cb(new Error('Passwords do not match')) : cb()),
      trigger: 'blur',
    },
  ],
}

async function onSubmit() {
  try {
    await formRef.value.validate()
    loading.value = true
    const resp = await apiRegister({ ...form })
    if (resp?.reply) {
      ElMessage.success(resp?.message || 'Registration successful')
      // Small delay so users can see the success toast
      await new Promise(resolve => setTimeout(resolve, 1_000))
      window.location.href = '/login'
    } else {
      throw new Error(resp?.message || 'Registration failed')
    }
  } catch (e) {
    if (e?.message) ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

function goLogin() {
  window.location.href = '/login'
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  padding-top: 60px;
  background: linear-gradient(
      to bottom,
      #d1fae5 0%,
      #b3f8d8 70%,
      #99face 80%,
      #70f8b8 100%
  );
}

.register-card {
  width: min(500px, 92vw);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 30px;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
  transition: transform 0.3s ease;
}

.register-card:hover {
  transform: translateY(-4px);
}

.title {
  font-size: 22px;
  font-weight: 700;
  color: #059669;
  text-align: center;
  margin-bottom: 25px;
  position: relative;
}

.title::after {
  content: '';
  display: block;
  width: 50px;
  height: 3px;
  background: linear-gradient(to right, #10b981, #3b82f6);
  margin: 10px auto 0;
  border-radius: 3px;
}

.full {
  width: 100%;
}

.pwd-hint {
  margin-top: 8px;
}

.hint-label {
  font-size: 14px;
  color: #374151;
  font-weight: 500;
}

.pwd-progress {
  width: 100%;
  margin: 6px 0;
}

.hint {
  font-size: 12px;
  text-align: right;
  margin-top: 2px;
}

.hint.success {
  color: #10b981;
}

.hint.warning {
  color: #f59e0b;
}

.hint.exception {
  color: #ef4444;
}

.button-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
  align-items: stretch;
}

.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(to right, #10b981, #3b82f6);
  border: none;
  border-radius: 10px;
  color: white;
  transition: all 0.3s ease;
  margin: 0;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(16, 185, 129, 0.4);
}

.back-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 10px;
  transition: all 0.3s ease;
  margin: 0;
}

.back-btn:hover {
  transform: translateY(-2px);
  border-color: #10b981;
  color: #10b981;
}

/* Element Plus overrides — consistent with login page */
:deep(.el-input__wrapper) {
  background: rgba(30, 41, 59, 0.05);
  border: 1px solid #d1d5db;
  border-radius: 10px;
  box-shadow: none;
  padding: 12px 16px;
}

:deep(.el-input__inner) {
  color: #111827;
}

:deep(.el-form-item__label) {
  color: #374151;
  font-weight: 500;
  margin-bottom: 8px;
}

:deep(.el-select .el-input__wrapper) {
  box-shadow: none;
}

:deep(.el-progress-bar__inner) {
  transition: all 0.3s ease;
}

.header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 20px;
}

.logo {
  width: 80px;
  height: 80px;
  object-fit: contain;
  margin-bottom: 12px;
}

.main-title {
  font-size: 25px;
  font-weight: 700;
  color: #111827;
  text-align: center;
}
</style>