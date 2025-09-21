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
              <el-button :disabled="sendDisabled || sending" @click="onSendCode">
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
  // 沿用 el-form 的 email 校验
  await formRef.value?.validateField('email')

  // 冷却中或正在发送则不处理
  if (sendDisabled.value || sending.value) return

  // 先进入 60s 冷却，不论请求结果
  sending.value = true
  startCountdown(60)

  try {
    const r = await apiSendEmailCode({ email: form.email })
    // 形状归一化（兼容拦截器已把响应拍扁为 data）
    const resp = (r && r.data && r.data.reply !== undefined) ? r.data : r

    if (resp?.reply) {
      ElMessage.success(resp?.message || 'Verification code sent, please check your inbox')
      // 不再二次 startCountdown，保持一次点击一次冷却
    } else {
      // 软提示，不打断冷却
      ElMessage.warning(resp?.message || 'Sending… please check your inbox shortly')
    }
  } catch (e) {
    // 仅对“超时”静默（配合 MDS.js 里的 silenceTimeout）
    const isTimeout =
        e?.code === 'ECONNABORTED' ||
        /timeout of \d+ms exceeded/i.test(e?.message || '') ||
        /ETIMEDOUT/i.test(e?.code || '')
    if (!isTimeout) {
      ElMessage.error(e?.message || 'Failed to send, try again later')
    }
    // 超时：保持冷却、静默/轻提示（这里选择静默）
  } finally {
    sending.value = false // 按钮仍因倒计时禁用
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

      // 1) 先写入 Pinia / Storage（保持你原逻辑）
      const tokenStore = useTokenStore(pinia)
      tokenStore.setToken(token, form.remember)
      const store = form.remember ? localStorage : sessionStorage
      store.setItem('auth_token', token)

      // 2) 立刻把 token 写进 axios 默认头，保证后续所有请求都会自动携带
      setAuthToken(token)
      // 如果你没导出 setAuthToken，也可以用这一行达到同样效果：
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
