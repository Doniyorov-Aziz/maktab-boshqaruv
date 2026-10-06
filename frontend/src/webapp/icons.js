// Same emoji system as the bot (bot/SubjectIcons.java), so both look alike.

const SUBJECTS = [
  ['algebra', '➗'],
  ['geometr', '📐'],
  ['matem', '🔢'],
  ['fizika', '⚛️'],
  ['kimyo', '🧪'],
  ['biolog', '🧬'],
  ['tabiiy', '🌱'],
  ['ona tili', '📝'],
  ['adabiyot', '📖'],
  ["o'qish", '📖'],
  ['ingliz', '🇬🇧'],
  ['rus', '🇷🇺'],
  ['nemis', '🇩🇪'],
  ['tarix', '🏛'],
  ['geograf', '🌍'],
  ['informat', '💻'],
  ['musiqa', '🎵'],
  ['tasviriy', '🎨'],
  ['jismoniy', '🏃'],
  ['texnolog', '🛠'],
  ['tarbiya', '🤝'],
  ['huquq', '⚖️'],
  ['iqtisod', '📈'],
  ['astronom', '🔭']
]

export function subjectIcon(name) {
  const n = (name || '').toLowerCase()
  for (const [fragment, icon] of SUBJECTS) if (n.includes(fragment)) return icon
  return '📚'
}

export function eventIcon(type) {
  return (
    { HOLIDAY: '🎉', EXAM: '📝', PARENT_MEETING: '👨‍👩‍👧', VACATION: '🏖' }[type] ||
    '📌'
  )
}

export function scoreColor(score) {
  if (score >= 5) return '#10b981'
  if (score >= 4) return '#4f46e5'
  if (score >= 3) return '#f59e0b'
  return '#ef4444'
}

export function averageColor(avg) {
  if (avg >= 4.5) return '#10b981'
  if (avg >= 3.5) return '#4f46e5'
  if (avg >= 2.5) return '#f59e0b'
  return '#ef4444'
}

// One stable colour per subject (the same subject is always the same colour).
const PALETTE = [
  '#4f46e5',
  '#0ea5e9',
  '#10b981',
  '#f59e0b',
  '#ef4444',
  '#8b5cf6',
  '#ec4899',
  '#14b8a6',
  '#f97316',
  '#6366f1'
]

function hash(text) {
  let h = 0
  for (const ch of text || '') h = (h * 31 + ch.codePointAt(0)) >>> 0
  return h
}

export function subjectColor(name) {
  return PALETTE[hash((name || '').toLowerCase()) % PALETTE.length]
}

/** "Ali Valiyev" → "AV". */
export function initials(name) {
  return (name || '?')
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map(w => w.charAt(0).toUpperCase())
    .join('')
}

export function avatarColor(id) {
  return PALETTE[Number(id || 0) % PALETTE.length]
}
