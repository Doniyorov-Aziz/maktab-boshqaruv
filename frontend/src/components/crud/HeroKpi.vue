<template>
  <div class="hkpi" :data-kpi="kpiKey">
    <span
      class="hkpi__icon"
      :style="{
        color,
        background: `color-mix(in srgb, ${color} 12%, transparent)`
      }"
      aria-hidden="true"
    >
      <q-icon :name="icon" />
    </span>
    <div class="hkpi__text">
      <b class="hkpi__value dash-num">
        <svg
          v-if="ring != null"
          class="hkpi__ring"
          viewBox="0 0 20 20"
          aria-hidden="true"
        >
          <circle cx="10" cy="10" r="8" class="hkpi__track" />
          <circle
            cx="10"
            cy="10"
            r="8"
            class="hkpi__arc"
            :style="{
              stroke: color,
              strokeDasharray: C,
              strokeDashoffset: C * (1 - Math.max(0, Math.min(100, ring)) / 100)
            }"
            transform="rotate(-90 10 10)"
          />
        </svg>
        {{ value }}
      </b>
      <small class="hkpi__label">{{ label }}</small>
    </div>
    <q-tooltip v-if="hint">{{ hint }}</q-tooltip>
  </div>
</template>

<script setup>
// One number of a hero card: a tinted icon, the number (with a small ring for a share) and its label.
defineProps({
  kpiKey: { type: String, default: '' },
  icon: { type: String, required: true },
  value: { type: [String, Number], default: '—' },
  label: { type: String, required: true },
  color: { type: String, default: 'var(--c-primary)' },
  // a percentage drawn as a small ring next to the number
  ring: { type: Number, default: null },
  hint: { type: String, default: '' }
})
const C = 2 * Math.PI * 8
</script>

<style scoped>
.hkpi {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  min-width: 0;
}

.hkpi__icon {
  display: inline-flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: var(--tile-sm);
  height: var(--tile-sm);
  border-radius: var(--radius-sm);
  font-size: var(--text-md);
}

.hkpi__text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.hkpi__value {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  color: var(--text-primary);
  font-size: var(--text-lg);
  font-weight: 800;
  line-height: 1.1;
  white-space: nowrap;
}

.hkpi__ring {
  width: var(--text-md);
  height: var(--text-md);
}

.hkpi__track {
  fill: none;
  stroke: var(--ring-track);
  stroke-width: 3.5;
}

.hkpi__arc {
  fill: none;
  stroke-linecap: round;
  stroke-width: 3.5;
}

.hkpi__label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--brand-text-muted);
  font-size: var(--text-xs);
}
</style>
