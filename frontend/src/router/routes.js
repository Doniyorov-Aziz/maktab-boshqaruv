const routes = [
  {
    path: '/login',
    component: () => import('@/pages/LoginPage.vue')
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
        path: 'notifications',
        component: () => import('@/pages/NotificationsPage.vue'),
        meta: { editorOnly: true }
      },
      {
        path: 'parent-messages',
        component: () => import('@/pages/ParentMessagesPage.vue'),
        meta: { editorOnly: true }
      },
      {
        path: 'absence-requests',
        component: () => import('@/pages/AbsenceRequestsPage.vue'),
        meta: { editorOnly: true }
      },
      {
        path: 'broadcasts',
        component: () => import('@/pages/BroadcastPage.vue'),
        meta: { adminOnly: true }
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
