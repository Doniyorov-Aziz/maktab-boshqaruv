import { formatDate } from '@/utils/date'
import { guardianLabel, guardianRelationOptions } from '@/utils/guardian'

const dateFormat = val => (val ? formatDate(val) : '')

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

const roomTypeLabels = {
  CLASSROOM: 'Sinfxona',
  LAB: 'Laboratoriya',
  COMPUTER_LAB: 'Kompyuter xonasi',
  GYM: 'Sport zali',
  LIBRARY: 'Kutubxona'
}
const roomTypeColors = {
  CLASSROOM: '#4f46e5',
  LAB: '#f59e0b',
  COMPUTER_LAB: '#0ea5e9',
  GYM: '#10b981',
  LIBRARY: '#ec4899'
}

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
        align: 'left',
        format: dateFormat
      },
      {
        name: 'endDate',
        label: 'Tugash',
        field: 'endDate',
        align: 'left',
        format: dateFormat
      },
      {
        name: 'schoolName',
        label: 'Maktab',
        field: 'schoolName',
        align: 'left'
      }
    ],
    fields: [
      { key: 'title', label: 'Sarlavha', type: 'text', required: true },
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
    endpoint: '/api/school-classes',
    schoolScoped: true,
    rowLink: 'class',
    viewType: 'cards',
    defaultSort: 'gradeNumber',
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
        name: 'fullName',
        label: "To'liq ism",
        field: 'fullName',
        sortable: true,
        align: 'left',
        link: { type: 'student', idField: 'id' }
      },
      {
        name: 'birthDate',
        label: "Tug'ilgan sana",
        field: 'birthDate',
        align: 'left',
        format: dateFormat
      },
      { name: 'className', label: 'Sinf', field: 'className', align: 'left' },
      {
        name: 'guardianName',
        label: 'Ota-ona',
        field: row => guardianLabel(row.guardianName, row.guardianRelation),
        align: 'left'
      }
    ],
    fields: [
      { key: 'firstName', label: 'Ism', type: 'text', required: true },
      { key: 'lastName', label: 'Familiya', type: 'text', required: true },
      {
        key: 'birthDate',
        label: "Tug'ilgan sana",
        type: 'date',
        required: true
      },
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
        key: 'guardianName',
        label: 'Ota-ona F.I.Sh.',
        type: 'text',
        required: false
      },
      {
        key: 'guardianRelation',
        label: "Kim bo'ladi",
        type: 'select',
        required: false,
        options: guardianRelationOptions
      },
      {
        key: 'guardianPhone',
        label: 'Ota-ona telefoni',
        type: 'text',
        required: false
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
    // "O'chirish" only retires a subject: old grades keep its name
    softDelete: true,
    toggles: [{ key: 'includeInactive', label: "Nofaollarni ko'rsatish" }],
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
        name: 'active',
        label: 'Holat',
        field: row => (row.active === false ? 'INACTIVE' : 'ACTIVE'),
        align: 'left',
        badgeColors: { ACTIVE: '#10b981', INACTIVE: '#94a3b8' },
        badgeLabels: { ACTIVE: 'Faol', INACTIVE: 'Nofaol' }
      }
    ],
    fields: [
      { key: 'name', label: 'Nomi', type: 'text', required: true },
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
    columns: [
      { name: 'id', label: 'ID', field: 'id', sortable: true, align: 'left' },
      {
        name: 'title',
        label: 'Nomi',
        field: 'title',
        sortable: true,
        align: 'left'
      }
    ],
    fields: [{ key: 'title', label: 'Nomi', type: 'text', required: true }]
  },
  {
    key: 'employees',
    title: 'Xodimlar',
    icon: 'work',
    color: '#ec4899',
    group: 'Odamlar',
    endpoint: '/api/employees',
    schoolScoped: true,
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
      }
    ],
    columns: [
      { name: 'id', label: 'ID', field: 'id', sortable: true, align: 'left' },
      {
        name: 'fullName',
        label: "To'liq ism",
        field: 'fullName',
        sortable: true,
        align: 'left',
        link: { type: 'teacher', idField: 'id' }
      },
      {
        name: 'status',
        label: 'Holat',
        field: 'status',
        align: 'left',
        badgeColors: employeeStatusColors,
        badgeLabels: employeeStatusLabels,
        badgeHint: row =>
          row.status === 'ON_LEAVE' && (row.leaveFrom || row.leaveTo)
            ? `${dateFormat(row.leaveFrom) || '…'} – ${dateFormat(row.leaveTo) || '…'}`
            : ''
      },
      {
        name: 'positionTitle',
        label: 'Lavozim',
        field: 'positionTitle',
        align: 'left',
        // long titles wrap instead of pushing the table wider than the screen
        style: 'max-width: 210px; white-space: normal',
        headerStyle: 'max-width: 210px'
      },
      {
        name: 'subjects',
        label: 'Fanlar',
        field: 'subjects',
        align: 'left',
        format: v => v || '—',
        style: 'max-width: 240px; white-space: normal',
        headerStyle: 'max-width: 240px'
      },
      {
        name: 'classes',
        label: 'Sinflar',
        field: 'classes',
        align: 'left',
        format: v => v || '—'
      },
      {
        name: 'classTeacherOf',
        label: 'Sinf rahbari',
        field: 'classTeacherOf',
        align: 'left',
        format: v => v || '—'
      },
      { name: 'phone', label: 'Telefon', field: 'phone', align: 'left' }
    ],
    fields: [
      { key: 'firstName', label: 'Ism', type: 'text', required: true },
      { key: 'lastName', label: 'Familiya', type: 'text', required: true },
      { key: 'phone', label: 'Telefon', type: 'text', required: true },
      {
        key: 'positionId',
        label: 'Lavozim',
        type: 'select',
        required: true,
        optionsEndpoint: '/api/positions',
        optionValue: 'id',
        optionLabel: 'title'
      },
      {
        key: 'status',
        label: 'Holat',
        type: 'select',
        required: false,
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
        showIf: form => form.status === 'ON_LEAVE'
      },
      {
        key: 'leaveTo',
        label: "Ta'til tugashi (ixtiyoriy) — keyin avtomatik «Ishda»",
        type: 'date',
        required: false,
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
        optionLabel: 'fullName'
      }
    ]
  }
]

export function getModule(key) {
  return modules.find(m => m.key === key)
}
