<template>
  <q-layout view="lHh Lpr lFf">
    <q-header class="app-surface app-header">
      <q-toolbar class="app-header__bar">
        <AppButton
          variant="ghost"
          :icon="menuIcon"
          aria-label="Menu"
          @click="toggleMenu"
        >
          <q-tooltip>{{
            $q.screen.gt.sm ? "Menyuni yig'ish" : 'Menyu'
          }}</q-tooltip>
        </AppButton>

        <q-breadcrumbs
          class="q-ml-sm breadcrumb-muted gt-xs"
          active-color="primary"
        >
          <template v-slot:separator>
            <q-icon
              size="1.2em"
              name="chevron_right"
              class="breadcrumb-muted"
            />
          </template>
          <q-breadcrumbs-el label="Maktab Boshqaruv" icon="dashboard" to="/" />
          <q-breadcrumbs-el v-if="currentModule" :label="currentModule.title" />
        </q-breadcrumbs>

        <q-space />

        <AppButton
          variant="ghost"
          icon="search"
          class="q-mr-xs"
          @click="openSearch"
        >
          <q-tooltip>Qidirish (Ctrl+K)</q-tooltip>
        </AppButton>

        <AppButton variant="ghost" icon="notifications" class="q-mr-xs">
          <q-badge v-if="notificationCount" color="negative" floating rounded>{{
            notificationCount
          }}</q-badge>
          <q-menu anchor="bottom right" self="top right">
            <q-list class="menu-list menu-list--lg">
              <q-item-label header>Bildirishnomalar</q-item-label>
              <q-item v-if="!notifications.length">
                <q-item-section class="muted-text"
                  >Yangi bildirishnoma yo'q</q-item-section
                >
              </q-item>
              <q-item
                v-for="n in notifications"
                :key="n.key"
                clickable
                v-close-popup
                :class="{ 'bell-item--unread': n.unread }"
                @click="goNotification(n)"
              >
                <q-item-section avatar>
                  <q-icon :name="n.icon" :style="{ color: n.color }" />
                </q-item-section>
                <q-item-section>
                  <q-item-label>{{ n.text }}</q-item-label>
                  <q-item-label v-if="n.caption" caption>{{
                    n.caption
                  }}</q-item-label>
                </q-item-section>
              </q-item>
              <q-item
                v-if="staffUnread"
                clickable
                v-close-popup
                @click="markAllRead"
              >
                <q-item-section class="text-primary text-weight-medium"
                  >Hammasini o'qilgan deb belgilash</q-item-section
                >
              </q-item>
            </q-list>
          </q-menu>
        </AppButton>

        <AppButton
          variant="ghost"
          v-if="schoolStore.activeSchoolName"
          icon="school"
          :label="schoolStore.activeSchoolName"
          icon-right="expand_more"
          class="q-mr-sm school-picker-btn"
        >
          <q-menu anchor="bottom right" self="top right">
            <q-list class="menu-list">
              <q-item-label header>Maktabni tanlang</q-item-label>
              <q-item
                v-for="s in schoolStore.schools"
                :key="s.id"
                clickable
                v-close-popup
                :active="s.id === schoolStore.activeSchoolId"
                active-class="text-primary"
                @click="onSelectSchool(s)"
              >
                <q-item-section avatar>
                  <q-icon
                    :name="
                      s.id === schoolStore.activeSchoolId
                        ? 'radio_button_checked'
                        : 'radio_button_unchecked'
                    "
                  />
                </q-item-section>
                <q-item-section>{{ s.name }}</q-item-section>
              </q-item>
              <q-separator />
              <q-item clickable v-close-popup to="/app/schools">
                <q-item-section avatar>
                  <q-icon name="settings" />
                </q-item-section>
                <q-item-section>Maktablarni boshqarish</q-item-section>
              </q-item>
            </q-list>
          </q-menu>
        </AppButton>

        <AppButton
          variant="ghost"
          :icon="$q.dark.isActive ? 'light_mode' : 'dark_mode'"
          class="q-mr-xs"
          @click="toggleDarkMode"
        >
          <q-tooltip>{{
            $q.dark.isActive ? "Yorug' rejim" : "Qorong'u rejim"
          }}</q-tooltip>
        </AppButton>

        <AppButton variant="ghost" square aria-label="Hisob">
          <q-avatar
            size="var(--avatar-sm)"
            color="primary"
            text-color="white"
            class="text-weight-bold"
          >
            {{ userInitial }}
          </q-avatar>
          <q-menu anchor="bottom right" self="top right">
            <q-list class="menu-list">
              <q-item>
                <q-item-section>
                  <q-item-label class="text-weight-semibold">{{
                    authStore.username
                  }}</q-item-label>
                  <q-item-label caption>
                    <q-badge :color="roleColor" outline>{{
                      authStore.role
                    }}</q-badge>
                  </q-item-label>
                </q-item-section>
              </q-item>
              <q-separator />
              <q-item clickable v-close-popup @click="accountDialogOpen = true">
                <q-item-section avatar>
                  <q-icon name="person" />
                </q-item-section>
                <q-item-section>Profil</q-item-section>
              </q-item>
              <q-item clickable v-close-popup @click="accountDialogOpen = true">
                <q-item-section avatar>
                  <q-icon name="settings" />
                </q-item-section>
                <q-item-section>Sozlamalar</q-item-section>
              </q-item>
              <q-separator />
              <q-item clickable v-close-popup @click="onLogout">
                <q-item-section avatar>
                  <q-icon name="logout" color="negative" />
                </q-item-section>
                <q-item-section class="text-negative">Chiqish</q-item-section>
              </q-item>
            </q-list>
          </q-menu>
        </AppButton>
      </q-toolbar>
    </q-header>

    <q-dialog v-model="accountDialogOpen">
      <q-card class="app-dialog app-dialog--sm">
        <q-card-section class="row items-center q-gutter-md">
          <q-avatar
            size="var(--avatar-lg)"
            color="primary"
            text-color="white"
            class="text-weight-bold text-h6"
          >
            {{ userInitial }}
          </q-avatar>
          <div>
            <div class="text-subtitle1 text-weight-bold">{{
              authStore.username
            }}</div>
            <q-badge :color="roleColor" outline>{{ authStore.role }}</q-badge>
          </div>
        </q-card-section>
        <q-separator />
        <q-card-section>
          <div class="text-caption muted-text q-mb-xs">Faol maktab</div>
          <div class="text-body2">{{
            schoolStore.activeSchoolName || '—'
          }}</div>
        </q-card-section>
        <q-separator />
        <q-card-section>
          <div class="row items-center justify-between q-mb-sm">
            <div class="text-body2">Qorong'u rejim</div>
            <q-toggle
              :model-value="$q.dark.isActive"
              @update:model-value="toggleDarkMode"
            />
          </div>
          <div class="row items-center justify-between">
            <div class="text-body2">Yig'ilgan menyu</div>
            <q-toggle :model-value="mini" @update:model-value="toggleMini" />
          </div>
        </q-card-section>
        <q-card-actions align="right">
          <AppButton variant="ghost" label="Yopish" v-close-popup />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <q-drawer
      v-model="drawerOpen"
      show-if-above
      bordered
      class="app-surface"
      :mini="mini"
      :width="240"
      :mini-width="64"
    >
      <div class="row items-center q-pa-md q-gutter-sm no-wrap">
        <IconTile icon="school" size="40" />
        <div v-if="!mini">
          <div class="text-subtitle1 text-weight-bold" style="line-height: 1.1"
            >Maktab Boshqaruv</div
          >
          <div class="text-caption breadcrumb-muted">Admin panel</div>
        </div>
      </div>

      <q-separator />

      <q-scroll-area class="drawer-scroll">
        <q-list padding>
          <q-item
            clickable
            exact
            to="/"
            active-class="nav-item--active"
            class="nav-item"
          >
            <q-item-section avatar>
              <q-icon name="dashboard" />
            </q-item-section>
            <q-item-section v-if="!mini">Bosh sahifa</q-item-section>
            <q-tooltip v-if="mini" anchor="center right" self="center left"
              >Bosh sahifa</q-tooltip
            >
          </q-item>

          <q-item-label
            v-if="!mini"
            header
            class="text-weight-semibold nav-group-title"
            >Kundalik ish</q-item-label
          >
          <q-item
            v-for="item in dailyNavItems"
            :key="item.path"
            clickable
            :to="item.path"
            exact
            active-class="nav-item--active"
            class="nav-item"
          >
            <q-item-section avatar>
              <q-icon :name="item.icon" :style="{ color: item.color }" />
            </q-item-section>
            <q-item-section v-if="!mini">{{ item.title }}</q-item-section>
            <q-tooltip v-if="mini" anchor="center right" self="center left">{{
              item.title
            }}</q-tooltip>
          </q-item>

          <template v-for="group in groupedModules" :key="group.name">
            <q-item-label
              v-if="!mini"
              header
              class="text-weight-semibold nav-group-title"
              >{{ group.name }}</q-item-label
            >
            <q-item
              v-for="item in group.items"
              :key="item.key"
              clickable
              :to="item.path || `/app/${item.key}`"
              exact
              active-class="nav-item--active"
              class="nav-item"
            >
              <q-item-section avatar>
                <q-icon :name="item.icon" :style="{ color: item.color }" />
              </q-item-section>
              <q-item-section v-if="!mini">{{ item.title }}</q-item-section>
              <q-item-section v-if="!mini && item.badge" side>
                <q-badge rounded color="negative">{{ item.badge }}</q-badge>
              </q-item-section>
              <q-badge
                v-if="mini && item.badge"
                rounded
                floating
                color="negative"
                >{{ item.badge }}</q-badge
              >
              <q-tooltip v-if="mini" anchor="center right" self="center left">{{
                item.title
              }}</q-tooltip>
            </q-item>
          </template>
        </q-list>
      </q-scroll-area>
    </q-drawer>

    <q-page-container>
      <div
        v-if="demoMode"
        class="demo-banner"
        role="status"
        data-testid="demo-banner"
      >
        🧪 Demo rejim — qo'lda kiritilmagan davomat va baholar avtomatik
        to'ldirilmoqda
      </div>
      <router-view v-slot="{ Component }">
        <transition name="content-fade" mode="out-in">
          <component :is="Component" :key="route.path" />
        </transition>
      </router-view>
    </q-page-container>

    <q-dialog v-model="searchOpen" position="top">
      <q-card class="app-dialog app-dialog--md">
        <AppInput
          ref="searchInputRef"
          v-model="searchQuery"
          autofocus
          class="q-pa-md"
          placeholder="O'quvchi, o'qituvchi, sinf yoki sahifa qidiring..."
          @keydown.down.prevent="moveSelection(1)"
          @keydown.up.prevent="moveSelection(-1)"
          @keydown.enter.prevent="selectHighlighted"
          @keydown.esc="searchOpen = false"
        >
          <template v-slot:prepend>
            <q-icon name="search" />
          </template>
        </AppInput>
        <q-separator />
        <q-list class="search-results">
          <q-item
            v-for="(r, i) in searchResults"
            :key="r.type + r.id"
            clickable
            :active="i === highlightedIndex"
            active-class="search-result--active"
            @click="selectResult(r)"
          >
            <q-item-section avatar>
              <q-icon :name="r.icon" :color="r.color" />
            </q-item-section>
            <q-item-section>
              <q-item-label>{{ r.label }}</q-item-label>
              <q-item-label caption>{{ r.sublabel }}</q-item-label>
            </q-item-section>
          </q-item>
          <q-item v-if="searchQuery && !searchResults.length">
            <q-item-section class="muted-text"
              >Hech narsa topilmadi</q-item-section
            >
          </q-item>
        </q-list>
      </q-card>
    </q-dialog>
  </q-layout>
</template>

<script setup>
import AppButton from '@/components/app/AppButton.vue'
import IconTile from '@/components/app/IconTile.vue'
import AppInput from '@/components/app/AppInput.vue'
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useQuasar } from 'quasar'
import { modules, getModule } from '@/config/modules'
import { useAuthStore } from '@/stores/auth'
import { useSchoolStore } from '@/stores/school'
import { api } from '@/boot/axios'
import { useSystemInfo } from '@/composables/useSystemInfo'

const router = useRouter()
const route = useRoute()
const $q = useQuasar()
const authStore = useAuthStore()
const schoolStore = useSchoolStore()
const drawerOpen = ref($q.screen.gt.sm)
const mini = ref(localStorage.getItem('sidebarMini') === 'true')
const accountDialogOpen = ref(false)

watch(
  () => $q.screen.gt.sm,
  isDesktop => {
    drawerOpen.value = isDesktop
  }
)

onMounted(async () => {
  const previousSchoolId = schoolStore.activeSchoolId
  try {
    await schoolStore.fetchSchools(api)
  } catch {
    // school list couldn't load — header picker just stays hidden
  }
  // The watch() below already reacts when fetchSchools() changes the active
  // school (fresh login, or a stale cached school got replaced). It only
  // fires on an actual change though, so a returning user whose cached
  // school was still valid needs this explicit call instead — otherwise
  // notifications never load for that case, and calling both unconditionally
  // was firing /api/dashboard/attention twice on every load.
  if (schoolStore.activeSchoolId === previousSchoolId) {
    loadNotifications()
  }
  window.addEventListener('keydown', onGlobalKeydown)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onGlobalKeydown)
})

function onGlobalKeydown(e) {
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
    e.preventDefault()
    openSearch()
  }
}

function toggleMini() {
  mini.value = !mini.value
  try {
    localStorage.setItem('sidebarMini', String(mini.value))
  } catch {
    // ignore
  }
}

// One menu button covers both cases: on mobile it opens/closes the
// overlay drawer, on desktop it collapses the sidebar to icons-only.
function toggleMenu() {
  if ($q.screen.gt.sm) {
    toggleMini()
  } else {
    drawerOpen.value = !drawerOpen.value
  }
}

const menuIcon = computed(() => {
  if ($q.screen.gt.sm) return mini.value ? 'menu_open' : 'menu'
  return 'menu'
})

function onSelectSchool(school) {
  schoolStore.setActiveSchool(school.id, school.name)
}

function toggleDarkMode() {
  $q.dark.toggle()
  try {
    localStorage.setItem('darkMode', String($q.dark.isActive))
  } catch {
    // localStorage unavailable — toggle still works for this session
  }
}

const dailyNavItems = [
  {
    path: '/attendance',
    title: 'Davomat olish',
    icon: 'fact_check',
    color: '#ef4444'
  },
  {
    path: '/attendance/calendar',
    title: 'Davomat kalendari',
    icon: 'calendar_month',
    color: '#f97316'
  },
  {
    path: '/gradebook',
    title: 'Baholar jurnali',
    icon: 'grade',
    color: '#eab308'
  },
  {
    path: '/timetable',
    title: 'Dars jadvali',
    icon: 'calendar_view_week',
    color: '#a855f7'
  },
  {
    path: '/calendar',
    title: 'Taqvim',
    icon: 'event_available',
    color: '#22c55e'
  }
]

const visibleModules = computed(() =>
  modules.filter(m => !m.hidden && (!m.adminOnly || authStore.isAdmin))
)

const groupedModules = computed(() => {
  const groups = []
  for (const item of visibleModules.value) {
    let group = groups.find(g => g.name === item.group)
    if (!group) {
      group = { name: item.group, items: [] }
      groups.push(group)
    }
    group.items.push(item)
  }
  // Dedicated (non-CRUD) admin pages that still belong in the "Boshqaruv" group.
  if (authStore.isEditor) {
    let admin = groups.find(g => g.name === 'Boshqaruv')
    if (!admin) {
      admin = { name: 'Boshqaruv', items: [] }
      groups.push(admin)
    }
    admin.items.push(
      {
        key: 'notifications',
        path: '/notifications',
        title: 'Xabarnomalar',
        icon: 'notifications_active',
        color: '#229ed9'
      },
      {
        key: 'parent-messages',
        path: '/parent-messages',
        title: 'Murojaatlar',
        icon: 'forum',
        color: '#0ea5e9',
        badge: botCounts.value.messages || null
      },
      {
        key: 'absence-requests',
        path: '/absence-requests',
        title: 'Sababli arizalar',
        icon: 'medical_information',
        color: '#f59e0b',
        badge: botCounts.value.absences || null
      },
      {
        key: 'telegram-parents',
        path: '/telegram-parents',
        title: 'Ota-onalar (Telegram)',
        icon: 'family_restroom',
        color: '#229ed9'
      },
      ...(authStore.isAdmin
        ? [
            {
              key: 'broadcasts',
              path: '/broadcasts',
              title: 'Ota-onalarga xabar',
              icon: 'campaign',
              color: '#ec4899'
            }
          ]
        : []),
      {
        key: 'bot-stats',
        path: '/bot-stats',
        title: 'Bot statistikasi',
        icon: 'insights',
        color: '#8b5cf6'
      },
      {
        key: 'bot-settings',
        path: '/bot-settings',
        title: 'Bot sozlamalari',
        icon: 'smart_toy',
        color: '#64748b'
      },
      {
        key: 'activity',
        path: '/activity',
        title: 'Faollik jurnali',
        icon: 'history',
        color: '#0f766e'
      },
      ...(authStore.isAdmin
        ? [
            {
              key: 'school-settings',
              path: '/school-settings',
              title: 'Maktab sozlamalari',
              icon: 'tune',
              color: '#475569'
            }
          ]
        : [])
    )
  }
  // the school library ("📚 Kitob mutolaasi" in the parents' bot)
  if (authStore.isEditor) {
    groups.push({
      name: 'Kutubxona',
      items: [
        {
          key: 'library-books',
          path: '/library/books',
          title: 'Kitoblar',
          icon: 'local_library',
          color: '#6366f1'
        },
        {
          key: 'library-categories',
          path: '/library/categories',
          title: "Bo'limlar",
          icon: 'category',
          color: '#0ea5e9'
        },
        {
          key: 'library-authors',
          path: '/library/authors',
          title: 'Mualliflar',
          icon: 'history_edu',
          color: '#0f766e'
        },
        ...(authStore.isAdmin
          ? [
              {
                key: 'library-permissions',
                path: '/library/permissions',
                title: 'Ruxsatnomalar',
                icon: 'gavel',
                color: '#b45309'
              },
              {
                key: 'library-stats',
                path: '/library/stats',
                title: 'Mutolaa statistikasi',
                icon: 'auto_stories',
                color: '#8b5cf6'
              }
            ]
          : [])
      ]
    })
  }
  // viewers may still look at who is linked to the bot (no chat ids, no invite links)
  if (!authStore.isEditor) {
    groups.push({
      name: 'Boshqaruv',
      items: [
        {
          key: 'telegram-parents',
          path: '/telegram-parents',
          title: 'Ota-onalar (Telegram)',
          icon: 'family_restroom',
          color: '#229ed9'
        }
      ]
    })
  }
  return groups
})

// Unread parent appeal messages / undecided absence requests, shown as sidebar badges.
// Polled every 30 s without the loading bar; a toast says when new appeals arrive.
const botCounts = ref({ messages: 0, absences: 0 })
let countsSchool = null

async function loadBotCounts() {
  if (!schoolStore.activeSchoolId || !authStore.isEditor) return
  const schoolId = schoolStore.activeSchoolId
  try {
    const params = { schoolId }
    const [m, a] = await Promise.all([
      api.get('/api/appeals/unread', { params, background: true }),
      api.get('/api/absence-requests/count-pending', {
        params,
        background: true
      })
    ])
    const before = botCounts.value.messages
    if (
      countsSchool === schoolId &&
      m.data > before &&
      route.path !== '/parent-messages'
    ) {
      $q.notify({
        icon: 'forum',
        color: 'primary',
        message: `Ota-onadan yangi xabar: ${m.data - before} ta`,
        actions: [
          {
            label: "Ko'rish",
            color: 'white',
            handler: () => router.push('/parent-messages')
          }
        ]
      })
    }
    countsSchool = schoolId
    botCounts.value = { messages: m.data, absences: a.data }
  } catch {
    // keep the last known numbers on a network hiccup
  }
}
loadBotCounts()
const countsTimer = setInterval(() => {
  if (!document.hidden) loadBotCounts()
}, 30000)
onBeforeUnmount(() => clearInterval(countsTimer))

const currentModule = computed(() =>
  route.params.moduleKey ? getModule(route.params.moduleKey) : null
)

const userInitial = computed(
  () => authStore.username?.charAt(0).toUpperCase() || '?'
)

const roleColor = computed(() => {
  if (authStore.role === 'ADMIN') return 'grey-7'
  if (authStore.role === 'EDITOR') return 'primary'
  return 'grey-7'
})

function onLogout() {
  authStore.logout()
  router.push('/login')
}

// --- notifications ---
const notifications = ref([])
// demo mode is switched on in application.properties only; the page just shows it
const systemInfo = useSystemInfo()
const demoMode = computed(() => !!systemInfo.value.demoSimulator)
const staffUnread = computed(
  () => notifications.value.filter(n => n.unread).length
)
const notificationCount = computed(() =>
  notifications.value.reduce((sum, n) => sum + (n.unread ? 1 : n.count || 0), 0)
)
const LEVEL_COLOR = {
  red: 'var(--st-absent)',
  orange: 'var(--st-late)',
  yellow: 'var(--gr-3)'
}

async function loadNotifications() {
  if (!schoolStore.activeSchoolId) return
  const [mine, attention] = await Promise.allSettled([
    api.get('/api/staff-notifications', {
      params: { unreadOnly: true },
      background: true
    }),
    api.get('/api/dashboard/attention', {
      params: { schoolId: schoolStore.activeSchoolId },
      background: true
    })
  ])
  const items = []
  if (mine.status === 'fulfilled') {
    for (const n of mine.value.data) {
      items.push({
        key: `s${n.id}`,
        id: n.id,
        unread: true,
        text: n.text,
        icon: 'alarm',
        color: 'var(--st-late)',
        link: n.link,
        caption: String(n.createdAt).replace('T', ' ').slice(0, 16)
      })
    }
  }
  if (attention.status === 'fulfilled') {
    for (const g of attention.value.data.data || []) {
      if (!g.total) continue
      items.push({
        key: `a${g.kind}`,
        text: `${g.title}: ${g.total}`,
        count: g.total,
        icon: 'priority_high',
        color: LEVEL_COLOR[g.level],
        link: '/'
      })
    }
  }
  notifications.value = items
}

watch(
  () => schoolStore.activeSchoolId,
  () => {
    loadNotifications()
    loadBotCounts()
    searchCache.value = null
  }
)

// Refresh the badges whenever the admin moves between pages (e.g. after answering).
watch(() => route.path, loadBotCounts)

async function goNotification(n) {
  if (n.unread) {
    try {
      await api.post('/api/staff-notifications/read', null, {
        params: { id: n.id },
        background: true
      })
    } catch {
      // still open the page
    }
  }
  if (n.link) router.push(n.link)
  loadNotifications()
}

async function markAllRead() {
  try {
    await api.post('/api/staff-notifications/read', null, { background: true })
  } finally {
    loadNotifications()
  }
}

// --- global search ---
const searchOpen = ref(false)
const searchQuery = ref('')
const searchInputRef = ref(null)
const highlightedIndex = ref(0)
const searchCache = ref(null)

const pageEntries = [
  {
    type: 'page',
    id: 'dashboard',
    label: 'Bosh sahifa',
    sublabel: 'Dashboard',
    icon: 'dashboard',
    color: 'primary',
    path: '/'
  },
  {
    type: 'page',
    id: 'attendance',
    label: 'Davomat olish',
    sublabel: 'Sahifa',
    icon: 'fact_check',
    color: 'negative',
    path: '/attendance'
  },
  {
    type: 'page',
    id: 'gradebook',
    label: 'Baholar jurnali',
    sublabel: 'Sahifa',
    icon: 'grade',
    color: 'warning',
    path: '/gradebook'
  },
  {
    type: 'page',
    id: 'timetable',
    label: 'Dars jadvali',
    sublabel: 'Sahifa',
    icon: 'calendar_view_week',
    color: 'purple',
    path: '/timetable'
  },
  {
    type: 'page',
    id: 'calendar',
    label: 'Taqvim',
    sublabel: 'Sahifa',
    icon: 'event_available',
    color: 'positive',
    path: '/calendar'
  },
  ...modules
    .filter(m => !m.hidden)
    .map(m => ({
      type: 'page',
      id: m.key,
      label: m.title,
      sublabel: 'Sahifa',
      icon: m.icon,
      color: 'primary',
      path: m.path || `/app/${m.key}`
    }))
]

async function openSearch() {
  searchOpen.value = true
  searchQuery.value = ''
  highlightedIndex.value = 0
  if (!searchCache.value) {
    await loadSearchCache()
  }
  nextTick(() => searchInputRef.value?.focus())
}

async function loadSearchCache() {
  if (!schoolStore.activeSchoolId) {
    searchCache.value = []
    return
  }
  try {
    const [students, employees, classes] = await Promise.all([
      api.get('/api/students', {
        params: { schoolId: schoolStore.activeSchoolId, size: 1000 }
      }),
      api.get('/api/employees', {
        params: { schoolId: schoolStore.activeSchoolId, size: 500 }
      }),
      api.get('/api/school-classes', {
        params: { schoolId: schoolStore.activeSchoolId, size: 100 }
      })
    ])
    searchCache.value = [
      ...students.data.content.map(s => ({
        type: 'student',
        id: s.id,
        label: s.fullName,
        sublabel: `O'quvchi · ${s.className}-sinf`,
        icon: 'face',
        color: 'cyan',
        path: `/profiles/student/${s.id}`
      })),
      ...employees.data.content.map(e => ({
        type: 'teacher',
        id: e.id,
        label: e.fullName,
        sublabel: `Xodim · ${e.positionTitle}`,
        icon: 'work',
        color: 'pink',
        path: `/profiles/teacher/${e.id}`
      })),
      ...classes.data.content.map(c => ({
        type: 'class',
        id: c.id,
        label: `${c.gradeNumber}-${c.sectionLetter}`,
        sublabel: 'Sinf',
        icon: 'groups',
        color: 'purple',
        path: `/profiles/class/${c.id}`
      }))
    ]
  } catch {
    searchCache.value = []
  }
}

const searchResults = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return pageEntries.slice(0, 8)
  const all = [...(searchCache.value || []), ...pageEntries]
  return all.filter(r => r.label.toLowerCase().includes(q)).slice(0, 20)
})

function moveSelection(dir) {
  const len = searchResults.value.length
  if (!len) return
  highlightedIndex.value = (highlightedIndex.value + dir + len) % len
}

function selectHighlighted() {
  const r = searchResults.value[highlightedIndex.value]
  if (r) selectResult(r)
}

function selectResult(r) {
  searchOpen.value = false
  router.push(r.path)
}
</script>

<style scoped>
.demo-banner {
  background: var(--login-warning-bg);
  color: var(--login-warning-text);
  font-size: var(--text-xs);
  font-weight: 600;
  text-align: center;
  padding: var(--space-1) var(--page-pad);
  border-bottom: 1px solid
    color-mix(in srgb, var(--login-warning-text) 18%, transparent);
}

.bell-item--unread {
  background: color-mix(in srgb, var(--st-late) 8%, transparent);
}

.app-header {
  border-bottom: 1px solid var(--brand-border);
  background: var(--card-bg) !important;
  -webkit-backdrop-filter: saturate(180%) blur(12px);
  backdrop-filter: saturate(180%) blur(12px);
}

.breadcrumb-muted {
  color: var(--brand-text-muted);
}

.muted-text {
  color: var(--brand-text-muted);
}

.app-header__bar {
  min-height: var(--header-h);
  gap: var(--space-1);
}

.menu-list {
  min-width: var(--menu-w);
}

.menu-list--lg {
  max-width: var(--menu-w-lg);
}

.search-results {
  max-height: var(--list-max-h);
  overflow-y: auto;
}

/* the brand row above it: a 40px tile with 16px around it and a hairline */
.drawer-scroll {
  height: calc(100% - var(--tile-md) - 2 * var(--space-4) - var(--hairline));
}

.school-picker-btn {
  max-width: var(--picker-max-w);
}

.school-picker-btn :deep(.q-btn__content) {
  overflow: hidden;
}

.school-picker-btn :deep(.block) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 599px) {
  .school-picker-btn {
    max-width: var(--picker-max-w-sm);
  }
}

.nav-group-title {
  color: var(--brand-text-muted);
  font-size: var(--text-xs);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  padding-left: 24px;
  padding-top: 18px;
  padding-bottom: 4px;
  min-height: 0;
}

.nav-item {
  border-radius: var(--radius-sm);
  margin: 2px 8px;
  color: var(--brand-text-muted);
}

.nav-item--active {
  background: rgba(79, 70, 229, 0.1);
  color: var(--q-primary);
  font-weight: 600;
  position: relative;
}

.nav-item--active::before {
  content: '';
  position: absolute;
  left: -8px;
  top: 8px;
  bottom: 8px;
  width: 3px;
  border-radius: var(--radius-sm);
  background: var(--q-primary);
}

.search-result--active {
  background: rgba(79, 70, 229, 0.1);
}
</style>

<style>
.content-fade-enter-active,
.content-fade-leave-active {
  transition: opacity 0.15s ease;
}

.content-fade-enter-from,
.content-fade-leave-to {
  opacity: 0;
}

@media (prefers-reduced-motion: reduce) {
  .content-fade-enter-active,
  .content-fade-leave-active {
    transition: none;
  }
}
</style>
