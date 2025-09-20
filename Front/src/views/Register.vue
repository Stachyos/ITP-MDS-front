<template>
  <div class="register-page">
    <el-card class="register-card" shadow="never">
      <div class="title">Register</div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="onSubmit">
        <el-form-item label="Account" prop="account">
          <el-input v-model.trim="form.account" placeholder="Enter account" autocomplete="username" />
        </el-form-item>

        <el-form-item label="Email" prop="email">
          <el-input v-model.trim="form.email" placeholder="you@example.com" autocomplete="email" />
        </el-form-item>

        <el-form-item label="Role" prop="role">
          <el-select v-model="form.role" placeholder="Select role" class="full">
            <el-option label="administrators" value="administrators" />
            <el-option label="researchers" value="researchers" />
            <el-option label="analysts" value="analysts" />
            <el-option label="auditors" value="auditors" />
          </el-select>
        </el-form-item>

        <el-form-item label="Password" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="Enter password" autocomplete="new-password" />
          <template #extra>
            <div class="pwd-hint">
              <span>Password strength:</span>
              <el-progress :percentage="strength.percent" :status="strength.status" :stroke-width="8" class="pwd-progress" />
              <div class="hint">{{ strength.hint }}</div>
            </div>
          </template>
        </el-form-item>

        <el-form-item label="Confirm Password" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" show-password placeholder="Re-enter password" autocomplete="new-password" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" class="submit-btn" :loading="loading" @click="onSubmit">Create Account</el-button>
          <el-button class="ml" @click="goLogin">Back to Login</el-button>
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
  const status = percent < 40 ? 'exception' : percent < 80 ? '' : 'success'
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
    { type: 'email', message: 'Invalid email', trigger: 'blur' },
  ],
  role: [{ required: true, message: 'Role is required', trigger: 'change' }],
  password: [
    { required: true, message: 'Password is required', trigger: 'blur' },
    { min: 6, message: 'At least 6 characters', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: 'Please confirm password', trigger: 'blur' },
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
      ElMessage.success(resp?.message || 'Registered successfully')
      window.location.href = '/login'
    } else {
      throw new Error(resp?.message || 'Register failed')
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
.register-page { min-height: 100vh; display: grid; place-items: center; background: #0f172a; padding: 24px; }
.register-card { width: min(720px, 92vw); background: #0b1220; border: 1px solid #1f2937; border-radius: 16px; padding: 24px 24px 8px; color: #e5e7eb; }
.title { font-size: 24px; font-weight: 700; margin-bottom: 16px; }
.full { width: 100%; }
.submit-btn { width: 180px; height: 40px; }
.ml { margin-left: 12px; }
.pwd-hint { margin-top: 6px; }
.pwd-progress { width: 220px; margin-top: 6px; }
:deep(.el-card__body) { padding: 16px 20px 8px; }
</style>
