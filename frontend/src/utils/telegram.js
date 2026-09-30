// Labels, colors and small helpers shared by the Telegram / notification UI.

import { formatDate } from '@/utils/date'

export const notificationTypes = {
  ATTENDANCE_ABSENT: { label: 'Kelmadi', icon: 'event_busy', color: '#ef4444' },
  ATTENDANCE_LATE: { label: 'Kechikdi', icon: 'schedule', color: '#eab308' },
  GRADE_NEW: { label: 'Yangi baho', icon: 'grade', color: '#0ea5e9' },
  GRADE_UPDATED: {
    label: "Baho o'zgardi",
    icon: 'edit_note',
    color: '#8b5cf6'
  },
  ANNOUNCEMENT: { label: "E'lon", icon: 'campaign', color: '#10b981' }
}

export const notificationStatuses = {
  PENDING: { label: 'Navbatda', color: 'warning', icon: 'hourglass_top' },
  SENT: { label: 'Yuborildi', color: 'positive', icon: 'done_all' },
  FAILED: { label: 'Xato', color: 'negative', icon: 'error' },
  SKIPPED: { label: "O'tkazildi", color: 'grey-6', icon: 'block' }
}

export const botModes = {
  LIVE: { label: 'Ulangan', color: 'positive', icon: 'check_circle' },
  MOCK: { label: 'Sinov (mock) rejimi', color: 'info', icon: 'science' },
  DISABLED: { label: "O'chirilgan", color: 'grey-7', icon: 'power_off' },
  NO_TOKEN: { label: 'Token berilmagan', color: 'warning', icon: 'key_off' }
}

/** "DD.MM.YYYY HH:mm" from a backend LocalDateTime ("2026-09-30T09:25:13.123"). */
export function formatDateTime(value) {
  if (!value) return ''
  const [date, time = ''] = value.split('T')
  return `${formatDate(date)} ${time.slice(0, 5)}`.trim()
}

/**
 * Message texts are Telegram HTML built by the backend, where every value is
 * already escaped and only <b>/<i> tags are emitted. Anything else that looks
 * like a tag is neutralized before the text reaches v-html.
 */
export function telegramHtmlToSafe(text) {
  return (text || '').replace(/<(?!\/?(b|i)>)/g, '&lt;')
}
