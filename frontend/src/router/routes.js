const routes = [
  {
    path: '/login',
    component: () => import('@/pages/LoginPage.vue')
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
