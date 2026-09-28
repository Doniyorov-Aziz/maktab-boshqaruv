<template>
  <router-view v-slot="{ Component }">
    <transition name="page-fade" mode="out-in">
      <component :is="Component" />
    </transition>
  </router-view>
</template>

<script setup>
import { onMounted } from 'vue'
import { useQuasar } from 'quasar'

const $q = useQuasar()

onMounted(() => {
  try {
    const saved = localStorage.getItem('darkMode')
    if (saved !== null) {
      $q.dark.set(saved === 'true')
    }
  } catch {
    // localStorage unavailable — keep default light theme
  }
})
</script>

<style>
.page-fade-enter-active,
.page-fade-leave-active {
  transition:
    opacity 0.18s ease,
    transform 0.18s ease;
}

.page-fade-enter-from {
  opacity: 0;
  transform: translateY(6px);
}

.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>
