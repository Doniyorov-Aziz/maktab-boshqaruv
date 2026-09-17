<template>
  <q-layout>
    <q-page-container>
      <q-page class="flex flex-center bg-grey-2">
        <q-card class="q-pa-lg" style="width: 100%; max-width: 380px">
          <q-card-section class="text-center">
            <div class="text-h5">Maktab boshqaruv</div>
            <div class="text-caption text-grey">Tizimga kirish</div>
          </q-card-section>

          <q-card-section>
            <q-form class="q-gutter-md" @submit.prevent="onSubmit">
              <q-input
                v-model="username"
                label="Username"
                filled
                autofocus
                :rules="[(val) => !!val || 'Majburiy maydon']"
              />
              <q-input
                v-model="password"
                label="Parol"
                type="password"
                filled
                :rules="[(val) => !!val || 'Majburiy maydon']"
              />

              <div v-if="errorMessage" class="text-negative text-caption">
                {{ errorMessage }}
              </div>

              <q-btn
                type="submit"
                color="primary"
                label="Kirish"
                class="full-width"
                :loading="loading"
                unelevated
              />
            </q-form>
          </q-card-section>
        </q-card>
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

async function onSubmit() {
  errorMessage.value = ''
  loading.value = true
  try {
    const response = await api.post(
      '/api/auth/login',
      { username: username.value, password: password.value },
      { responseType: 'text' },
    )
    authStore.setToken(response.data)
    router.push('/')
  } catch (error) {
    errorMessage.value = error.response?.data || 'Kirishda xatolik yuz berdi'
  } finally {
    loading.value = false
  }
}
</script>
