// Shared bits of the school-module pages (homework, tabel, workload, exams, meetings, payments, achievements).
import { api } from '@/boot/axios'

export const examKinds = {
  CONTROL: { label: 'Nazorat ishi', icon: 'assignment', color: '#6366f1' },
  EXAM: { label: 'Imtihon', icon: 'school', color: '#ef4444' },
  TEST: { label: 'Test', icon: 'quiz', color: '#0ea5e9' },
  WRITTEN: { label: 'Yozma ish', icon: 'edit_note', color: '#f59e0b' },
  ORAL: { label: "Og'zaki so'rov", icon: 'record_voice_over', color: '#10b981' }
}

export const achievementCategories = {
  OLYMPIAD: { label: 'Olimpiada', icon: 'emoji_events', color: '#f59e0b' },
  COMPETITION: { label: 'Tanlov', icon: 'military_tech', color: '#8b5cf6' },
  SPORT: { label: 'Sport', icon: 'sports_soccer', color: '#10b981' },
  ART: { label: "San'at", icon: 'palette', color: '#ec4899' },
  CERTIFICATE: {
    label: 'Sertifikat',
    icon: 'workspace_premium',
    color: '#0ea5e9'
  },
  PROJECT: { label: 'Loyiha', icon: 'science', color: '#6366f1' },
  OTHER: { label: 'Boshqa', icon: 'star', color: '#64748b' }
}

export const achievementLevels = {
  SCHOOL: { label: 'Maktab', color: '#64748b' },
  DISTRICT: { label: 'Tuman', color: '#0ea5e9' },
  REGION: { label: 'Viloyat', color: '#6366f1' },
  NATIONAL: { label: 'Respublika', color: '#f59e0b' },
  INTERNATIONAL: { label: 'Xalqaro', color: '#ef4444' }
}

export const paymentMethods = {
  CASH: 'Naqd',
  CARD: 'Karta',
  TRANSFER: "Bank o'tkazmasi"
}

export const monthStatuses = {
  PAID: { label: "To'langan", color: '#10b981', icon: 'check_circle' },
  PARTIAL: { label: 'Qisman', color: '#f59e0b', icon: 'timelapse' },
  UNPAID: { label: "To'lanmagan", color: '#ef4444', icon: 'error' },
  UPCOMING: { label: 'Muddati kelmagan', color: '#94a3b8', icon: 'schedule' },
  FREE: { label: 'Ozod', color: '#8b5cf6', icon: 'redeem' }
}

export const toOptions = map =>
  Object.entries(map).map(([value, v]) => ({
    value,
    label: typeof v === 'string' ? v : v.label
  }))

const MONTHS = [
  'Yanvar',
  'Fevral',
  'Mart',
  'Aprel',
  'May',
  'Iyun',
  'Iyul',
  'Avgust',
  'Sentabr',
  'Oktabr',
  'Noyabr',
  'Dekabr'
]

/** '2026-09' → 'Sentabr 2026' */
export function monthLabel(ym) {
  if (!ym) return ''
  const [y, m] = String(ym).split('-').map(Number)
  return `${MONTHS[m - 1]} ${y}`
}

/** 1250000 → '1 250 000 so'm' */
export function money(v) {
  if (v == null) return '—'
  return `${Math.round(v)
    .toString()
    .replace(/\B(?=(\d{3})+(?!\d))/g, ' ')} so'm`
}

/** '15:00:00' → '15:00' */
export function hm(t) {
  return t ? String(t).slice(0, 5) : ''
}

export function fileSize(bytes) {
  if (!bytes) return ''
  if (bytes < 1024 * 1024) return `${Math.max(1, Math.round(bytes / 1024))} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

/** A file from the API (signed-in) saved in the browser. */
export async function saveFile(url, params, fileName) {
  const res = await api.get(url, { params, responseType: 'blob' })
  const href = URL.createObjectURL(res.data)
  const a = document.createElement('a')
  a.href = href
  a.download = fileName
  document.body.appendChild(a)
  a.click()
  a.remove()
  setTimeout(() => URL.revokeObjectURL(href), 1000)
}

/** The school's classes for a select, "5-A" order. */
export async function loadClasses(schoolId) {
  const res = await api.get('/api/school-classes', {
    params: { schoolId, size: 500, sort: 'gradeNumber,asc' }
  })
  return res.data.content
    .map(c => ({
      ...c,
      label: `${c.gradeNumber}-${c.sectionLetter}`,
      value: c.id
    }))
    .sort((a, b) =>
      a.label.localeCompare(b.label, undefined, { numeric: true })
    )
}

export async function loadSubjects(schoolId) {
  const res = await api.get('/api/subjects', {
    params: { schoolId, size: 500, sort: 'name,asc' }
  })
  return res.data.content
    .filter(s => s.active !== false)
    .map(s => ({ ...s, label: s.name, value: s.id }))
}

export async function loadEmployees(schoolId) {
  const res = await api.get('/api/employees', {
    params: { schoolId, size: 500, sort: 'lastName,asc' }
  })
  return res.data.content.map(e => ({
    ...e,
    label: e.fullName || `${e.lastName} ${e.firstName}`,
    value: e.id
  }))
}

export async function loadStudents(schoolId, schoolClassId) {
  const res = await api.get('/api/students', {
    params: { schoolId, schoolClassId, size: 2000, sort: 'lastName,asc' }
  })
  return res.data.content.map(s => ({
    ...s,
    label: s.fullName || `${s.lastName} ${s.firstName}`,
    value: s.id
  }))
}

export function errorText(e, fallback = 'Xatolik yuz berdi') {
  const d = e?.response?.data
  if (typeof d === 'string' && d) return d
  if (d?.message) return d.message
  if (d && typeof d === 'object') {
    const first = Object.values(d).find(v => typeof v === 'string')
    if (first) return first
  }
  return e?.friendlyMessage || fallback
}
