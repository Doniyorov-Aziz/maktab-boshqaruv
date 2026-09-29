<template>
  <q-page class="page-layout q-pa-md">
    <div v-if="!noHeader" class="row items-center q-mb-lg q-col-gutter-md">
      <slot name="header">
        <div class="col-auto">
          <q-avatar
            size="48px"
            text-color="white"
            :icon="icon"
            :style="{ background: color || 'var(--brand-gradient)' }"
          />
        </div>
        <div class="col page-layout__title-col">
          <div class="text-h5 text-weight-bold ellipsis">{{ title }}</div>
          <div v-if="subtitle" class="text-caption muted-text ellipsis">
            {{ subtitle }}
          </div>
        </div>
        <div class="col-auto row items-center q-gutter-sm no-wrap">
          <slot name="actions" />
        </div>
      </slot>
    </div>
    <slot />
  </q-page>
</template>

<script setup>
defineProps({
  icon: { type: String, default: '' },
  title: { type: String, default: '' },
  subtitle: { type: String, default: '' },
  color: { type: String, default: '' },
  noHeader: { type: Boolean, default: false }
})
</script>

<style scoped>
/* Every page in the app shares this one wrapper so width, side padding and
   the header block never drift page-to-page again. */
.page-layout {
  max-width: 1600px;
  margin: 0 auto;
}

.muted-text {
  color: var(--brand-text-muted);
}

.page-layout__title-col {
  min-width: 0;
}
</style>
