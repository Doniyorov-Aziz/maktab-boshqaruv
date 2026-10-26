<template>
  <page-layout no-header>
    <div class="locked" data-testid="guest-locked" role="alert">
      <div class="locked__icon" aria-hidden="true">
        <q-icon name="lock" size="40px" />
      </div>
      <h1 class="locked__title">Bu bo'lim yopiq</h1>
      <p class="locked__text">
        Uzr, sizga bu bo'limga kirishga ruxsat yo'q. Ota-onalar yozishmalari
        maxfiy ma'lumot hisoblanadi.
      </p>
      <p v-if="section" class="locked__section">{{ section }}</p>
      <div class="locked__actions">
        <AppButton label="Bosh sahifaga" icon="dashboard" to="/" />
        <AppButton
          variant="secondary"
          label="Orqaga"
          icon="arrow_back"
          @click="$router.back()"
        />
      </div>
    </div>
  </page-layout>
</template>

<script setup>
// What the demo visitor sees instead of a closed section ("Murojaatlar", Telegram history,
// "Ota-onalarga xabar", visit statistics): the menu keeps the item with a lock, this page explains.
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import PageLayout from '@/components/PageLayout.vue'
import AppButton from '@/components/app/AppButton.vue'

const route = useRoute()

const NAMES = {
  '/parent-messages': 'Murojaatlar (Maktabga xat)',
  '/absence-requests': 'Sababli arizalar',
  '/notifications': 'Xabarnomalar (Telegram tarixi)',
  '/broadcasts': 'Ota-onalarga xabar',
  '/visits': 'Tashriflar'
}
const section = computed(() => {
  const from = String(route.query.from || '').split('?')[0]
  return NAMES[from] || ''
})
</script>

<style scoped>
.locked {
  max-width: var(--dialog-w-md);
  margin: var(--space-6) auto;
  padding: var(--space-6) var(--space-5);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-3);
  text-align: center;
  background: var(--card-bg);
  border: 1px solid var(--brand-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

.locked__icon {
  width: 80px;
  height: 80px;
  border-radius: var(--radius-pill);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-warning);
  background: color-mix(in srgb, var(--color-warning) 14%, transparent);
}

.locked__title {
  margin: 0;
  font-size: var(--text-xl);
  font-weight: 700;
}

.locked__text {
  margin: 0;
  max-width: 44ch;
  color: var(--brand-text-muted);
  line-height: 1.55;
}

.locked__section {
  margin: 0;
  font-size: var(--text-sm);
  font-weight: 600;
}

.locked__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: var(--space-2);
  margin-top: var(--space-2);
}
</style>
