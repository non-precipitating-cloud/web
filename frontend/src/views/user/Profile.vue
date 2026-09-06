<template>
  <div class="profile-page">
    <h2>👤 个人中心</h2>

    <!-- 头像卡片 -->
    <div class="profile-card avatar-card">
      <div class="avatar-wrap">
        <el-avatar :size="88" :src="avatarPreview" class="main-avatar" />
        <div class="avatar-camera">
          <el-upload
            :show-file-list="false"
            :before-upload="beforeAvatarUpload"
            :http-request="handleAvatarUpload"
            accept="image/*"
          >
            <el-icon :size="16"><Camera /></el-icon>
          </el-upload>
        </div>
      </div>
      <h3>{{ form.nickname || form.username }}</h3>
      <p class="avatar-tip">支持 JPG/PNG，不超过 2MB</p>
      <div class="avatar-actions">
        <el-upload
          :show-file-list="false"
          :before-upload="beforeAvatarUpload"
          :http-request="handleAvatarUpload"
          accept="image/*"
        >
          <el-button size="small" round :loading="uploading">
            {{ avatarUrl ? '更换图片' : '选择图片' }}
          </el-button>
        </el-upload>
        <el-button type="primary" size="small" round
                   :loading="savingAvatar" :disabled="!avatarUrl"
                   @click="saveAvatar">
          保存头像
        </el-button>
      </div>
    </div>

    <!-- 信息 Tab -->
    <div class="profile-card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="基本信息" name="info">
          <el-form :model="form" label-width="70px" class="profile-form">
            <el-form-item label="账号">
              <el-input v-model="form.username" disabled size="large" />
            </el-form-item>
            <el-form-item label="昵称">
              <el-input v-model="form.nickname" placeholder="给自己取个昵称" size="large" clearable />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="form.phone" placeholder="绑定手机号" size="large" clearable />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="saveProfile" round>
                保存信息
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="修改密码" name="pwd">
          <el-form :model="pwdForm" label-width="80px" class="profile-form">
            <el-form-item label="原密码">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password
                        placeholder="请输入原密码" size="large" />
            </el-form-item>
            <el-form-item label="新密码">
              <el-input v-model="pwdForm.newPassword" type="password" show-password
                        placeholder="请输入新密码" size="large" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="changing" @click="changePwd" round>
                修改密码
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { getUserInfo, updateProfile, changePassword, uploadAvatar } from '@/api/modules/user'
import { useUserStore } from '@/store'
import { ElMessage } from 'element-plus'
import { Camera } from '@element-plus/icons-vue'

const userStore = useUserStore()

const activeTab = ref('info')
const saving = ref(false)
const savingAvatar = ref(false)
const changing = ref(false)
const uploading = ref(false)
const form = reactive({ username: '', nickname: '', phone: '', avatar: '' })
const pwdForm = reactive({ oldPassword: '', newPassword: '' })
const avatarUrl = ref('')
const avatarVersion = ref(0)

const avatarPreview = computed(() => {
  if (!avatarUrl.value) return ''
  const sep = avatarUrl.value.includes('?') ? '&' : '?'
  return avatarUrl.value + sep + 'v=' + avatarVersion.value
})

onMounted(async () => {
  try {
    const res = await getUserInfo()
    Object.assign(form, res.data)
    if (res.data.avatar) {
      avatarUrl.value = res.data.avatar
      avatarVersion.value++
    }
  } catch {}
})

const beforeAvatarUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isImage) { ElMessage.error('只能上传图片文件'); return false }
  if (!isLt2M) { ElMessage.error('图片大小不能超过 2MB'); return false }
  return true
}

const handleAvatarUpload = async (options) => {
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', options.file)
    const res = await uploadAvatar(formData)
    const url = res.data
    form.avatar = url
    avatarUrl.value = url
    avatarVersion.value++
    ElMessage.success('头像上传成功')
  } catch { ElMessage.error('上传失败') }
  finally { uploading.value = false }
}

const saveAvatar = async () => {
  if (!avatarUrl.value) return
  savingAvatar.value = true
  try {
    await updateProfile({ avatar: avatarUrl.value })
    userStore.updateAvatar(avatarUrl.value)
    ElMessage.success('头像保存成功')
  } catch {} finally { savingAvatar.value = false }
}

const saveProfile = async () => {
  saving.value = true
  try {
    await updateProfile({ nickname: form.nickname, phone: form.phone, avatar: form.avatar })
    userStore.updateProfile({ nickname: form.nickname, phone: form.phone, avatar: form.avatar })
    ElMessage.success('保存成功')
  } catch {} finally { saving.value = false }
}

const changePwd = async () => {
  if (!pwdForm.oldPassword || !pwdForm.newPassword) {
    ElMessage.warning('请填写完整'); return
  }
  changing.value = true
  try {
    await changePassword(pwdForm)
    ElMessage.success('密码修改成功')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
  } catch {} finally { changing.value = false }
}
</script>

<style scoped>
.profile-page { max-width: 600px; margin: 0 auto; }
.profile-page h2 { font-size: 22px; margin: 0 0 20px; }

.profile-card {
  background: #fff;
  border-radius: var(--radius-lg);
  padding: 28px;
  box-shadow: var(--shadow-card);
  margin-bottom: 18px;
}

/* 头像卡片 */
.avatar-card { text-align: center; }
.avatar-wrap { position: relative; display: inline-block; margin-bottom: 12px; }
.main-avatar { border: 3px solid #fff; box-shadow: 0 2px 12px rgba(0,0,0,0.1); }
.avatar-camera {
  position: absolute;
  bottom: 2px;
  right: 2px;
  width: 30px; height: 30px;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
}
.avatar-card h3 { margin: 0 0 4px; font-size: 18px; }
.avatar-tip { font-size: 12px; color: var(--color-text-muted); margin: 0 0 14px; }
.avatar-actions { display: flex; gap: 10px; justify-content: center; }

/* 表单 */
.profile-form { max-width: 420px; margin: 0 auto; padding-top: 8px; }
</style>
