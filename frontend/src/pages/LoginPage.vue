<template>
  <q-layout>
    <q-page-container>
      <q-page class="login-page row">
        <div class="col-12 col-md-6 login-page__brand brand-gradient">
          <div class="login-page__brand-content">
            <div class="row items-center q-gutter-sm q-mb-xl">
              <q-avatar
                size="42px"
                color="white"
                text-color="primary"
                icon="school"
              />
              <div class="text-h6 text-white text-weight-bold"
                >Maktab Boshqaruv</div
              >
            </div>

            <div
              class="text-h3 text-white text-weight-bold q-mb-md"
              style="max-width: 480px"
            >
              Maktabingizni bitta joydan boshqaring
            </div>
            <div
              class="text-body1 text-white"
              style="max-width: 440px; opacity: 0.88"
            >
              O'quvchilar, xodimlar, sinflar va dars jadvalini — barchasini
              tartibli, xavfsiz va tezkor tizimda nazorat qiling.
            </div>

            <div class="row q-col-gutter-md q-mt-xl">
              <div v-for="stat in brandStats" :key="stat.label" class="col-4">
                <div class="text-h5 text-white text-weight-bold">{{
                  stat.value
                }}</div>
                <div class="text-caption text-white" style="opacity: 0.75">{{
                  stat.label
                }}</div>
              </div>
            </div>
          </div>

          <div class="login-page__blob login-page__blob--1" />
          <div class="login-page__blob login-page__blob--2" />
        </div>

        <div class="col-12 col-md-6 flex flex-center login-page__form-side">
          <q-card flat class="login-page__card">
            <q-card-section>
              <div class="text-h5 text-weight-bold">Xush kelibsiz</div>
              <div class="text-body2 text-grey-7 q-mt-xs">
                Davom etish uchun akkauntingizga kiring
              </div>
            </q-card-section>

            <q-card-section>
              <q-form class="q-gutter-md" @submit.prevent="onSubmit">
                <q-input
                  v-model="username"
                  label="Username"
                  outlined
                  autofocus
                  :rules="[val => !!val || 'Majburiy maydon']"
                >
                  <template v-slot:prepend>
                    <q-icon name="person_outline" />
                  </template>
                </q-input>

                <q-input
                  v-model="password"
                  label="Parol"
                  :type="showPassword ? 'text' : 'password'"
                  outlined
                  :rules="[val => !!val || 'Majburiy maydon']"
                >
                  <template v-slot:prepend>
                    <q-icon name="lock_outline" />
                  </template>
                  <template v-slot:append>
                    <q-icon
                      :name="showPassword ? 'visibility_off' : 'visibility'"
                      class="cursor-pointer"
                      @click="showPassword = !showPassword"
                    />
                  </template>
                </q-input>

                <q-banner
                  v-if="errorMessage"
                  class="bg-red-1 text-negative rounded-borders"
                  dense
                >
                  <template v-slot:avatar>
                    <q-icon name="error_outline" color="negative" />
                  </template>
                  {{ errorMessage }}
                </q-banner>

                <q-btn
                  type="submit"
                  color="primary"
                  label="Kirish"
                  class="full-width q-py-sm"
                  size="lg"
                  unelevated
                  no-caps
                  :loading="loading"
                />
              </q-form>
            </q-card-section>
          </q-card>
        </div>
      </q-page>
    </q-page-container>
  </q-layout>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '@/boot/axios'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const username = ref('')
const password = ref('')
const loading = ref(false)
const errorMessage = ref('')
const showPassword = ref(false)

const brandStats = [
  { value: '16', label: 'Modul' },
  { value: '3', label: 'Rol darajasi' },
  { value: '100%', label: 'Nazorat' }
]

async function onSubmit() {
  if (loading.value) return // guards against a duplicate submit firing (e.g. Enter + button both resolving)
  errorMessage.value = ''
  loading.value = true
  try {
    const response = await api.post(
      '/api/auth/login',
      { username: username.value, password: password.value },
      { responseType: 'text' }
    )
    authStore.setToken(response.data)
    await router.push('/')
  } catch (error) {
    errorMessage.value = error.friendlyMessage || 'Kirishda xatolik yuz berdi'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
}

.login-page__brand {
  position: relative;
  display: flex;
  align-items: center;
  padding: 48px;
  overflow: hidden;
  min-height: 320px;
}

.login-page__brand-content {
  position: relative;
  z-index: 1;
}

.login-page__blob {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
}

.login-page__blob--1 {
  width: 420px;
  height: 420px;
  right: -140px;
  bottom: -160px;
}

.login-page__blob--2 {
  width: 220px;
  height: 220px;
  left: -60px;
  top: -60px;
}

.login-page__form-side {
  padding: 32px;
}

.login-page__card {
  width: 100%;
  max-width: 400px;
  animation: loginCardEnter 0.45s ease both;
}

@keyframes loginCardEnter {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 1023px) {
  .login-page__brand {
    padding: 32px 24px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .login-page__card {
    animation: none;
  }
}
</style>
