import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建 axios 实例
const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器 - 自动携带 token
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器 - 统一处理错误
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 401) {
      ElMessage.error('登录已过期，请重新登录')
      localStorage.clear()
      window.location.href = '/login'
      return Promise.reject(new Error(res.msg))
    }
    if (res.code !== 200) {
      ElMessage.error(res.msg || '请求失败')
      return Promise.reject(new Error(res.msg))
    }
    return res
  },
  error => {
    ElMessage.error('网络请求失败，请检查网络连接')
    return Promise.reject(error)
  }
)

export default request
