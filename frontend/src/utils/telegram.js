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
  ANNOUNCEMENT: { label: "E'lon", icon: 'campaign', color: '#10b981' },
  GRADE_LOW: { label: 'Past baho', icon: 'trending_down', color: '#f97316' },
  TOMORROW_SCHEDULE: {
    label: 'Ertangi jadval',
    icon: 'calendar_month',
    color: '#6366f1'
  },
  WEEKLY_REPORT: {
    label: 'Haftalik hisobot',
    icon: 'insights',
    color: '#14b8a6'
  },
  EVENT_REMINDER: {
    label: 'Tadbir eslatmasi',
    icon: 'alarm',
    color: '#a855f7'
  },
  MESSAGE_REPLY: { label: 'Murojaatga javob', icon: 'reply', color: '#0ea5e9' },
  ABSENCE_DECISION: {
    label: 'Ariza qarori',
    icon: 'task_alt',
    color: '#22c55e'
  },
  BROADCAST: { label: 'Umumiy xabar', icon: 'campaign', color: '#ec4899' }
}

export const absenceReasons = {
  ILLNESS: { label: 'Kasallik', icon: 'sick', color: '#ef4444' },
  FAMILY: { label: 'Oilaviy sabab', icon: 'family_restroom', color: '#8b5cf6' },
  OTHER: { label: 'Boshqa', icon: 'more_horiz', color: '#64748b' }
}

export const absenceStatuses = {
  PENDING: {
    label: "Ko'rib chiqilmoqda",
    color: 'warning',
    icon: 'hourglass_top'
  },
  APPROVED: { label: 'Tasdiqlangan', color: 'positive', icon: 'task_alt' },
  REJECTED: { label: 'Rad etilgan', color: 'negative', icon: 'block' }
}

export const recipients = {
  CLASS_TEACHER: { label: 'Sinf rahbariga', icon: 'school' },
  ADMINISTRATION: { label: "Ma'muriyatga", icon: 'apartment' }
}

/** Section codes recorded by the bot (BotUsageEvent.section) -> readable names. */
export const botSections = {
  home: 'Bosh menyu',
  schedule: 'Dars jadvali',
  attendance: 'Davomat',
  attendance_img: 'Davomat (rasm)',
  grades: 'Baholar',
  grades_img: 'Baholar (grafik)',
  report: 'Hisobot',
  report_img: 'Hisobot (rasm)',
  behavior: 'Xulq',
  announcements: "E'lonlar",
  events: 'Tadbirlar',
  teachers: "O'qituvchilar",
  write: "Ma'muriyatga xat",
  absence: 'Sababli ariza',
  school: 'Maktab haqida',
  settings: 'Sozlamalar',
  children: 'Farzandlarim',
  onboarding: 'Ulanish',
  welcome: 'Kutib olish',
  help: 'Yordam',
  unknown: 'Tushunarsiz xabar',
  stop: "Obunani to'xtatish",
  link_error: 'Ulanish xatosi'
}

export const weekdays = {
  MONDAY: 'Dushanba',
  TUESDAY: 'Seshanba',
  WEDNESDAY: 'Chorshanba',
  THURSDAY: 'Payshanba',
  FRIDAY: 'Juma',
  SATURDAY: 'Shanba',
  SUNDAY: 'Yakshanba'
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
