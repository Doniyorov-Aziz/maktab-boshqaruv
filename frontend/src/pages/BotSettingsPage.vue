<template>
  <page-layout
    icon="smart_toy"
    color="#64748b"
    title="Bot sozlamalari"
    :subtitle="`«🏫 Maktab haqida» ma'lumotlari va avtomatik xabarlar · ${schoolStore.activeSchoolName || ''}`"
  >
    <template #actions>
      <q-chip
        v-if="status"
        square
        :color="botModes[status.mode]?.color"
        text-color="white"
        :icon="botModes[status.mode]?.icon"
      >
        {{ botModes[status.mode]?.label }}
        <template v-if="status.botUsername">
          · @{{ status.botUsername }}</template
        >
      </q-chip>
    </template>

    <q-banner
      v-if="status && !status.active"
      rounded
      class="brand-card q-mb-md warn-banner"
    >
      <template v-slot:avatar>
        <q-icon name="warning" color="warning" />
      </template>
      Bot hali ulanmagan — sozlamalarni saqlash mumkin, lekin ota-onalar
      hozircha hech narsa olmaydi. Ulash yo'riqnomasi:
      <router-link to="/notifications" class="link">Xabarnomalar</router-link>
      sahifasida.
    </q-banner>

    <div v-if="!form" class="brand-card q-pa-md">
      <q-skeleton v-for="i in 6" :key="i" type="QInput" class="q-mb-md" />
    </div>

    <div v-else class="row q-col-gutter-md">
      <div class="col-12 col-lg-6">
        <div class="brand-card q-pa-md section-card settings-card">
          <div class="section-title"
            ><q-icon name="apartment" /> Maktab kontaktlari</div
          >
          <div class="text-caption muted-text q-mb-md">
            Ota-onalar botdagi «🏫 Maktab haqida» bo'limida ko'radi.
          </div>
          <AppInput
            v-model="form.phone"
            label="Telefon"
            placeholder="+998 71 200-00-00"
            class="q-mb-md"
            :disable="!canEdit"
          />
          <AppInput
            v-model="form.directorName"
            label="Direktor (F.I.Sh.)"
            class="q-mb-md"
            :disable="!canEdit"
          />
          <AppInput
            v-model="form.receptionHours"
            autogrow
            type="textarea"
            label="Direktor qabul kunlari"
            placeholder="Dushanba, chorshanba 14:00–16:00"
            class="q-mb-md"
            :disable="!canEdit"
          />
          <q-toggle
            v-model="form.showTeacherPhones"
            color="primary"
            :disable="!canEdit"
            label="O'qituvchilar telefonini ota-onalarga ko'rsatish"
          />
          <div class="text-caption muted-text q-ml-sm">
            O'chiq bo'lsa, ota-onalar «✉️ Ma'muriyatga xat» orqali bog'lanadi.
          </div>
        </div>
      </div>

      <div class="col-12 col-lg-6">
        <div class="brand-card q-pa-md section-card settings-card">
          <div class="section-title"
            ><q-icon name="schedule_send" /> Avtomatik xabarlar vaqti</div
          >
          <div class="text-caption muted-text q-mb-md">
            Toshkent vaqti. Ota-ona «⚙️ Sozlamalar»da ertangi jadval vaqtini
            o'zi o'zgartira oladi.
          </div>
          <div class="row q-col-gutter-md">
            <div class="col-12 col-sm-6">
              <AppTimeInput
                hide-bottom-space
                v-model="form.tomorrowScheduleTime"
                label="Ertangi dars jadvali"
                :disable="!canEdit"
              />
            </div>
            <div class="col-12 col-sm-6">
              <AppTimeInput
                hide-bottom-space
                v-model="form.eventReminderTime"
                label="Tadbir eslatmasi (1 kun oldin)"
                :disable="!canEdit"
              />
            </div>
            <div class="col-12 col-sm-6">
              <AppSelect
                v-model="form.weeklyReportDay"
                :options="dayOptions"
                emit-value
                map-options
                label="Haftalik hisobot kuni"
                :disable="!canEdit"
              />
            </div>
            <div class="col-12 col-sm-6">
              <AppTimeInput
                hide-bottom-space
                v-model="form.weeklyReportTime"
                label="Haftalik hisobot vaqti"
                :disable="!canEdit"
              />
            </div>
            <div class="col-12">
              <div class="text-caption muted-text q-mb-xs"
                >Past baho chegarasi</div
              >
              <q-btn-toggle
                v-model="form.lowGradeThreshold"
                no-caps
                unelevated
                rounded
                toggle-color="primary"
                :disable="!canEdit"
                :options="[
                  { label: '≤ 2', value: 2 },
                  { label: '≤ 3', value: 3 }
                ]"
              />
              <div class="text-caption muted-text q-mt-xs">
                Shu yoki undan past baho qo'yilsa, ota-onaga alohida, mehribon
                ohangdagi xabar boradi.
              </div>
            </div>
            <div class="col-12">
              <q-toggle
                v-model="form.libraryWeeklyEnabled"
                color="primary"
                :disable="!canEdit"
                label="📚 Haftalik kitob tavsiyasi (juma 19:00)"
              />
              <div class="text-caption muted-text q-ml-sm">
                «Dam olish kunlari uchun 3 ta kitob» — har bir ota-onaga o'z
                tavsiyalaridan. Ota-ona botdagi «⚙️ Sozlamalar»da o'zi ham
                o'chira oladi.
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="col-12">
        <div class="brand-card q-pa-md">
          <div class="section-title"
            ><q-icon name="notifications" /> Qo'ng'iroq jadvali</div
          >
          <div class="row q-col-gutter-md">
            <div class="col-12 col-md-5">
              <div class="text-caption muted-text q-mb-sm"
                >Dars jadvalidan avtomatik olingan:</div
              >
              <div v-if="!form.derivedBells?.length" class="muted-text"
                >Dars jadvali hali kiritilmagan</div
              >
              <div class="bells">
                <div
                  v-for="b in form.derivedBells"
                  :key="b"
                  class="bell-chip tabular-nums"
                  >{{ b }}</div
                >
              </div>
            </div>
            <div class="col-12 col-md-7">
              <AppInput
                v-model="form.bellScheduleNote"
                autogrow
                type="textarea"
                label="Qo'lda yozilgan jadval (ixtiyoriy)"
                hint="To'ldirilsa, avtomatik jadval o'rniga shu matn ko'rsatiladi (masalan, qisqartirilgan kunlar uchun)"
                :disable="!canEdit"
              />
            </div>
          </div>
        </div>
      </div>

      <div class="col-12 row justify-end">
        <div
          v-if="!canEdit"
          class="text-caption muted-text q-mr-md self-center"
        >
          Faqat administrator o'zgartira oladi.
        </div>
        <AppButton
          v-if="canEdit"
          icon="save"
          label="Saqlash"
          :loading="saving"
          @click="save"
        />
      </div>
    </div>
  </page-layout>
</template>

<script setup>
import AppTimeInput from '@/components/app/AppTimeInput.vue'
import AppButton from '@/components/app/AppButton.vue'
import AppInput from '@/components/app/AppInput.vue'
import AppSelect from '@/components/app/AppSelect.vue'
import { ref, computed, watch } from 'vue'
import { useQuasar } from 'quasar'
import { api } from '@/boot/axios'
import PageLayout from '@/components/PageLayout.vue'
import { useSchoolStore } from '@/stores/school'
import { useAuthStore } from '@/stores/auth'
import { botModes, weekdays } from '@/utils/telegram'

const $q = useQuasar()
const schoolStore = useSchoolStore()
const authStore = useAuthStore()
const canEdit = computed(() => authStore.isAdmin)

const form = ref(null)
const status = ref(null)
const saving = ref(false)
const dayOptions = Object.entries(weekdays).map(([value, label]) => ({
  value,
  label
}))

function hhmm(t) {
  return (t || '').slice(0, 5)
}

async function load() {
  if (!schoolStore.activeSchoolId) return
  try {
    const [settings, st] = await Promise.all([
      api.get('/api/bot/settings', {
        params: { schoolId: schoolStore.activeSchoolId }
      }),
      api.get('/api/telegram/status')
    ])
    const s = settings.data
    form.value = {
      ...s,
      tomorrowScheduleTime: hhmm(s.tomorrowScheduleTime),
      weeklyReportTime: hhmm(s.weeklyReportTime),
      eventReminderTime: hhmm(s.eventReminderTime)
    }
    status.value = st.data
  } catch (error) {
    $q.notify({
      type: 'negative',
      message: error.friendlyMessage || 'Xatolik yuz berdi'
    })
  }
}

async function save() {
  saving.value = true
  try {
    const { derivedBells: _ignored, ...body } = form.value
    await api.put('/api/bot/settings', body, {
      params: { schoolId: schoolStore.activeSchoolId }
    })
    $q.notify({ type: 'positive', message: 'Saqlandi', icon: 'check_circle' })
  } catch (error) {
    $q.notify({
      type: 'negative',
      message: error.friendlyMessage || 'Xatolik yuz berdi'
    })
  } finally {
    saving.value = false
  }
}

watch(() => schoolStore.activeSchoolId, load, { immediate: true })
</script>

<style scoped>
/* the cards span the page; the form inside keeps a readable column */
.settings-card > * {
  max-width: var(--form-max);
}

.muted-text {
  color: var(--brand-text-muted);
}

.section-card {
  height: 100%;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: var(--text-md);
  font-weight: 600;
  margin-bottom: 4px;
}

.warn-banner {
  border-color: rgba(245, 158, 11, 0.45);
}

.link {
  color: var(--color-brand);
  font-weight: 600;
}

.bells {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.bell-chip {
  padding: 6px 12px;
  border-radius: var(--radius-sm);
  background: var(--surface-2);
  border: 1px solid var(--brand-border);
  font-size: var(--text-sm);
}
</style>
