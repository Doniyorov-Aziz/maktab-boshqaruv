// Mini App texts in the parent's bot language (O'zbekcha, Ўзбекча, Русский).

const DICT = {
  uz: {
    title: 'Farzandim kundaligi',
    tab_today: 'Bugun',
    tab_schedule: 'Jadval',
    tab_attendance: 'Davomat',
    tab_grades: 'Baholar',
    tab_announcements: "E'lonlar",
    class: 'sinf',
    class_teacher: 'Sinf rahbari',
    lessons_today: 'Bugungi darslar',
    no_lessons: "Bugun dars yo'q — dam olish kuni 🌿",
    holiday: "dars yo'q",
    now: 'Hozir',
    break_min: 'daqiqa tanaffus',
    att_none: 'Davomat hali belgilanmagan',
    att_PRESENT: 'Darslarda qatnashmoqda',
    att_LATE: 'Kechikdi',
    att_ABSENT: 'Kelmadi',
    att_EXCUSED: 'Sababli',
    grades_today: 'Bugungi baholar',
    no_grades_today: "Bugun baho hali yo'q",
    new_announcements: "Yangi e'lonlar",
    next_event: 'Yaqin tadbir',
    today: 'Bugun',
    tomorrow: 'Ertaga',
    week: 'Hafta',
    teacher: "O'qituvchi",
    room: 'xona',
    weekend: "Dars yo'q — dam olish kuni 🌿",
    no_schedule: 'Bu kunga dars jadvali kiritilmagan',
    rate: 'davomat',
    class_avg: "sinf o'rtachasi",
    total: 'Jami',
    present: 'Keldi',
    late: 'Kechikdi',
    absent: 'Kelmadi',
    excused: 'Sababli',
    no_lessons_day: "Dars yo'q",
    day_details: 'Kun tafsiloti',
    all_present: 'Barcha darslarda qatnashgan ✅',
    nothing_marked: 'Bu kunga davomat belgilanmagan',
    by_subject: "Fanlar bo'yicha qoldirilgan darslar",
    lesson: 'dars',
    averages: "Fanlar bo'yicha o'rtacha",
    recent: "So'nggi baholar",
    no_grades: "Hali baho yo'q 🙂",
    grade_CURRENT: 'joriy',
    grade_EXAM: 'imtihon',
    grade_QUARTERLY: 'chorak',
    all_grades: 'Barcha baholar',
    average: "O'rtacha",
    no_announcements: "Hozircha e'lon yo'q 🙂",
    important: 'Muhim',
    new: 'Yangi',
    deadline: 'Muddat',
    for_class: 'sinf uchun',
    loading: 'Yuklanmoqda…',
    error: "Ma'lumotni yuklab bo'lmadi. Keyinroq qayta urinib ko'ring.",
    not_telegram:
      'Kundalik faqat Telegram ichida, botdagi «📱 Kundalikni ochish» tugmasi orqali ochiladi.',
    no_children: 'Hali farzand ulanmagan. Botda /start bosing.',
    retry: 'Qayta urinish'
  },
  cy: {
    title: 'Фарзандим кундалиги',
    tab_today: 'Бугун',
    tab_schedule: 'Жадвал',
    tab_attendance: 'Давомат',
    tab_grades: 'Баҳолар',
    tab_announcements: 'Эълонлар',
    class: 'синф',
    class_teacher: 'Синф раҳбари',
    lessons_today: 'Бугунги дарслар',
    no_lessons: 'Бугун дарс йўқ — дам олиш куни 🌿',
    holiday: 'дарс йўқ',
    now: 'Ҳозир',
    break_min: 'дақиқа танаффус',
    att_none: 'Давомат ҳали белгиланмаган',
    att_PRESENT: 'Дарсларда қатнашмоқда',
    att_LATE: 'Кечикди',
    att_ABSENT: 'Келмади',
    att_EXCUSED: 'Сабабли',
    grades_today: 'Бугунги баҳолар',
    no_grades_today: 'Бугун баҳо ҳали йўқ',
    new_announcements: 'Янги эълонлар',
    next_event: 'Яқин тадбир',
    today: 'Бугун',
    tomorrow: 'Эртага',
    week: 'Ҳафта',
    teacher: 'Ўқитувчи',
    room: 'хона',
    weekend: 'Дарс йўқ — дам олиш куни 🌿',
    no_schedule: 'Бу кунга дарс жадвали киритилмаган',
    rate: 'давомат',
    class_avg: 'синф ўртачаси',
    total: 'Жами',
    present: 'Келди',
    late: 'Кечикди',
    absent: 'Келмади',
    excused: 'Сабабли',
    no_lessons_day: 'Дарс йўқ',
    day_details: 'Кун тафсилоти',
    all_present: 'Барча дарсларда қатнашган ✅',
    nothing_marked: 'Бу кунга давомат белгиланмаган',
    by_subject: 'Фанлар бўйича қолдирилган дарслар',
    lesson: 'дарс',
    averages: 'Фанлар бўйича ўртача',
    recent: 'Сўнгги баҳолар',
    no_grades: 'Ҳали баҳо йўқ 🙂',
    grade_CURRENT: 'жорий',
    grade_EXAM: 'имтиҳон',
    grade_QUARTERLY: 'чорак',
    all_grades: 'Барча баҳолар',
    average: 'Ўртача',
    no_announcements: 'Ҳозирча эълон йўқ 🙂',
    important: 'Муҳим',
    new: 'Янги',
    deadline: 'Муддат',
    for_class: 'синф учун',
    loading: 'Юкланмоқда…',
    error: 'Маълумотни юклаб бўлмади. Кейинроқ қайта уриниб кўринг.',
    not_telegram:
      'Кундалик фақат Telegram ичида, ботдаги «📱 Кундаликни очиш» тугмаси орқали очилади.',
    no_children: 'Ҳали фарзанд уланмаган. Ботда /start босинг.',
    retry: 'Қайта уриниш'
  },
  ru: {
    title: 'Дневник ребёнка',
    tab_today: 'Сегодня',
    tab_schedule: 'Расписание',
    tab_attendance: 'Посещаемость',
    tab_grades: 'Оценки',
    tab_announcements: 'Объявления',
    class: 'класс',
    class_teacher: 'Классный руководитель',
    lessons_today: 'Уроки сегодня',
    no_lessons: 'Сегодня уроков нет — выходной 🌿',
    holiday: 'уроков нет',
    now: 'Сейчас',
    break_min: 'мин перемена',
    att_none: 'Посещаемость ещё не отмечена',
    att_PRESENT: 'На уроках',
    att_LATE: 'Опоздал(а)',
    att_ABSENT: 'Отсутствует',
    att_EXCUSED: 'По уважительной причине',
    grades_today: 'Оценки за сегодня',
    no_grades_today: 'Сегодня оценок пока нет',
    new_announcements: 'Новые объявления',
    next_event: 'Ближайшее событие',
    today: 'Сегодня',
    tomorrow: 'Завтра',
    week: 'Неделя',
    teacher: 'Учитель',
    room: 'каб.',
    weekend: 'Уроков нет — выходной 🌿',
    no_schedule: 'Расписание на этот день не внесено',
    rate: 'посещаемость',
    class_avg: 'среднее по классу',
    total: 'Всего',
    present: 'Был',
    late: 'Опоздал',
    absent: 'Пропуск',
    excused: 'Уважит.',
    no_lessons_day: 'Нет уроков',
    day_details: 'Подробно за день',
    all_present: 'Был на всех уроках ✅',
    nothing_marked: 'Посещаемость за этот день не отмечена',
    by_subject: 'Пропуски по предметам',
    lesson: 'урок',
    averages: 'Средний балл по предметам',
    recent: 'Последние оценки',
    no_grades: 'Оценок пока нет 🙂',
    grade_CURRENT: 'текущая',
    grade_EXAM: 'экзамен',
    grade_QUARTERLY: 'четвертная',
    all_grades: 'Все оценки',
    average: 'Средний балл',
    no_announcements: 'Объявлений пока нет 🙂',
    important: 'Важное',
    new: 'Новое',
    deadline: 'Срок',
    for_class: 'для класса',
    loading: 'Загрузка…',
    error: 'Не удалось загрузить данные. Попробуйте позже.',
    not_telegram:
      'Дневник открывается только внутри Telegram — кнопкой «📱 Открыть дневник» в боте.',
    no_children: 'Дети ещё не подключены. Нажмите /start в боте.',
    retry: 'Повторить'
  }
}

const MONTHS = {
  uz: [
    'yanvar',
    'fevral',
    'mart',
    'aprel',
    'may',
    'iyun',
    'iyul',
    'avgust',
    'sentabr',
    'oktabr',
    'noyabr',
    'dekabr'
  ],
  cy: [
    'январ',
    'феврал',
    'март',
    'апрел',
    'май',
    'июн',
    'июл',
    'август',
    'сентябр',
    'октябр',
    'ноябр',
    'декабр'
  ],
  ru: [
    'января',
    'февраля',
    'марта',
    'апреля',
    'мая',
    'июня',
    'июля',
    'августа',
    'сентября',
    'октября',
    'ноября',
    'декабря'
  ]
}
const MONTHS_TITLE = {
  uz: [
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
  ],
  cy: [
    'Январ',
    'Феврал',
    'Март',
    'Апрел',
    'Май',
    'Июн',
    'Июл',
    'Август',
    'Сентябр',
    'Октябр',
    'Ноябр',
    'Декабр'
  ],
  ru: [
    'Январь',
    'Февраль',
    'Март',
    'Апрель',
    'Май',
    'Июнь',
    'Июль',
    'Август',
    'Сентябрь',
    'Октябрь',
    'Ноябрь',
    'Декабрь'
  ]
}
// Monday first, matching the bot and the backend's DayOfWeek order.
const DAYS = {
  uz: [
    'dushanba',
    'seshanba',
    'chorshanba',
    'payshanba',
    'juma',
    'shanba',
    'yakshanba'
  ],
  cy: [
    'душанба',
    'сешанба',
    'чоршанба',
    'пайшанба',
    'жума',
    'шанба',
    'якшанба'
  ],
  ru: [
    'понедельник',
    'вторник',
    'среда',
    'четверг',
    'пятница',
    'суббота',
    'воскресенье'
  ]
}
const DAYS_SHORT = {
  uz: ['Du', 'Se', 'Ch', 'Pa', 'Ju', 'Sh', 'Ya'],
  cy: ['Ду', 'Се', 'Чо', 'Па', 'Жу', 'Ша', 'Як'],
  ru: ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Вс']
}

export function translate(lang, key) {
  return DICT[lang]?.[key] ?? DICT.uz[key] ?? key
}

export function keys(lang) {
  return Object.keys(DICT[lang] || {})
}

function parts(dateStr) {
  const [y, m, d] = dateStr.split('-').map(Number)
  const date = new Date(y, m - 1, d)
  return { y, m, d, dow: (date.getDay() + 6) % 7 }
}

/** "30-sentabr, chorshanba" / "30 сентября, среда". */
export function fullDate(lang, dateStr) {
  const { m, d, dow } = parts(dateStr)
  const day = DAYS[lang] || DAYS.uz
  const month = MONTHS[lang] || MONTHS.uz
  return lang === 'ru'
    ? `${d} ${month[m - 1]}, ${day[dow]}`
    : `${d}-${month[m - 1]}, ${day[dow]}`
}

export function shortDate(lang, dateStr) {
  const { m, d } = parts(dateStr)
  const month = MONTHS[lang] || MONTHS.uz
  return lang === 'ru' ? `${d} ${month[m - 1]}` : `${d}-${month[m - 1]}`
}

export function monthTitle(lang, year, month) {
  return `${(MONTHS_TITLE[lang] || MONTHS_TITLE.uz)[month - 1]} ${year}`
}

export function dayShort(lang, index) {
  return (DAYS_SHORT[lang] || DAYS_SHORT.uz)[index]
}

export function dayTitle(lang, dateStr) {
  const name = (DAYS[lang] || DAYS.uz)[parts(dateStr).dow]
  return name.charAt(0).toUpperCase() + name.slice(1)
}

export const LANGUAGES = Object.keys(DICT)
