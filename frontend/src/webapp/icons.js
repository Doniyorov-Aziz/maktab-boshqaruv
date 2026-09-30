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
