<template>
  <q-page :class="['page-layout', { 'page-layout--fill': fill }]">
    <div v-if="!noHeader" class="page-layout__header">
      <slot name="header">
        <IconTile v-if="icon" :icon="icon" size="48" :color="color" />
        <div class="page-layout__title">
          <div class="text-h5 text-weight-bold ellipsis">{{ title }}</div>
          <div
            v-if="subtitle || $slots.subtitle"
            class="text-caption muted-text"
          >
            <slot name="subtitle">{{ subtitle }}</slot>
          </div>
        </div>
        <div class="page-layout__actions">
          <slot name="actions" />
        </div>
      </slot>
    </div>
    <div class="page-layout__body">
      <slot />
    </div>
  </q-page>
</template>

<script setup>
// Every page sits in this frame: it uses the whole width next to the sidebar,
// the side padding follows the screen (--page-pad) and the space above the
// header, under it and at the bottom is one token (--page-gap).
// fill: the body takes the rest of the screen height (tables, calendars that
// scroll inside instead of the page).
import IconTile from '@/components/app/IconTile.vue'

defineProps({
  icon: { type: String, default: '' },
  title: { type: String, default: '' },
  subtitle: { type: String, default: '' },
  color: { type: String, default: '' },
  noHeader: { type: Boolean, default: false },
  fill: { type: Boolean, default: false }
})
</script>

<style scoped>
.page-layout {
  width: 100%;
  padding: var(--page-gap) var(--page-pad);
  display: flex;
  flex-direction: column;
  gap: var(--page-gap);
}

.page-layout__header {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-4);
}

.page-layout__title {
  flex: 1 1 0;
  min-width: 0;
}

.page-layout__actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--space-2);
}

.page-layout__body {
  min-width: 0;
}

/* on a desktop the page is exactly one screen high and its table scrolls inside;
   on a phone the blocks stack and the page scrolls as usual */
@media (min-width: 1024px) {
  .page-layout--fill {
    height: calc(100vh - var(--header-h));
    min-height: 0;
  }

  .page-layout--fill .page-layout__body {
    flex: 1 1 0;
    min-height: 0;
    display: flex;
    flex-direction: column;
  }
}

.muted-text {
  color: var(--brand-text-muted);
}

@media (max-width: 599px) {
  .page-layout__actions {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
