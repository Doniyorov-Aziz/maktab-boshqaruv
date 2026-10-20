<template>
  <div class="sd" data-subject-detail>
    <div class="sd__hero" :style="{ '--c': color }">
      <IconTile
        :icon="subjectIcon(row)"
        size="48"
        :color="`color-mix(in srgb, ${color} 16%, transparent)`"
        :text-color="color"
      />
      <div>
        <h2>{{ row.name }}</h2>
        <small
          >{{ row.weeklyLessons ?? 0 }} soat / hafta ·
          {{ row.classCount ?? 0 }} sinf ·
          {{ row.teacherCount ?? 0 }} o'qituvchi</small
        >
      </div>
    </div>

    <div v-if="!extra" class="skel sd__skel" />
    <template v-else>
      <section>
        <h4>Baholar taqsimoti — {{ extra.window }}</h4>
        <div
          v-if="gradeTotal"
          class="sd__dist"
          role="img"
          :aria-label="distLabel"
          data-distribution
        >
          <span
            v-for="v in [5, 4, 3, 2]"
            :key="v"
            :style="{
              flex: extra.distribution[v] || 0,
              '--g': `var(--gr-${v})`
            }"
            :title="`${v}: ${extra.distribution[v]} ta`"
            >{{ share(v) >= 8 ? `${v} · ${share(v)}%` : '' }}</span
          >
        </div>
        <p v-else class="sd__muted">Bu davrda baho yo'q.</p>
        <p class="sd__muted">
          Sifat (4 va 5): <b>{{ pct(extra.quality) }}</b>
          <template v-if="extra.qualityPrev != null">
            · oldingi davr {{ pct(extra.qualityPrev) }}</template
          >
        </p>
      </section>

      <section>
        <h4>O'qituvchilar va yuklama</h4>
        <div v-if="!extra.teachers.length" class="sd__muted">
          Jadvalda bu fan hali yo'q.
        </div>
        <div
          v-for="t in extra.teachers"
          :key="t.id"
          class="sd__teacher"
          :data-teacher-row="t.id"
        >
          <div class="sd__row">
            <AvatarInitials :name="t.name" :id="t.id" size="sm" />
            <span class="sd__who">{{ t.name }}</span>
            <span class="sd__bar"
              ><i :style="{ width: `${(t.lessons / maxLessons) * 100}%` }"
            /></span>
            <b class="tnum">{{ t.lessons }} soat</b>
          </div>
          <!-- in which classes, hours a week each -->
          <div v-if="extra.byTeacher?.[t.id]?.length" class="sd__classes">
            <span
              v-for="c in extra.byTeacher[t.id]"
              :key="c.classId"
              class="sd__class-chip tnum"
              :data-teacher-class="c.name"
              >{{ c.name }} · {{ c.lessons }} soat</span
            >
          </div>
        </div>
      </section>

      <section>
        <h4>Sinflar bo'yicha (haftasiga soat, sifat)</h4>
        <div
          v-for="c in extra.classes"
          :key="c.classId"
          class="sd__row"
          :data-class-row="c.classId"
        >
          <span class="sd__class">{{ c.name }}</span>
          <small class="sd__muted tnum">{{ c.lessons }} soat</small>
          <span class="sd__bar"
            ><i
              :style="{
                width: `${c.quality ?? 0}%`,
                background: tone(c.quality)
              }"
          /></span>
          <b class="tnum" :style="{ color: tone(c.quality) }">{{
            pct(c.quality)
          }}</b>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup>
// One subject in the side panel: the grade spread this quarter (5/4/3/2), quality against the last
// quarter, who teaches it and how many hours a week, and every class with its hours and quality.
import { ref, computed, watch } from 'vue'
import { api } from '@/boot/axios'
import IconTile from '@/components/app/IconTile.vue'
import AvatarInitials from '@/components/pages/AvatarInitials.vue'
import { subjectColor } from '@/utils/subjectColor'
import { subjectIcon } from '@/utils/subjectIcon'

const props = defineProps({ row: { type: Object, required: true } })

const extra = ref(null)
watch(
  () => props.row.id,
  async id => {
    extra.value = null
    try {
      extra.value = (
        await api.get(`/api/pages/lists/subjects/${id}/extra`)
      ).data
    } catch {
      extra.value = {
        teachers: [],
        classes: [],
        distribution: {},
        window: '',
        quality: null
      }
    }
  },
  { immediate: true }
)

const color = computed(() => subjectColor(props.row.id, props.row.color))
const gradeTotal = computed(() =>
  Object.values(extra.value?.distribution || {}).reduce((a, b) => a + b, 0)
)
const share = v =>
  gradeTotal.value
    ? Math.round(((extra.value.distribution[v] || 0) / gradeTotal.value) * 100)
    : 0
const distLabel = computed(() =>
  [5, 4, 3, 2].map(v => `${v}: ${extra.value?.distribution[v] || 0}`).join(', ')
)
const maxLessons = computed(() =>
  Math.max(1, ...(extra.value?.teachers || []).map(t => t.lessons || 0))
)
const pct = v => (v == null ? '—' : `${Math.round(v)}%`)
const tone = q =>
  q == null
    ? 'var(--st-unmarked)'
    : q >= 60
      ? 'var(--st-present)'
      : q >= 40
        ? 'var(--gr-3)'
        : 'var(--st-absent)'
</script>

<style scoped>
.tnum {
  font-variant-numeric: tabular-nums;
}

.sd {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.sd__hero {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.sd__hero h2 {
  margin: 0;
  font-size: var(--text-lg);
  font-weight: 800;
}

.sd__hero small,
.sd__muted {
  color: var(--brand-text-muted);
  font-size: var(--text-sm);
}

h4 {
  margin: 0 0 var(--space-2);
  font-size: var(--text-xs);
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--brand-text-muted);
}

.sd__skel {
  height: calc(var(--crud-row-h) * 5);
  border-radius: var(--radius-md);
}

.sd__dist {
  display: flex;
  overflow: hidden;
  height: var(--space-6);
  border-radius: var(--radius-md);
  background: var(--ring-track);
}

.sd__dist span {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 0;
  background: var(--g);
  color: white;
  font-size: var(--text-xs);
  font-weight: 700;
  white-space: nowrap;
}

.sd__row {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) minmax(0, 1.2fr) auto;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-1) 0;
  font-size: var(--text-sm);
}

.sd__who,
.sd__class {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 600;
}

.sd__teacher {
  padding: var(--space-1) 0;
  border-bottom: var(--hairline) solid var(--dash-edge);
}

.sd__teacher:last-child {
  border-bottom: 0;
}

.sd__classes {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
  padding: 0 0 var(--space-1) calc(var(--space-7) + var(--space-1));
}

.sd__class-chip {
  padding: 0 var(--space-2);
  border-radius: var(--radius-pill);
  background: var(--surface-2);
  color: var(--brand-text-muted);
  font-size: var(--text-xs);
  font-weight: 600;
}

.sd__bar {
  position: relative;
  height: var(--space-2);
  border-radius: var(--radius-pill);
  background: var(--ring-track);
  overflow: hidden;
}

.sd__bar i {
  position: absolute;
  inset: 0 auto 0 0;
  border-radius: inherit;
  background: var(--chart-primary);
}
</style>
