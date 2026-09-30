<template>
  <div class="webapp" :class="{ 'webapp--dark': dark }">
    <header class="wa-header">
      <div class="wa-title">
        <span class="wa-logo">📘</span>
        <span>{{ t('title') }}</span>
      </div>
      <div v-if="state.children.length > 1" class="wa-children">
        <button
          v-for="c in state.children"
          :key="c.studentId"
          class="wa-child"
          :class="{ 'wa-child--active': c.studentId === state.childId }"
          @click="pick(c.studentId)"
        >
          {{ c.firstName }} · {{ c.className }}
        </button>
      </div>
      <div v-else-if="child" class="wa-subtitle">
        {{ child.fullName }} · {{ child.className }} {{ t('class') }}
      </div>
    </header>

    <main class="wa-main">
      <div v-if="!state.ready" class="wa-center">
        <q-spinner size="36px" class="wa-accent-text" />
        <div class="wa-hint q-mt-sm">{{ t('loading') }}</div>
      </div>
      <div v-else-if="state.error" class="wa-center">
        <div class="wa-empty-icon">📱</div>
        <div class="wa-text-center">{{ t(state.error) }}</div>
        <button
          v-if="state.error === 'error'"
          class="wa-button q-mt-md"
          @click="loadMe"
          >{{ t('retry') }}</button
        >
      </div>
      <div v-else-if="!state.children.length" class="wa-center">
        <div class="wa-empty-icon">👨‍👩‍👧</div>
        <div class="wa-text-center">{{ t('no_children') }}</div>
      </div>
      <router-view v-else v-slot="{ Component }">
        <component :is="Component" :key="`${route.path}:${state.childId}`" />
      </router-view>
    </main>

    <nav
      v-if="state.ready && !state.error && state.children.length"
      class="wa-tabs"
    >
      <router-link
        v-for="tab in tabs"
        :key="tab.to"
        :to="tab.to"
        class="wa-tab"
        exact-active-class="wa-tab--active"
        @click="haptic()"
      >
        <q-icon :name="tab.icon" size="22px" />
        <span>{{ t(tab.label) }}</span>
      </router-link>
    </nav>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { useQuasar } from 'quasar'
import { loadTelegramSdk, webApp, colorScheme, haptic } from '@/webapp/telegram'
import { state, child, t, loadMe, selectChild } from '@/webapp/state'

const route = useRoute()
const $q = useQuasar()
const dark = ref(false)

const tabs = [
  { to: '/webapp', icon: 'today', label: 'tab_today' },
  { to: '/webapp/schedule', icon: 'calendar_view_week', label: 'tab_schedule' },
  { to: '/webapp/attendance', icon: 'fact_check', label: 'tab_attendance' },
  { to: '/webapp/grades', icon: 'grade', label: 'tab_grades' },
  { to: '/webapp/announcements', icon: 'campaign', label: 'tab_announcements' }
]

// Follow Telegram's light/dark theme (Quasar components included).
function applyTheme() {
  dark.value = colorScheme() === 'dark'
  $q.dark.set(dark.value)
  const wa = webApp()
  try {
    const bg = getComputedStyle(document.documentElement)
      .getPropertyValue('--tg-theme-bg-color')
      .trim()
    if (wa && bg) {
      wa.setHeaderColor?.(bg)
      wa.setBackgroundColor?.(bg)
    }
  } catch {
    // older Telegram clients
  }
}

function pick(id) {
  haptic()
  selectChild(id)
}

onMounted(async () => {
  const wa = await loadTelegramSdk()
  if (wa) {
    wa.ready?.()
    wa.expand?.()
    wa.onEvent?.('themeChanged', applyTheme)
  }
  applyTheme()
  loadMe()
})

onBeforeUnmount(() => webApp()?.offEvent?.('themeChanged', applyTheme))
</script>

<style>
/* Telegram publishes the user's theme as --tg-theme-* variables; outside
   Telegram the app's own tokens are used, so light/dark both look right. */
.webapp {
  --wa-bg: var(--tg-theme-secondary-bg-color, var(--page-bg));
  --wa-card: var(
    --tg-theme-section-bg-color,
    var(--tg-theme-bg-color, var(--card-bg))
  );
  --wa-text: var(--tg-theme-text-color, var(--text-primary));
  --wa-hint: var(--tg-theme-hint-color, var(--brand-text-muted));
  --wa-accent: var(--tg-theme-button-color, var(--color-brand));
  --wa-accent-text: var(--tg-theme-button-text-color, #ffffff);
  --wa-link: var(--tg-theme-link-color, var(--color-brand));
  --wa-border: var(--brand-border);
  --wa-good: #10b981;
  --wa-warn: #f59e0b;
  --wa-bad: #ef4444;
  --wa-info: #3b82f6;
  min-height: 100vh;
  background: var(--wa-bg);
  color: var(--wa-text);
  font-size: 15px;
  display: flex;
  flex-direction: column;
}

.wa-card {
  background: var(--wa-card);
  border-radius: 14px;
  padding: 14px 16px;
  margin-bottom: 12px;
  border: 1px solid var(--wa-border);
}

.wa-card-title {
  font-weight: 700;
  font-size: 16px;
  margin-bottom: 8px;
}

.wa-hint {
  color: var(--wa-hint);
  font-size: 13px;
}

.wa-accent-text {
  color: var(--wa-accent);
}

.wa-button {
  background: var(--wa-accent);
  color: var(--wa-accent-text);
  border: 0;
  border-radius: 10px;
  padding: 10px 18px;
  font: inherit;
  font-weight: 600;
  cursor: pointer;
}

.wa-segment {
  display: flex;
  background: var(--wa-card);
  border: 1px solid var(--wa-border);
  border-radius: 12px;
  padding: 3px;
  margin-bottom: 12px;
}

.wa-segment button {
  flex: 1;
  border: 0;
  background: transparent;
  color: var(--wa-text);
  font: inherit;
  font-weight: 600;
  padding: 8px 4px;
  border-radius: 9px;
  cursor: pointer;
}

.wa-segment button.active {
  background: var(--wa-accent);
  color: var(--wa-accent-text);
}

.wa-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 0;
  border-bottom: 1px solid var(--wa-border);
}

.wa-row:last-child {
  border-bottom: 0;
}

.wa-pill {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.wa-score {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  color: #fff;
  flex-shrink: 0;
}

.wa-empty {
  text-align: center;
  color: var(--wa-hint);
  padding: 18px 0;
}
</style>

<style scoped>
.wa-header {
  position: sticky;
  top: 0;
  z-index: 5;
  background: var(--wa-card);
  border-bottom: 1px solid var(--wa-border);
  padding: 12px 16px 10px;
}

.wa-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 800;
  font-size: 18px;
}

.wa-subtitle {
  color: var(--wa-hint);
  font-size: 13px;
  margin-top: 2px;
}

.wa-children {
  display: flex;
  gap: 8px;
  margin-top: 8px;
  overflow-x: auto;
}

.wa-child {
  border: 1px solid var(--wa-border);
  background: transparent;
  color: var(--wa-text);
  border-radius: 999px;
  padding: 5px 12px;
  font: inherit;
  font-size: 13px;
  white-space: nowrap;
  cursor: pointer;
}

.wa-child--active {
  background: var(--wa-accent);
  color: var(--wa-accent-text);
  border-color: var(--wa-accent);
}

.wa-main {
  flex: 1;
  padding: 12px 12px 84px;
  max-width: 640px;
  width: 100%;
  margin: 0 auto;
}

.wa-center {
  min-height: 60vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.wa-empty-icon {
  font-size: 44px;
  margin-bottom: 8px;
}

.wa-text-center {
  text-align: center;
  line-height: 1.5;
}

.wa-tabs {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 5;
  display: flex;
  background: var(--wa-card);
  border-top: 1px solid var(--wa-border);
  padding: 6px 4px calc(6px + env(safe-area-inset-bottom));
}

.wa-tab {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  color: var(--wa-hint);
  text-decoration: none;
  font-size: 11px;
  font-weight: 600;
  padding: 4px 0;
  border-radius: 10px;
}

.wa-tab--active {
  color: var(--wa-accent);
}
</style>
