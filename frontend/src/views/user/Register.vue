<template>
  <div class="register-page">
    <div class="bg-shapes">
      <div class="shape shape-1"></div>
      <div class="shape shape-2"></div>
    </div>

    <div class="register-card">
      <!-- 左侧表单 -->
      <div class="card-form">
        <div class="form-header">
          <h3>🎉 创建账号</h3>
          <p>加入我们，开启美食之旅</p>
        </div>
        <el-form :model="form" :rules="rules" ref="formRef">
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入账号 (3-20位)"
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
              placeholder="请输入密码 (6-20位)"
              size="large"
              :prefix-icon="Lock"
            />
          </el-form-item>
          <el-form-item prop="confirmPwd">
            <el-input
              v-model="form.confirmPwd"
              type="password"
              show-password
              placeholder="请确认密码"
              size="large"
              :prefix-icon="Lock"
            />
          </el-form-item>
          <el-form-item prop="phone">
            <el-input
              v-model="form.phone"
              placeholder="手机号 (选填)"
              size="large"
              :prefix-icon="Phone"
              clearable
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="loading"
              @click="handleRegister"
              class="submit-btn"
            >
              注 册
            </el-button>
          </el-form-item>
        </el-form>
        <div class="form-footer">
          已有账号？<router-link to="/login">立即登录 →</router-link>
        </div>
      </div>

      <!-- 右侧插图 -->
      <div class="card-illustration">
        <div class="illustration-content">
          <span class="illust-emoji">👨‍🍳</span>
          <h2>注册有礼</h2>
          <p>新用户专享优惠</p>
          <div class="illust-features">
            <span>🎁 首单立减</span>
            <span>⭐ 积分翻倍</span>
            <span>💬 AI 专属推荐</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '@/api/modules/user'
import { ElMessage } from 'element-plus'
import { User, Lock, Phone } from '@element-plus/icons-vue'

const router = useRouter()
const loading = ref(false)
const formRef = ref(null)

const form = reactive({
  username: '', password: '', confirmPwd: '', phone: ''
})

const validateConfirm = (rule, value, callback) => {
  if (value !== form.password) callback(new Error('两次输入密码不一致'))
  else callback()
}

const rules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 20, message: '账号长度 3-20 位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度 6-20 位', trigger: 'blur' }
  ],
  confirmPwd: [{ validator: validateConfirm, trigger: 'blur' }]
}

const handleRegister = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await register({
      username: form.username,
      password: form.password,
      phone: form.phone
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch {
    // 拦截器已处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f0fdf4 0%, #dcfce7 40%, #d1fae5 100%);
  position: relative;
  overflow: hidden;
}

.bg-shapes { position: absolute; inset: 0; pointer-events: none; }
.shape {
  position: absolute;
  border-radius: 50%;
  opacity: 0.10;
}
.shape-1 {
  width: 350px; height: 350px;
  background: #27ae60;
  top: -80px; left: -80px;
  animation: float 8s ease-in-out infinite;
}
.shape-2 {
  width: 200px; height: 200px;
  background: #27ae60;
  bottom: -40px; right: -40px;
  animation: float 6s ease-in-out infinite reverse;
}
@keyframes float {
  0%, 100% { transform: translateY(0) scale(1); }
  50% { transform: translateY(-20px) scale(1.05); }
}

.register-card {
  position: relative;
  z-index: 1;
  display: flex;
  width: 820px;
  min-height: 520px;
  background: #fff;
  border-radius: var(--radius-xl);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.10);
  overflow: hidden;
}

.card-form {
  flex: 1;
  padding: 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.form-header { margin-bottom: 24px; }
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
  margin-top: 4px;
}
.form-footer a {
  color: var(--color-primary);
  font-weight: 500;
}

.card-illustration {
  width: 45%;
  background: linear-gradient(160deg, #27ae60 0%, #1abc9c 100%);
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

@media (max-width: 768px) {
  .register-card { flex-direction: column-reverse; width: 90%; min-height: auto; }
  .card-illustration { width: 100%; padding: 24px; }
  .illust-features { flex-direction: row; flex-wrap: wrap; justify-content: center; }
  .card-form { padding: 32px 24px; }
}
</style>
