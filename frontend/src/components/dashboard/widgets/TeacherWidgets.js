// The teacher's own widgets (9): small render components on top of ChartCard, so the
// teacher page uses exactly the same frame, states and refresh as the school page.
import { h, computed } from 'vue'
import { RouterLink } from 'vue-router'
import ChartCard from '../ChartCard.vue'
import Sparkline from '../Sparkline.vue'
import AppButton from '@/components/app/AppButton.vue'
import { useWidget } from '@/composables/useWidget'
import { formatPercent, roomLabel } from '@/utils/format'

const STATE = { MANUAL: "✓ qo'lda", AUTO: '🤖 avtomatik', NONE: '⏳ olinmagan' }
const hm = t => (t ? String(t).slice(0, 5) : '')
const dmy = d => d.split('-').reverse().join('.')
const cardProps = w => ({
  asOf: w.asOf.value,
  loading: w.loading.value,
  error: w.error.value,
  data: w.data.value,
  onRetry: w.reload
})

const badge = state =>
  h('span', { class: ['tw__state', `tw__state--${state}`] }, STATE[state])

/** "Bugungi darslarim": time, class, subject, ✓ / 🤖 / ⏳, [Davomat] [Jurnal]. */
export const MyLessons = {
  name: 'MyLessons',
  setup() {
    const w = useWidget('my-lessons')
    return () =>
      h(
        ChartCard,
        {
          title: 'Bugungi darslarim',
          ...cardProps(w),
          empty: !!w.data.value && !w.data.value.length,
          emptyText: "Bugun darsingiz yo'q"
        },
        () =>
          h(
            'ul',
            { class: 'tw__list' },
            (w.data.value || []).map(l =>
              h(
                'li',
                { class: ['tw__row', { 'tw__row--now': l.phase === 'NOW' }] },
                [
                  h('span', { class: 'tw__time' }, [
                    h('b', `${l.number}-dars`),
                    h('small', `${hm(l.start)}–${hm(l.end)}`)
                  ]),
                  h('span', { class: 'tw__main' }, [
                    h('b', `${l.className} · ${l.subject}`),
                    h('small', roomLabel(l.room))
                  ]),
                  badge(l.state),
                  h(AppButton, {
                    size: 'sm',
                    variant: l.state === 'MANUAL' ? 'ghost' : 'secondary',
                    label: 'Davomat',
                    to: {
                      path: '/attendance',
                      query: { lessonSlotId: l.lessonSlotId }
                    }
                  }),
                  h(AppButton, {
                    size: 'sm',
                    variant: 'ghost',
                    label: 'Jurnal',
                    to: { path: '/gradebook', query: { class: l.classId } }
                  })
                ]
              )
            )
          )
      )
  }
}

/** "Davomat yoki baho kiritilmagan darslarim" (7 school days); automatic ones ask to be confirmed. */
export const MyMissing = {
  name: 'MyMissing',
  setup() {
    const w = useWidget('my-missing')
    return () =>
      h(
        ChartCard,
        {
          title: 'Davomat yoki baho kiritilmagan darslarim',
          subtitle: 'Oxirgi 7 ish kuni',
          ...cardProps(w),
          empty: !!w.data.value && !w.data.value.length,
          emptyText: "Hammasi to'ldirilgan",
          scroll: true
        },
        () =>
          h(
            'ul',
            { class: 'tw__list' },
            (w.data.value || []).map(m =>
              h('li', { class: 'tw__row' }, [
                h('span', { class: 'tw__time' }, [
                  h('b', dmy(m.date).slice(0, 5)),
                  h('small', `${m.number}-dars`)
                ]),
                h('span', { class: 'tw__main' }, [
                  h('b', `${m.className} · ${m.subject}`),
                  h('small', [
                    m.attendance === 'NONE'
                      ? '⏳ davomat yo‘q'
                      : m.attendance === 'AUTO'
                        ? '🤖 davomat avtomatik — tasdiqlang'
                        : '✓ davomat bor',
                    ' · ',
                    m.graded ? '✓ baho bor' : 'baho yo‘q'
                  ])
                ]),
                m.attendance !== 'MANUAL'
                  ? h(AppButton, {
                      size: 'sm',
                      variant: 'secondary',
                      label: m.attendance === 'AUTO' ? 'Tasdiqlash' : 'Davomat',
                      to: {
                        path: '/attendance',
                        query: { lessonSlotId: m.lessonSlotId, date: m.date }
                      }
                    })
                  : null,
                !m.graded
                  ? h(AppButton, {
                      size: 'sm',
                      variant: 'ghost',
                      label: 'Jurnal',
                      to: { path: '/gradebook', query: { class: m.classId } }
                    })
                  : null
              ])
            )
          )
      )
  }
}

/** "Menga kelgan murojaatlar": appeals to the teacher's own class. */
export const MyAppeals = {
  name: 'MyAppeals',
  setup() {
    const w = useWidget('my-appeals')
    return () =>
      h(
        ChartCard,
        {
          title: 'Menga kelgan murojaatlar',
          ...cardProps(w),
          empty: !!w.data.value && !w.data.value.length,
          emptyText: 'Javob kutayotgan murojaat yo‘q'
        },
        () =>
          h(
            'ul',
            { class: 'tw__list' },
            (w.data.value || []).map(a =>
              h('li', { class: 'tw__row' }, [
                h('span', { class: 'tw__main' }, [
                  h('b', `#${a.id} · ${a.parentName || 'Ota-ona'}`),
                  h(
                    'small',
                    `${a.student}${a.unread ? ` · ${a.unread} ta o‘qilmagan` : ''}`
                  )
                ]),
                h(AppButton, {
                  size: 'sm',
                  variant: 'secondary',
                  label: 'Ochish',
                  to: { path: '/parent-messages', query: { id: a.id } }
                })
              ])
            )
          )
      )
  }
}

/** "Mening sinfim": today's attendance, quarter grades, parents in the bot, 14 days. */
export const MyClass = {
  name: 'MyClass',
  setup() {
    const w = useWidget('my-class')
    const d = computed(() => w.data.value)
    return () =>
      h(
        ChartCard,
        {
          title: d.value ? `Mening sinfim · ${d.value.name}` : 'Mening sinfim',
          ...cardProps(w)
        },
        () =>
          d.value
            ? h('div', { class: 'tw__class' }, [
                h('div', { class: 'tw__facts' }, [
                  fact(
                    'Bugungi davomat',
                    d.value.todayRate === null
                      ? '— Hali olinmagan'
                      : formatPercent(d.value.todayRate)
                  ),
                  fact("O'zlashtirish", formatPercent(d.value.mastery)),
                  fact('Sifat', formatPercent(d.value.quality)),
                  fact('Ota-onalar botda', formatPercent(d.value.telegramPct))
                ]),
                h(Sparkline, {
                  values: d.value.spark,
                  color: 'var(--chart-primary)',
                  large: true,
                  label: '14 kunlik davomat'
                }),
                h(
                  RouterLink,
                  {
                    class: 'tw__link',
                    to: `/profiles/class/${d.value.classId}`
                  },
                  () => 'Sinf profili'
                )
              ])
            : null
      )
  }
}

const fact = (label, value) =>
  h('div', { class: 'tw__fact' }, [h('small', label), h('b', value)])
