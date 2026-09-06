import request from '../request'

export function aiRecommend(data) {
  return request.post('/ai/recommend', data)
}

export function aiGenerateDescription(data) {
  return request.post('/ai/generate-description', data)
}

export function aiAnalyzeSentiment(data) {
  return request.post('/ai/analyze-sentiment', data)
}

export function aiBusinessAnalysis(data) {
  return request.post('/ai/business-analysis', data)
}

export function aiCustomerService(data) {
  return request.post('/ai/customer-service', data)
}
