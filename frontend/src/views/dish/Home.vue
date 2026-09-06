<template>
  <div class="home-page">
    <!-- 搜索 & 分类 -->
    <div class="filter-bar">
      <div class="search-box">
        <el-icon class="search-icon" @click="applyFilter"><Search /></el-icon>
        <input
          v-model="keyword"
          placeholder="搜索你想吃的..."
          class="search-input"
          @keyup.enter="applyFilter"
        />
        <el-icon v-if="keyword" class="clear-icon" @click="keyword='';applyFilter()">
          <Close />
        </el-icon>
      </div>
      <div class="category-pills">
        <button
          :class="['pill', { active: categoryId === null }]"
          @click="categoryId = null; applyFilter()"
        >
          🔥 全部
        </button>
        <button
          v-for="cat in categories"
          :key="cat.id"
          :class="['pill', { active: categoryId === cat.id }]"
          @click="categoryId = cat.id; applyFilter()"
        >
          {{ cat.name }}
        </button>
      </div>
    </div>

    <!-- 骨架屏 -->
    <div v-if="loading" class="dish-grid">
      <div v-for="i in 8" :key="'s'+i" class="dish-card skeleton">
        <div class="sk-img"></div>
        <div class="sk-info">
          <div class="sk-line w-70"></div>
          <div class="sk-line w-50"></div>
          <div class="sk-line w-40"></div>
        </div>
      </div>
    </div>

    <!-- 菜品列表 -->
    <div v-else class="dish-grid">
      <el-empty v-if="dishes.length === 0" description="暂无菜品">
        <el-button type="primary" @click="keyword='';categoryId=null;loadDishes()">
          查看全部菜品
        </el-button>
      </el-empty>
      <div
        v-for="dish in dishes"
        :key="dish.id"
        class="dish-card"
        @click="$router.push(`/dish/${dish.id}`)"
      >
        <!-- 图片区域 -->
        <div class="dish-img-wrap">
          <el-image :src="dish.image || ''" fit="cover" class="dish-img">
            <template #error>
              <div class="img-fallback">🍽️</div>
            </template>
          </el-image>
          <div class="img-overlay"></div>
          <span class="price-tag">¥{{ dish.price }}</span>
          <span v-if="dish.stock === 0" class="sold-out-badge">售罄</span>
          <span v-if="dish.sales > 100" class="hot-badge">🔥 热销</span>
        </div>
        <!-- 信息区域 -->
        <div class="dish-info">
          <h4 class="dish-name">{{ dish.name }}</h4>
          <p class="dish-ingredients">{{ dish.ingredients || '美味菜品，等您品尝' }}</p>
          <div class="dish-meta">
            <span class="meta-sales">月销 {{ dish.sales }}</span>
            <span class="meta-score">⭐ {{ dish.score }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination" v-if="total > pageSize">
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="pageSize"
        v-model:current-page="pageNum"
        @change="loadDishes"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getDishList } from '@/api/modules/dish'
import { getCategoryList } from '@/api/modules/category'
import { Search, Close } from '@element-plus/icons-vue'

const loading = ref(false)
const dishes = ref([])
const categories = ref([])
const keyword = ref('')
const categoryId = ref(null)
const pageNum = ref(1)
const pageSize = ref(12)
const total = ref(0)

onMounted(async () => {
  try {
    const res = await getCategoryList()
    categories.value = res.data
  } catch {}
  loadDishes()
})

const loadDishes = async () => {
  loading.value = true
  try {
    const res = await getDishList({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      categoryId: categoryId.value,
      keyword: keyword.value || undefined
    })
    dishes.value = res.data.records
    total.value = res.data.total
  } catch {} finally {
    loading.value = false
  }
}

const applyFilter = () => {
  pageNum.value = 1
  loadDishes()
}
</script>

<style scoped>
.home-page { max-width: 1200px; margin: 0 auto; }

/* ===== 筛选栏 ===== */
.filter-bar {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 24px;
}

/* 搜索框 */
.search-box {
  position: relative;
  max-width: 420px;
}
.search-icon {
  position: absolute;
  left: 16px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--color-text-muted);
  font-size: 18px;
  z-index: 1;
}
.search-input {
  width: 100%;
  height: 48px;
  padding: 0 44px;
  border: 2px solid #f0ebe3;
  border-radius: 28px;
  font-size: 15px;
  color: var(--color-text);
  background: #fff;
  outline: none;
  transition: all var(--transition-normal);
  box-sizing: border-box;
}
.search-input::placeholder { color: var(--color-text-muted); }
.search-input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 4px var(--color-primary-light);
}
.clear-icon {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  cursor: pointer;
  color: var(--color-text-muted);
  z-index: 1;
}
.clear-icon:hover { color: var(--color-text-secondary); }

/* 分类 Pills */
.category-pills {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.pill {
  padding: 8px 20px;
  border: 1.5px solid #f0ebe3;
  border-radius: 24px;
  background: #fff;
  font-size: 13px;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
  white-space: nowrap;
  font-family: inherit;
}
.pill:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
.pill.active {
  background: var(--color-primary);
  color: #fff;
  border-color: var(--color-primary);
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(231, 76, 60, 0.25);
}

/* ===== 菜品网格 ===== */
.dish-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

/* ===== 菜品卡片 ===== */
.dish-card {
  background: #fff;
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;
  transition: all var(--transition-slow);
  box-shadow: var(--shadow-card);
}
.dish-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-hover);
}

/* 图片 */
.dish-img-wrap {
  position: relative;
  width: 100%;
  height: 190px;
  overflow: hidden;
}
.dish-img {
  width: 100%;
  height: 100%;
  transition: transform var(--transition-slow);
}
.dish-card:hover .dish-img { transform: scale(1.06); }
.img-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8f4f0;
  font-size: 52px;
}
.img-overlay {
  position: absolute;
  bottom: 0; left: 0; right: 0;
  height: 60px;
  background: linear-gradient(transparent, rgba(0,0,0,0.35));
  pointer-events: none;
}
.price-tag {
  position: absolute;
  bottom: 10px;
  left: 12px;
  color: #fff;
  font-size: 20px;
  font-weight: 800;
  text-shadow: 0 1px 3px rgba(0,0,0,0.3);
}
.sold-out-badge {
  position: absolute;
  top: 0; right: 0;
  padding: 5px 14px;
  background: rgba(0,0,0,0.7);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  border-bottom-left-radius: 8px;
  backdrop-filter: blur(4px);
}
.hot-badge {
  position: absolute;
  top: 10px;
  left: 10px;
  padding: 3px 10px;
  background: var(--color-accent);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  border-radius: 10px;
}

/* 信息 */
.dish-info { padding: 14px 16px; }
.dish-name {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 4px;
  color: var(--color-text);
}
.dish-ingredients {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin: 0 0 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.dish-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--color-text-muted);
}
.meta-score { color: var(--color-accent); font-weight: 500; }

/* ===== 骨架屏 ===== */
.skeleton { pointer-events: none; }
.skeleton .sk-img {
  width: 100%;
  height: 190px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e8e8e8 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}
.skeleton .sk-info { padding: 14px 16px; }
.skeleton .sk-line {
  height: 13px;
  border-radius: 6px;
  margin-bottom: 10px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e8e8e8 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}
.skeleton .sk-line.w-70 { width: 70%; }
.skeleton .sk-line.w-50 { width: 50%; }
.skeleton .sk-line.w-40 { width: 40%; }
@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* 分页 */
.pagination { margin-top: 28px; display: flex; justify-content: center; }

/* 响应式 */
@media (max-width: 1200px) { .dish-grid { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 768px)  { .dish-grid { grid-template-columns: repeat(2, 1fr); gap: 12px; } }
@media (max-width: 480px)  { .dish-grid { grid-template-columns: 1fr; } }
</style>
