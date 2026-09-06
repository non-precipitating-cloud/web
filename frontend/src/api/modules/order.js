import request from '../request'

export function submitOrder(data) {
  return request.post('/order/submit', data)
}

export function cancelOrder(orderId) {
  return request.put(`/order/${orderId}/cancel`)
}

export function getUserOrderList(params) {
  return request.get('/order/list', { params })
}

export function getOrderDetail(orderId) {
  return request.get(`/order/detail/${orderId}`)
}

// 管理员
export function adminGetOrderList(params) {
  return request.get('/order/admin/list', { params })
}

export function payOrder(orderId, data) {
  return request.post(`/order/${orderId}/pay`, data)
}

export function deleteOrder(orderId) {
  return request.delete(`/order/${orderId}`)
}

export function updateOrderStatus(orderId, status) {
  return request.put(`/order/${orderId}/status`, null, { params: { status } })
}

// 待支付订单
export function getPendingOrders() {
  return request.get('/order/pending')
}

export function updateOrderItemQuantity(orderId, itemId, quantity) {
  return request.put(`/order/${orderId}/item/${itemId}/quantity`, { quantity })
}

// 退款
export function refundOrder(orderId) {
  return request.put(`/order/${orderId}/userRefund`)
}

export function adminRefundOrder(orderId) {
  return request.put(`/order/${orderId}/refund`)
}

export function approveRefund(orderId) {
  return request.put(`/order/${orderId}/approveRefund`)
}

export function rejectRefund(orderId) {
  return request.put(`/order/${orderId}/rejectRefund`)
}
