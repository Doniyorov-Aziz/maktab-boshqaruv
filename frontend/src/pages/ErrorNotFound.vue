<template>
  <q-layout>
    <q-page-container>
      <q-page class="not-found flex flex-center column text-center">
        <IconTile icon="explore_off" size="48" />
        <div class="text-h2 text-weight-bold q-mt-lg">404</div>
        <div class="text-h6 q-mt-sm q-mb-lg not-found__text">
          Bunday sahifa topilmadi
        </div>
        <AppButton icon="home" label="Bosh sahifaga qaytish" to="/" />
      </q-page>
    </q-page-container>
  </q-layout>
</template>

<script setup>
import AppButton from '@/components/app/AppButton.vue'
import IconTile from '@/components/app/IconTile.vue'
</script>

<style scoped>
.not-found {
  min-height: 100vh;
  padding: var(--page-pad);
}

.not-found__text {
  color: var(--brand-text-muted);
}
</style>
