<template>
  <div class="demo-entry" data-testid="demo-entry">
    <div class="demo-entry__card app-surface">
      <IconTile icon="visibility" size="56" />
      <template v-if="!error">
        <h1 class="demo-entry__title">Demo yuklanmoqda…</h1>
        <p class="demo-entry__text"
          >Maktab boshqaruv tizimi bilan mehmon sifatida tanishasiz: barcha
          bo'limlarni ko'rishingiz mumkin, o'zgartirish o'chirilgan.</p
        >
        <q-spinner size="32px" color="primary" />
      </template>
      <template v-else>
        <h1 class="demo-entry__title">{{ error.title }}</h1>
        <p class="demo-entry__text">{{ error.text }}</p>
        <div class="demo-entry__actions">
          <AppButton
            v-if="error.retry"
            label="Qayta urinish"
            icon="refresh"
            @click="enter"
          />
          <AppButton variant="secondary" label="Kirish sahifasi" to="/login" />
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
// #/demo?ref=linkedin — the public demo link. Signs in as the read-only "Mehmon" (no password,
// 8 hours), remembers where the link came from for the visit count, and opens the home page.
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { LoadingBar } from 'quasar'
import AppButton from '@/components/app/AppButton.vue'
import IconTile from '@/components/app/IconTile.vue'
import { api } from '@/boot/axios'
import { useAuthStore } from '@/stores/auth'
import { visitorId, setDemoRef } from '@/utils/visitor'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const error = ref(null)

async function enter() {
  error.value = null
  const ref = String(route.query.ref || 'direct')
  try {
    // a staff member already signed in stays who they are
    if (!auth.isAuthenticated || auth.isGuest) {
      const res = await api.post(
        '/api/auth/guest-login',
        { visitorId: visitorId(), ref },
        { responseType: 'text' }
      )
      auth.setToken(String(res.data).trim())
    }
    setDemoRef(ref)
    await router.replace('/')
  } catch (e) {
    const status = e.response?.status
    error.value =
      status === 404
        ? {
            title: "Demo hozircha o'chirilgan",
            text: "Bu havola orqali kirish vaqtincha yopilgan. Login va parolingiz bo'lsa, kirish sahifasidan foydalaning.",
            retry: false
          }
        : status === 429
          ? {
              title: "Juda ko'p urinish",
              text: "Bir manzildan juda ko'p kirildi. Birozdan so'ng qayta urinib ko'ring.",
              retry: true
            }
          : {
              title: "Serverga ulanib bo'lmadi",
              text: e.friendlyMessage || 'Birozdan so‘ng qayta urinib ko‘ring.',
              retry: true
            }
  } finally {
    LoadingBar.stop()
  }
}

onMounted(enter)
</script>

<style scoped>
.demo-entry {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--page-pad);
  background: var(--page-bg);
}

.demo-entry__card {
  width: 100%;
  max-width: var(--dialog-w-sm);
  border: 1px solid var(--brand-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  padding: var(--space-6) var(--space-5);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-3);
  text-align: center;
}

.demo-entry__title {
  margin: 0;
  font-size: var(--text-xl);
  font-weight: 700;
  line-height: 1.25;
}

.demo-entry__text {
  margin: 0;
  color: var(--brand-text-muted);
  font-size: var(--text-sm);
  line-height: 1.5;
}

.demo-entry__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: var(--space-2);
}
</style>
