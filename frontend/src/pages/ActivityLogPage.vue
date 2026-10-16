<template>
  <page-layout
    icon="history"
    color="#0f766e"
    title="Faollik jurnali"
    subtitle="Tizimda kim nima qildi — yangilari tepada"
  >
    <div class="brand-card">
      <q-list separator>
        <q-item v-for="a in rows" :key="a.id">
          <q-item-section avatar>
            <IconTile
              :icon="a.icon || 'history'"
              size="32"
              color="var(--surface-2)"
              text-color="var(--chart-primary)"
            />
          </q-item-section>
          <q-item-section>
            <q-item-label>{{ a.description }}</q-item-label>
            <q-item-label caption>{{
              a.actor ? `${a.actor} tomonidan` : 'Tizim'
            }}</q-item-label>
          </q-item-section>
          <q-item-section side class="activity__time">{{
            fmt(a.occurredAt)
          }}</q-item-section>
        </q-item>
        <q-item v-if="!loading && !rows.length">
          <q-item-section class="activity__empty"
            >Hali yozuv yo'q</q-item-section
          >
        </q-item>
      </q-list>
      <div class="activity__more">
        <AppButton
          v-if="!last"
          variant="secondary"
          label="Yana yuklash"
          :loading="loading"
          @click="load(page + 1)"
        />
      </div>
    </div>
  </page-layout>
</template>

<script setup>
// "Faollik jurnali": every recorded action of the school (moved here from the home page).
import { ref, watch, onMounted } from 'vue'
import PageLayout from '@/components/PageLayout.vue'
import AppButton from '@/components/app/AppButton.vue'
import IconTile from '@/components/app/IconTile.vue'
import { api } from '@/boot/axios'
import { useSchoolStore } from '@/stores/school'

const school = useSchoolStore()
const rows = ref([])
const page = ref(0)
const last = ref(true)
const loading = ref(false)
const fmt = iso => (iso ? String(iso).replace('T', ' ').slice(0, 16) : '')

async function load(p = 0) {
  if (!school.activeSchoolId) return
  loading.value = true
  try {
    const res = await api.get('/api/dashboard/activity', {
      params: { schoolId: school.activeSchoolId, page: p, size: 30 }
    })
    rows.value =
      p === 0 ? res.data.content : [...rows.value, ...res.data.content]
    page.value = p
    last.value = res.data.last
  } finally {
    loading.value = false
  }
}

onMounted(() => load())
watch(
  () => school.activeSchoolId,
  () => load()
)
</script>

<style scoped>
.activity__time {
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
  white-space: nowrap;
}

.activity__empty {
  color: var(--brand-text-muted);
}

.activity__more {
  display: flex;
  justify-content: center;
  padding: var(--space-3);
}
</style>
