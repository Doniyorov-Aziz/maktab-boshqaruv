<template>
  <component
    :is="clickable ? 'button' : 'div'"
    :type="clickable ? 'button' : undefined"
    class="sig"
    :class="[`sig--${tone}`, { 'sig--click': clickable }]"
    @click="clickable && emit('click')"
  >
    <span class="sig__icon"><q-icon :name="icon" /></span>
    <span class="sig__text">
      <b>{{ title }}</b>
      <small v-if="subtitle">{{ subtitle }}</small>
    </span>
    <span v-if="count !== null" class="sig__count">{{ count }}</span>
    <q-icon v-if="clickable" name="chevron_right" class="sig__chev" />
  </component>
</template>

<script setup>
// One line of a "something to look at" list: an icon in the tone's colour, what it is,
// a detail line, an optional number. tone: bad / warn / info / ok.
defineProps({
  icon: { type: String, default: 'info' },
  tone: { type: String, default: 'info' },
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  count: { type: [Number, String], default: null },
  clickable: { type: Boolean, default: false }
})
const emit = defineEmits(['click'])
</script>

<style scoped>
.sig {
  --tone: var(--chart-secondary);
  display: flex;
  align-items: center;
  gap: var(--space-3);
  width: 100%;
  padding: var(--space-2);
  border: 0;
  border-radius: var(--radius-md);
  background: transparent;
  color: inherit;
  font: inherit;
  text-align: left;
}

.sig--bad {
  --tone: var(--st-absent);
}

.sig--warn {
  --tone: var(--st-late);
}

.sig--ok {
  --tone: var(--st-present);
}

.sig--click {
  cursor: pointer;
}

.sig--click:hover,
.sig--click:focus-visible {
  background: var(--surface-2);
  outline: none;
}

.sig__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: var(--tile-sm);
  height: var(--tile-sm);
  flex-shrink: 0;
  border-radius: var(--radius-sm);
  color: var(--tone);
  background: color-mix(in srgb, var(--tone) 13%, transparent);
}

.sig__text {
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.sig__text b {
  font-size: var(--text-sm);
  font-weight: 600;
}

.sig__text small {
  color: var(--brand-text-muted);
  font-size: var(--text-xs);
}

.sig__count {
  font-size: var(--text-md);
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--tone);
}

.sig__chev {
  color: var(--brand-text-muted);
}
</style>
