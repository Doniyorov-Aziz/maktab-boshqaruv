<template>
  <page-layout
    icon="history_edu"
    color="#0f766e"
    title="Mualliflar"
    subtitle="Mualliflik huquqi muallif vafotidan keyin 70 yil amal qiladi (qonun, 35-modda) — holat avtomatik hisoblanadi"
  >
    <template #actions>
      <AppButton
        v-if="authStore.isAdmin"
        icon="person_add"
        label="Muallif qo'shish"
        @click="openForm()"
      />
    </template>

    <div class="brand-card q-pa-md q-mb-md">
      <div class="row q-col-gutter-sm items-center">
        <div class="col-12 col-md-5">
          <AppInput
            v-model="query"
            clearable
            placeholder="Ism, taxallus yoki kirillcha yozilishi"
            aria-label="Qidirish"
          >
            <template #prepend><q-icon name="search" /></template>
          </AppInput>
        </div>
        <div class="col-12 col-md-7">
          <q-btn-toggle
            v-model="status"
            dense
            unelevated
            rounded
            no-caps
            padding="4px 14px"
            toggle-color="primary"
            class="status-filter"
            :options="[
              { value: 'all', label: `Hammasi · ${authors.length}` },
              { value: 'FREE', label: `✅ Jamoat mulki · ${count('FREE')}` },
              {
                value: 'PROTECTED',
                label: `🔒 Himoyada · ${count('PROTECTED')}`
              },
              { value: 'UNKNOWN', label: `❔ Noma'lum · ${count('UNKNOWN')}` }
            ]"
          />
        </div>
      </div>
    </div>

    <div class="brand-card">
      <q-table
        flat
        :rows="rows"
        :columns="columns"
        row-key="id"
        :loading="loading"
        :pagination="{ rowsPerPage: 25, sortBy: 'fullName' }"
        :rows-per-page-options="[25, 50, 0]"
        no-data-label="Muallif topilmadi"
      >
        <template #body-cell-fullName="p">
          <q-td :props="p">
            <div class="text-weight-semibold">{{ p.row.fullName }}</div>
            <div v-if="p.row.aliases" class="text-caption muted-text aliases">
              {{ p.row.aliases.split('\n').join(' · ') }}
            </div>
          </q-td>
        </template>
        <template #body-cell-status="p">
          <q-td :props="p">
            <span
              class="author-status"
              :class="`author-status--${p.row.status.toLowerCase()}`"
              data-test="author-badge"
              >{{ authorStatusText(p.row) }}</span
            >
          </q-td>
        </template>
        <template #body-cell-actions="p">
          <q-td :props="p" auto-width>
            <template v-if="authStore.isAdmin">
              <AppButton
                variant="ghost"
                size="sm"
                icon="edit"
                :aria-label="`${p.row.fullName} — tahrirlash`"
                @click="openForm(p.row)"
              />
              <AppButton
                variant="ghost"
                size="sm"
                color="negative"
                icon="delete"
                :disable="p.row.bookCount > 0"
                :aria-label="`${p.row.fullName} — o'chirish`"
                @click="remove(p.row)"
              >
                <q-tooltip v-if="p.row.bookCount > 0"
                  >Kitoblari bor — o'chirib bo'lmaydi</q-tooltip
                >
              </AppButton>
            </template>
          </q-td>
        </template>
      </q-table>
    </div>

    <q-dialog v-model="formOpen">
      <q-card class="app-dialog app-dialog--md">
        <q-card-section class="row items-center q-pb-none">
          <div class="text-h6">
            {{ form.id ? 'Muallifni tahrirlash' : 'Yangi muallif' }}
          </div>
          <q-space />
          <AppButton
            variant="ghost"
            icon="close"
            aria-label="Yopish"
            v-close-popup
          />
        </q-card-section>
        <q-card-section class="q-gutter-sm">
          <AppInput
            v-model="form.fullName"
            maxlength="200"
            label="To'liq ismi *"
          />
          <AppInput
            v-model="form.aliases"
            autogrow
            type="textarea"
            label="Boshqa yozilishlari va taxalluslari (har biri yangi qatorda)"
            hint="Lotin / kirill, taxallus — qidiruv va Wikisource'da topish uchun"
          />
          <div class="row q-col-gutter-sm">
            <div class="col-6">
              <AppInput
                v-model.number="form.birthYear"
                type="number"
                label="Tug'ilgan yili"
              />
            </div>
            <div class="col-6">
              <AppInput
                v-model.number="form.deathYear"
                type="number"
                label="Vafot yili"
                hint="Bo'sh — tirik yoki noma'lum"
              />
            </div>
          </div>
          <div
            v-if="preview"
            class="author-status q-mt-sm"
            :class="`author-status--${preview.kind}`"
          >
            {{ preview.text }}
          </div>
          <AppInput v-model="form.note" maxlength="1000" label="Izoh" />
        </q-card-section>
        <q-card-actions align="right" class="q-px-md q-pb-md">
          <AppButton variant="ghost" label="Bekor qilish" v-close-popup />
          <AppButton
            icon="save"
            label="Saqlash"
            :loading="saving"
            @click="save"
          />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </page-layout>
</template>

<script setup>
import AppButton from '@/components/app/AppButton.vue'
import AppInput from '@/components/app/AppInput.vue'
import { ref, reactive, computed, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { api } from '@/boot/axios'
import PageLayout from '@/components/PageLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { authorStatusText } from '@/utils/library'

const $q = useQuasar()
const authStore = useAuthStore()
const authors = ref([])
const loading = ref(false)
const query = ref('')
const status = ref('all')
const formOpen = ref(false)
const saving = ref(false)
const form = reactive({
  id: null,
  fullName: '',
  aliases: '',
  birthYear: null,
  deathYear: null,
  note: ''
})

const columns = [
  {
    name: 'fullName',
    label: 'Muallif',
    field: 'fullName',
    align: 'left',
    sortable: true
  },
  {
    name: 'years',
    label: 'Hayot yillari',
    field: r =>
      r.birthYear || r.deathYear
        ? `${r.birthYear || '?'}–${r.deathYear || ''}`
        : '—',
    align: 'left'
  },
  {
    name: 'status',
    label: 'Holat',
    field: 'status',
    align: 'left',
    sortable: true
  },
  {
    name: 'books',
    label: 'Kitoblar',
    field: 'bookCount',
    align: 'right',
    sortable: true
  },
  { name: 'actions', label: '', field: 'id', align: 'right' }
]

const rows = computed(() => {
  const q = (query.value || '').trim().toLowerCase()
  return authors.value.filter(
    a =>
      (status.value === 'all' || a.status === status.value) &&
      (!q || `${a.fullName} ${a.aliases || ''}`.toLowerCase().includes(q))
  )
})

function count(s) {
  return authors.value.filter(a => a.status === s).length
}

/** The same rule as the server (qonun, 35-modda), for the form preview. */
const preview = computed(() => {
  const d = Number(form.deathYear)
  if (!d)
    return {
      kind: 'unknown',
      text: "❔ Vafot yili kiritilmagan — jamoat mulki ekanini aniqlab bo'lmaydi"
    }
  const year = new Date().getFullYear()
  return d + 70 < year
    ? { kind: 'free', text: `✅ Jamoat mulki (${d + 71}-yildan)` }
    : { kind: 'protected', text: `🔒 Himoyada (${d + 70}-yilgacha)` }
})

function notifyError(e) {
  $q.notify({
    type: 'negative',
    message: e.friendlyMessage || 'Xatolik yuz berdi'
  })
}

async function load() {
  loading.value = true
  try {
    authors.value = (await api.get('/api/library/authors')).data
  } catch (e) {
    notifyError(e)
  } finally {
    loading.value = false
  }
}

function openForm(a) {
  Object.assign(
    form,
    a
      ? {
          id: a.id,
          fullName: a.fullName,
          aliases: a.aliases || '',
          birthYear: a.birthYear,
          deathYear: a.deathYear,
          note: a.note || ''
        }
      : {
          id: null,
          fullName: '',
          aliases: '',
          birthYear: null,
          deathYear: null,
          note: ''
        }
  )
  formOpen.value = true
}

async function save() {
  if (!form.fullName.trim()) {
    $q.notify({ type: 'warning', message: 'Muallif ismini kiriting' })
    return
  }
  saving.value = true
  const body = {
    fullName: form.fullName,
    aliases: form.aliases || null,
    birthYear: form.birthYear || null,
    deathYear: form.deathYear || null,
    note: form.note || null
  }
  try {
    if (form.id) await api.put(`/api/library/authors/${form.id}`, body)
    else await api.post('/api/library/authors', body)
    formOpen.value = false
    $q.notify({ type: 'positive', message: 'Muallif saqlandi' })
    load()
  } catch (e) {
    notifyError(e)
  } finally {
    saving.value = false
  }
}

function remove(a) {
  $q.dialog({
    title: "Muallifni o'chirish",
    message: `«${a.fullName}» ma'lumotnomadan o'chirilsinmi?`,
    cancel: { flat: true, noCaps: true, label: 'Bekor qilish' },
    ok: {
      color: 'negative',
      unelevated: true,
      noCaps: true,
      label: "O'chirish"
    }
  }).onOk(async () => {
    try {
      await api.delete(`/api/library/authors/${a.id}`)
      load()
    } catch (e) {
      notifyError(e)
    }
  })
}

onMounted(load)
</script>

<style scoped>
.author-status {
  font-size: 12px;
  font-weight: 600;
  border-radius: var(--radius-sm);
  padding: 3px 8px;
  display: inline-block;
  white-space: nowrap;
}
.author-status--free {
  color: #047857;
  background: rgba(16, 185, 129, 0.12);
}
.author-status--protected {
  color: #b91c1c;
  background: rgba(239, 68, 68, 0.12);
}
.author-status--unknown {
  color: #92400e;
  background: rgba(245, 158, 11, 0.14);
}
:global(.body--dark) .author-status--free {
  color: #6ee7b7;
}
:global(.body--dark) .author-status--protected {
  color: #fca5a5;
}
:global(.body--dark) .author-status--unknown {
  color: #fcd34d;
}
.aliases {
  max-width: var(--cell-max-lg);
  white-space: normal;
}
.status-filter {
  border: 1px solid rgba(148, 163, 184, 0.45);
  flex-wrap: wrap;
}
.muted-text {
  color: var(--brand-text-muted);
}
</style>
