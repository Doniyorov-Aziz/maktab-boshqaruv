<template>
  <SideDrawer
    :model-value="modelValue"
    :title="card?.name || student?.fullName || ''"
    :subtitle="card ? `${card.className} · ${subjectName}` : subjectName"
    icon="person"
    @update:model-value="v => emit('update:modelValue', v)"
  >
    <div v-if="!card" class="sd__skel">
      <q-skeleton type="rect" class="sd__skel-kpis" />
      <q-skeleton type="rect" class="sd__skel-chart" />
    </div>
    <div v-else class="sd" data-student-drawer>
      <div class="sd__kpis">
        <div class="sd__kpi" data-kpi="average">
          <span>O'rtacha (yil)</span>
          <b :style="{ color: avgColor(card.average) }"
            ><CountUp :value="card.average" :format="v => v.toFixed(2)"
          /></b>
        </div>
        <div class="sd__kpi" data-kpi="attendance">
          <span>Davomat (30 kun)</span>
          <b :style="{ color: attColor }"
            ><CountUp
              :value="card.attendance30"
              :format="v => `${Math.round(v)}%`"
          /></b>
        </div>
        <div class="sd__kpi" data-kpi="risk">
          <span>Xavf</span>
          <b :style="{ color: riskColor }">{{
            card.risk == null ? "Yo'q" : card.risk
          }}</b>
          <small v-if="card.risk != null">/ 100</small>
        </div>
      </div>

      <section class="sd__block">
        <h4>O'rtacha baho — oxirgi 30 kun</h4>
        <div
          v-if="trendValues.filter(v => v != null).length > 1"
          class="sd__chart"
          role="img"
          aria-label="30 kunlik o'rtacha baho chizig'i"
          data-student-trend
        >
          <Line :key="themeKey" :data="chartData" :options="chartOptions" />
        </div>
        <p v-else class="sd__muted">Chiziq uchun baholar kam.</p>
      </section>

      <section class="sd__block">
        <h4>Baholar ({{ card.grades.length }})</h4>
        <div v-if="!card.grades.length" class="sd__muted"
          >Bu fandan hali baho yo'q.</div
        >
        <div v-for="m in months" :key="m.key" class="sd__month">
          <span class="sd__mname">{{ m.title }}</span>
          <span
            v-for="(g, i) in m.grades"
            :key="i"
            class="sd__chip"
            :class="{
              'sd__chip--exam': isTest(g.type),
              'sd__chip--hw': g.type === 'HOMEWORK'
            }"
            :style="{ '--g': GRADE_COLOR[g.value] }"
            :title="`${g.date.slice(8)}.${g.date.slice(5, 7)} · ${TYPE_LABEL[g.type] || ''}${g.comment ? ` · ${g.comment}` : ''}`"
            >{{ g.value }}</span
          >
        </div>
      </section>

      <section class="sd__block">
        <h4>Ota-ona</h4>
        <div v-if="card.guardianName" class="sd__guardian">
          <AvatarInitials
            :name="card.guardianName"
            :id="card.studentId"
            size="sm"
          />
          <div>
            <b>{{ guardianLabel(card.guardianName, card.guardianRelation) }}</b>
            <div class="sd__muted">
              {{
                card.guardianPhone
                  ? formatPhone(card.guardianPhone)
                  : "Telefon yo'q"
              }}
              <template v-if="card.telegram"> · Telegram ulangan</template>
            </div>
          </div>
        </div>
        <div v-else class="sd__muted">Ota-ona kiritilmagan.</div>
      </section>
    </div>

    <template #footer>
      <AppButton
        variant="secondary"
        icon="sms"
        label="Ota-onaga xabar"
        :disable="!card?.guardianPhone"
        :href="card?.guardianPhone ? `sms:${card.guardianPhone}` : undefined"
        data-message-parent
      />
      <AppButton
        icon="person"
        label="Profil"
        data-open-profile
        @click="router.push(`/profiles/student/${student.id}`)"
      />
    </template>
  </SideDrawer>
</template>

<script setup>
// A student in this subject, opened from their name: the year's average, attendance of the
// last 30 days and the risk score, the 30-day running average as a line, every grade by month,
// the parent with a phone — and the ways on: a message to the parent, the full profile.
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import SideDrawer from '@/components/dashboard/SideDrawer.vue'
import CountUp from '@/components/dashboard/CountUp.vue'
import { Line } from 'vue-chartjs'
import {
  Chart,
  LineElement,
  PointElement,
  LinearScale,
  CategoryScale,
  Filler,
  Tooltip
} from 'chart.js'
import { useChartTheme, alpha, token } from '@/charts/chartTheme'
import AppButton from '@/components/app/AppButton.vue'
import AvatarInitials from '@/components/pages/AvatarInitials.vue'
import { GRADE_COLOR, TYPE_LABEL, avgColor, isTest, MONTHS } from './journal'
import { guardianLabel } from '@/utils/guardian'
import { formatPhone } from '@/utils/tashkent'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  student: { type: Object, default: null },
  // /api/pages/journal/student
  card: { type: Object, default: null },
  subjectName: { type: String, default: '' }
})
const emit = defineEmits(['update:modelValue'])
const router = useRouter()

Chart.register(
  LineElement,
  PointElement,
  LinearScale,
  CategoryScale,
  Filler,
  Tooltip
)
const { themeKey } = useChartTheme()

const trend = computed(() => props.card?.trend || [])
const trendValues = computed(() => trend.value.map(p => p.average))
// the line takes the colour of the year's average (canvas needs the token's value)
const lineColor = computed(() => {
  const name =
    /var\((--[\w-]+)\)/.exec(avgColor(props.card?.average))?.[1] ||
    '--chart-primary'
  // themeKey changes when light / dark flips: the token is read again then
  return themeKey.value >= 0 ? token(name) : ''
})
const chartData = computed(() => ({
  labels: trend.value.map(p => `${p.date.slice(8)}.${p.date.slice(5, 7)}`),
  datasets: [
    {
      label: "O'rtacha",
      data: trendValues.value,
      borderColor: lineColor.value,
      backgroundColor: ctx => {
        const area = ctx.chart.chartArea
        if (!area) return alpha(lineColor.value, 0.12)
        const g = ctx.chart.ctx.createLinearGradient(
          0,
          area.top,
          0,
          area.bottom
        )
        g.addColorStop(0, alpha(lineColor.value, 0.28))
        g.addColorStop(1, alpha(lineColor.value, 0))
        return g
      },
      fill: true,
      borderWidth: 2,
      cubicInterpolationMode: 'monotone',
      spanGaps: true,
      pointRadius: 0,
      pointHoverRadius: 4
    }
  ]
}))
const chartOptions = {
  animations: { colors: false },
  layout: { padding: { top: 4, right: 4, left: 0, bottom: 0 } },
  interaction: { mode: 'index', intersect: false },
  scales: {
    x: {
      grid: { display: false },
      ticks: { maxRotation: 0, autoSkip: true, maxTicksLimit: 6 }
    },
    y: { min: 2, max: 5, ticks: { stepSize: 1 } }
  },
  plugins: {
    tooltip: {
      callbacks: {
        label: item =>
          item.raw == null ? "Baho yo'q" : `O'rtacha: ${item.raw.toFixed(2)}`
      }
    }
  }
}
const months = computed(() => {
  const by = {}
  for (const g of props.card?.grades || [])
    (by[g.date.slice(0, 7)] ||= []).push(g)
  return Object.keys(by)
    .sort()
    .reverse()
    .map(k => ({
      key: k,
      title: `${MONTHS[Number(k.slice(5)) - 1]}`,
      grades: by[k]
    }))
})
const attColor = computed(() => {
  const a = props.card?.attendance30
  if (a == null) return 'var(--st-unmarked)'
  return a >= 95
    ? 'var(--st-present)'
    : a >= 85
      ? 'var(--gr-3)'
      : 'var(--st-absent)'
})
const riskColor = computed(() => {
  const r = props.card?.risk
  if (r == null) return 'var(--st-present)'
  return r >= 70
    ? 'var(--st-absent)'
    : r >= 40
      ? 'var(--st-late)'
      : 'var(--gr-3)'
})
</script>

<style scoped>
.sd {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.sd__kpis {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-2);
}

.sd__kpi {
  display: flex;
  flex-direction: column;
  gap: var(--hairline);
  padding: var(--space-3);
  border-radius: var(--radius-md);
  background: var(--surface-2);
}

.sd__kpi span,
.sd__kpi small {
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.sd__kpi b {
  font-size: var(--text-xl);
  font-variant-numeric: tabular-nums;
}

.sd__block h4 {
  margin: 0 0 var(--space-2);
  font-size: var(--text-sm);
  font-weight: 700;
}

.sd__month {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-1);
  padding: var(--space-1) 0;
}

.sd__mname {
  width: var(--space-8);
  flex: none;
  font-size: var(--text-xs);
  color: var(--brand-text-muted);
}

.sd__chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: var(--avatar-xs);
  height: var(--avatar-xs);
  border-radius: var(--radius-sm);
  background: color-mix(in srgb, var(--g) 18%, transparent);
  color: var(--g);
  font-size: var(--text-xs);
  font-weight: 800;
}

.sd__chip--exam {
  box-shadow: inset 0 0 0 2px var(--g);
}

.sd__chip--hw {
  outline: var(--hairline) dashed var(--g);
  outline-offset: calc(var(--hairline) * -3);
}

.sd__guardian {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.sd__muted {
  margin: 0;
  font-size: var(--text-sm);
  color: var(--brand-text-muted);
}

.sd__skel {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.sd__skel-kpis {
  height: calc(var(--control-h) * 2);
  border-radius: var(--radius-md);
}

.sd__chart {
  position: relative;
  height: calc(var(--control-h) * 3.5);
}

.sd__skel-chart {
  height: calc(var(--control-h) * 3);
  border-radius: var(--radius-md);
}
</style>
