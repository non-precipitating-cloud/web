import request from '../request'

export function submitReview(data) {
  return request.post('/review/submit', data)
}

export function getDishReviews(dishId, params) {
  return request.get(`/review/list/${dishId}`, { params })
}

// 管理员
export function adminGetReviewList(params) {
  return request.get('/review/admin/list', { params })
}

export function toggleReviewStatus(reviewId, status) {
  return request.put(`/review/admin/${reviewId}/status`, null, { params: { status } })
}

/**
 * 根据订单ID查询评价
 */
export function getOrderReviews(orderId) {
  return request.get(`/review/byOrder/${orderId}`)
}
