// A subject's icon: the one chosen on the subjects page, else one suggested from its name
// (Matematika → calculate, Fizika → science, Ona tili → menu_book …), else a book.
const RULES = [
  [/matem|algebra|geometr|hisob/, 'calculate'],
  [/fizik|astronom/, 'science'],
  [/kimyo|chem/, 'biotech'],
  [/biolog|tabiat|botan|zoolog/, 'eco'],
  [/geograf/, 'public'],
  [/tarix|history/, 'history_edu'],
  [/huquq|davlat|konstitut/, 'gavel'],
  [/ona tili|o'zbek tili|o‘zbek tili|savod/, 'menu_book'],
  [/adabiyot|o'qish|o‘qish/, 'auto_stories'],
  [/ingliz|english|nemis|fransuz|rus tili|xorijiy|tili/, 'translate'],
  [/informat|kompyuter|axborot|dasturlash/, 'computer'],
  [/jismoniy|sport|tarbiya.*jism/, 'sports_soccer'],
  [/musiqa|music|ashula/, 'music_note'],
  [/tasviriy|rasm|chizmachilik|san'at|san‘at/, 'palette'],
  [/texnolog|mehnat/, 'construction'],
  [/tarbiya|odobnoma|ma'naviyat|ma‘naviyat/, 'volunteer_activism'],
  [/iqtisod/, 'payments'],
  [/chaqiriq|harbiy/, 'shield']
]

export const SUBJECT_ICONS = [
  'menu_book',
  'auto_stories',
  'calculate',
  'science',
  'biotech',
  'eco',
  'public',
  'history_edu',
  'gavel',
  'translate',
  'computer',
  'sports_soccer',
  'music_note',
  'palette',
  'construction',
  'volunteer_activism',
  'payments',
  'shield',
  'psychology',
  'school'
]

/** The suggested icon for a subject name ("Ona tili" → "menu_book"). */
export function suggestSubjectIcon(name) {
  const n = String(name || '').toLowerCase()
  for (const [re, icon] of RULES) if (re.test(n)) return icon
  return 'menu_book'
}

/** The icon to show: chosen, else suggested. */
export const subjectIcon = s => s?.icon || suggestSubjectIcon(s?.name)

// positions: an icon and a token colour from the title (Direktor → admin_panel_settings …)
const POSITION_RULES = [
  [
    /direktor o'rinbosar|o‘rinbosar|o'rinbosar/,
    'supervisor_account',
    '--subj-2'
  ],
  [/direktor/, 'admin_panel_settings', '--subj-1'],
  [/sinf rahbar/, 'groups', '--subj-3'],
  [/o'qituvchi|o‘qituvchi|ustoz|pedagog/, 'school', '--subj-6'],
  [/psixolog/, 'psychology', '--subj-5'],
  [/kutubxon/, 'local_library', '--subj-7'],
  [/hisobchi|buxgalter|kassir/, 'account_balance_wallet', '--subj-4'],
  [/qorovul|xavfsiz|qo'riqchi/, 'security', '--subj-8'],
  [/hamshira|shifokor|tibbiy/, 'medical_services', '--subj-8'],
  [/oshpaz|oshxona/, 'restaurant', '--subj-11'],
  [/farrosh|tozal|xo'jalik|xo‘jalik/, 'cleaning_services', '--subj-10'],
  [/haydovchi/, 'directions_bus', '--subj-9'],
  [/kotib|ish yurit|kadr/, 'description', '--subj-12']
]

/** { icon, color } for a position title. */
export function positionLook(title) {
  const t = String(title || '').toLowerCase()
  for (const [re, icon, token] of POSITION_RULES)
    if (re.test(t)) return { icon, color: `var(${token})` }
  return { icon: 'badge', color: 'var(--subj-6)' }
}
