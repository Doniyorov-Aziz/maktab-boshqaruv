<template>
  <page-layout
    icon="gavel"
    color="#b45309"
    title="Ruxsatnomalar"
    :subtitle="`Muallif / huquq egasi ruxsati bilan tarqatiladigan kitoblar · ${schoolStore.activeSchoolName || ''}`"
  >
    <template #actions>
      <q-btn
        outline
        color="primary"
        no-caps
        icon="description"
        label="📄 Ruxsat xati shablonini yuklab olish"
        @click="templateOpen = true"
      />
      <q-btn
        unelevated
        color="primary"
        no-caps
        icon="add"
        label="Ruxsatnoma qo'shish"
        @click="openForm()"
      />
    </template>

    <q-banner dense rounded class="info-banner q-mb-md">
      <template #avatar><q-icon name="info" color="primary" /></template>
      «Muallif ruxsati» yoki «Litsenziya» asosidagi kitob faqat amaldagi
      ruxsatnoma bilan saqlanadi. Muddati tugasa yoki bekor qilinsa, kitoblari
      botdan avtomatik olinadi (har kuni 06:15 da tekshiriladi, 30 kun qolganda
      ogohlantiriladi).
    </q-banner>

    <div class="brand-card q-pa-md q-mb-md">
      <q-btn-toggle
        v-model="status"
        dense
        unelevated
        rounded
        no-caps
        padding="4px 14px"
        toggle-color="primary"
        class="status-filter"
        :options="statusOptions"
      />
    </div>

    <div class="brand-card">
      <q-table
        flat
        :rows="rows"
        :columns="columns"
        row-key="id"
        :loading="loading"
        :pagination="{ rowsPerPage: 20 }"
        no-data-label="Ruxsatnoma yo'q"
      >
        <template #body-cell-grantor="p">
          <q-td :props="p">
            <div class="text-weight-semibold">{{ p.row.grantorFullName }}</div>
            <div class="text-caption muted-text">
              {{ grantors[p.row.grantor]
              }}{{ p.row.phone ? ' · ' + p.row.phone : '' }}
            </div>
          </q-td>
        </template>
        <template #body-cell-term="p">
          <q-td :props="p">
            {{ p.row.validFrom }} — {{ p.row.validUntil || 'muddatsiz' }}
          </q-td>
        </template>
        <template #body-cell-status="p">
          <q-td :props="p">
            <q-badge
              :color="statusInfo(p.row).color"
              :label="statusInfo(p.row).label"
              data-test="permission-status"
            />
          </q-td>
        </template>
        <template #body-cell-books="p">
          <q-td :props="p" style="white-space: normal; min-width: 200px">
            <span v-if="!p.row.books.length" class="muted-text">—</span>
            <q-chip
              v-for="b in p.row.books"
              :key="b.id"
              dense
              :color="b.active ? 'teal-1' : 'grey-3'"
              :text-color="b.active ? 'teal-9' : 'grey-7'"
              :icon="b.active ? 'menu_book' : 'visibility_off'"
              :label="b.title"
            />
          </q-td>
        </template>
        <template #body-cell-actions="p">
          <q-td :props="p" auto-width>
            <q-btn
              flat
              round
              dense
              icon="attach_file"
              aria-label="Imzolangan skanni ko'rish"
              @click="viewDocument(p.row)"
            >
              <q-tooltip>Imzolangan skan</q-tooltip>
            </q-btn>
            <q-btn
              v-if="p.row.status !== 'REVOKED'"
              flat
              round
              dense
              icon="edit"
              aria-label="Tahrirlash"
              @click="openForm(p.row)"
            />
            <q-btn
              v-if="p.row.status !== 'REVOKED'"
              flat
              dense
              no-caps
              color="negative"
              label="Bekor qilish"
              @click="revoke(p.row)"
            />
          </q-td>
        </template>
      </q-table>
    </div>

    <!-- create / edit -->
    <q-dialog v-model="formOpen">
      <q-card style="width: 640px; max-width: 96vw">
        <q-card-section class="row items-center q-pb-none">
          <div class="text-h6">
            {{ form.id ? 'Ruxsatnomani tahrirlash' : 'Yangi ruxsatnoma' }}
          </div>
          <q-space />
          <q-btn
            flat
            round
            dense
            icon="close"
            aria-label="Yopish"
            v-close-popup
          />
        </q-card-section>
        <q-card-section class="q-gutter-sm">
          <q-btn-toggle
            v-model="form.grantor"
            no-caps
            unelevated
            rounded
            toggle-color="primary"
            class="status-filter"
            :options="[
              { value: 'MUALLIF', label: 'Muallif' },
              { value: 'MEROSXOR', label: 'Merosxo\'r' },
              { value: 'NASHRIYOT', label: 'Nashriyot' }
            ]"
          />
          <div class="row q-col-gutter-sm">
            <div class="col-12 col-sm-7">
              <q-input
                v-model="form.grantorFullName"
                dense
                outlined
                maxlength="200"
                label="Ruxsat beruvchining F.I.Sh. *"
              />
            </div>
            <div class="col-12 col-sm-5">
              <q-input
                v-model="form.phone"
                dense
                outlined
                maxlength="40"
                label="Telefon"
              />
            </div>
            <div class="col-6">
              <date-field
                v-model="form.validFrom"
                label="Boshlanish sanasi *"
              />
            </div>
            <div class="col-6">
              <date-field
                v-model="form.validUntil"
                label="Tugash sanasi (bo'sh — muddatsiz)"
              />
            </div>
          </div>
          <q-select
            v-model="form.bookIds"
            dense
            outlined
            multiple
            use-chips
            use-input
            emit-value
            map-options
            input-debounce="300"
            :options="bookOptions"
            label="Kitob(lar)"
            hint="Bog'langan kitoblar «Muallif ruxsati» asosiga o'tadi"
            @filter="filterBooks"
          />
          <div class="text-caption text-weight-semibold q-mt-md">
            Imzolangan ruxsat xati (PDF/JPG/PNG, 10 MB gacha)
            {{ form.id ? '' : '*' }}
          </div>
          <q-file
            v-model="document"
            dense
            outlined
            clearable
            accept="application/pdf,image/jpeg,image/png"
            :label="form.id ? 'Yangi skan (ixtiyoriy)' : 'Skanni tanlang'"
          >
            <template #prepend><q-icon name="upload_file" /></template>
          </q-file>
          <q-input
            v-model="form.note"
            dense
            outlined
            maxlength="1000"
            label="Izoh"
          />
          <div class="text-caption muted-text">
            Doirasi: faqat shu maktab o'quvchilari va ularning ota-onalariga
            bepul elektron (PDF) tarqatish.
          </div>
        </q-card-section>
        <q-card-actions align="right" class="q-px-md q-pb-md">
          <q-btn flat no-caps label="Bekor qilish" v-close-popup />
          <q-btn
            unelevated
            no-caps
            color="primary"
            icon="save"
            label="Saqlash"
            :loading="saving"
            @click="save"
          />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- letter template -->
    <q-dialog v-model="templateOpen">
      <q-card style="width: 520px; max-width: 96vw">
        <q-card-section class="row items-center q-pb-none">
          <div class="text-h6">📄 Ruxsat xati shabloni</div>
          <q-space />
          <q-btn
            flat
            round
            dense
            icon="close"
            aria-label="Yopish"
            v-close-popup
          />
        </q-card-section>
        <q-card-section class="q-gutter-sm">
          <div class="text-caption muted-text">
            Maktab nomi va kitob nomlari oldindan yoziladi. Chop eting, huquq
            egasi to'ldirib imzolaydi, skanini shu yerga yuklaysiz.
          </div>
          <q-select
            v-model="tpl.bookIds"
            dense
            outlined
            multiple
            use-chips
            use-input
            emit-value
            map-options
            input-debounce="300"
            :options="bookOptions"
            label="Kutubxonadagi kitob(lar)"
            @filter="filterBooks"
          />
          <q-input
            v-model="tpl.title"
            dense
            outlined
            label="Yoki asar nomi (hali kutubxonada bo'lmasa)"
          />
          <q-input
            v-model="tpl.grantor"
            dense
            outlined
            label="Huquq egasining F.I.Sh. (ixtiyoriy)"
          />
        </q-card-section>
        <q-card-actions align="right" class="q-px-md q-pb-md">
          <q-btn
            unelevated
            no-caps
            color="primary"
            icon="download"
            label="PDF yuklab olish"
            :loading="downloading"
            @click="downloadTemplate"
          />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </page-layout>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { useQuasar } from 'quasar'
import { api } from '@/boot/axios'
import PageLayout from '@/components/PageLayout.vue'
import DateField from '@/components/DateField.vue'
import { useSchoolStore } from '@/stores/school'
import { todayStr } from '@/utils/date'

const $q = useQuasar()
const schoolStore = useSchoolStore()
const items = ref([])
const loading = ref(false)
const status = ref('all')
const formOpen = ref(false)
const templateOpen = ref(false)
const saving = ref(false)
const downloading = ref(false)
const document = ref(null)
const bookOptions = ref([])
const form = reactive({})
const tpl = reactive({ bookIds: [], title: '', grantor: '' })

const grantors = {
  MUALLIF: 'Muallif',
  MEROSXOR: "Merosxo'r",
  NASHRIYOT: 'Nashriyot'
}

const STATUS = {
  ACTIVE: { label: 'Amalda', color: 'positive' },
  ENDING: { label: 'Tez tugaydi', color: 'warning' },
  EXPIRED: { label: "Muddati o'tgan", color: 'negative' },
  REVOKED: { label: 'Bekor qilingan', color: 'grey-7' },
  NOT_STARTED: { label: 'Hali boshlanmagan', color: 'info' }
}

function statusInfo(p) {
  const s = STATUS[p.status] || { label: p.status, color: 'grey' }
  if (p.status === 'ENDING')
    return { ...s, label: `${p.daysLeft} kunda tugaydi` }
  return s
}

const statusOptions = computed(() => [
  { value: 'all', label: `Hammasi · ${items.value.length}` },
  ...['ACTIVE', 'ENDING', 'EXPIRED', 'REVOKED'].map(s => ({
    value: s,
    label: `${STATUS[s].label} · ${items.value.filter(p => p.status === s).length}`
  }))
])

const rows = computed(() =>
  status.value === 'all'
    ? items.value
    : items.value.filter(p => p.status === status.value)
)

const columns = [
  {
    name: 'grantor',
    label: 'Ruxsat beruvchi',
    field: 'grantorFullName',
    align: 'left'
  },
  { name: 'term', label: 'Muddat', field: 'validFrom', align: 'left' },
  { name: 'status', label: 'Holat', field: 'status', align: 'left' },
  { name: 'books', label: 'Kitoblar', field: 'books', align: 'left' },
  { name: 'actions', label: '', field: 'id', align: 'right' }
]

function notifyError(e) {
  $q.notify({
    type: 'negative',
    message: e.friendlyMessage || 'Xatolik yuz berdi'
  })
}

async function load() {
  if (!schoolStore.activeSchoolId) return
  loading.value = true
  try {
    items.value = (
      await api.get('/api/library/permissions', {
        params: { schoolId: schoolStore.activeSchoolId }
      })
    ).data
  } catch (e) {
    notifyError(e)
  } finally {
    loading.value = false
  }
}

async function filterBooks(text, update) {
  try {
    const res = await api.get('/api/library/books', {
      params: {
        schoolId: schoolStore.activeSchoolId,
        q: text || undefined,
        size: 30
      },
      background: true
    })
    update(() => {
      bookOptions.value = res.data.items.map(b => ({
        value: b.id,
        label: `${b.title}${b.author ? ' — ' + b.author : ''}`
      }))
    })
  } catch {
    update(() => {})
  }
}

function openForm(p) {
  document.value = null
  Object.keys(form).forEach(k => delete form[k])
  Object.assign(
    form,
    p
      ? {
          id: p.id,
          grantor: p.grantor,
          grantorFullName: p.grantorFullName,
          phone: p.phone || '',
          validFrom: p.validFrom,
          validUntil: p.validUntil || '',
          note: p.note || '',
          bookIds: p.books.map(b => b.id)
        }
      : {
          id: null,
          grantor: 'MUALLIF',
          grantorFullName: '',
          phone: '',
          validFrom: todayStr(),
          validUntil: '',
          note: '',
          bookIds: []
        }
  )
  if (p) bookOptions.value = p.books.map(b => ({ value: b.id, label: b.title }))
  formOpen.value = true
}

async function save() {
  if (!form.grantorFullName.trim())
    return warn('Ruxsat beruvchining F.I.Sh. ni kiriting')
  if (!form.validFrom) return warn('Boshlanish sanasini kiriting')
  if (!form.id && !document.value)
    return warn('Imzolangan ruxsat xatining skanini yuklang')
  const data = new FormData()
  data.append(
    'permission',
    new Blob(
      [
        JSON.stringify({
          grantor: form.grantor,
          grantorFullName: form.grantorFullName,
          phone: form.phone || null,
          validFrom: form.validFrom,
          validUntil: form.validUntil || null,
          note: form.note || null,
          bookIds: form.bookIds
        })
      ],
      { type: 'application/json' }
    )
  )
  if (document.value) data.append('document', document.value)
  saving.value = true
  try {
    if (form.id) await api.put(`/api/library/permissions/${form.id}`, data)
    else
      await api.post('/api/library/permissions', data, {
        params: { schoolId: schoolStore.activeSchoolId }
      })
    formOpen.value = false
    $q.notify({ type: 'positive', message: 'Ruxsatnoma saqlandi' })
    load()
  } catch (e) {
    notifyError(e)
  } finally {
    saving.value = false
  }
}

function warn(message) {
  $q.notify({ type: 'warning', message })
}

function revoke(p) {
  $q.dialog({
    title: 'Ruxsatnomani bekor qilish',
    message: `«${p.grantorFullName}» ruxsatnomasi bekor qilinadi va unga bog'langan ${p.books.length} ta kitob darhol botdan olinadi. Davom etamizmi?`,
    cancel: { flat: true, noCaps: true, label: "Yo'q" },
    ok: {
      color: 'negative',
      unelevated: true,
      noCaps: true,
      label: 'Bekor qilish'
    }
  }).onOk(async () => {
    try {
      await api.post(`/api/library/permissions/${p.id}/revoke`)
      $q.notify({
        type: 'positive',
        message: 'Ruxsatnoma bekor qilindi, kitoblari botdan olindi'
      })
      load()
    } catch (e) {
      notifyError(e)
    }
  })
}

async function viewDocument(p) {
  const tab = window.open('', '_blank')
  try {
    const res = await api.get(`/api/library/permissions/${p.id}/document`, {
      responseType: 'blob'
    })
    const url = URL.createObjectURL(res.data)
    if (tab) tab.location.href = url
    setTimeout(() => URL.revokeObjectURL(url), 60_000)
  } catch (e) {
    if (tab) tab.close()
    notifyError(e)
  }
}

async function downloadTemplate() {
  downloading.value = true
  try {
    const res = await api.get('/api/library/permissions/template', {
      params: {
        schoolId: schoolStore.activeSchoolId,
        bookIds: tpl.bookIds.length ? tpl.bookIds.join(',') : undefined,
        title: tpl.title || undefined,
        grantor: tpl.grantor || undefined
      },
      responseType: 'blob'
    })
    const url = URL.createObjectURL(res.data)
    const a = window.document.createElement('a')
    a.href = url
    a.download = 'ruxsat-xati-shablon.pdf'
    window.document.body.appendChild(a)
    a.click()
    a.remove()
    setTimeout(() => URL.revokeObjectURL(url), 10_000)
    templateOpen.value = false
  } catch (e) {
    notifyError(e)
  } finally {
    downloading.value = false
  }
}

watch(() => schoolStore.activeSchoolId, load, { immediate: true })
</script>

<style scoped>
.info-banner {
  background: rgba(99, 102, 241, 0.08);
  border: 1px solid rgba(99, 102, 241, 0.25);
}
.status-filter {
  border: 1px solid rgba(148, 163, 184, 0.45);
  flex-wrap: wrap;
}
.muted-text {
  color: var(--brand-text-muted);
}
</style>
