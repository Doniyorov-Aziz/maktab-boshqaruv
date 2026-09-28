<template>
  <q-layout view="lHh Lpr lFf">
    <q-header elevated class="app-surface header-shadow">
      <q-toolbar class="q-py-xs">
        <q-btn
          flat
          dense
          round
          icon="menu"
          aria-label="Menu"
          @click="drawerOpen = !drawerOpen"
        />

        <q-breadcrumbs class="q-ml-sm breadcrumb-muted" active-color="primary">
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

        <q-btn
          flat
          round
          dense
          :icon="$q.dark.isActive ? 'light_mode' : 'dark_mode'"
          class="q-mr-xs"
          @click="toggleDarkMode"
        >
          <q-tooltip>{{
            $q.dark.isActive ? "Yorug' rejim" : "Qorong'u rejim"
          }}</q-tooltip>
        </q-btn>

        <q-btn flat round dense no-caps class="q-pa-xs">
          <q-avatar
            size="34px"
            color="primary"
            text-color="white"
            class="text-weight-bold"
          >
            {{ userInitial }}
          </q-avatar>
          <q-menu anchor="bottom right" self="top right">
            <q-list style="min-width: 200px">
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
              <q-item clickable v-close-popup @click="onLogout">
                <q-item-section avatar>
                  <q-icon name="logout" color="negative" />
                </q-item-section>
                <q-item-section class="text-negative">Chiqish</q-item-section>
              </q-item>
            </q-list>
          </q-menu>
        </q-btn>
      </q-toolbar>
    </q-header>

    <q-drawer v-model="drawerOpen" show-if-above bordered class="app-surface">
      <div class="row items-center q-pa-md q-gutter-sm">
        <q-avatar
          size="38px"
          class="brand-gradient"
          text-color="white"
          icon="school"
        />
        <div>
          <div class="text-subtitle1 text-weight-bold" style="line-height: 1.1">
            Maktab Boshqaruv
          </div>
          <div class="text-caption breadcrumb-muted">Admin panel</div>
        </div>
      </div>

      <q-separator />

      <q-scroll-area style="height: calc(100% - 76px)">
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
            <q-item-section>Bosh sahifa</q-item-section>
          </q-item>

          <template v-for="group in groupedModules" :key="group.name">
            <q-item-label header class="text-weight-semibold nav-group-title">{{
              group.name
            }}</q-item-label>
            <q-item
              v-for="item in group.items"
              :key="item.key"
              clickable
              :to="`/app/${item.key}`"
              exact
              active-class="nav-item--active"
              class="nav-item"
            >
              <q-item-section avatar>
                <q-icon :name="item.icon" />
              </q-item-section>
              <q-item-section>{{ item.title }}</q-item-section>
            </q-item>
          </template>
        </q-list>
      </q-scroll-area>
    </q-drawer>

    <q-page-container>
      <router-view v-slot="{ Component }">
        <transition name="content-fade" mode="out-in">
          <component :is="Component" :key="route.path" />
        </transition>
      </router-view>
    </q-page-container>
  </q-layout>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useQuasar } from 'quasar'
import { modules, getModule } from '@/config/modules'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const $q = useQuasar()
const authStore = useAuthStore()
const drawerOpen = ref(true)

function toggleDarkMode() {
  $q.dark.toggle()
  try {
    localStorage.setItem('darkMode', String($q.dark.isActive))
  } catch {
    // localStorage unavailable — toggle still works for this session
  }
}

const visibleModules = computed(() =>
  modules.filter(m => !m.adminOnly || authStore.isAdmin)
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
  return groups
})

const currentModule = computed(() =>
  route.params.moduleKey ? getModule(route.params.moduleKey) : null
)

const userInitial = computed(
  () => authStore.username?.charAt(0).toUpperCase() || '?'
)

const roleColor = computed(() => {
  if (authStore.role === 'ADMIN') return 'negative'
  if (authStore.role === 'EDITOR') return 'primary'
  return 'grey-7'
})

function onLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.header-shadow {
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.06);
}

.breadcrumb-muted {
  color: var(--brand-text-muted);
}

.nav-group-title {
  color: var(--brand-text-muted);
}

.nav-item {
  border-radius: 10px;
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
  border-radius: 4px;
  background: var(--q-primary);
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
</style>
