import request from '../request'

export function getDishList(params) {
  return request.get('/dish/list', { params })
}

export function getDishDetail(id) {
  return request.get(`/dish/detail/${id}`)
}

// 管理员
export function adminGetDishList(params) {
  return request.get('/dish/admin/list', { params })
}

export function addDish(data) {
  return request.post('/dish', data)
}

export function updateDish(id, data) {
  return request.put(`/dish/${id}`, data)
}

export function changeDishStatus(id, status) {
  return request.put(`/dish/${id}/status`, null, { params: { status } })
}

export function uploadDishImage(formData) {
  return request.post('/upload', formData)
}
