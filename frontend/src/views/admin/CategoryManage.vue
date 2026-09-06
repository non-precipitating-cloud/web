<template>
  <div class="category-manage">
    <!-- 工具栏 -->
    <div class="toolbar">
      <el-button type="primary" @click="showDialog()">
        <el-icon><Plus /></el-icon> 新增分类
      </el-button>
      <el-input
        v-model="keyword"
        placeholder="搜索分类名称"
        style="width: 240px"
        clearable
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button type="default" @click="handleSearch">搜索</el-button>
    </div>

    <!-- 数据表格 -->
    <el-table
      :data="categories"
      v-loading="loading"
      style="margin-top: 16px"
      empty-text="暂无分类数据"
      stripe
      highlight-current-row
    >
      <el-table-column prop="id" label="ID" width="80" align="center" />
      <el-table-column prop="name" label="分类名称" min-width="150">
        <template #default="{ row }">
          <el-tag type="info" size="default" effect="plain">{{ row.name }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序权重" width="100" align="center">
        <template #default="{ row }">
          <span class="sort-badge">{{ row.sortOrder }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180" align="center">
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" align="center" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" plain @click="showDialog(row)">
            <el-icon><Edit /></el-icon> 编辑
          </el-button>
          <el-button size="small" type="danger" plain @click="handleDelete(row)">
            <el-icon><Delete /></el-icon> 删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        v-model:page-size="pageSize"
        v-model:current-page="pageNum"
        :page-sizes="[5, 10, 20, 50]"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog
      :title="dialogTitle"
      v-model="dialogVisible"
      width="480px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="80px"
        label-position="right"
      >
        <el-form-item label="分类名称" prop="name">
          <el-input
            v-model="form.name"
            placeholder="请输入分类名称（如：主食、饮品）"
            maxlength="50"
            show-word-limit
            clearable
          />
        </el-form-item>
        <el-form-item label="排序权重" prop="sortOrder">
          <el-input-number
            v-model="form.sortOrder"
            :min="0"
            :max="9999"
            placeholder="数字越小越靠前"
            controls-position="right"
            style="width: 100%"
          />
          <span class="form-tip">数字越小排序越靠前，默认为 0</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">
          {{ form.id ? '保存修改' : '确认新增' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import {
  getCategoryPage,
  addCategory,
  updateCategory,
  deleteCategory
} from '@/api/modules/category'
import { ElMessage, ElMessageBox } from 'element-plus'

// ---------- 列表数据 ----------
const loading = ref(false)
const categories = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const keyword = ref('')

// ---------- 弹窗数据 ----------
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref(null)
const form = reactive({
  id: null,
  name: '',
  sortOrder: 0
})

const dialogTitle = computed(() => (form.id ? '编辑分类' : '新增分类'))

const formRules = {
  name: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { max: 50, message: '分类名称不能超过50个字符', trigger: 'blur' }
  ]
}

// ---------- 生命周期 ----------
onMounted(() => {
  loadData()
})

// ---------- 方法 ----------
const loadData = async () => {
  loading.value = true
  try {
    const res = await getCategoryPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined
    })
    categories.value = res.data.records
    total.value = res.data.total
  } catch {
    // 错误已在拦截器中统一处理
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  loadData()
}

const showDialog = (row) => {
  if (row) {
    form.id = row.id
    form.name = row.name
    form.sortOrder = row.sortOrder ?? 0
  } else {
    form.id = null
    form.name = ''
    form.sortOrder = 0
  }
  dialogVisible.value = true
  // 清除上一次的表单校验
  setTimeout(() => formRef.value?.clearValidate(), 0)
}

const handleSave = async () => {
  // 表单校验
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const payload = { name: form.name.trim(), sortOrder: form.sortOrder }
    if (form.id) {
      await updateCategory(form.id, payload)
      ElMessage.success('分类修改成功')
    } else {
      await addCategory(payload)
      ElMessage.success('分类新增成功')
    }
    dialogVisible.value = false
    loadData()
  } catch {
    // 错误已在拦截器中统一处理
  } finally {
    saving.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除分类「${row.name}」吗？删除后不可恢复。`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    )
    await deleteCategory(row.id)
    ElMessage.success('分类删除成功')
    // 如果当前页只有一条数据且不是第一页，则回到上一页
    if (categories.value.length === 1 && pageNum.value > 1) {
      pageNum.value--
    }
    loadData()
  } catch (e) {
    // cancel 或 请求错误（拦截器已提示）
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}
</script>

<style scoped>
.category-manage {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  min-height: calc(100vh - 140px);
}

/* 工具栏 */
.toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

/* 排序权重徽标 */
.sort-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  height: 28px;
  padding: 0 8px;
  border-radius: 14px;
  background: #ecf5ff;
  color: #409eff;
  font-weight: 600;
  font-size: 13px;
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

/* 表单提示 */
.form-tip {
  display: block;
  font-size: 12px;
  color: #999;
  margin-top: 4px;
  line-height: 1.4;
}
</style>
