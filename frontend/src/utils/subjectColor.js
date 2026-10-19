// One colour per subject on every page and in the PDF (SubjectColors.java, the same order):
// the colour picked on the subjects page, else one of 12 theme tokens from the subject's id.
export const SUBJECT_PALETTE = [
  '#6d28d9',
  '#0284c7',
  '#059669',
  '#d97706',
  '#db2777',
  '#4f46e5',
  '#0d9488',
  '#dc2626',
  '#2563eb',
  '#65a30d',
  '#c2410c',
  '#9333ea'
]

/** CSS colour of a subject: its own "#rrggbb", else var(--subj-N) (light and dark shades). */
export function subjectColor(subjectId, chosen) {
  if (chosen) return chosen
  const n = (((Number(subjectId) || 0) % 12) + 12) % 12
  return `var(--subj-${n + 1})`
}

/** Options for the subjects page: "Avtomatik" and the 12 palette colours. */
export const SUBJECT_COLOR_OPTIONS = [
  { value: '', label: 'Avtomatik' },
  ...[
    'Binafsha',
    'Moviy',
    'Zumrad',
    'Qahrabo',
    'Pushti',
    'Indigo',
    'Feruza',
    'Qizil',
    "Ko'k",
    'Yashil',
    "To'q sariq",
    'Siyohrang'
  ].map((label, k) => ({ value: SUBJECT_PALETTE[k], label }))
]
