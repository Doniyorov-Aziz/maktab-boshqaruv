<template>
  <div class="wa-state" :class="`wa-state--${kind}`">
    <svg viewBox="0 0 160 120" class="art" aria-hidden="true">
      <ellipse cx="80" cy="108" rx="54" ry="7" class="shadow" />
      <!-- open notebook -->
      <path d="M28 30 Q54 22 78 32 L78 98 Q54 88 28 96 Z" class="page" />
      <path d="M132 30 Q106 22 82 32 L82 98 Q106 88 132 96 Z" class="page" />
      <path d="M78 32 L82 32 L82 98 L78 98 Z" class="spine" />
      <g class="lines">
        <path d="M38 44 Q54 39 70 45" />
        <path d="M38 56 Q54 51 70 57" />
        <path d="M38 68 Q54 63 70 69" />
        <path d="M90 45 Q106 39 122 44" />
        <path d="M90 57 Q106 51 122 56" />
      </g>
      <template v-if="kind === 'error'">
        <circle cx="112" cy="74" r="15" class="badge-bad" />
        <path d="M112 66 L112 76 M112 81 L112 82" class="badge-mark" />
      </template>
      <template v-else>
        <path
          d="M112 62 l3.5 7.2 7.9 1.1 -5.7 5.6 1.3 7.9 -7-3.7 -7 3.7 1.3-7.9 -5.7-5.6 7.9-1.1z"
          class="star"
        />
        <circle cx="134" cy="22" r="3" class="dot" />
        <circle cx="22" cy="40" r="2.2" class="dot" />
      </template>
    </svg>
    <div class="text">{{ text }}</div>
    <button
      v-if="kind === 'error'"
      class="wa-button q-mt-md"
      @click="emit('retry')"
      >{{ retryText }}</button
    >
  </div>
</template>

<script setup>
defineProps({
  kind: { type: String, default: 'empty' },
  text: { type: String, default: '' },
  retryText: { type: String, default: '' }
})
const emit = defineEmits(['retry'])
</script>

<style scoped>
.wa-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 18px 12px 12px;
  color: var(--wa-hint);
  line-height: 1.45;
}

.art {
  width: 150px;
  height: 112px;
  margin-bottom: 6px;
}

.shadow {
  fill: var(--wa-border);
  opacity: 0.7;
}

.page {
  fill: var(--wa-card);
  stroke: var(--wa-accent);
  stroke-width: 2;
  stroke-linejoin: round;
}

.spine {
  fill: var(--wa-accent);
}

.lines path {
  fill: none;
  stroke: var(--wa-accent);
  stroke-width: 2;
  stroke-linecap: round;
  opacity: 0.35;
}

.star {
  fill: #f59e0b;
}

.dot {
  fill: var(--wa-accent);
  opacity: 0.5;
}

.badge-bad {
  fill: var(--wa-bad);
}

.badge-mark {
  stroke: #fff;
  stroke-width: 3.2;
  stroke-linecap: round;
}

.text {
  max-width: 280px;
  font-size: 14px;
}
</style>
