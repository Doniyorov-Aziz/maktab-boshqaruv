<template>
  <div>
    <div v-if="!items" class="wa-card"
      ><q-skeleton v-for="i in 4" :key="i" type="text"
    /></div>
    <div v-else-if="!items.length" class="wa-card wa-empty">{{
      t('no_announcements')
    }}</div>
    <div
      v-for="a in items || []"
      :key="a.id"
      class="wa-card ann"
      :class="{ important: a.important }"
      @click="toggle(a.id)"
    >
      <div class="row items-center q-gutter-x-xs q-mb-xs">
        <span v-if="a.unread" class="wa-pill new">🆕 {{ t('new') }}</span>
        <span v-if="a.important" class="wa-pill imp"
          >🔴 {{ t('important') }}</span
        >
        <q-space />
        <span class="wa-hint">{{
          a.date ? shortDate(state.lang, a.date) : ''
        }}</span>
      </div>
      <div class="text-weight-bold title">{{ a.title }}</div>
      <div v-if="a.classLabel" class="wa-hint"
        >{{ a.classLabel }} {{ t('for_class') }}</div
      >
      <div class="content" :class="{ clamp: open !== a.id }">{{
        a.content
      }}</div>
      <div v-if="a.deadline && open === a.id" class="q-mt-sm">
        ⏰ <b>{{ t('deadline') }}:</b> {{ fullDate(state.lang, a.deadline) }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { parentApi } from '@/webapp/api'
import { state, t } from '@/webapp/state'
import { fullDate, shortDate } from '@/webapp/i18n'
import { haptic } from '@/webapp/telegram'

const items = ref(null)
const open = ref(null)

function toggle(id) {
  haptic()
  open.value = open.value === id ? null : id
}

onMounted(async () => {
  items.value = (
    await parentApi.get(`/api/parent/students/${state.childId}/announcements`)
  ).data
})
</script>

<style scoped>
.ann {
  cursor: pointer;
}

.ann.important {
  border-left: 4px solid var(--wa-bad);
}

.title {
  font-size: 16px;
}

.content {
  margin-top: 6px;
  white-space: pre-wrap;
  line-height: 1.45;
}

.clamp {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.new {
  background: rgba(79, 70, 229, 0.14);
  color: var(--wa-accent);
}

.imp {
  background: rgba(239, 68, 68, 0.14);
  color: var(--wa-bad);
}
</style>
