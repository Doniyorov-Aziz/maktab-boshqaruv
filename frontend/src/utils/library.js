// Labels and small helpers of the school library ("Kutubxona") pages.

/** Why the school may hand a book out — required on every book. */
export const legalBases = [
  {
    value: 'JAMOAT_MULKI',
    label: 'Jamoat mulki',
    hint: 'Mualliflik huquqi muddati tugagan'
  },
  { value: 'DAVLAT_DARSLIGI', label: 'Davlat darsligi', hint: '' },
  {
    value: 'MUALLIF_RUXSATI',
    label: 'Muallif ruxsati',
    hint: 'Muallif yoki nashriyotning yozma ruxsati'
  },
  {
    value: 'LITSENZIYA',
    label: 'Litsenziya',
    hint: 'Ochiq yoki sotib olingan'
  },
  {
    value: 'MAKTAB_MATERIALI',
    label: 'Maktab materiali',
    hint: "Maktabning o'z materiali"
  }
]

export const ageGroups = [
  { value: 'KATTALAR', label: 'Kattalar', icon: 'person' },
  { value: 'OSMIRLAR', label: "O'smirlar", icon: 'face' },
  { value: 'BOLALAR', label: 'Bolalar', icon: 'child_care' }
]

export const languages = [
  { value: 'uz', label: "O'zbekcha" },
  { value: 'ru', label: 'Ruscha' },
  { value: 'en', label: 'Inglizcha' }
]

export const downloadSources = {
  SEARCH: { label: 'Qidiruv', icon: 'search', color: '#0ea5e9' },
  CATEGORY: { label: "Bo'lim", icon: 'folder', color: '#6366f1' },
  RECOMMENDATION: { label: 'Tavsiya', icon: 'star', color: '#f59e0b' },
  TOP: {
    label: "Eng ko'p o'qilgan",
    icon: 'local_fire_department',
    color: '#ef4444'
  },
  NEW: { label: 'Yangi kitoblar', icon: 'fiber_new', color: '#10b981' },
  SAVED: { label: 'Saqlanganlar', icon: 'favorite', color: '#ec4899' }
}

export const LEGAL_WARNING =
  'Faqat tarqatishga huquqingiz bor kitoblarni yuklang.'

/**
 * An author's public-domain state as the server computes it (qonun, 35-modda: vafotdan keyin 70 yil):
 * status FREE (statusYear = free since), PROTECTED (statusYear = protected until) or UNKNOWN.
 */
export function authorStatusText(a) {
  if (!a) return ''
  if (a.status === 'FREE') return `✅ Jamoat mulki (${a.statusYear}-yildan)`
  if (a.status === 'PROTECTED') return `🔒 Himoyada (${a.statusYear}-yilgacha)`
  return '❔ Vafot yili kiritilmagan'
}

/** Several authors: the one who died last decides; an unknown year means it cannot be told. */
export function combinedAuthorStatus(list) {
  if (!list || !list.length) return null
  if (list.some(a => a.status === 'UNKNOWN')) {
    return {
      kind: 'unknown',
      text: "❔ Muallifning vafot yili kiritilmagan — jamoat mulki ekanini aniqlab bo'lmaydi"
    }
  }
  const protectedUntil = Math.max(
    ...list.filter(a => a.status === 'PROTECTED').map(a => a.statusYear),
    0
  )
  if (protectedUntil) {
    return {
      kind: 'protected',
      text: `🔒 ${protectedUntil}-yilgacha himoyada — faqat ruxsatnoma bilan`
    }
  }
  return { kind: 'free', text: '✅ Jamoat mulki — yuklash mumkin' }
}

export const NEEDS_PERMISSION = ['MUALLIF_RUXSATI', 'LITSENZIYA']

export function labelOf(list, value) {
  return list.find(x => x.value === value)?.label || value || '—'
}

export function formatSize(bytes) {
  if (bytes == null) return '—'
  if (bytes < 1024 * 1024) return `${Math.max(1, Math.round(bytes / 1024))} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

/** SHA-256 of a file in the browser (hex) — to warn about a duplicate before uploading it. */
export async function sha256Hex(file) {
  const buffer = await file.arrayBuffer()
  const hash = await crypto.subtle.digest('SHA-256', buffer)
  return Array.from(new Uint8Array(hash))
    .map(b => b.toString(16).padStart(2, '0'))
    .join('')
}

/** A file is a PDF by its first bytes ("%PDF"), not by its name. */
export async function looksLikePdf(file) {
  const head = new Uint8Array(await file.slice(0, 1024).arrayBuffer())
  for (let i = 0; i + 3 < head.length; i++) {
    if (
      head[i] === 0x25 &&
      head[i + 1] === 0x50 &&
      head[i + 2] === 0x44 &&
      head[i + 3] === 0x46
    ) {
      return true
    }
  }
  return false
}
