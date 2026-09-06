<template>
  <div class="dish-manage">
    <div class="page-header">
      <h3>🍜 菜品管理</h3>
      <p>管理餐厅菜品信息、上下架及 AI 描述生成</p>
    </div>

    <!-- 工具栏 -->
    <div class="toolbar">
      <el-button type="primary" @click="showDialog()" round>
        <el-icon><Plus /></el-icon> 新增菜品
      </el-button>
      <el-select v-model="filterCategory" placeholder="按分类筛选" clearable
                 style="width:160px" @change="loadData" round>
        <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id"/>
      </el-select>
      <el-input v-model="keyword" placeholder="搜索菜品" style="width:220px" clearable
                @keyup.enter="loadData" :prefix-icon="Search" />
    </div>

    <!-- 表格 -->
    <div class="table-card">
      <el-table :data="dishes" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column prop="name" label="菜品名称" width="140" />
        <el-table-column prop="categoryId" label="分类ID" width="70" align="center" />
        <el-table-column prop="price" label="价格" width="80" align="center">
          <template #default="{ row }">
            <span class="price-cell">¥{{ row.price }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="70" align="center" />
        <el-table-column prop="sales" label="销量" width="70" align="center" />
        <el-table-column prop="score" label="评分" width="70" align="center" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.status === 1 ? 'success' : 'danger'"
              size="small"
              effect="dark"
              round
            >
              {{ row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain round @click="showDialog(row)">
              编辑
            </el-button>
            <el-button
              size="small"
              :type="row.status===1 ? 'warning' : 'success'"
              plain
              round
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '下架' : '上架' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :total="total"
          :page-size="pageSize"
          v-model:current-page="pageNum"
          @change="loadData"
        />
      </div>
    </div>

    <!-- 弹窗 -->
    <el-dialog
      :title="editForm.id ? '编辑菜品' : '新增菜品'"
      v-model="dialogVisible"
      width="620px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="editForm.name" placeholder="菜品名称" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="editForm.categoryId" placeholder="选择分类" style="width:100%">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="食材">
          <el-input v-model="editForm.ingredients" placeholder="主要食材" />
        </el-form-item>
        <el-form-item label="价格">
          <el-input-number v-model="editForm.price" :min="0" :precision="2" controls-position="right" style="width:100%" />
        </el-form-item>
        <el-form-item label="库存">
          <el-input-number v-model="editForm.stock" :min="0" controls-position="right" style="width:100%" />
        </el-form-item>
        <el-form-item label="图片">
          <div class="image-upload">
            <el-image
              v-if="editForm.image"
              :src="editForm.image"
              style="width:120px;height:90px;border-radius:8px;margin-bottom:8px"
              fit="cover"
            />
            <el-upload
              :show-file-list="false"
              :before-upload="beforeImageUpload"
              :http-request="handleImageUpload"
              accept="image/*"
            >
              <el-button size="small" round :loading="imageUploading">
                {{ editForm.image ? '更换图片' : '上传图片' }}
              </el-button>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item label="AI 介绍">
          <el-input
            v-model="editForm.description"
            type="textarea"
            :rows="3"
            placeholder="可手动填写，或点击下方 AI 生成"
          />
          <el-button size="small" round style="margin-top:8px" :loading="aiLoading"
                     @click="generateDesc">
            🤖 AI 生成介绍
          </el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button round @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" round :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminGetDishList, addDish, updateDish, changeDishStatus, uploadDishImage } from '@/api/modules/dish'
import { getCategoryList } from '@/api/modules/category'
import { aiGenerateDescription } from '@/api/modules/ai'
import { ElMessage } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'

const loading = ref(false)
const saving = ref(false)
const aiLoading = ref(false)
const imageUploading = ref(false)
const dialogVisible = ref(false)
const dishes = ref([])
const categories = ref([])
const keyword = ref('')
const filterCategory = ref(null)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const editForm = ref({})

onMounted(async () => {
  try { categories.value = (await getCategoryList()).data } catch {}
  loadData()
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await adminGetDishList({
      pageNum: pageNum.value, pageSize: pageSize.value,
      categoryId: filterCategory.value, keyword: keyword.value
    })
    dishes.value = res.data.records
    total.value = res.data.total
  } catch {} finally { loading.value = false }
}

const showDialog = (row) => {
  editForm.value = row ? { ...row } : {
    name: '', categoryId: null, ingredients: '', price: 0,
    stock: 0, image: '', description: '', status: 1
  }
  dialogVisible.value = true
}

const beforeImageUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isImage) { ElMessage.error('只能上传图片文件'); return false }
  if (!isLt2M) { ElMessage.error('图片不能超过 2MB'); return false }
  return true
}

const handleImageUpload = async (options) => {
  imageUploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', options.file)
    const res = await uploadDishImage(formData)
    editForm.value.image = res.data
    ElMessage.success('图片上传成功')
  } catch {} finally { imageUploading.value = false }
}

const generateDesc = async () => {
  if (!editForm.value.name) { ElMessage.warning('请先填写菜品名称'); return }
  aiLoading.value = true
  try {
    const res = await aiGenerateDescription({
      name: editForm.value.name,
      ingredients: editForm.value.ingredients || '',
      price: String(editForm.value.price || '')
    })
    editForm.value.description = res.data
  } catch {} finally { aiLoading.value = false }
}

const save = async () => {
  saving.value = true
  try {
    if (editForm.value.id) {
      await updateDish(editForm.value.id, editForm.value)
      ElMessage.success('修改成功')
    } else {
      await addDish(editForm.value)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } catch {} finally { saving.value = false }
}

const toggleStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  try {
    await changeDishStatus(row.id, newStatus)
    ElMessage.success(newStatus === 1 ? '已上架' : '已下架')
    loadData()
  } catch {}
}
</script>

<style scoped>
.page-header { margin-bottom: 18px; }
.page-header h3 { margin: 0 0 4px; font-size: 20px; }
.page-header p { margin: 0; font-size: 13px; color: var(--color-text-secondary); }

.toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.table-card {
  background: #fff;
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: var(--shadow-card);
}
.price-cell { color: var(--color-primary); font-weight: 600; }
.table-footer { display: flex; justify-content: flex-end; margin-top: 16px; }

.image-upload { display: flex; flex-direction: column; align-items: flex-start; }
</style>
