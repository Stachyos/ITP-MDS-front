<template>
  <div class="login-page">
    <el-card class="login-card" shadow="never">
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
            />
          </el-form-item>
          <el-form-item label="Password" prop="password">
            <el-input
                v-model="form.password"
                type="password"
                placeholder="Enter password"
                show-password
                autocomplete="current-password"
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
            />
          </el-form-item>
          <el-form-item label="Verification Code" prop="code">
            <div class="code-row">
              <el-input
                  v-model.trim="form.code"
                  maxlength="6"
                  placeholder="6-digit code"
                  class="code-input"
              />
              <el-button :disabled="sendDisabled" @click="onSendCode">
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
import axios from 'axios'

/** login mode: 'password' | 'email' */
const mode = ref('password')

const formRef = ref()
const loading = ref(false)

const form = reactive({
  // account/password login
  account: '',
  password: '',
  // email code login
  email: '',
  code: '',
  // common
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
let timer = null

const sendDisabled = computed(() => counter.value > 0)
const sendBtnText = computed(() => (counter.value > 0 ? `${counter.value}s to resend` : 'Send'))

function startCountdown(sec = 60) {
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
  // quick email check
  if (!form.email || !/.+@.+\..+/.test(form.email)) {
    ElMessage.warning('Please enter a valid email first')
    return
  }
  try {
    // your backend API to send email code
    // e.g. POST /api/auth/send-email-code  { email }
    await axios.post('/api/auth/send-email-code', { email: form.email })
    ElMessage.success('Verification code sent, please check your inbox')
    startCountdown(60)
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || 'Failed to send, try again later')
  }
}

/* ========== submit login ========== */
async function onSubmit() {
  try {
    await formRef.value.validate()

    loading.value = true
    let resp

    if (mode.value === 'password') {
      // account/password login: POST /api/user/login
      resp = await axios.post('/api/user/login', {
        account: form.account,
        password: form.password,
        remember: form.remember
      })
    } else {
      // email code login: POST /api/user/login-email
      resp = await axios.post('/api/user/login-email', {
        email: form.email,
        code: form.code,
        remember: form.remember
      })
    }

    const { data } = resp
    // expected: { reply: true, data: "token", message: "Login success" }
    if (data?.reply) {
      const token = data.data
      // remember me: localStorage; otherwise sessionStorage
      const store = form.remember ? localStorage : sessionStorage
      store.setItem('auth_token', token)

      ElMessage.success(data?.message || 'Login success')
      // navigate to home or previous route
      window.location.href = '/'
    } else {
      throw new Error(data?.message || 'Login failed')
    }
  } catch (err) {
    if (err?.name !== 'Error') {
      // form validation error already shown by Element Plus
    } else {
      ElMessage.error(err.message)
    }
  } finally {
    loading.value = false
  }
}

function onForgot() {
  ElMessageBox.alert('Please contact the administrator or go to the password recovery page.', 'Forgot Password', {
    confirmButtonText: 'OK'
  })
}

function onRegister() {
  // navigate to register page (adjust to your routes)
  window.location.href = '/register'
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  background: #0f172a;
  padding: 24px;
}
.login-card {
  width: min(720px, 92vw);
  background: #0b1220;
  border: 1px solid #1f2937;
  border-radius: 16px;
  padding: 24px 24px 8px;
  color: #e5e7eb;
}
.title {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 16px;
}
.mode-switch {
  margin-bottom: 8px;
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
}
.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
}
.register-line {
  text-align: center;
  margin: 8px 0 16px;
  color: #9ca3af;
}
:deep(.el-card__body) {
  padding: 16px 20px 8px;
}
</style>
