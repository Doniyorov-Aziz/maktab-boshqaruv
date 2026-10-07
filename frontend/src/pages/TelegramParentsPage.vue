<template>
  <page-layout
    icon="family_restroom"
    color="#229ed9"
    title="Ota-onalar (Telegram)"
    :subtitle="`Sinf bo'yicha botga ulanish holati · ${schoolStore.activeSchoolName || ''}`"
  >
    <template #actions>
      <q-btn
        v-if="authStore.isEditor && notLinkedWithLink.length"
        outline
        no-caps
        color="primary"
        icon="content_copy"
        :label="`Taklif havolalari (${notLinkedWithLink.length})`"
        @click="copyAll"
      >
        <q-tooltip
          >Ulanmagan ota-onalar uchun havolalar ro'yxatini nusxalash (sinf
          chatiga yuborish uchun)</q-tooltip
        >
      </q-btn>
    </template>

    <div class="brand-card q-pa-md q-mb-md">
      <div class="row items-center q-col-gutter-md">
        <div class="col-12 col-sm-4 col-md-3">
          <q-select
            v-model="classId"
            outlined
            dense
            emit-value
            map-options
            options-dense
            label="Sinf"
            :options="classOptions"
          />
        </div>
        <div class="col-12 col-sm-8 col-md">
          <q-btn-toggle
            v-model="status"
            no-caps
            unelevated
            rounded
            toggle-color="primary"
            class="status-toggle"
            :options="[
              {
                label: `Barchasi${data ? ' · ' + data.total : ''}`,
                value: 'ALL'
              },
              {
                label: `Botga ulanganlar${data ? ' · ' + data.linked : ''}`,
                value: 'LINKED'
              },
              {
                label: `Ulanmaganlar${data ? ' · ' + (data.total - data.linked) : ''}`,
                value: 'NOT_LINKED'
              }
            ]"
          />
        </div>
        <div v-if="data" class="col-12 col-md-auto">
          <div class="row items-center no-wrap q-gutter-x-sm coverage">
            <q-circular-progress
              :value="percent"
              size="44px"
              :thickness="0.18"
              :color="
                percent >= 70
                  ? 'positive'
                  : percent >= 30
                    ? 'warning'
                    : 'negative'
              "
              :track-color="$q.dark.isActive ? 'grey-9' : 'grey-3'"
              show-value
              class="text-weight-bold"
              >{{ percent }}%</q-circular-progress
            >
            <div class="text-caption muted-text">
              {{ data.linked }} / {{ data.total }} o'quvchining<br />ota-onasi
              ulangan
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="brand-card overflow-hidden">
      <q-table
        :rows="rows"
        :columns="columns"
        row-key="studentId"
        flat
        class="brand-table"
        :loading="loading"
        :pagination="{ rowsPerPage: 0 }"
        hide-pagination
      >
        <template v-slot:loading>
          <q-inner-loading showing color="primary" />
        </template>
        <template v-slot:no-data>
          <div class="full-width column flex-center q-py-lg muted-text">
            <q-icon name="groups" size="40px" class="q-mb-sm" />
            {{
              !classId
                ? 'Sinfni tanlang'
                : status === 'LINKED'
                  ? "Bu sinfda botga ulangan ota-ona yo'q"
                  : status === 'NOT_LINKED'
                    ? 'Hamma ota-onalar ulangan 🎉'
                    : "Sinfda o'quvchi yo'q"
            }}
          </div>
        </template>
        <template v-slot:body-cell-student="props">
          <q-td :props="props">
            <router-link
              :to="`/profiles/student/${props.row.studentId}`"
              class="student-link"
              >{{ props.row.studentName }}</router-link
            >
          </q-td>
        </template>
        <template v-slot:body-cell-parent="props">
          <q-td :props="props">
            <div>{{
              guardianLabel(
                props.row.guardianName,
                props.row.guardianRelation
              ) || '—'
            }}</div>
            <div
              v-for="p in props.row.parents"
              :key="p.linkId"
              class="text-caption muted-text"
            >
              <q-icon name="send" size="12px" color="info" />
              {{ p.name || 'Telegram'
              }}<template v-if="p.username"> · @{{ p.username }}</template>
            </div>
          </q-td>
        </template>
        <template v-slot:body-cell-telegram="props">
          <q-td :props="props">
            <div
              v-if="props.row.linked"
              class="row items-center no-wrap q-gutter-x-xs"
            >
              <q-icon name="check_circle" color="positive" size="18px" />
              <div>
                <div class="text-weight-medium">Botga ulangan</div>
                <div class="text-caption muted-text">
                  {{
                    formatDate(
                      String(firstLinked(props.row) || '').slice(0, 10)
                    )
                  }}
                  <template v-if="props.row.parents.length > 1">
                    · {{ props.row.parents.length }} ta ota-ona</template
                  >
                </div>
              </div>
            </div>
            <div v-else class="row items-center no-wrap q-gutter-x-xs">
              <q-icon name="cancel" color="negative" size="18px" />
              <span class="text-weight-medium">Ulanmagan</span>
            </div>
          </q-td>
        </template>
        <template v-slot:body-cell-chatId="props">
          <q-td :props="props" class="tabular-nums muted-text">
            <div v-for="p in props.row.parents" :key="p.linkId">
              {{ p.chatId }}
            </div>
            <span v-if="!props.row.parents.length">—</span>
          </q-td>
        </template>
        <template v-slot:body-cell-actions="props">
          <q-td :props="props" class="text-right">
            <q-btn
              v-if="props.row.inviteLink"
              flat
              dense
              no-caps
              size="sm"
              color="primary"
              icon="link"
              label="Taklif havolasini nusxalash"
              @click="copy(props.row)"
            />
          </q-td>
        </template>
      </q-table>
    </div>
  </page-layout>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useQuasar, copyToClipboard } from 'quasar'
import { useRoute, useRouter } from 'vue-router'
import { api } from '@/boot/axios'
import PageLayout from '@/components/PageLayout.vue'
import { useSchoolStore } from '@/stores/school'
import { useAuthStore } from '@/stores/auth'
import { guardianLabel } from '@/utils/guardian'
import { formatDate } from '@/utils/date'

const $q = useQuasar()
const route = useRoute()
const router = useRouter()
const schoolStore = useSchoolStore()
const authStore = useAuthStore()

const classOptions = ref([])
const classId = ref(route.query.class ? Number(route.query.class) : null)
const status = ref(
  ['LINKED', 'NOT_LINKED'].includes(route.query.status)
    ? route.query.status
    : 'ALL'
)
const data = ref(null)
const loading = ref(false)

const rows = computed(() => data.value?.rows || [])
const percent = computed(() =>
  data.value?.total
    ? Math.round((data.value.linked / data.value.total) * 100)
    : 0
)
const notLinkedWithLink = computed(() => rows.value.filter(r => r.inviteLink))

const columns = computed(() => [
  { name: 'student', label: "O'quvchi", field: 'studentName', align: 'left' },
  { name: 'parent', label: 'Ota-ona', field: 'guardianName', align: 'left' },
  {
    name: 'phone',
    label: 'Telefon',
    field: 'guardianPhone',
    align: 'left',
    format: v => v || '—',
    classes: 'tabular-nums'
  },
  {
    name: 'telegram',
    label: 'Telegram holati',
    field: 'linked',
    align: 'left'
  },
  ...(authStore.isAdmin
    ? [{ name: 'chatId', label: 'Chat ID', field: 'studentId', align: 'left' }]
    : []),
  { name: 'actions', label: '', field: 'studentId', align: 'right' }
])

function firstLinked(row) {
  return row.parents
    .map(p => p.linkedAt)
    .filter(Boolean)
    .sort()[0]
}

async function copy(row) {
  await copyToClipboard(row.inviteLink)
  $q.notify({
    type: 'positive',
    icon: 'link',
    message: `${row.studentName}: taklif havolasi nusxalandi`
  })
}

async function copyAll() {
  const className = data.value?.className || ''
  const lines = notLinkedWithLink.value.map(
    r => `${r.studentName} (${className}): ${r.inviteLink}`
  )
  await copyToClipboard(
    `Hurmatli ota-onalar! Maktab botiga ulanish uchun farzandingiz qatoridagi havolani bosing:\n\n${lines.join('\n')}`
  )
  $q.notify({
    type: 'positive',
    icon: 'content_copy',
    message: `${lines.length} ta havola nusxalandi`
  })
}

async function loadClasses() {
  const res = await api.get('/api/school-classes', {
    params: { schoolId: schoolStore.activeSchoolId, size: 300 }
  })
  classOptions.value = res.data.content
    .map(c => ({
      value: c.id,
      label: `${c.gradeNumber}-${c.sectionLetter}`,
      g: c.gradeNumber,
      s: c.sectionLetter
    }))
    .sort((a, b) => a.g - b.g || a.s.localeCompare(b.s))
  if (!classOptions.value.some(c => c.value === classId.value))
    classId.value = classOptions.value[0]?.value ?? null
}

async function load() {
  if (!classId.value) {
    data.value = null
    return
  }
  loading.value = true
  try {
    data.value = (
      await api.get('/api/telegram/parents', {
        params: { classId: classId.value, status: status.value }
      })
    ).data
  } catch (error) {
    $q.notify({
      type: 'negative',
      message: error.friendlyMessage || 'Xatolik yuz berdi'
    })
  } finally {
    loading.value = false
  }
}

watch([classId, status], ([c, s]) => {
  router.replace({
    query: {
      ...route.query,
      class: c || undefined,
      status: s === 'ALL' ? undefined : s
    }
  })
  load()
})

watch(
  () => schoolStore.activeSchoolId,
  async id => {
    if (!id) return
    await loadClasses().catch(() => {})
    load()
  },
  { immediate: true }
)
</script>

<style scoped>
.muted-text {
  color: var(--brand-text-muted);
}

.status-toggle {
  flex-wrap: wrap;
}

.coverage {
  line-height: 1.3;
}

.student-link {
  color: var(--text-primary);
  font-weight: 600;
  text-decoration: none;
}

.student-link:hover {
  color: var(--color-brand);
  text-decoration: underline;
}
</style>
