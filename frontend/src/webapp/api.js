import axios from 'axios'
import { initData } from '@/webapp/telegram'

// Separate client for the Mini App: no admin JWT, every request carries
// Telegram's signed initData, which the backend verifies with the bot token.
export const parentApi = axios.create({
  baseURL: import.meta.env.QCLI_API_BASE_URL || 'http://localhost:8080',
  timeout: 15000
})

parentApi.interceptors.request.use(config => {
  config.headers['X-Telegram-Init-Data'] = initData()
  return config
})
