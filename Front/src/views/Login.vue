<template>
  <div class="login-page">
    <el-card class="login-card" shadow="never">
      <!-- Header: Logo & Title -->
      <div class="header">
        <img src="@/assets/logo.png" alt="Logo" class="logo" />
        <h1 class="main-title">Health Big Data Application</h1>
      </div>

      <div class="title">Sign In</div>

      <!-- Login mode switch -->
      <el-segmented
          v-model="mode"
          :options="[
          { label: 'Account & Password', value: 'password' },
          { label: 'Email & Code', value: 'email' }
        ]"
          class="mode-switch"
      />

      <!-- Form -->
      <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          @keyup.enter="onSubmit"
      >
        <!-- Account & Password -->
        <template v-if="mode === 'password'">
          <el-form-item label="Account" prop="account">
            <el-input
                v-model.trim="form.account"
                placeholder="Enter account"
                clearable
                autocomplete="username"
                size="large"
            />
          </el-form-item>
          <el-form-item label="Password" prop="password">
            <el-input
                v-model="form.password"
                type="password"
                placeholder="Enter password"
                show-password
                autocomplete="current-password"
                size="large"
            />
            <template #extra>
              <el-link type="primary" :underline="false" @click="onForgot">Forgot password?</el-link>
            </template>
          </el-form-item>
        </template>

        <!-- Email & Code -->
        <template v-else>
          <el-form-item label="Email" prop="email">
            <el-input
                v-model.trim="form.email"
                placeholder="you@example.com"
                clearable
                autocomplete="email"
                size="large"
            />
          </el-form-item>
          <el-form-item label="Verification Code" prop="code">
            <div class="code-row">
              <el-input
                  v-model.trim="form.code"
                  maxlength="6"
                  placeholder="6-digit code"
                  class="code-input"
                  size="large"
              />
              <el-button
                  :disabled="sendDisabled || sending"
                  @click="onSendCode"
                  size="large"
              >
                {{ sendBtnText }}
              </el-button>
            </div>
          </el-form-item>
        </template>

        <el-form-item>
          <div class="between">
            <el-checkbox v-model="form.remember">Remember me (this device)</el-checkbox>
            <el-space>
              <!-- Optional: TOTP -->
            </el-space>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" class="submit-btn" :loading="loading" @click="onSubmit">
            Sign In
          </el-button>
        </el-form-item>

        <div class="register-line">
          Don’t have an account?
          <el-button type="success" text @click="onRegister">Register</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { apiLogin, apiLoginByEmail, apiSendEmailCode } from '@/api/User.js'
import Request, { setAuthToken } from '@/utils/MDS.js'
import { useTokenStore } from '@/utils/stores/token'
import { pinia } from '@/utils/stores'

/** login mode: 'password' | 'email' */
const mode = ref('password')

const formRef = ref()
const loading = ref(false)

const form = reactive({
  account: '',
  password: '',
  email: '',
  code: '',
  remember: true
})

/** validation rules */
const rules = {
  account: [
    { required: () => mode.value === 'password', message: 'Please enter account', trigger: 'blur' },
    { min: 3, max: 60, message: 'Account length must be 3–60', trigger: 'blur' }
  ],
  password: [
    { required: () => mode.value === 'password', message: 'Please enter password', trigger: 'blur' },
    { min: 6, message: 'Password must be at least 6 characters', trigger: 'blur' }
  ],
  email: [
    { required: () => mode.value === 'email', message: 'Please enter email', trigger: 'blur' },
    { type: 'email', message: 'Invalid email format', trigger: 'blur' }
  ],
  code: [
    { required: () => mode.value === 'email', message: 'Please enter verification code', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: 'Code must be 6 digits', trigger: 'blur' }
  ]
}

/* ========== code sending countdown ========== */
const counter = ref(0)
const sending = ref(false)
let timer = null

const sendDisabled = computed(() => counter.value > 0)
const sendBtnText = computed(() => (counter.value > 0 ? `${counter.value}s to resend` : 'Send'))

function startCountdown(sec = 60) {
  if (timer) clearInterval(timer)
  counter.value = sec
  timer = setInterval(() => {
    counter.value -= 1
    if (counter.value <= 0) {
      clearInterval(timer)
      timer = null
    }
  }, 1000)
}
onBeforeUnmount(() => timer && clearInterval(timer))

async function onSendCode() {
  // Reuse el-form's email validation
  await formRef.value?.validateField('email')

  // Ignore clicks while cooling down or actively sending
  if (sendDisabled.value || sending.value) return

  // Immediately start 60s cooldown regardless of request result
  sending.value = true
  startCountdown(60)

  try {
    const r = await apiSendEmailCode({ email: form.email })
    // Normalize shape (compatible with interceptors that flatten to data)
    const resp = (r && r.data && r.data.reply !== undefined) ? r.data : r

    if (resp?.reply) {
      ElMessage.success(resp?.message || 'Verification code sent, please check your inbox')
      // Keep the single cooldown for this click
    } else {
      // Soft notice; do not interrupt cooldown
      ElMessage.warning(resp?.message || 'Sending… please check your inbox shortly')
    }
  } catch (e) {
    // Silent on timeout only (works with silenceTimeout in MDS.js)
    const isTimeout =
        e?.code === 'ECONNABORTED' ||
        /timeout of \d+ms exceeded/i.test(e?.message || '') ||
        /ETIMEDOUT/i.test(e?.code || '')
    if (!isTimeout) {
      ElMessage.error(e?.message || 'Failed to send, try again later')
    }
    // On timeout: keep cooldown and stay quiet
  } finally {
    sending.value = false // Button remains disabled due to countdown
  }
}

/* ========== submit login ========== */
async function onSubmit() {
  try {
    await formRef.value.validate()
    loading.value = true

    let resp
    if (mode.value === 'password') {
      resp = await apiLogin({ account: form.account, password: form.password, remember: form.remember })
    } else {
      resp = await apiLoginByEmail({ email: form.email, code: form.code, remember: form.remember })
    }

    if (resp?.reply) {
      const token = resp.data

      // 1) Persist token via Pinia/Storage (your original logic)
      const tokenStore = useTokenStore(pinia)
      tokenStore.setToken(token, form.remember)

      // 2) Set axios default Authorization header immediately
      setAuthToken(token)
      // Alternatively, if setAuthToken is not exported:
      // Request.defaults.headers.common.Authorization = `Bearer ${token}`

      console.log('✅ Login success:', {
        account: form.account || form.email,
        mode: mode.value,
        tokenPreview: String(token).slice(0, 16) + '...'
      })

      ElMessage.success(resp?.message || 'Login success')

      await new Promise(resolve => setTimeout(resolve, 1_000))
      window.location.href = '/healthRecordShow'
    } else {
      throw new Error(resp?.message || 'Login failed')
    }
  } catch (err) {
    if (err?.message) ElMessage.error(err.message)
  } finally {
    loading.value = false
  }
}

function onForgot() {
  ElMessageBox.alert(
      'Please contact the administrator or go to the password recovery page.',
      'Forgot Password',
      { confirmButtonText: 'OK' }
  )
}

function onRegister() {
  window.location.href = '/register'
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start; /* start at top for a natural layout */
  padding-top: 60px;          /* avoid the card sitting too low */
  background: linear-gradient(
      to bottom,
      #d1fae5 0%,
      #b3f8d8 70%,
      #99face 80%,
      #70f8b8 100%
  );
}

.login-card {
  width: min(420px, 92vw);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 30px;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
  transition: transform 0.3s ease;
}

.login-card:hover {
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

.mode-switch {
  margin-bottom: 20px;
}

.code-row {
  display: flex;
  gap: 12px;
  align-items: center;
}

.code-input {
  flex: 1;
}

.between {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
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
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(16, 185, 129, 0.4);
}

.register-line {
  text-align: center;
  margin: 20px 0 10px;
  color: #4b5563;
  font-size: 15px;
}

/* Element Plus overrides */
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

:deep(.el-segmented) {
  background: transparent;
}

:deep(.el-segmented__item) {
  color: #6b7280;
  background: transparent;
}

:deep(.el-segmented__item.is-selected) {
  color: white;
  background: linear-gradient(to right, #10b981, #3b82f6);
}

:deep(.el-checkbox__label) {
  color: #374151;
}

:deep(.el-link) {
  font-size: 14px;
}

:deep(.el-button--success.is-text) {
  color: #10b981;
  padding: 0 4px;
}

:deep(.el-button--success.is-text:hover) {
  color: #34d399;
  background: transparent;
}

:deep(.el-button:disabled) {
  opacity: 0.6;
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
