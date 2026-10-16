// Every screen of the admin panel the UI checks visit. Ids point at the demo data
// (school 1); override with UI_STUDENT_ID / UI_TEACHER_ID / UI_CLASS_ID if needed.
const env = typeof process !== 'undefined' ? process.env : {}
const student = env.UI_STUDENT_ID || '1'
const teacher = env.UI_TEACHER_ID || '9'
const klass = env.UI_CLASS_ID || '1'

export const modules = [
  'schools',
  'buildings',
  'rooms',
  'academic-years',
  'school-classes',
  'students',
  'subjects',
  'positions',
  'employees',
  'lesson-slots',
  'announcements',
  'calendar-events',
  'behavior-records',
  'users'
]

export const pages = [
  { name: 'login', path: '/login', public: true },
  { name: 'dashboard', path: '/' },
  ...modules.map(m => ({ name: 'crud-' + m, path: '/app/' + m })),
  { name: 'attendance', path: '/attendance' },
  { name: 'attendance-calendar', path: '/attendance/calendar' },
  { name: 'gradebook', path: '/gradebook' },
  { name: 'timetable', path: '/timetable' },
  { name: 'calendar', path: '/calendar' },
  { name: 'notifications', path: '/notifications' },
  { name: 'telegram-parents', path: '/telegram-parents' },
  { name: 'parent-messages', path: '/parent-messages' },
  { name: 'absence-requests', path: '/absence-requests' },
  { name: 'broadcasts', path: '/broadcasts' },
  { name: 'bot-stats', path: '/bot-stats' },
  { name: 'bot-settings', path: '/bot-settings' },
  { name: 'library-books', path: '/library/books' },
  { name: 'library-categories', path: '/library/categories' },
  { name: 'library-authors', path: '/library/authors' },
  { name: 'library-permissions', path: '/library/permissions' },
  { name: 'library-stats', path: '/library/stats' },
  { name: 'student-profile', path: '/profiles/student/' + student },
  { name: 'teacher-profile', path: '/profiles/teacher/' + teacher },
  { name: 'class-profile', path: '/profiles/class/' + klass },
  { name: 'class-qr-print', path: '/print/class-qr/' + klass, bare: true },
  { name: 'not-found', path: '/no-such-page', public: true }
]

export const viewports = [
  { width: 1366, height: 768 },
  { width: 1920, height: 1080 },
  { width: 2560, height: 1440 },
  { width: 390, height: 844 }
]
