import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  // ==================== 用户端 ====================
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/user/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/user/Register.vue'),
    meta: { title: '注册' }
  },
  {
    path: '/',
    component: () => import('../components/layout/AppLayout.vue'),
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('../views/dish/Home.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'dish/:id',
        name: 'DishDetail',
        component: () => import('../views/dish/DishDetail.vue'),
        meta: { title: '菜品详情' }
      },
      {
        path: 'cart',
        name: 'Cart',
        component: () => import('../views/cart/Cart.vue'),
        meta: { title: '点餐车', requireAuth: true }
      },
      {
        path: 'orders',
        name: 'OrderList',
        component: () => import('../views/order/OrderList.vue'),
        meta: { title: '我的订单', requireAuth: true }
      },
      {
        path: 'review/:orderId',
        name: 'ReviewSubmit',
        component: () => import('../views/review/ReviewSubmit.vue'),
        meta: { title: '发布评价', requireAuth: true }
      },
      {
        path: 'ai',
        name: 'AiAssistant',
        component: () => import('../views/ai/AiAssistant.vue'),
        meta: { title: 'AI 点餐助手' }
      },
      {
        path: 'checkout/:orderId',
        name: 'Checkout',
        component: () => import('../views/order/Checkout.vue'),
        meta: { title: '确认支付', requireAuth: true }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('../views/user/Profile.vue'),
        meta: { title: '个人中心', requireAuth: true }
      }
    ]
  },
  // ==================== 管理员端 ====================
  {
    path: '/admin',
    component: () => import('../components/layout/AdminLayout.vue'),
    redirect: '/admin/dashboard',
    meta: { requireAdmin: true },
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('../views/admin/Dashboard.vue'),
        meta: { title: '管理后台' }
      },
      {
        path: 'dishes',
        name: 'AdminDishes',
        component: () => import('../views/admin/DishManage.vue'),
        meta: { title: '菜品管理' }
      },
      {
        path: 'categories',
        name: 'AdminCategories',
        component: () => import('../views/admin/CategoryManage.vue'),
        meta: { title: '分类管理' }
      },
      {
        path: 'orders',
        name: 'AdminOrders',
        component: () => import('../views/admin/OrderManage.vue'),
        meta: { title: '订单管理' }
      },
      {
        path: 'reviews',
        name: 'AdminReviews',
        component: () => import('../views/admin/ReviewManage.vue'),
        meta: { title: '评价管理' }
      },
      {
        path: 'analysis',
        name: 'AdminAnalysis',
        component: () => import('../views/admin/AiAnalysis.vue'),
        meta: { title: 'AI 经营分析' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：登录校验 + 管理员权限校验
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const userRole = localStorage.getItem('userRole')

  // 需要登录
  if (to.meta.requireAuth && !token) {
    return next('/login')
  }

  // 需要管理员权限
  if (to.meta.requireAdmin && (!token || userRole !== '1')) {
    return next('/login')
  }

  next()
})

export default router
