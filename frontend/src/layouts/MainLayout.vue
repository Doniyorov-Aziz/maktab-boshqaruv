<template>
  <q-layout view="lHh Lpr lFf">
    <q-header elevated>
      <q-toolbar>
        <q-btn flat dense round icon="menu" aria-label="Menu" @click="drawerOpen = !drawerOpen" />

        <q-toolbar-title>Maktab boshqaruv</q-toolbar-title>

        <div class="q-mr-md text-caption">
          {{ authStore.username }}
          <q-badge outline color="white" class="q-ml-xs">{{ authStore.role }}</q-badge>
        </div>

        <q-btn flat dense round icon="logout" aria-label="Chiqish" @click="onLogout" />
      </q-toolbar>
    </q-header>

    <q-drawer v-model="drawerOpen" show-if-above bordered>
      <q-list>
        <q-item-label header>Modullar</q-item-label>

        <q-item
          v-for="item in visibleModules"
          :key="item.key"
          clickable
          :to="`/app/${item.key}`"
          exact
        >
          <q-item-section avatar>
            <q-icon :name="item.icon" />
          </q-item-section>
          <q-item-section>{{ item.title }}</q-item-section>
        </q-item>
      </q-list>
    </q-drawer>

    <q-page-container>
      <router-view />
    </q-page-container>
  </q-layout>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { modules } from '@/config/modules'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const drawerOpen = ref(true)

const visibleModules = computed(() =>
  modules.filter((m) => !m.adminOnly || authStore.isAdmin),
)

function onLogout() {
  authStore.logout()
  router.push('/login')
}
</script>
