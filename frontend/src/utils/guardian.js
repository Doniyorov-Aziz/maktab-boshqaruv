// Who the guardian is to the student: "Furqat, otasi" / "Ota: Furqat".

export const guardianRelations = {
  FATHER: { label: 'Otasi', short: 'Ota' },
  MOTHER: { label: 'Onasi', short: 'Ona' },
  OTHER: { label: 'Boshqa (vasiy)', short: 'Vasiy' }
}

export const guardianRelationOptions = Object.entries(guardianRelations).map(
  ([value, r]) => ({ value, label: r.label })
)

/** "Furqat Karimov, otasi" — or just the name when the relation is not set. */
export function guardianLabel(name, relation) {
  if (!name) return ''
  const r = guardianRelations[relation]
  return r ? `${name}, ${r.label.toLowerCase()}` : name
}

/** "Ota: Furqat Karimov" — "Ota-ona: …" when the relation is not set. */
export function guardianShort(name, relation) {
  if (!name) return ''
  return `${guardianRelations[relation]?.short || 'Ota-ona'}: ${name}`
}
