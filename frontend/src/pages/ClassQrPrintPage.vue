<template>
  <div class="print-root">
    <div class="print-toolbar no-print">
      <q-btn
        flat
        dense
        no-caps
        icon="arrow_back"
        label="Orqaga"
        @click="goBack"
      />
      <div class="col q-ml-sm ellipsis">
        <div class="text-subtitle1 text-weight-semibold ellipsis">
          Ota-onalar uchun QR kodlar
          <template v-if="data">· {{ data.className }} sinf</template>
        </div>
        <div class="text-caption toolbar-muted">
          A4 varaqqa chop eting, kesib ota-onalar yig'ilishida tarqating.
        </div>
      </div>
      <q-btn
        unelevated
        no-caps
        color="primary"
        icon="print"
        label="Chop etish"
        :disable="!data || !data.botUsername"
        @click="print"
      />
    </div>

    <div v-if="loading" class="q-pa-xl text-center toolbar-muted">
      <q-spinner size="32px" color="primary" />
    </div>

    <div
      v-else-if="data && !data.botUsername"
      class="q-pa-xl text-center toolbar-muted"
    >
      <q-icon name="warning" size="40px" color="warning" />
      <div class="q-mt-sm">
        TELEGRAM_BOT_USERNAME sozlanmagan — QR kodlar yaratib bo'lmaydi.
      </div>
    </div>

    <template v-else-if="data">
      <div v-for="(page, pi) in pages" :key="pi" class="sheet">
        <div v-for="s in page" :key="s.studentId" class="slip">
          <div class="slip-school">{{ data.schoolName }}</div>
          <div class="slip-name">{{ s.studentName }}</div>
          <div class="slip-class">{{ data.className }} sinf</div>
          <qr-code :value="s.deepLink" :size="118" class="slip-qr" />
          <div class="slip-help">
            Telefon kamerasi bilan skanerlang va Telegram'da
            <b>Start</b> tugmasini bosing.
          </div>
          <div class="slip-code">
            @{{ data.botUsername }} · kod: <b>{{ s.linkCode }}</b>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { api } from '@/boot/axios'
import QrCode from '@/components/QrCode.vue'

const PER_PAGE = 12 // 3 x 4 slips on an A4 sheet

const route = useRoute()
const router = useRouter()
const $q = useQuasar()

const data = ref(null)
const loading = ref(true)

const pages = computed(() => {
  const students = data.value?.students || []
  const result = []
  for (let i = 0; i < students.length; i += PER_PAGE) {
    result.push(students.slice(i, i + PER_PAGE))
  }
  return result
})

function goBack() {
  if (window.history.length > 1) router.back()
  else router.push(`/profiles/class/${route.params.id}`)
}

function print() {
  window.print()
}

async function load() {
  loading.value = true
  try {
    const response = await api.get(
      `/api/telegram/classes/${route.params.id}/codes`
    )
    data.value = response.data
  } catch (error) {
    $q.notify({
      type: 'negative',
      message: error.friendlyMessage || 'Xatolik yuz berdi'
    })
  } finally {
    loading.value = false
  }
}

load()
</script>

<style scoped>
.print-root {
  min-height: 100vh;
  background: var(--page-bg);
  padding-bottom: 32px;
}

.print-toolbar {
  position: sticky;
  top: 0;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: var(--card-bg);
  color: var(--text-primary);
  border-bottom: 1px solid var(--brand-border);
}

.toolbar-muted {
  color: var(--brand-text-muted);
}

/* The sheet is a preview of real paper, so it stays white in dark mode too. */
.sheet {
  width: 210mm;
  min-height: 297mm;
  margin: 16px auto;
  padding: 10mm;
  background: #ffffff;
  color: #0f172a;
  box-shadow: var(--shadow-card-hover);
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  grid-auto-rows: calc((297mm - 20mm) / 4);
  box-sizing: border-box;
}

.slip {
  border: 1px dashed #94a3b8;
  padding: 4mm 3mm;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  break-inside: avoid;
  overflow: hidden;
}

.slip-school {
  font-size: 9px;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: #64748b;
}

.slip-name {
  font-size: 14px;
  font-weight: 700;
  line-height: 1.2;
  margin-top: 2px;
}

.slip-class {
  font-size: 11px;
  color: #475569;
  margin-bottom: 4px;
}

.slip-qr :deep(.qr-box) {
  border-color: transparent;
}

.slip-help {
  font-size: 9.5px;
  line-height: 1.3;
  color: #334155;
  margin-top: 4px;
}

.slip-code {
  font-size: 9px;
  color: #64748b;
  margin-top: auto;
  word-break: break-all;
}

@media screen and (max-width: 840px) {
  .sheet {
    width: 100%;
    min-height: 0;
    margin: 12px 0;
    padding: 12px;
    grid-template-columns: repeat(2, 1fr);
    grid-auto-rows: auto;
  }
}

@media print {
  .no-print {
    display: none !important;
  }

  .print-root {
    background: #ffffff;
    padding: 0;
  }

  /* No global @page rule (it would leak into other pages' printing): the
     browser's default ~10mm margins stay, and 4 rows x 66mm fit inside them. */
  .sheet {
    width: auto;
    min-height: 0;
    margin: 0;
    padding: 0;
    box-shadow: none;
    grid-auto-rows: 66mm;
    page-break-after: always;
  }

  .sheet:last-child {
    page-break-after: auto;
  }
}
</style>
