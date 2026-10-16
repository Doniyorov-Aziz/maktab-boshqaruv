<template>
  <page-layout
    icon="tune"
    color="#475569"
    title="Maktab sozlamalari"
    :subtitle="subtitle"
  >
    <template #actions>
      <AppButton
        icon="save"
        label="Saqlash"
        :loading="saving"
        :disable="!form"
        @click="save"
      />
    </template>

    <div v-if="!form" class="brand-card q-pa-md">
      <q-skeleton v-for="i in 6" :key="i" type="text" class="q-mb-sm" />
    </div>

    <div v-else class="app-stack">
      <section class="brand-card q-pa-md settings-card">
        <div class="settings-card__head">
          <IconTile icon="smart_toy" size="32" color="var(--chart-primary)" />
          <div>
            <div class="text-subtitle1 text-weight-bold"
              >Davomat avtomatikasi</div
            >
            <div class="text-caption muted">
              Qo'lda qo'yilgan davomatga avtomatika hech qachon tegmaydi — u
              faqat bo'sh joylarni to'ldiradi.
            </div>
          </div>
        </div>
        <div class="settings-grid">
          <q-toggle
            v-model="form.autoCarryEnabled"
            label="Holatni keyingi darslarga ko'chirish (AUTO_CARRY)"
          />
          <AppInput
            v-model.number="form.autoCarryDelayMinutes"
            type="number"
            label="Dars boshlanganidan necha daqiqa keyin ko'chiriladi"
            :disable="!form.autoCarryEnabled"
          />
          <AppInput
            v-model.number="form.reminderMinutes"
            type="number"
            label="O'qituvchiga eslatma (daqiqa)"
          />
          <AppInput
            v-model.number="form.escalationMinutes"
            type="number"
            label="«E'tibor talab qiladi» ga tushishi (daqiqa)"
          />
          <q-toggle
            v-model="form.notifyOnAuto"
            label="Avtomatik «kelmadi» bo'yicha ham ota-onaga xabar yuborish"
          />
        </div>
        <div class="text-caption muted q-mt-sm">
          Ko'chirish: oldingi darsda Keldi → Keldi, Kechikdi → Keldi, Kelmadi →
          Kelmadi, Sababli → Sababli. Oldingi darsda belgisi yo'q o'quvchiga
          hech narsa yozilmaydi.
        </div>
      </section>

      <section class="brand-card q-pa-md settings-card">
        <div class="settings-card__head">
          <IconTile icon="insights" size="32" color="var(--chart-secondary)" />
          <div>
            <div class="text-subtitle1 text-weight-bold"
              >Bosh sahifa chegaralari</div
            >
            <div class="text-caption muted"
              >«E'tibor talab qiladi», maqsad chizig'i va davomat xaritasi
              ranglari.</div
            >
          </div>
        </div>
        <div class="settings-grid">
          <AppInput
            v-model.number="form.attendanceTarget"
            type="number"
            label="Davomat maqsadi, %"
          />
          <AppInput
            v-model.number="form.absentStreakDays"
            type="number"
            label="Ketma-ket kelmagan kunlar"
          />
          <AppInput
            v-model.number="form.disciplineThreshold"
            type="number"
            label="O'qituvchi intizomi chegarasi, %"
          />
          <AppInput
            v-model.number="form.appealOverdueHours"
            type="number"
            label="Murojaatga javob muddati, soat"
          />
          <AppInput
            v-model.number="form.telegramLowPercent"
            type="number"
            label="Telegram ulanishi past, % dan kam"
          />
          <AppInput
            v-model.number="form.heatGood"
            type="number"
            label="Xarita: to'q yashil, % dan"
          />
          <AppInput
            v-model.number="form.heatOk"
            type="number"
            label="Xarita: yashil, % dan"
          />
          <AppInput
            v-model.number="form.heatWarn"
            type="number"
            label="Xarita: sariq, % dan (pasti qizil)"
          />
        </div>
      </section>

      <section class="brand-card q-pa-md settings-card">
        <div class="settings-card__head">
          <IconTile
            icon="monitor_heart"
            size="32"
            color="var(--chart-primary)"
          />
          <div>
            <div class="text-subtitle1 text-weight-bold"
              >Maktab salomatlik indeksi</div
            >
            <div class="text-caption muted"
              >Bosh sahifadagi indeks to'rt qismdan iborat; og'irliklar
              yig'indisi 100 bo'lishi kerak (hozir {{ weightSum }}).</div
            >
          </div>
        </div>
        <div class="settings-grid">
          <AppInput
            v-model.number="form.healthWeightAttendance"
            type="number"
            label="Davomat, %"
          />
          <AppInput
            v-model.number="form.healthWeightQuality"
            type="number"
            label="Sifat (4 va 5), %"
          />
          <AppInput
            v-model.number="form.healthWeightJournal"
            type="number"
            label="Jurnal to'liqligi, %"
          />
          <AppInput
            v-model.number="form.healthWeightTelegram"
            type="number"
            label="Ota-onalar Telegramda, %"
          />
        </div>
      </section>

      <section class="brand-card q-pa-md settings-card">
        <div class="settings-card__head">
          <IconTile icon="sensors" size="32" color="var(--st-present)" />
          <div class="col">
            <div class="text-subtitle1 text-weight-bold"
              >Qurilmalar (turniket, QR, Face ID)</div
            >
            <div class="text-caption muted">
              Qurilma kelishni <code>POST /api/attendance/checkin</code> ga shu
              kalit bilan yuboradi. Kalit faqat bir marta ko'rsatiladi.
            </div>
          </div>
          <AppButton
            variant="secondary"
            size="sm"
            icon="add"
            label="Kalit yaratish"
            @click="newKeyOpen = true"
          />
        </div>
        <div v-if="!keys.length" class="text-caption muted"
          >Hali kalit yo'q.</div
        >
        <q-list v-else separator>
          <q-item v-for="k in keys" :key="k.id">
            <q-item-section>
              <q-item-label
                >{{ k.name }}
                <span class="muted">· {{ k.prefix }}…</span></q-item-label
              >
              <q-item-label caption>
                {{ fmt(k.createdAt) }} · {{ k.createdBy || '—' }}
                <template v-if="k.lastUsedAt">
                  · oxirgi marta {{ fmt(k.lastUsedAt) }}</template
                >
              </q-item-label>
            </q-item-section>
            <q-item-section side>
              <q-badge v-if="k.revokedAt" color="grey">Bekor qilingan</q-badge>
              <AppButton
                v-else
                variant="ghost"
                size="sm"
                color="negative"
                label="Bekor qilish"
                @click="revoke(k)"
              />
            </q-item-section>
          </q-item>
        </q-list>
      </section>

      <section class="brand-card q-pa-md settings-card">
        <div class="settings-card__head">
          <IconTile icon="science" size="32" color="var(--gr-3)" />
          <div class="col">
            <div class="text-subtitle1 text-weight-bold">Demo rejim</div>
            <div class="text-caption muted">
              <template v-if="demo">
                Demo yoqilgan: qo'lda kiritilmagan davomat va baholar har
                daqiqada realistik taqsimot bilan to'ldiriladi (manba «DEMO»,
                🤖). Ota-onalarga xabar yuborilmaydi; qo'lda kiritilganlarga
                tegilmaydi.
              </template>
              <template v-else>
                Demo o'chiq. U faqat <code>application.properties</code> da
                (<code>app.demo.enabled=true</code>) yoqiladi va «prod»
                profilida ishlamaydi.
              </template>
            </div>
          </div>
        </div>
        <div class="row q-gutter-sm">
          <AppButton
            v-if="demo"
            variant="secondary"
            icon="play_arrow"
            label="Hozir to'ldirish"
            :loading="busy === 'run'"
            @click="runNow"
          />
          <AppButton
            v-if="demo"
            variant="secondary"
            icon="history"
            label="Oxirgi 30 kunni to'ldirish"
            :loading="busy === 'fill'"
            @click="backfill"
          />
          <AppButton
            variant="danger"
            icon="delete_sweep"
            label="Demo ma'lumotlarini tozalash"
            :loading="busy === 'clear'"
            @click="clearDemo"
          />
        </div>
      </section>
    </div>

    <q-dialog v-model="newKeyOpen">
      <q-card class="app-dialog app-dialog--sm">
        <q-card-section class="text-h6">Qurilma kaliti</q-card-section>
        <q-card-section v-if="!createdKey">
          <AppInput
            v-model="newKeyName"
            label="Qurilma nomi (masalan «Asosiy kirish turniketi»)"
            autofocus
          />
        </q-card-section>
        <q-card-section v-else>
          <div class="text-caption muted q-mb-sm"
            >Kalitni hozir nusxalab oling — u boshqa ko'rsatilmaydi.</div
          >
          <AppInput :model-value="createdKey" readonly>
            <template #append>
              <AppButton
                variant="ghost"
                size="sm"
                icon="content_copy"
                aria-label="Nusxalash"
                @click="copyKey"
              />
            </template>
          </AppInput>
        </q-card-section>
        <q-card-actions align="right">
          <AppButton
            variant="ghost"
            label="Yopish"
            v-close-popup
            @click="createdKey = ''"
          />
          <AppButton
            v-if="!createdKey"
            label="Yaratish"
            :disable="!newKeyName.trim()"
            @click="createKey"
          />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </page-layout>
</template>

<script setup>
// "Maktab sozlamalari" (ADMIN): attendance automation, the home page's thresholds, device
// keys, and the demo tools (the simulator itself is switched on in application.properties only).
import { ref, computed, watch, onMounted } from 'vue'
import { useQuasar, copyToClipboard } from 'quasar'
import PageLayout from '@/components/PageLayout.vue'
import AppButton from '@/components/app/AppButton.vue'
import AppInput from '@/components/app/AppInput.vue'
import IconTile from '@/components/app/IconTile.vue'
import { api } from '@/boot/axios'
import { useSchoolStore } from '@/stores/school'

const $q = useQuasar()
const school = useSchoolStore()
const form = ref(null)
const saving = ref(false)
const keys = ref([])
const demo = ref(false)
const busy = ref('')
const newKeyOpen = ref(false)
const newKeyName = ref('')
const createdKey = ref('')

const subtitle = computed(() =>
  form.value?.saved
    ? `Oxirgi o'zgarish: ${fmt(form.value.updatedAt)} · ${form.value.updatedBy || ''}`
    : 'Standart qiymatlar (hali saqlanmagan)'
)
const fmt = iso => (iso ? String(iso).replace('T', ' ').slice(0, 16) : '')
const fail = e =>
  $q.notify({
    type: 'negative',
    message: e.friendlyMessage || 'Xatolik yuz berdi'
  })

async function load() {
  if (!school.activeSchoolId) return
  const params = { schoolId: school.activeSchoolId }
  try {
    const [s, k, info] = await Promise.all([
      api.get('/api/school-settings', { params }),
      api.get('/api/device-keys', { params }),
      api.get('/api/system/info')
    ])
    form.value = s.data
    keys.value = k.data
    demo.value = !!info.data.demoSimulator
  } catch (e) {
    fail(e)
  }
}

async function save() {
  saving.value = true
  try {
    const res = await api.put('/api/school-settings', form.value, {
      params: { schoolId: school.activeSchoolId }
    })
    form.value = res.data
    $q.notify({
      type: 'positive',
      message: 'Sozlamalar saqlandi',
      icon: 'check_circle'
    })
  } catch (e) {
    fail(e)
  } finally {
    saving.value = false
  }
}

async function createKey() {
  try {
    const res = await api.post('/api/device-keys', {
      schoolId: school.activeSchoolId,
      name: newKeyName.value.trim()
    })
    createdKey.value = res.data.apiKey
    newKeyName.value = ''
    keys.value = [res.data.key, ...keys.value]
  } catch (e) {
    fail(e)
  }
}

function copyKey() {
  copyToClipboard(createdKey.value).then(() =>
    $q.notify({ type: 'positive', message: 'Nusxalandi' })
  )
}

function revoke(k) {
  $q.dialog({
    title: 'Kalitni bekor qilish',
    message: `«${k.name}» kaliti shu zahoti ishlamay qoladi.`,
    cancel: { label: 'Yo‘q', flat: true, noCaps: true },
    ok: {
      label: 'Bekor qilish',
      color: 'negative',
      noCaps: true,
      unelevated: true
    }
  }).onOk(async () => {
    try {
      const res = await api.delete(`/api/device-keys/${k.id}`)
      keys.value = keys.value.map(x => (x.id === k.id ? res.data : x))
    } catch (e) {
      fail(e)
    }
  })
}

async function runNow() {
  busy.value = 'run'
  try {
    const res = await api.post('/api/demo/run', null, {
      params: { schoolId: school.activeSchoolId }
    })
    $q.notify({
      type: 'positive',
      message: `Bugun: ${res.data.attendance} ta davomat, ${res.data.grades} ta baho qo'shildi`
    })
  } catch (e) {
    fail(e)
  } finally {
    busy.value = ''
  }
}

async function backfill() {
  busy.value = 'fill'
  try {
    const res = await api.post('/api/demo/backfill', null, {
      params: { schoolId: school.activeSchoolId, days: 30 }
    })
    $q.notify({
      type: 'positive',
      message: `${res.data.attendance} ta davomat va ${res.data.grades} ta baho qo'shildi (qo'lda kiritilganlarga tegilmadi)`
    })
  } catch (e) {
    fail(e)
  } finally {
    busy.value = ''
  }
}

function clearDemo() {
  $q.dialog({
    title: "Demo ma'lumotlarini tozalash",
    message:
      "Faqat «DEMO» manbali davomat va baholar o'chiriladi. Qo'lda, avtomatik va qurilma yozuvlariga tegilmaydi.",
    cancel: { label: 'Yo‘q', flat: true, noCaps: true },
    ok: { label: 'Tozalash', color: 'negative', noCaps: true, unelevated: true }
  }).onOk(async () => {
    busy.value = 'clear'
    try {
      const res = await api.delete('/api/demo', {
        params: { schoolId: school.activeSchoolId }
      })
      $q.notify({
        type: 'positive',
        message: `${res.data.attendance} ta davomat va ${res.data.grades} ta baho o'chirildi`
      })
    } catch (e) {
      fail(e)
    } finally {
      busy.value = ''
    }
  })
}

const weightSum = computed(() =>
  form.value
    ? (form.value.healthWeightAttendance || 0) +
      (form.value.healthWeightQuality || 0) +
      (form.value.healthWeightJournal || 0) +
      (form.value.healthWeightTelegram || 0)
    : 0
)

onMounted(load)
watch(() => school.activeSchoolId, load)
</script>

<style scoped>
.muted {
  color: var(--brand-text-muted);
}

.settings-card > * {
  max-width: var(--form-max);
}

.settings-card__head {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}

.settings-grid {
  display: grid;
  grid-template-columns: repeat(
    auto-fill,
    minmax(min(var(--field-w), 100%), 1fr)
  );
  gap: var(--space-3) var(--space-4);
  align-items: center;
}
</style>
