import request from '../request'

/**
 * 获取所有分类 (用于下拉选择)
 */
export function getCategoryList() {
  return request.get('/category/list')
}

/**
 * 分页查询分类 (管理员，支持搜索)
 */
export function getCategoryPage(params) {
  return request.get('/category/admin/list', { params })
}

/**
 * 新增分类
 */
export function addCategory(data) {
  return request.post('/category', data)
}

/**
 * 修改分类
 */
export function updateCategory(id, data) {
  return request.put(`/category/${id}`, data)
}

/**
 * 删除分类
 */
export function deleteCategory(id) {
  return request.delete(`/category/${id}`)
}
