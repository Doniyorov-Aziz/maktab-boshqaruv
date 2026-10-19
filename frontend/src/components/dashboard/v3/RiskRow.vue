<template>
  <div
    class="rr"
    :class="{ 'rr--link': !readonly }"
    :role="readonly ? undefined : 'link'"
    :tabindex="readonly ? undefined : 0"
    :data-risk-index="index"
    data-risk-panel-row
    @click="emit('open', row)"
    @keydown.enter="emit('open', row)"
  >
    <RiskAvatar :name="row.name" :id="row.studentId" />
    <span class="rr__who">
      <b>{{ row.name }}</b>
      <span class="rr__chips">
        <span class="rr__class">{{ row.className }}</span>
        <span v-if="row.absentDays" class="chip chip--abs"
          >{{ row.absentDays }} kun kelmagan</span
        >
        <span v-if="avgChip" class="chip chip--avg">{{ avgChip }}</span>
        <span v-if="row.twos" class="chip chip--two"
          >{{ row.twos }} ta «2»</span
        >
      </span>
    </span>
    <span v-if="!readonly" class="rr__actions" @click.stop>
      <AppButton
        v-if="row.phone"
        variant="ghost"
        size="sm"
        icon="call"
        :href="`tel:${row.phone}`"
        :aria-label="`Ota-onaga qo'ng'iroq: ${row.phone}`"
        :title="`Ota-onaga qo'ng'iroq: ${row.phone}`"
      />
      <AppButton
        variant="ghost"
        size="sm"
        icon="send"
        :to="{ path: '/telegram-parents', query: { class: row.classId } }"
        :aria-label="
          row.telegram
            ? 'Telegram orqali yozish'
            : 'Ota-ona Telegramga ulanmagan'
        "
        :title="
          row.telegram
            ? 'Telegram orqali yozish'
            : 'Ota-ona Telegramga ulanmagan — ulash'
        "
      />
      <AppButton
        variant="ghost"
        size="sm"
        icon="badge"
        :to="`/profiles/student/${row.studentId}`"
        aria-label="Profil"
        title="Profil"
      />
    </span>
    <ScoreRing :value="row.score" :title="`Xavf balli ${row.score}`" />
  </div>
</template>

<script setup>
// One student in the risk panel: who, why (chips) and the score; quick actions on hover.
import { computed } from 'vue'
import RiskAvatar from '../RiskAvatar.vue'
import ScoreRing from '../ScoreRing.vue'
import AppButton from '@/components/app/AppButton.vue'

const props = defineProps({
  row: { type: Object, required: true },
  index: { type: Number, default: 0 },
  readonly: { type: Boolean, default: false }
})
const emit = defineEmits(['open'])

const one = v => v.toFixed(1)
const avgChip = computed(() => {
  const r = props.row
  if (r.avgNow === null || r.avgNow === undefined) return null
  if (
    r.avgBefore !== null &&
    r.avgBefore !== undefined &&
    r.avgBefore - r.avgNow >= 0.3
  )
    return `${one(r.avgBefore)} → ${one(r.avgNow)}`
  return `o'rtacha ${one(r.avgNow)}`
})
</script>

<style scoped>
.rr {
  position: relative;
  display: flex;
  align-items: center;
  gap: var(--space-3);
  min-height: calc(var(--control-h) + var(--space-5));
  padding: var(--space-2);
  margin: 0 calc(var(--space-2) * -1);
  border-radius: var(--radius-md);
}

.rr--link {
  cursor: pointer;
}

.rr--link:hover,
.rr--link:focus-visible {
  background: var(--surface-2);
  outline: none;
}

.rr__who {
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.rr__who b {
  font-size: var(--text-sm);
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rr__chips {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
  align-items: center;
}

.rr__class {
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
  margin-right: var(--space-1);
}

.chip {
  font-size: var(--text-xs);
  font-weight: 600;
  line-height: 1.6;
  padding: 0 var(--space-2);
  border-radius: var(--radius-pill);
  white-space: nowrap;
}

.chip--abs {
  color: var(--st-absent);
  background: color-mix(in srgb, var(--st-absent) 12%, transparent);
}

.chip--avg {
  color: var(--st-late);
  background: color-mix(in srgb, var(--st-late) 12%, transparent);
}

.chip--two {
  color: var(--gr-3);
  background: color-mix(in srgb, var(--gr-3) 14%, transparent);
}

/* quick actions appear on hover / focus (always on touch screens) */
.rr__actions {
  display: inline-flex;
  gap: var(--hairline);
  opacity: 0;
  transition: opacity var(--dur-fast) var(--ease-out);
}

.rr:hover .rr__actions,
.rr:focus-within .rr__actions {
  opacity: 1;
}

@media (hover: none) {
  .rr__actions {
    opacity: 1;
  }
}

@media (max-width: 599px) {
  .rr__actions {
    display: none;
  }
}
</style>
