const routes = [
  {
    path: '/login',
    component: () => import('@/pages/LoginPage.vue')
  },
  {
    // the public demo link (#/demo?ref=linkedin): signs in as the read-only "Mehmon"
    path: '/demo',
    component: () => import('@/pages/DemoPage.vue'),
    meta: { public: true }
  },
  {
    // Telegram Mini App "Farzandim kundaligi" — public route, authenticated by Telegram initData.
    path: '/webapp',
    component: () => import('@/layouts/WebAppLayout.vue'),
    meta: { public: true },
    children: [
      {
        path: '',
        component: () => import('@/pages/webapp/WebTodayPage.vue'),
        meta: { public: true }
      },
      {
        path: 'schedule',
        component: () => import('@/pages/webapp/WebSchedulePage.vue'),
        meta: { public: true }
      },
      {
        path: 'attendance',
        component: () => import('@/pages/webapp/WebAttendancePage.vue'),
        meta: { public: true }
      },
      {
        path: 'grades',
        component: () => import('@/pages/webapp/WebGradesPage.vue'),
        meta: { public: true }
      },
      {
        path: 'announcements',
        component: () => import('@/pages/webapp/WebAnnouncementsPage.vue'),
        meta: { public: true }
      }
    ]
  },
  {
    // Outside MainLayout on purpose: a clean page with no header/drawer prints as-is.
    path: '/print/class-qr/:id',
    component: () => import('@/pages/ClassQrPrintPage.vue'),
    meta: { editorOnly: true }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      { path: '', component: () => import('@/pages/DashboardPage.vue') },
      {
        path: 'app/:moduleKey',
        component: () => import('@/pages/CrudPage.vue')
      },
      {
        path: 'attendance',
        component: () => import('@/pages/AttendancePage.vue')
      },
      {
        path: 'attendance/calendar',
        component: () => import('@/pages/AttendanceCalendarPage.vue')
      },
      {
        path: 'gradebook',
        component: () => import('@/pages/GradebookPage.vue')
      },
      {
        path: 'timetable',
        component: () => import('@/pages/TimetablePage.vue')
      },
      {
        path: 'calendar',
        component: () => import('@/pages/CalendarPage.vue')
      },
      {
        path: 'classes',
        component: () => import('@/pages/ClassesPage.vue')
      },
      {
        path: 'classes/:id',
        component: () => import('@/pages/ClassPage.vue')
      },
      {
        path: 'rooms',
        component: () => import('@/pages/RoomsPage.vue')
      },
      {
        path: 'notifications',
        component: () => import('@/pages/NotificationsPage.vue'),
        meta: { editorOnly: true, guestLocked: true }
      },
      {
        // a section closed to the demo visitor
        path: 'locked',
        component: () => import('@/pages/LockedPage.vue')
      },
      {
        // the public demo's visitors: the real administrator only
        path: 'visits',
        component: () => import('@/pages/VisitsPage.vue'),
        meta: { realAdminOnly: true, guestLocked: true }
      },
      {
        path: 'telegram-parents',
        component: () => import('@/pages/TelegramParentsPage.vue')
      },
      {
        path: 'parent-messages',
        component: () => import('@/pages/AppealsPage.vue'),
        meta: { editorOnly: true, guestLocked: true }
      },
      {
        path: 'absence-requests',
        component: () => import('@/pages/AbsenceRequestsPage.vue'),
        meta: { editorOnly: true, guestLocked: true }
      },
      {
        path: 'broadcasts',
        component: () => import('@/pages/BroadcastPage.vue'),
        meta: { adminOnly: true, guestLocked: true }
      },
      {
        path: 'bot-stats',
        component: () => import('@/pages/BotStatsPage.vue'),
        meta: { editorOnly: true }
      },
      {
        path: 'bot-settings',
        component: () => import('@/pages/BotSettingsPage.vue'),
        meta: { editorOnly: true }
      },
      {
        path: 'library/books',
        component: () => import('@/pages/LibraryBooksPage.vue'),
        meta: { editorOnly: true }
      },
      {
        path: 'library/categories',
        component: () => import('@/pages/LibraryCategoriesPage.vue'),
        meta: { editorOnly: true }
      },
      {
        path: 'library/authors',
        component: () => import('@/pages/LibraryAuthorsPage.vue'),
        meta: { editorOnly: true }
      },
      {
        // rights holders' permissions with their signed scans — administrators only
        path: 'library/permissions',
        component: () => import('@/pages/LibraryPermissionsPage.vue'),
        meta: { adminOnly: true }
      },
      {
        // who read what, with parents' phones — administrators only
        path: 'library/stats',
        component: () => import('@/pages/LibraryStatsPage.vue'),
        meta: { adminOnly: true }
      },
      {
        // attendance automation, home-page thresholds, device keys, demo tools
        path: 'school-settings',
        component: () => import('@/pages/SchoolSettingsPage.vue'),
        meta: { adminOnly: true }
      },
      {
        path: 'activity',
        component: () => import('@/pages/ActivityLogPage.vue'),
        meta: { editorOnly: true }
      },
      {
        path: 'profiles/student/:id',
        component: () => import('@/pages/StudentProfilePage.vue')
      },
      {
        path: 'profiles/teacher/:id',
        component: () => import('@/pages/TeacherProfilePage.vue')
      },
      {
        path: 'profiles/class/:id',
        component: () => import('@/pages/ClassProfilePage.vue')
      }
    ]
  },

  // Always leave this as last one,
  // but you can also remove it
  {
    path: '/:catchAll(.*)*',
    component: () => import('@/pages/ErrorNotFound.vue')
  }
]

export default routes
