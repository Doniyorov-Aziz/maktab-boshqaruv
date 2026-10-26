import { defineRouter } from '#q-app'
import {
  createMemoryHistory,
  createRouter,
  createWebHashHistory,
  createWebHistory
} from 'vue-router'
import { LoadingBar } from 'quasar'

import routes from './routes.js'
import { useAuthStore, expired } from '@/stores/auth'

/*
 * If not building with SSR mode, you can
 * directly export the Router instantiation;
 *
 * The function below can be async too; either use
 * async/await or return a Promise which resolves
 * with the Router instance.
 */

export default defineRouter((/* { store, ssrContext } */) => {
  const createHistory = import.meta.env.QUASAR_SERVER
    ? createMemoryHistory
    : import.meta.env.QUASAR_VUE_ROUTER_MODE === 'history'
      ? createWebHistory
      : createWebHashHistory

  const Router = createRouter({
    scrollBehavior: () => ({ left: 0, top: 0 }),
    routes,

    // Leave this as is and make changes in quasar.conf.js instead!
    // quasar.conf.js -> build -> vueRouterMode
    // quasar.conf.js -> build -> publicPath
    history: createHistory(import.meta.env.QUASAR_VUE_ROUTER_BASE)
  })

  Router.beforeEach(to => {
    const authStore = useAuthStore()
    const isLoginRoute = to.path === '/login'

    // The Telegram Mini App authenticates with Telegram's signed initData, not the admin login.
    if (to.meta.public) {
      LoadingBar.start()
      return true
    }

    // an expired token (the demo visitor's lasts 8 hours): the demo starts over, staff sign in again
    if (authStore.token && expired(authStore.token)) {
      const wasGuest = authStore.isGuest
      authStore.logout()
      if (wasGuest && !isLoginRoute) return '/demo'
    }

    if (!authStore.isAuthenticated && !isLoginRoute) {
      return '/login'
    }
    if (authStore.isAuthenticated && isLoginRoute && !authStore.isGuest) {
      return '/'
    }
    // the demo visitor sees these sections in the menu (with a lock) but not their content
    if (authStore.isGuest && to.meta.guestLocked) {
      return { path: '/locked', query: { from: to.fullPath } }
    }
    if (to.meta.realAdminOnly && !authStore.isRealAdmin) {
      return '/'
    }
    if (to.meta.editorOnly && !authStore.isEditor) {
      return '/'
    }
    if (to.meta.adminOnly && !authStore.isAdmin) {
      return '/'
    }
    // Only start the bar for navigations that actually proceed — starting it
    // on every intermediate redirect step left it started more times than
    // afterEach (which only fires once, for the final navigation) could stop,
    // so it never visually finished and the whole app felt stuck loading.
    LoadingBar.start()
    return true
  })

  Router.afterEach(() => {
    LoadingBar.stop()
  })

  Router.onError(() => {
    LoadingBar.stop()
  })

  return Router
})
