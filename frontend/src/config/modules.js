import { formatDate } from '@/utils/date'
import { guardianRelationOptions } from '@/utils/guardian'
import { markRaw } from 'vue'
import { SUBJECT_COLOR_OPTIONS } from '@/utils/subjectColor'
import { SUBJECT_ICONS, suggestSubjectIcon } from '@/utils/subjectIcon'
import { api } from '@/boot/axios'
import YearsView from '@/components/crud/pages/YearsView.vue'
import YearDetail from '@/components/crud/pages/YearDetail.vue'
import SubjectCard from '@/components/crud/pages/SubjectCard.vue'
import SubjectDetail from '@/components/crud/pages/SubjectDetail.vue'
import StudentDetail from '@/components/crud/pages/StudentDetail.vue'
import PositionCard from '@/components/crud/pages/PositionCard.vue'
import EmployeeCard from '@/components/crud/pages/EmployeeCard.vue'
import EmployeeDetail from '@/components/crud/pages/EmployeeDetail.vue'

const dateFormat = val => (val ? formatDate(val) : '')

const MONTHS_SHORT = [
  'yan',
  'fev',
  'mar',
  'apr',
  'may',
  'iyun',
  'iyul',
  'avg',
  'sen',
  'okt',
  'noy',
  'dek'
]
/** '2026-10-20' → '20-okt' */
const shortDay = iso => {
  const [, m, d] = String(iso).split('-').map(Number)
  return `${d}-${MONTHS_SHORT[m - 1]}`
}

/** "7 yosh" from a birth date (today in this browser). */
function ageText(birth) {
  if (!birth) return ''
  const [y, m, d] = String(birth).split('-').map(Number)
  const t = new Date()
  let age = t.getFullYear() - y
  if (t.getMonth() + 1 < m || (t.getMonth() + 1 === m && t.getDate() < d)) age--
  return `${age} yosh`
}

/** "Boshqa sinfga ko'chirish": a class to choose, then the checked students move there. */
async function moveStudents({ ids, $q, schoolId, reload }) {
  const res = await api.get('/api/school-classes', {
    params: { schoolId, size: 500, sort: 'gradeNumber,asc' }
  })
  const items = res.data.content
    .map(c => ({ label: `${c.gradeNumber}-${c.sectionLetter}`, value: c.id }))
    .sort((a, b) =>
      a.label.localeCompare(b.label, undefined, { numeric: true })
    )
  $q.dialog({
    title: "Boshqa sinfga ko'chirish",
    message: `${ids.length} ta o'quvchi qaysi sinfga o'tadi?`,
    options: { type: 'radio', model: null, items },
    cancel: {
      flat: true,
      label: 'Bekor qilish',
      color: 'grey-7',
      noCaps: true
    },
    ok: {
      label: "Ko'chirish",
      color: 'primary',
      unelevated: true,
      noCaps: true
    },
    persistent: true
  }).onOk(async classId => {
    if (!classId) return
    try {
      const r = await api.post('/api/pages/lists/students/move', {
        studentIds: ids,
        schoolClassId: classId
      })
      $q.notify({
        type: 'positive',
        message: `${r.data.moved} ta o'quvchi ${r.data.className} sinfiga ko'chirildi`
      })
      reload()
    } catch (e) {
      $q.notify({ type: 'negative', message: e.response?.data || 'Xatolik' })
    }
  })
}

const weekdayOptions = [
  'Dushanba',
  'Seshanba',
  'Chorshanba',
  'Payshanba',
  'Juma',
  'Shanba',
  'Yakshanba'
]

const roleOptions = ['ADMIN', 'EDITOR', 'VIEWER']

const housingTypeOptions = [
  { label: "Ko'p qavatli uy (xonadon)", value: 'KVARTIRA' },
  { label: 'Hovli (xususiy uy)', value: 'HOVLI' }
]

// hudud → tuman pair for a student's place (prefix 'birth' / 'residence'); only the tuman is stored
function placeFields(prefix, label) {
  return [
    {
      key: `${prefix}RegionId`,
      label: `${label}: hudud`,
      type: 'select',
      required: false,
      optionsEndpoint: '/api/regions',
      optionValue: 'id',
      optionLabel: 'nameUz'
    },
    {
      key: `${prefix}DistrictId`,
      label: `${label}: tuman / shahar`,
      type: 'select',
      required: false,
      optionsEndpoint: '/api/districts',
      optionValue: 'id',
      optionLabel: 'nameUz',
      parentKey: `${prefix}RegionId`,
      parentValue: 'regionId'
    }
  ]
}

export const roomTypeLabels = {
  CLASSROOM: 'Sinfxona',
  LAB: 'Laboratoriya',
  COMPUTER_LAB: 'Kompyuter xonasi',
  GYM: 'Sport zali',
  ASSEMBLY_HALL: 'Faollar zali',
  LIBRARY: 'Kutubxona',
  OTHER: 'Boshqa'
}
const roomTypeColors = {
  CLASSROOM: '#4f46e5',
  LAB: '#f59e0b',
  COMPUTER_LAB: '#0ea5e9',
  GYM: '#10b981',
  ASSEMBLY_HALL: '#db2777',
  LIBRARY: '#ec4899',
  OTHER: '#64748b'
}
export const roomEquipmentLabels = {
  PROJECTOR: 'Proyektor',
  SMARTBOARD: 'Interaktiv doska',
  AC: 'Konditsioner'
}
export const roomStatusLabels = { ACTIVE: 'Faol', REPAIR: "Ta'mirda" }

export const employeeStatusLabels = {
  ACTIVE: 'Ishda',
  ON_LEAVE: "Ta'tilda",
  DISMISSED: 'Ishdan ketgan'
}
const employeeStatusColors = {
  ACTIVE: '#10b981',
  ON_LEAVE: '#f59e0b',
  DISMISSED: '#94a3b8'
}

export const modules = [
  {
    key: 'schools',
    title: 'Maktablar',
    icon: 'school',
    color: '#4f46e5',
    group: "Ta'lim tuzilmasi",
    endpoint: '/api/schools',
    schoolScoped: false,
    viewType: 'cards',
    cards: {
      title: row => row.name,
      subtitle: row => row.address,
      badge: row =>
        String(row.id) === localStorage.getItem('activeSchoolId')
          ? { label: 'Joriy', color: 'var(--st-present)' }
          : null,
      stats: row => [
        { label: "O'quvchilar", value: row.studentCount ?? 0 },
        { label: "O'qituvchilar", value: row.teacherCount ?? 0 },
        { label: 'Sinflar', value: row.classCount ?? 0 }
      ],
      footer: row =>
        String(row.id) === localStorage.getItem('activeSchoolId')
          ? 'Joriy maktab — bosing, bosh sahifa ochiladi'
          : "Bosing — shu maktabga o'tish",
      footerIcon: 'login'
    },
    columns: [
      { name: 'id', label: 'ID', field: 'id', sortable: true, align: 'left' },
      {
        name: 'name',
        label: 'Nomi',
        field: 'name',
        sortable: true,
        align: 'left'
      },
      { name: 'address', label: 'Manzil', field: 'address', align: 'left' }
    ],
    fields: [
      { key: 'name', label: 'Nomi', type: 'text', required: true },
      { key: 'address', label: 'Manzil', type: 'text', required: true }
    ]
  },
  {
    key: 'buildings',
    title: 'Binolar',
    icon: 'apartment',
    color: '#0ea5e9',
    group: "Ta'lim tuzilmasi",
    endpoint: '/api/buildings',
    schoolScoped: true,
    viewType: 'cards',
    cards: {
      title: row => row.name,
      subtitle: row => row.schoolName,
      stats: row => [
        { label: 'Qavat', value: row.floorCount ?? 1 },
        { label: 'Xona', value: row.roomCount ?? 0 },
        { label: 'Band', value: `${row.occupiedPercentage ?? 0}%` }
      ]
    },
    columns: [
      { name: 'id', label: 'ID', field: 'id', sortable: true, align: 'left' },
      {
        name: 'name',
        label: 'Nomi',
        field: 'name',
        sortable: true,
        align: 'left'
      },
      {
        name: 'schoolName',
        label: 'Maktab',
        field: 'schoolName',
        align: 'left'
      }
    ],
    fields: [
      { key: 'name', label: 'Nomi', type: 'text', required: true },
      { key: 'schoolId', label: 'Maktab', autoSchool: true, required: true }
    ]
  },
  {
    key: 'rooms',
    title: 'Xonalar',
    icon: 'meeting_room',
    color: '#14b8a6',
    group: "Ta'lim tuzilmasi",
    // the menu opens the live map (/rooms); this list is its "Boshqarish"
    path: '/rooms',
    endpoint: '/api/rooms',
    schoolScoped: true,
    filters: [
      {
        key: 'buildingId',
        label: 'Bino',
        optionsEndpoint: '/api/buildings',
        optionValue: 'id',
        optionLabel: 'name',
        schoolScoped: true
      },
      {
        key: 'type',
        label: 'Turi',
        options: Object.entries(roomTypeLabels).map(([value, label]) => ({
          value,
          label
        }))
      }
    ],
    columns: [
      { name: 'id', label: 'ID', field: 'id', sortable: true, align: 'left' },
      {
        name: 'roomNumber',
        label: 'Xona raqami',
        field: 'roomNumber',
        sortable: true,
        align: 'left'
      },
      {
        name: 'type',
        label: 'Turi',
        field: 'type',
        align: 'left',
        badgeColors: roomTypeColors,
        badgeLabels: roomTypeLabels
      },
      { name: 'floor', label: 'Qavat', field: 'floor', align: 'left' },
      { name: 'capacity', label: "Sig'im", field: 'capacity', align: 'left' },
      {
        name: 'buildingName',
        label: 'Bino',
        field: 'buildingName',
        align: 'left'
      },
      {
        name: 'currentStatus',
        label: 'Holati',
        field: 'currentStatus',
        align: 'left'
      }
    ],
    fields: [
      { key: 'roomNumber', label: 'Xona raqami', type: 'text', required: true },
      {
        key: 'type',
        label: 'Turi',
        type: 'select',
        required: true,
        options: Object.entries(roomTypeLabels).map(([value, label]) => ({
          value,
          label
        }))
      },
      { key: 'floor', label: 'Qavat', type: 'number', required: false },
      { key: 'capacity', label: "Sig'im", type: 'number', required: true },
      {
        key: 'buildingId',
        label: 'Bino',
        type: 'select',
        required: true,
        schoolScoped: true,
        optionsEndpoint: '/api/buildings',
        optionValue: 'id',
        optionLabel: 'name'
      },
      {
        key: 'status',
        label: 'Holati',
        type: 'select',
        required: false,
        options: Object.entries(roomStatusLabels).map(([value, label]) => ({
          value,
          label
        }))
      },
      {
        key: 'equipment',
        label: 'Jihozlar',
        type: 'select',
        multiple: true,
        required: false,
        options: Object.entries(roomEquipmentLabels).map(([value, label]) => ({
          value,
          label
        }))
      },
      {
        key: 'computers',
        label: 'Kompyuterlar soni',
        type: 'number',
        required: false
      }
    ]
  },
  {
    key: 'academic-years',
    title: "O'quv yillari",
    icon: 'event',
    color: '#f59e0b',
    group: "O'quv jarayoni",
    endpoint: '/api/academic-years',
    schoolScoped: true,
    serverSearch: true,
    titleOf: row => row.title,
    // year cards (status, terms bar, "Bugun", numbers) instead of a table; terms in the panel
    viewComponent: markRaw(YearsView),
    detailComponent: markRaw(YearDetail),
    detailWide: true,
    columns: [
      { name: 'id', label: 'ID', field: 'id', sortable: true, align: 'left' },
      {
        name: 'title',
        label: 'Sarlavha',
        field: 'title',
        sortable: true,
        align: 'left'
      },
      {
        name: 'startDate',
        label: 'Boshlanish',
        field: 'startDate',
        type: 'date',
        align: 'left'
      },
      {
        name: 'endDate',
        label: 'Tugash',
        field: 'endDate',
        type: 'date',
        align: 'left'
      },
      {
        name: 'studentCount',
        label: "O'quvchilar",
        field: 'studentCount',
        type: 'number',
        align: 'right'
      }
    ],
    fields: [
      {
        key: 'title',
        label: 'Sarlavha',
        type: 'text',
        required: true,
        full: true
      },
      {
        key: 'startDate',
        label: 'Boshlanish sanasi',
        type: 'date',
        required: true
      },
      { key: 'endDate', label: 'Tugash sanasi', type: 'date', required: true },
      { key: 'schoolId', label: 'Maktab', autoSchool: true, required: true }
    ]
  },
  {
    key: 'school-classes',
    title: 'Sinflar',
    icon: 'groups',
    color: '#8b5cf6',
    group: "O'quv jarayoni",
    // the menu opens the class cards (/classes); this list is its "Boshqarish"
    path: '/classes',
    endpoint: '/api/school-classes',
    schoolScoped: true,
    rowLink: 'class',
    viewType: 'cards',
    defaultSort: 'gradeNumber',
    titleOf: row => `${row.gradeNumber}-${row.sectionLetter}`,
    cards: {
      avatar: false,
      icon: () => 'groups',
      title: row => `${row.gradeNumber}-${row.sectionLetter} sinf`,
      subtitle: row => row.classTeacherName || 'Sinf rahbari tayinlanmagan',
      stats: row => [
        { label: "O'quvchi", value: row.studentCount ?? 0 },
        { label: 'Maks.', value: row.maxStudents ?? '—' }
      ],
      footer: row => row.academicYearTitle,
      footerIcon: 'event'
    },
    // "5-a", "5 A", teacher's name — the search box filters the cards as you type
    searchText: row =>
      `${row.gradeNumber}-${row.sectionLetter} ${row.gradeNumber} ${row.sectionLetter} ${row.classTeacherName || ''}`,
    columns: [
      { name: 'id', label: 'ID', field: 'id', sortable: true, align: 'left' },
      {
        name: 'gradeNumber',
        label: 'Sinf',
        field: 'gradeNumber',
        sortable: true,
        align: 'left'
      },
      {
        name: 'sectionLetter',
        label: 'Harf',
        field: 'sectionLetter',
        align: 'left'
      },
      {
        name: 'maxStudents',
        label: "Maks. o'quvchi",
        field: 'maxStudents',
        align: 'left'
      },
      {
        name: 'academicYearTitle',
        label: "O'quv yili",
        field: 'academicYearTitle',
        align: 'left'
      },
      {
        name: 'classTeacherName',
        label: 'Sinf rahbari',
        field: 'classTeacherName',
        align: 'left'
      }
    ],
    fields: [
      {
        key: 'gradeNumber',
        label: 'Sinf raqami',
        type: 'number',
        required: true
      },
      { key: 'sectionLetter', label: 'Harf', type: 'text', required: true },
      {
        key: 'maxStudents',
        label: "Maksimal o'quvchilar soni",
        type: 'number',
        required: true
      },
      {
        key: 'classTeacherId',
        label: 'Sinf rahbari',
        type: 'select',
        required: false,
        schoolScoped: true,
        optionsEndpoint: '/api/employees',
        optionValue: 'id',
        optionLabel: 'fullName'
      },
      {
        key: 'academicYearId',
        label: "O'quv yili",
        type: 'select',
        required: true,
        schoolScoped: true,
        optionsEndpoint: '/api/academic-years',
        optionValue: 'id',
        optionLabel: 'title'
      }
    ]
  },
  {
    key: 'students',
    title: "O'quvchilar",
    icon: 'face',
    color: '#06b6d4',
    group: 'Odamlar',
    endpoint: '/api/students',
    schoolScoped: true,
    serverSearch: true,
    facetsEndpoint: '/api/students/facets',
    profileLink: { type: 'student', idField: 'id' },
    titleOf: row => row.fullName,
    detailComponent: markRaw(StudentDetail),
    hero: {
      endpoint: '/api/pages/lists/students/hero',
      cards: [
        {
          key: 'students',
          label: "Jami o'quvchilar",
          icon: 'face',
          color: 'var(--chart-primary)'
        },
        {
          key: 'today',
          label: 'Bugungi davomat',
          icon: 'how_to_reg',
          color: 'var(--st-present)',
          format: v => `${Math.round(v)}%`,
          deltaPercent: true,
          sparkMin: 0,
          sparkMax: 100
        },
        {
          key: 'risk',
          label: 'Xavf ostida',
          icon: 'warning_amber',
          color: 'var(--st-late)',
          filter: { atRisk: 'true' }
        },
        {
          key: 'telegram',
          label: 'Ota-ona Telegramda',
          icon: 'send',
          color: 'var(--st-excused)',
          format: v => `${Math.round(v)}%`
        }
      ]
    },
    cards: {
      avatar: true,
      title: row => row.fullName,
      subtitle: row => `${row.className} sinf · ${dateFormat(row.birthDate)}`,
      stats: row => [
        {
          label: 'Davomat',
          value:
            row.attendance30 == null ? '—' : `${Math.round(row.attendance30)}%`,
          color:
            row.attendance30 == null
              ? null
              : row.attendance30 >= 95
                ? 'var(--st-present)'
                : row.attendance30 >= 85
                  ? 'var(--gr-3)'
                  : 'var(--st-absent)'
        },
        {
          label: "O'rtacha",
          value: row.average30 == null ? '—' : row.average30.toFixed(2)
        },
        {
          label: 'Xavf',
          value: row.risk ?? '—',
          color:
            row.risk == null
              ? null
              : row.risk >= 70
                ? 'var(--st-absent)'
                : row.risk >= 40
                  ? 'var(--st-late)'
                  : 'var(--st-present)'
        }
      ]
    },
    filters: [
      { key: 'schoolClassId', label: 'Sinf', picker: 'class' },
      {
        key: 'atRisk',
        label: 'Holat',
        options: [{ value: 'true', label: 'Xavf ostida' }]
      },
      {
        key: 'telegram',
        label: 'Telegram',
        options: [
          { value: 'true', label: '✓ Ulangan' },
          { value: 'false', label: '✗ Ulanmagan' }
        ]
      },
      {
        key: 'birthday',
        label: "Tug'ilgan kun",
        options: [{ value: 'true', label: '🎂 Bugun' }]
      }
    ],
    bulkActions: [
      {
        key: 'move',
        label: 'Boshqa sinfga',
        icon: 'drive_file_move',
        run: moveStudents
      },
      {
        key: 'message',
        label: 'Ota-onalarga xabar',
        icon: 'send',
        run: ({ ids, router }) =>
          router.push({
            path: '/broadcasts',
            query: { students: ids.join(',') }
          })
      }
    ],
    columns: [
      { name: 'id', label: 'ID', field: 'id', sortable: true, align: 'left' },
      {
        name: 'fullName',
        label: "O'quvchi",
        field: 'fullName',
        type: 'person',
        sortable: true,
        align: 'left',
        sub: row =>
          [row.className && `${row.className} sinf`, ageText(row.birthDate)]
            .filter(Boolean)
            .join(' · '),
        link: { type: 'student', idField: 'id' }
      },
      {
        name: 'guardianName',
        label: 'Ota-ona',
        field: 'guardianName',
        type: 'contact',
        phoneField: 'guardianPhone',
        align: 'left',
        width: '220px'
      },
      {
        name: 'attendance30',
        label: 'Davomat (30 kun)',
        field: 'attendance30',
        type: 'percent',
        thresholds: [95, 85],
        align: 'left'
      },
      {
        name: 'average30',
        label: "O'rtacha baho",
        field: 'average30',
        type: 'grade',
        trend: row =>
          row.average30 == null ||
          row.averagePrev == null ||
          Math.abs(row.average30 - row.averagePrev) < 0.15
            ? null
            : row.average30 > row.averagePrev
              ? 'up'
              : 'down',
        align: 'left'
      },
      {
        name: 'risk',
        label: 'Xavf',
        field: 'risk',
        type: 'risk',
        align: 'left'
      },
      {
        name: 'telegram',
        label: 'Telegram',
        field: 'telegram',
        type: 'bool',
        align: 'center'
      },
      {
        name: 'birthDate',
        label: "Tug'ilgan sana",
        field: 'birthDate',
        type: 'date',
        sortable: true,
        hidden: true,
        align: 'left'
      },
      {
        name: 'className',
        label: 'Sinf',
        field: 'className',
        type: 'chip',
        sortable: true,
        hidden: true,
        align: 'left'
      },
      // the address only in the side panel, never in the table
      {
        name: 'fullAddress',
        label: 'Yashash manzili',
        field: row => row.fullAddress || '—',
        detailOnly: true,
        align: 'left'
      }
    ],
    fields: [
      {
        key: 'firstName',
        label: 'Ism',
        type: 'text',
        required: true,
        section: "O'quvchi"
      },
      {
        key: 'lastName',
        label: 'Familiya',
        type: 'text',
        required: true,
        section: "O'quvchi"
      },
      {
        key: 'birthDate',
        label: "Tug'ilgan sana",
        type: 'date',
        required: true,
        section: "O'quvchi"
      },
      {
        key: 'schoolClassId',
        label: 'Sinf',
        type: 'select',
        required: true,
        schoolScoped: true,
        optionsEndpoint: '/api/school-classes',
        optionValue: 'id',
        optionLabel: item => `${item.gradeNumber}-${item.sectionLetter}`,
        section: "O'quvchi"
      },
      {
        key: 'guardianName',
        label: 'Ota-ona F.I.Sh.',
        type: 'text',
        required: false,
        section: 'Ota-ona',
        full: true
      },
      {
        key: 'guardianRelation',
        label: "Kim bo'ladi",
        type: 'select',
        required: false,
        options: guardianRelationOptions,
        section: 'Ota-ona'
      },
      {
        key: 'guardianPhone',
        label: 'Ota-ona telefoni',
        type: 'phone',
        required: false,
        section: 'Ota-ona'
      },
      // tug'ilgan joyi: hudud → tuman (only the tuman is stored; the hudud filters the list)
      ...placeFields('birth', "Tug'ilgan joyi").map(f => ({
        ...f,
        section: "Tug'ilgan joyi"
      })),
      // yashash joyi va uy manzili
      ...placeFields('residence', 'Yashash joyi').map(f => ({
        ...f,
        section: 'Yashash manzili'
      })),
      {
        key: 'mahalla',
        label: 'Mahalla',
        type: 'text',
        required: false,
        section: 'Yashash manzili'
      },
      {
        key: 'street',
        label: "Ko'cha (massiv, kvartal)",
        type: 'text',
        required: false,
        section: 'Yashash manzili'
      },
      {
        key: 'housingType',
        label: 'Uy turi',
        type: 'select',
        required: false,
        options: housingTypeOptions,
        section: 'Yashash manzili'
      },
      {
        // ko'p qavatli uy yoki hovli raqami
        key: 'houseNumber',
        label: 'Uy raqami',
        type: 'text',
        required: true,
        showIf: form => !!form.housingType,
        section: 'Yashash manzili'
      },
      {
        key: 'apartmentNumber',
        label: 'Xonadon raqami',
        type: 'text',
        required: true,
        showIf: form => form.housingType === 'KVARTIRA',
        section: 'Yashash manzili'
      }
    ]
  },
  {
    key: 'subjects',
    title: 'Fanlar',
    icon: 'menu_book',
    color: '#10b981',
    group: "O'quv jarayoni",
    endpoint: '/api/subjects',
    schoolScoped: true,
    serverSearch: true,
    facetsEndpoint: '/api/subjects/facets',
    // "O'chirish" only retires a subject: old grades keep its name
    softDelete: true,
    titleOf: row => row.name,
    viewType: 'cards',
    cards: { component: markRaw(SubjectCard), title: row => row.name },
    detailComponent: markRaw(SubjectDetail),
    tabs: {
      key: 'status',
      label: 'Holat',
      default: 'ACTIVE',
      options: [
        { value: 'ACTIVE', label: 'Faol', facet: ['active', 'true'] },
        { value: 'INACTIVE', label: 'Nofaol', facet: ['active', 'false'] }
      ]
    },
    columns: [
      { name: 'id', label: 'ID', field: 'id', sortable: true, align: 'left' },
      {
        name: 'name',
        label: 'Nomi',
        field: 'name',
        sortable: true,
        align: 'left'
      },
      {
        name: 'teachers',
        label: "O'qituvchilar",
        field: row => row.teachers || [],
        type: 'avatars',
        align: 'left'
      },
      {
        name: 'weeklyLessons',
        label: 'Soat / hafta',
        field: 'weeklyLessons',
        type: 'number',
        align: 'right'
      },
      {
        name: 'classCount',
        label: 'Sinflar',
        field: 'classCount',
        type: 'number',
        align: 'right'
      },
      {
        name: 'qualityNow',
        label: 'Sifat (chorak)',
        field: 'qualityNow',
        type: 'percent',
        thresholds: [60, 40],
        align: 'left'
      },
      {
        name: 'averageSpark',
        label: "O'rtacha, 30 kun",
        field: 'averageSpark',
        type: 'spark',
        align: 'left'
      },
      {
        name: 'active',
        label: 'Holat',
        field: row => (row.active === false ? 'INACTIVE' : 'ACTIVE'),
        type: 'badge',
        align: 'left',
        badgeColors: { ACTIVE: '#10b981', INACTIVE: '#94a3b8' },
        badgeLabels: { ACTIVE: 'Faol', INACTIVE: 'Nofaol' }
      }
    ],
    fields: [
      { key: 'name', label: 'Nomi', type: 'text', required: true },
      {
        key: 'color',
        label: 'Rangi (jadval, taqvim, sinflar)',
        type: 'color',
        required: false,
        palette: SUBJECT_COLOR_OPTIONS.filter(o => o.value),
        hint: 'Avtomatik — fan raqamidan 12 rangdan biri'
      },
      {
        key: 'icon',
        label: 'Ikonka',
        type: 'icon',
        required: false,
        icons: SUBJECT_ICONS,
        suggest: form => suggestSubjectIcon(form.name)
      },
      { key: 'schoolId', label: 'Maktab', autoSchool: true, required: true }
    ]
  },
  {
    key: 'positions',
    title: 'Lavozimlar',
    icon: 'badge',
    color: '#64748b',
    group: 'Odamlar',
    endpoint: '/api/positions',
    schoolScoped: false,
    serverSearch: true,
    titleOf: row => row.title,
    viewType: 'cards',
    cards: { component: markRaw(PositionCard), title: row => row.title },
    // a card opens Xodimlar with this position chosen
    cardClick: (row, router) =>
      router.push({
        path: '/app/employees',
        query: { positionId: String(row.id) }
      }),
    // employeeCount (all schools) comes with the list: a position with people cannot go
    deleteImpact: row =>
      row.employeeCount > 0
        ? {
            blocked: true,
            message: `Bu lavozimda ${row.employeeCount} ta xodim bor — o'chirib bo'lmaydi. Avval ularni boshqa lavozimga o'tkazing.`
          }
        : null,
    columns: [
      { name: 'id', label: 'ID', field: 'id', sortable: true, align: 'left' },
      {
        name: 'title',
        label: 'Nomi',
        field: 'title',
        sortable: true,
        align: 'left'
      },
      {
        name: 'description',
        label: 'Tavsif',
        field: 'description',
        align: 'left',
        width: '320px'
      },
      {
        name: 'employees',
        label: 'Xodimlar',
        field: row => row.employees || [],
        type: 'avatars',
        align: 'left'
      },
      {
        name: 'employeeCount',
        label: 'Soni',
        field: 'employeeCount',
        type: 'number',
        align: 'right'
      },
      {
        name: 'defaultRole',
        label: 'Rol (taklif)',
        field: 'defaultRole',
        type: 'badge',
        badgeColors: { ADMIN: '#dc2626', EDITOR: '#6d28d9', VIEWER: '#64748b' },
        badgeLabels: { ADMIN: 'ADMIN', EDITOR: 'EDITOR', VIEWER: 'VIEWER' },
        align: 'left'
      }
    ],
    fields: [
      { key: 'title', label: 'Nomi', type: 'text', required: true, full: true },
      {
        key: 'description',
        label: 'Tavsif',
        type: 'textarea',
        required: false
      },
      {
        key: 'defaultRole',
        label: 'Tizimdagi rol (yangi foydalanuvchiga taklif)',
        type: 'select',
        required: false,
        full: true,
        options: [
          { value: 'ADMIN', label: 'ADMIN — hammasi' },
          { value: 'EDITOR', label: "EDITOR — davomat, baho, ma'lumot" },
          { value: 'VIEWER', label: "VIEWER — faqat ko'rish" }
        ],
        hint: 'Foydalanuvchi yaratilganda shu lavozimdagi xodimga taklif qilinadi'
      }
    ]
  },
  {
    key: 'employees',
    title: 'Xodimlar',
    icon: 'work',
    color: '#ec4899',
    group: 'Odamlar',
    endpoint: '/api/employees',
    schoolScoped: true,
    serverSearch: true,
    facetsEndpoint: '/api/employees/facets',
    profileLink: { type: 'teacher', idField: 'id' },
    titleOf: row => row.fullName,
    detailComponent: markRaw(EmployeeDetail),
    hero: {
      endpoint: '/api/pages/lists/employees/hero',
      cards: [
        {
          key: 'employees',
          label: 'Jami xodimlar',
          icon: 'work',
          color: 'var(--chart-primary)'
        },
        {
          key: 'active',
          label: 'Ishda',
          icon: 'how_to_reg',
          color: 'var(--st-present)',
          filter: { status: 'ACTIVE' }
        },
        {
          key: 'load',
          label: "O'rtacha yuklama",
          icon: 'schedule',
          color: 'var(--st-excused)',
          format: v => `${v.toFixed(1)} soat`
        },
        {
          key: 'journal',
          label: "Jurnal to'liqligi",
          icon: 'fact_check',
          color: 'var(--ring-journal)',
          format: v => `${Math.round(v)}%`
        }
      ]
    },
    cards: { component: markRaw(EmployeeCard), title: row => row.fullName },
    // instead of deleting someone, mark them as gone (their lessons and grades keep the name)
    deactivate: {
      label: 'Ishdan ketgan deb belgilash',
      applies: row => row.status !== 'DISMISSED',
      payload: row => ({
        firstName: row.firstName,
        lastName: row.lastName,
        phone: row.phone,
        positionId: row.positionId,
        schoolId: row.schoolId,
        status: 'DISMISSED',
        leaveFrom: null,
        leaveTo: null
      })
    },
    filters: [
      {
        key: 'positionId',
        label: 'Lavozim',
        optionsEndpoint: '/api/positions',
        optionValue: 'id',
        optionLabel: 'title'
      },
      {
        key: 'status',
        label: 'Holat',
        options: Object.entries(employeeStatusLabels).map(([value, label]) => ({
          value,
          label
        }))
      },
      {
        key: 'classTeacher',
        label: 'Rahbarlik',
        options: [{ value: 'true', label: 'Sinf rahbarlari' }]
      }
    ],
    columns: [
      { name: 'id', label: 'ID', field: 'id', sortable: true, align: 'left' },
      {
        name: 'fullName',
        label: 'Xodim',
        field: 'fullName',
        type: 'person',
        sortable: true,
        align: 'left',
        sub: row => row.positionTitle,
        link: { type: 'teacher', idField: 'id' }
      },
      {
        name: 'status',
        label: 'Holat',
        field: 'status',
        type: 'badge',
        sortable: true,
        align: 'left',
        badgeColors: employeeStatusColors,
        badgeLabels: employeeStatusLabels,
        badgeHint: row =>
          row.status === 'ON_LEAVE' && row.leaveTo
            ? `${shortDay(row.leaveTo)} gacha`
            : ''
      },
      {
        name: 'positionTitle',
        label: 'Lavozim',
        field: 'positionTitle',
        sortable: true,
        hidden: true,
        align: 'left',
        width: '220px'
      },
      {
        name: 'subjects',
        label: 'Fanlar',
        field: 'subjects',
        type: 'subjects',
        align: 'left'
      },
      {
        name: 'classes',
        label: 'Sinflar',
        field: 'classes',
        type: 'list',
        listLabel: 'sinf',
        align: 'left'
      },
      {
        name: 'classTeacherOf',
        label: 'Sinf rahbari',
        field: 'classTeacherOf',
        type: 'list',
        listLabel: 'sinf',
        align: 'left'
      },
      {
        name: 'weeklyLessons',
        label: 'Yuklama',
        field: 'weeklyLessons',
        type: 'load',
        norm: 18,
        align: 'left'
      },
      {
        name: 'journal7',
        label: 'Jurnal',
        field: 'journal7',
        type: 'percent',
        thresholds: [80, 50],
        align: 'left'
      },
      {
        name: 'phone',
        label: 'Telefon',
        field: 'phone',
        type: 'phone',
        sortable: true,
        align: 'left'
      }
    ],
    fields: [
      {
        key: 'firstName',
        label: 'Ism',
        type: 'text',
        required: true,
        section: 'Xodim'
      },
      {
        key: 'lastName',
        label: 'Familiya',
        type: 'text',
        required: true,
        section: 'Xodim'
      },
      {
        key: 'phone',
        label: 'Telefon',
        type: 'phone',
        required: true,
        section: 'Xodim'
      },
      {
        key: 'positionId',
        label: 'Lavozim',
        type: 'select',
        required: true,
        optionsEndpoint: '/api/positions',
        optionValue: 'id',
        optionLabel: 'title',
        section: 'Xodim'
      },
      {
        key: 'status',
        label: 'Holat',
        type: 'select',
        required: false,
        section: 'Holat',
        full: true,
        hint: "Ta'tilda yoki ishdan ketgan xodim tizimga kira olmaydi",
        options: Object.entries(employeeStatusLabels).map(([value, label]) => ({
          value,
          label
        }))
      },
      {
        key: 'leaveFrom',
        label: "Ta'til boshlanishi (ixtiyoriy)",
        type: 'date',
        required: false,
        section: 'Holat',
        showIf: form => form.status === 'ON_LEAVE'
      },
      {
        key: 'leaveTo',
        label: "Ta'til tugashi (ixtiyoriy) — keyin avtomatik «Ishda»",
        type: 'date',
        required: false,
        section: 'Holat',
        showIf: form => form.status === 'ON_LEAVE'
      },
      { key: 'schoolId', label: 'Maktab', autoSchool: true, required: true }
    ]
  },
  {
    key: 'lesson-slots',
    title: "Dars jadvali (ro'yxat)",
    icon: 'schedule',
    color: '#a855f7',
    group: "O'quv jarayoni",
    endpoint: '/api/lesson-slots',
    schoolScoped: true,
    hidden: true,
    columns: [
      { name: 'id', label: 'ID', field: 'id', sortable: true, align: 'left' },
      { name: 'className', label: 'Sinf', field: 'className', align: 'left' },
      {
        name: 'subjectName',
        label: 'Fan',
        field: 'subjectName',
        align: 'left'
      },
      {
        name: 'employeeName',
        label: "O'qituvchi",
        field: 'employeeName',
        align: 'left'
      },
      { name: 'roomNumber', label: 'Xona', field: 'roomNumber', align: 'left' },
      { name: 'weekday', label: 'Kun', field: 'weekday', align: 'left' },
      {
        name: 'startTime',
        label: 'Boshlanish',
        field: 'startTime',
        align: 'left'
      },
      { name: 'endTime', label: 'Tugash', field: 'endTime', align: 'left' }
    ],
    fields: [
      {
        key: 'schoolClassId',
        label: 'Sinf',
        type: 'select',
        required: true,
        schoolScoped: true,
        optionsEndpoint: '/api/school-classes',
        optionValue: 'id',
        optionLabel: item => `${item.gradeNumber}-${item.sectionLetter}`
      },
      {
        key: 'subjectId',
        label: 'Fan',
        type: 'select',
        required: true,
        schoolScoped: true,
        optionsEndpoint: '/api/subjects',
        optionValue: 'id',
        optionLabel: 'name'
      },
      {
        key: 'employeeId',
        label: "O'qituvchi",
        type: 'select',
        required: true,
        schoolScoped: true,
        optionsEndpoint: '/api/employees',
        optionValue: 'id',
        optionLabel: 'fullName'
      },
      {
        key: 'roomId',
        label: 'Xona',
        type: 'select',
        required: true,
        schoolScoped: true,
        optionsEndpoint: '/api/rooms',
        optionValue: 'id',
        optionLabel: 'roomNumber'
      },
      {
        key: 'weekday',
        label: 'Hafta kuni',
        type: 'select',
        required: true,
        options: weekdayOptions
      },
      {
        key: 'startTime',
        label: 'Boshlanish vaqti',
        type: 'time',
        required: true
      },
      { key: 'endTime', label: 'Tugash vaqti', type: 'time', required: true }
    ]
  },
  {
    key: 'announcements',
    title: "E'lonlar",
    icon: 'campaign',
    color: '#3b82f6',
    group: 'Kundalik hayot',
    endpoint: '/api/announcements',
    schoolScoped: true,
    columns: [
      { name: 'id', label: 'ID', field: 'id', sortable: true, align: 'left' },
      {
        name: 'title',
        label: 'Sarlavha',
        field: 'title',
        sortable: true,
        align: 'left'
      },
      { name: 'audience', label: 'Kimga', field: 'audience', align: 'left' },
      {
        name: 'priority',
        label: 'Muhimligi',
        field: 'priority',
        align: 'left'
      },
      {
        name: 'deadline',
        label: 'Muddat',
        field: 'deadline',
        align: 'left',
        format: dateFormat
      }
    ],
    fields: [
      { key: 'title', label: 'Sarlavha', type: 'text', required: true },
      { key: 'content', label: 'Matn', type: 'textarea', required: true },
      {
        key: 'audience',
        label: 'Kimga',
        type: 'select',
        required: true,
        options: ['ALL', 'TEACHERS', 'CLASS']
      },
      {
        key: 'schoolClassId',
        label: "Sinf (Kimga: CLASS bo'lsa)",
        type: 'select',
        required: false,
        schoolScoped: true,
        optionsEndpoint: '/api/school-classes',
        optionValue: 'id',
        optionLabel: item => `${item.gradeNumber}-${item.sectionLetter}`
      },
      {
        key: 'priority',
        label: 'Muhimlik darajasi',
        type: 'select',
        required: true,
        options: ['LOW', 'NORMAL', 'HIGH']
      },
      { key: 'deadline', label: 'Muddat', type: 'date', required: false },
      { key: 'schoolId', label: 'Maktab', autoSchool: true, required: true }
    ]
  },
  {
    key: 'calendar-events',
    title: 'Tadbirlar',
    icon: 'event_available',
    color: '#22c55e',
    group: 'Kundalik hayot',
    endpoint: '/api/calendar-events',
    schoolScoped: true,
    columns: [
      { name: 'id', label: 'ID', field: 'id', sortable: true, align: 'left' },
      {
        name: 'title',
        label: 'Nomi',
        field: 'title',
        sortable: true,
        align: 'left'
      },
      { name: 'type', label: 'Turi', field: 'type', align: 'left' },
      {
        name: 'startDate',
        label: 'Boshlanish',
        field: 'startDate',
        align: 'left',
        format: dateFormat
      },
      {
        name: 'endDate',
        label: 'Tugash',
        field: 'endDate',
        align: 'left',
        format: dateFormat
      }
    ],
    fields: [
      { key: 'title', label: 'Nomi', type: 'text', required: true },
      {
        key: 'description',
        label: 'Tavsif',
        type: 'textarea',
        required: false
      },
      {
        key: 'type',
        label: 'Turi',
        type: 'select',
        required: true,
        options: ['HOLIDAY', 'EXAM', 'PARENT_MEETING', 'VACATION', 'OTHER']
      },
      {
        key: 'startDate',
        label: 'Boshlanish sanasi',
        type: 'date',
        required: true
      },
      { key: 'endDate', label: 'Tugash sanasi', type: 'date', required: true },
      { key: 'schoolId', label: 'Maktab', autoSchool: true, required: true }
    ]
  },
  {
    key: 'behavior-records',
    title: 'Xulq yozuvlari',
    icon: 'emoji_events',
    color: '#f43f5e',
    group: 'Kundalik hayot',
    endpoint: '/api/behavior-records',
    schoolScoped: true,
    defaultSort: 'recordDate',
    defaultDescending: true,
    filters: [
      {
        key: 'schoolClassId',
        label: 'Sinf',
        placeholder: 'Barcha sinflar',
        optionsEndpoint: '/api/school-classes',
        optionValue: 'id',
        optionLabel: item => `${item.gradeNumber}-${item.sectionLetter}`,
        schoolScoped: true,
        sortOptions: true
      }
    ],
    columns: [
      { name: 'id', label: 'ID', field: 'id', sortable: true, align: 'left' },
      {
        name: 'studentName',
        label: "O'quvchi",
        field: 'studentName',
        align: 'left',
        link: { type: 'student', idField: 'studentId' }
      },
      { name: 'className', label: 'Sinf', field: 'className', align: 'left' },
      {
        name: 'guardian',
        label: 'Ota-ona',
        field: row =>
          [row.guardianName, row.guardianPhone].filter(Boolean).join(' · ') ||
          '—',
        align: 'left',
        style: 'max-width: 220px; white-space: normal'
      },
      {
        name: 'type',
        label: 'Turi',
        field: 'type',
        align: 'left',
        badgeColors: { REWARD: '#10b981', WARNING: '#f59e0b' },
        badgeLabels: { REWARD: "Rag'bat", WARNING: 'Ogohlantirish' }
      },
      {
        name: 'description',
        label: 'Tavsif',
        field: 'description',
        align: 'left',
        style: 'max-width: 260px; white-space: normal'
      },
      {
        name: 'createdBy',
        label: 'Kim yozgan',
        field: 'createdBy',
        align: 'left',
        format: v => v || '—'
      },
      {
        name: 'recordDate',
        label: 'Sana',
        field: 'recordDate',
        sortable: true,
        align: 'left',
        format: dateFormat
      }
    ],
    fields: [
      {
        key: 'studentId',
        label: "O'quvchi",
        type: 'select',
        required: true,
        schoolScoped: true,
        optionsEndpoint: '/api/students',
        optionValue: 'id',
        optionLabel: 'fullName'
      },
      { key: 'recordDate', label: 'Sana', type: 'date', required: true },
      {
        key: 'type',
        label: 'Turi',
        type: 'select',
        required: true,
        options: ['REWARD', 'WARNING']
      },
      { key: 'description', label: 'Tavsif', type: 'text', required: true }
    ]
  },
  {
    key: 'users',
    title: 'Foydalanuvchilar',
    icon: 'manage_accounts',
    color: '#475569',
    group: 'Boshqaruv',
    endpoint: '/api/users',
    adminOnly: true,
    schoolScoped: false,
    columns: [
      { name: 'id', label: 'ID', field: 'id', sortable: true, align: 'left' },
      {
        name: 'username',
        label: 'Username',
        field: 'username',
        sortable: true,
        align: 'left'
      },
      { name: 'role', label: 'Rol', field: 'role', align: 'left' },
      {
        name: 'employeeName',
        label: "Bog'langan xodim",
        field: 'employeeName',
        align: 'left'
      }
    ],
    fields: [
      { key: 'username', label: 'Username', type: 'text', required: true },
      {
        key: 'password',
        label: 'Parol',
        type: 'password',
        required: true,
        requiredOnCreateOnly: true,
        hint: "Tahrirlashda bo'sh qoldirilsa, eski parol saqlanadi"
      },
      {
        key: 'role',
        label: 'Rol',
        type: 'select',
        required: true,
        options: roleOptions
      },
      {
        key: 'employeeId',
        label: "Bog'langan xodim (o'z darslarini ko'rish uchun)",
        type: 'select',
        required: false,
        schoolScoped: true,
        optionsEndpoint: '/api/employees',
        optionValue: 'id',
        optionLabel: 'fullName',
        // the position's default role is offered when nothing was chosen yet
        onSelect: (form, employee) => {
          if (!form.role && employee?.positionDefaultRole)
            form.role = employee.positionDefaultRole
        }
      }
    ]
  }
]

// older configs: a column with badgeColors is a badge
for (const m of modules) {
  for (const c of m.columns) {
    if (c.badgeColors && !c.type) c.type = 'badge'
  }
}

export function getModule(key) {
  return modules.find(m => m.key === key)
}
