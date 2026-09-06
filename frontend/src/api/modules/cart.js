import request from '../request'

export function getCartList() {
  return request.get('/cart/list')
}

export function addToCart(data) {
  return request.post('/cart/add', data)
}

export function updateCartQuantity(cartId, quantity) {
  return request.put(`/cart/${cartId}`, { quantity })
}

export function removeFromCart(cartId) {
  return request.delete(`/cart/${cartId}`)
}

export function clearCart() {
  return request.delete('/cart/clear')
}
