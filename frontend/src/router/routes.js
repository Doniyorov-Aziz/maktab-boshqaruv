const routes = [
  {
    path: '/login',
    component: () => import('@/pages/LoginPage.vue')
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
