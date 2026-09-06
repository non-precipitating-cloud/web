<template>
  <div class="login-page">
    <!-- 装饰背景 -->
    <div class="bg-shapes">
      <div class="shape shape-1"></div>
      <div class="shape shape-2"></div>
      <div class="shape shape-3"></div>
    </div>

    <div class="login-card">
      <!-- 左侧插图 -->
      <div class="card-illustration">
        <div class="illustration-content">
          <span class="illust-emoji">🍽️</span>
          <h2>美味餐厅</h2>
          <p>品尝幸福的味道</p>
          <div class="illust-features">
            <span>🍜 丰富菜品</span>
            <span>⚡ 快速下单</span>
            <span>🤖 AI 推荐</span>
          </div>
        </div>
      </div>

      <!-- 右侧表单 -->
      <div class="card-form">
        <div class="form-header">
          <h3>欢迎回来</h3>
          <p>登录您的账号继续点餐</p>
        </div>
        <el-form :model="form" :rules="rules" ref="formRef" @keyup.enter="handleLogin">
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入账号"
              size="large"
              :prefix-icon="User"
              clearable
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              show-password
              placeholder="请输入密码"
              size="large"
              :prefix-icon="Lock"
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="loading"
              @click="handleLogin"
              class="submit-btn"
            >
              登 录
            </el-button>
          </el-form-item>
        </el-form>
        <div class="form-footer">
          还没有账号？<router-link to="/register">立即注册 →</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { login, getUserInfo } from '@/api/modules/user'
import { useUserStore } from '@/store'
import { User, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const formRef = ref(null)

const form = reactive({ username: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await login(form)
    const token = res.data.token
    localStorage.setItem('token', token)
    const userRes = await getUserInfo()
    userStore.setLogin(token, userRes.data)
    router.push(userRes.data.role === 1 ? '/admin' : '/home')
  } catch {
    // 拦截器已处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #fff5f0 0%, #ffe8e0 40%, #fde0d0 100%);
  position: relative;
  overflow: hidden;
}

/* 装饰形状 */
.bg-shapes { position: absolute; inset: 0; pointer-events: none; }
.shape {
  position: absolute;
  border-radius: 50%;
  opacity: 0.12;
}
.shape-1 {
  width: 400px; height: 400px;
  background: var(--color-primary);
  top: -120px; right: -100px;
  animation: float 8s ease-in-out infinite;
}
.shape-2 {
  width: 250px; height: 250px;
  background: var(--color-accent);
  bottom: -60px; left: -60px;
  animation: float 6s ease-in-out infinite reverse;
}
.shape-3 {
  width: 180px; height: 180px;
  background: var(--color-primary);
  top: 50%; left: 10%;
  animation: float 7s ease-in-out infinite 2s;
}
@keyframes float {
  0%, 100% { transform: translateY(0) scale(1); }
  50% { transform: translateY(-20px) scale(1.05); }
}

/* 卡片 */
.login-card {
  position: relative;
  z-index: 1;
  display: flex;
  width: 820px;
  min-height: 460px;
  background: #fff;
  border-radius: var(--radius-xl);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.10);
  overflow: hidden;
}

/* 左侧插图 */
.card-illustration {
  width: 45%;
  background: linear-gradient(160deg, #e74c3c 0%, #f0a500 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: #fff;
  text-align: center;
}
.illust-emoji { font-size: 56px; display: block; margin-bottom: 12px; }
.illustration-content h2 {
  font-size: 24px;
  margin: 0 0 6px;
  letter-spacing: 2px;
}
.illustration-content p {
  font-size: 14px;
  opacity: 0.85;
  margin: 0 0 24px;
}
.illust-features {
  display: flex;
  flex-direction: column;
  gap: 10px;
  font-size: 13px;
  opacity: 0.9;
}
.illust-features span {
  padding: 6px 16px;
  background: rgba(255,255,255,0.15);
  border-radius: 20px;
  backdrop-filter: blur(4px);
}

/* 右侧表单 */
.card-form {
  flex: 1;
  padding: 48px 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.form-header { margin-bottom: 28px; }
.form-header h3 {
  font-size: 22px;
  margin: 0 0 6px;
  color: var(--color-text);
}
.form-header p {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin: 0;
}
.submit-btn {
  width: 100%;
  letter-spacing: 4px;
  font-size: 16px;
  height: 44px;
}
.form-footer {
  text-align: center;
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-top: 8px;
}
.form-footer a {
  color: var(--color-primary);
  font-weight: 500;
  transition: opacity var(--transition-fast);
}
.form-footer a:hover { opacity: 0.8; }

@media (max-width: 768px) {
  .login-card { flex-direction: column; width: 90%; min-height: auto; }
  .card-illustration { width: 100%; padding: 28px; }
  .illust-features { flex-direction: row; flex-wrap: wrap; justify-content: center; }
  .card-form { padding: 32px 24px; }
}
</style>
