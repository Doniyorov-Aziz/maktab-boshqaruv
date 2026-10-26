<template>
  <page-layout
    icon="local_library"
    color="#6366f1"
    title="Kutubxona"
    :subtitle="`Ota-onalar botidagi «📚 Kitob mutolaasi» kitoblari · ${schoolStore.activeSchoolName || ''}`"
  >
    <template #actions>
      <q-btn-toggle
        v-model="view"
        dense
        unelevated
        no-caps
        toggle-color="primary"
        :options="[
          { value: 'grid', icon: 'grid_view', slot: 'grid' },
          { value: 'table', icon: 'table_rows', slot: 'table' }
        ]"
        aria-label="Ko'rinish"
      >
        <template #grid><q-tooltip>Kartochkalar</q-tooltip></template>
        <template #table><q-tooltip>Jadval</q-tooltip></template>
      </q-btn-toggle>
      <AppButton
        variant="secondary"
        v-if="authStore.isAdmin"
        icon="history_edu"
        label="📜 Wikisource dan qo'shish"
        @click="openWikisource"
      />
      <AppButton
        variant="secondary"
        icon="drive_folder_upload"
        label="Ommaviy yuklash"
        @click="openBulk"
      />
      <AppButton icon="add" label="Kitob qo'shish" @click="openCreate" />
    </template>

    <!-- filters -->
    <div class="brand-card q-pa-md q-mb-md">
      <div class="row q-col-gutter-sm items-center">
        <div class="col-12 col-md-4">
          <AppInput
            v-model="filters.q"
            clearable
            debounce="350"
            placeholder="Nomi yoki muallif bo'yicha qidirish"
            aria-label="Qidirish"
          >
            <template #prepend><q-icon name="search" /></template>
          </AppInput>
        </div>
        <div class="col-6 col-md-2">
          <AppSelect
            v-model="filters.categoryId"
            clearable
            emit-value
            map-options
            :options="categoryOptions"
            label="Bo'lim"
          />
        </div>
        <div class="col-6 col-md-2">
          <AppSelect
            v-model="filters.language"
            clearable
            emit-value
            map-options
            :options="languages"
            label="Til"
          />
        </div>
        <div class="col-6 col-md-2">
          <AppSelect
            v-model="filters.ageGroup"
            clearable
            emit-value
            map-options
            :options="ageGroups"
            label="Yosh toifasi"
          />
        </div>
        <div class="col-6 col-md-2">
          <AppSelect
            v-model="filters.legalBasis"
            clearable
            emit-value
            map-options
            :options="legalBases"
            label="Huquqiy asos"
          />
        </div>
      </div>
      <div class="row items-center q-mt-sm q-gutter-sm">
        <q-btn-toggle
          v-model="filters.status"
          dense
          unelevated
          no-caps
          rounded
          padding="4px 14px"
          toggle-color="primary"
          class="status-filter"
          :options="statusOptions"
        />
        <q-space />
        <span class="text-caption muted-text tabular-nums"
          >Jami: {{ total }} ta</span
        >
      </div>
    </div>

    <!-- grid -->
    <template v-if="view === 'grid'">
      <div v-if="loading && !books.length" class="book-grid">
        <div v-for="n in 8" :key="n">
          <q-skeleton height="300px" class="rounded-borders" />
        </div>
      </div>
      <div v-else-if="!books.length" class="brand-card q-pa-xl text-center">
        <q-icon name="local_library" size="56px" color="grey-5" />
        <div class="text-subtitle1 q-mt-sm">Kitob topilmadi</div>
        <div class="text-caption muted-text q-mb-md">
          Filtrni o'zgartiring yoki birinchi kitobni qo'shing.
        </div>
        <AppButton icon="add" label="Kitob qo'shish" @click="openCreate" />
      </div>
      <div v-else class="book-grid">
        <div v-for="b in books" :key="b.id">
          <div
            class="brand-card book-card cursor-pointer"
            :class="{ 'book-card--off': !b.active || b.draft }"
            role="button"
            tabindex="0"
            :aria-label="b.title"
            @click="openEdit(b)"
            @keyup.enter="openEdit(b)"
          >
            <div class="book-card__cover">
              <book-cover
                :book-id="b.id"
                :has-cover="b.hasCover"
                :title="b.title"
                :version="coverVersion(b)"
              />
              <q-badge
                v-if="b.draft"
                color="orange"
                class="book-card__badge"
                label="Qoralama"
              />
              <q-badge
                v-else-if="!b.active"
                color="grey-7"
                class="book-card__badge"
                label="Nofaol"
              />
              <q-badge
                v-else-if="b.featured"
                color="amber-8"
                class="book-card__badge"
                label="⭐ Tavsiya"
              />
            </div>
            <div class="q-pa-sm">
              <div class="book-card__title" :title="b.title">{{ b.title }}</div>
              <div class="text-caption muted-text ellipsis">
                {{ b.author || "Muallif ko'rsatilmagan" }}
              </div>
              <div class="row items-center no-wrap q-mt-xs text-caption">
                <span class="ellipsis col">
                  {{ b.categoryEmoji }} {{ b.categoryName || "Bo'limsiz" }}
                </span>
                <span class="tabular-nums muted-text q-ml-xs">
                  <q-icon name="download" size="14px" />
                  {{ b.downloadCount }}
                </span>
              </div>
            </div>
            <AppButton
              variant="ghost"
              size="sm"
              icon="more_vert"
              class="book-card__menu"
              aria-label="Amallar"
              @click.stop
            >
              <q-menu auto-close>
                <q-list dense style="min-width: 180px">
                  <q-item clickable @click="viewPdf(b)">
                    <q-item-section avatar
                      ><q-icon name="picture_as_pdf"
                    /></q-item-section>
                    <q-item-section>PDF ni ko'rish</q-item-section>
                  </q-item>
                  <q-item clickable @click="openEdit(b)">
                    <q-item-section avatar
                      ><q-icon name="edit"
                    /></q-item-section>
                    <q-item-section>Tahrirlash</q-item-section>
                  </q-item>
                  <q-item
                    v-if="b.active"
                    clickable
                    class="text-negative"
                    @click="deactivate(b)"
                  >
                    <q-item-section avatar
                      ><q-icon name="visibility_off"
                    /></q-item-section>
                    <q-item-section>Nofaol qilish</q-item-section>
                  </q-item>
                </q-list>
              </q-menu>
            </AppButton>
          </div>
        </div>
      </div>
      <div v-if="pages > 1" class="row justify-center q-mt-md">
        <q-pagination
          v-model="page"
          :max="pages"
          :max-pages="7"
          direction-links
          boundary-links
        />
      </div>
    </template>

    <!-- table -->
    <div v-else class="brand-card">
      <q-table
        v-model:pagination="tablePagination"
        flat
        :rows="books"
        :columns="columns"
        row-key="id"
        :loading="loading"
        :rows-per-page-options="[12, 24, 48, 96]"
        no-data-label="Kitob topilmadi"
        @request="onTableRequest"
        @row-click="(_, row) => openEdit(row)"
      >
        <template #body-cell-cover="p">
          <q-td :props="p" style="width: 56px">
            <book-cover
              :book-id="p.row.id"
              :has-cover="p.row.hasCover"
              :title="p.row.title"
              :version="coverVersion(p.row)"
              width="40px"
              height="54px"
            />
          </q-td>
        </template>
        <template #body-cell-title="p">
          <q-td :props="p">
            <div class="text-weight-semibold">{{ p.row.title }}</div>
            <div class="text-caption muted-text">{{ p.row.author || '—' }}</div>
          </q-td>
        </template>
        <template #body-cell-status="p">
          <q-td :props="p">
            <q-badge v-if="p.row.draft" color="orange" label="Qoralama" />
            <q-badge v-else-if="!p.row.active" color="grey-7" label="Nofaol" />
            <q-badge v-else color="positive" label="Faol" />
            <q-badge
              v-if="p.row.featured"
              color="amber-8"
              class="q-ml-xs"
              label="⭐"
            />
          </q-td>
        </template>
        <template #body-cell-actions="p">
          <q-td :props="p" auto-width>
            <AppButton
              variant="ghost"
              size="sm"
              icon="picture_as_pdf"
              aria-label="PDF ni ko'rish"
              guest="allow"
              @click.stop="viewPdf(p.row)"
            >
              <q-tooltip>PDF ni ko'rish</q-tooltip>
            </AppButton>
            <AppButton
              variant="ghost"
              size="sm"
              v-if="p.row.active"
              color="negative"
              icon="visibility_off"
              aria-label="Nofaol qilish"
              @click.stop="deactivate(p.row)"
            >
              <q-tooltip>Nofaol qilish</q-tooltip>
            </AppButton>
          </q-td>
        </template>
      </q-table>
    </div>

    <!-- create / edit -->
    <q-dialog v-model="formOpen" persistent>
      <q-card class="book-form">
        <q-card-section class="row items-center q-pb-none">
          <q-icon
            :name="form.id ? 'edit' : 'add_circle'"
            color="primary"
            size="24px"
            class="q-mr-sm"
          />
          <div class="text-h6">
            {{ form.id ? 'Kitobni tahrirlash' : 'Yangi kitob' }}
          </div>
          <q-badge
            v-if="form.draft"
            color="orange"
            class="q-ml-sm"
            label="Qoralama — saqlansa faol bo'ladi"
          />
          <q-space />
          <AppButton
            variant="ghost"
            icon="close"
            aria-label="Yopish"
            v-close-popup
          />
        </q-card-section>

        <q-card-section>
          <q-banner dense rounded class="legal-banner q-mb-md">
            <template #avatar><q-icon name="gavel" color="warning" /></template>
            <b>{{ LEGAL_WARNING }}</b>
            Bot kitoblarni faqat shu yerda yuklangan fayllardan beradi —
            internetdan hech narsa olinmaydi.
          </q-banner>

          <div class="row q-col-gutter-md">
            <!-- files -->
            <div class="col-12 col-md-4">
              <div class="text-caption text-weight-semibold q-mb-xs">
                PDF fayl {{ form.id ? '' : '*' }}
              </div>
              <div
                class="drop-zone"
                :class="{
                  'drop-zone--over': dragPdf,
                  'drop-zone--ok': pdfFile
                }"
                role="button"
                tabindex="0"
                aria-label="PDF faylni tanlash"
                @click="pdfInput.click()"
                @keyup.enter="pdfInput.click()"
                @dragover.prevent="dragPdf = true"
                @dragleave.prevent="dragPdf = false"
                @drop.prevent="onPdfDrop"
              >
                <q-icon
                  :name="pdfFile ? 'task' : 'picture_as_pdf'"
                  size="36px"
                  :color="pdfFile ? 'positive' : 'red-5'"
                />
                <div v-if="pdfFile" class="text-caption ellipsis full-width">
                  {{ pdfFile.name }} · {{ formatSize(pdfFile.size) }}
                </div>
                <div v-else class="text-caption">
                  PDF ni shu yerga tashlang yoki bosing
                  <div class="muted-text"
                    >faqat PDF, {{ MAX_PDF_MB }} MB gacha</div
                  >
                </div>
                <div
                  v-if="form.id && !pdfFile"
                  class="text-caption muted-text q-mt-xs"
                >
                  Hozirgi: {{ formatSize(form.pdfSize) }}
                </div>
              </div>
              <input
                ref="pdfInput"
                type="file"
                accept="application/pdf,.pdf"
                class="hidden"
                @change="e => pickPdf(e.target.files[0])"
              />
              <AppButton
                variant="ghost"
                size="sm"
                v-if="form.id"
                icon="open_in_new"
                label="Hozirgi PDF ni ko'rish"
                class="q-mt-xs"
                @click="viewPdf(form)"
              />
              <q-banner
                v-if="duplicates.length"
                dense
                rounded
                class="bg-orange-1 text-orange-10 q-mt-sm"
              >
                <template #avatar
                  ><q-icon name="content_copy" color="orange"
                /></template>
                Bu PDF allaqachon yuklangan:
                <div v-for="d in duplicates" :key="d.id">
                  • «{{ d.title }}»{{ d.active ? '' : ' (nofaol)' }}
                </div>
              </q-banner>

              <div class="text-caption text-weight-semibold q-mt-md q-mb-xs">
                Muqova (ixtiyoriy)
              </div>
              <div
                class="drop-zone drop-zone--cover"
                :class="{ 'drop-zone--over': dragCover }"
                role="button"
                tabindex="0"
                aria-label="Muqovani tanlash"
                @click="coverInput.click()"
                @keyup.enter="coverInput.click()"
                @dragover.prevent="dragCover = true"
                @dragleave.prevent="dragCover = false"
                @drop.prevent="onCoverDrop"
              >
                <img
                  v-if="coverPreview"
                  :src="coverPreview"
                  alt="Muqova"
                  class="cover-preview"
                />
                <book-cover
                  v-else-if="form.id && form.hasCover && !removeCover"
                  :book-id="form.id"
                  :has-cover="true"
                  :title="form.title"
                  :version="coverVersion(form)"
                  width="96px"
                  height="128px"
                />
                <template v-else>
                  <q-icon name="image" size="30px" color="grey-6" />
                  <div class="text-caption">
                    JPG/PNG, {{ MAX_COVER_MB }} MB gacha
                    <div class="muted-text">
                      Bo'lmasa — PDF ning 1-sahifasidan avtomatik
                    </div>
                  </div>
                </template>
              </div>
              <input
                ref="coverInput"
                type="file"
                accept="image/jpeg,image/png"
                class="hidden"
                @change="e => pickCover(e.target.files[0])"
              />
              <q-checkbox
                v-if="form.id && form.hasCover && !coverFile"
                v-model="removeCover"
                dense
                class="q-mt-xs text-caption"
                label="Muqovani PDF dan qayta chizish"
              />
            </div>

            <!-- fields -->
            <div class="col-12 col-md-8">
              <div class="row q-col-gutter-sm">
                <div class="col-12">
                  <AppInput
                    v-model="form.title"
                    label="Kitob nomi *"
                    maxlength="255"
                    :rules="[
                      v => !!(v && v.trim()) || 'Nomi kiritilishi shart'
                    ]"
                    lazy-rules
                  />
                </div>
                <div class="col-12 col-sm-7">
                  <AppSelect
                    v-model="form.authorIds"
                    multiple
                    use-chips
                    use-input
                    emit-value
                    map-options
                    input-debounce="0"
                    :options="authorOptionsFiltered"
                    label="Muallif(lar) — ma'lumotnomadan"
                    @filter="filterAuthors"
                  >
                    <template #option="scope">
                      <q-item v-bind="scope.itemProps">
                        <q-item-section>
                          <q-item-label>{{ scope.opt.label }}</q-item-label>
                          <q-item-label caption>{{
                            authorStatusText(scope.opt.author)
                          }}</q-item-label>
                        </q-item-section>
                      </q-item>
                    </template>
                    <template #no-option>
                      <q-item>
                        <q-item-section class="text-caption muted-text">
                          Topilmadi — «Mualliflar» bo'limida qo'shing
                        </q-item-section>
                      </q-item>
                    </template>
                  </AppSelect>
                  <div
                    v-if="selectedStatus"
                    class="author-status q-mt-xs"
                    :class="`author-status--${selectedStatus.kind}`"
                    data-test="author-status"
                  >
                    {{ selectedStatus.text }}
                  </div>
                </div>
                <div class="col-12 col-sm-5">
                  <AppSelect
                    v-model="form.categoryId"
                    clearable
                    emit-value
                    map-options
                    :options="categoryOptions"
                    label="Bo'lim"
                  />
                </div>
                <div class="col-6 col-sm-3">
                  <AppSelect
                    v-model="form.language"
                    emit-value
                    map-options
                    :options="languages"
                    label="Til"
                  />
                </div>
                <div class="col-6 col-sm-3">
                  <AppSelect
                    v-model="form.ageGroup"
                    emit-value
                    map-options
                    :options="ageGroups"
                    label="Yosh toifasi"
                  />
                </div>
                <div class="col-6 col-sm-3">
                  <AppInput
                    v-model.number="form.pages"
                    type="number"
                    min="1"
                    label="Bet soni"
                    hint="Bo'sh — PDF dan"
                  />
                </div>
                <div class="col-6 col-sm-3">
                  <AppInput
                    v-model.number="form.year"
                    type="number"
                    min="1000"
                    max="2100"
                    label="Yili"
                  />
                </div>
                <div class="col-12">
                  <AppInput
                    v-model="form.description"
                    autogrow
                    type="textarea"
                    maxlength="2000"
                    counter
                    label="Qisqa annotatsiya"
                    hint="Botda 600 belgigacha ko'rsatiladi"
                  />
                </div>
                <div class="col-12 col-sm-6">
                  <AppSelect
                    v-model="form.legalBasis"
                    emit-value
                    map-options
                    :options="legalBases"
                    label="Huquqiy asos *"
                    :rules="[v => !!v || 'Huquqiy asos tanlanishi shart']"
                    lazy-rules
                  >
                    <template #option="scope">
                      <q-item v-bind="scope.itemProps">
                        <q-item-section>
                          <q-item-label>{{ scope.opt.label }}</q-item-label>
                          <q-item-label v-if="scope.opt.hint" caption>{{
                            scope.opt.hint
                          }}</q-item-label>
                        </q-item-section>
                      </q-item>
                    </template>
                  </AppSelect>
                </div>
                <div class="col-12 col-sm-6">
                  <AppInput
                    v-model="form.legalNote"
                    maxlength="500"
                    label="Izoh (masalan, ruxsat xati raqami)"
                  />
                </div>
                <!-- Jamoat mulki: the two confirmations the law asks for -->
                <div
                  v-if="form.legalBasis === 'JAMOAT_MULKI'"
                  class="col-12 legal-box"
                >
                  <q-checkbox
                    v-model="form.publishedInLifetime"
                    dense
                    label="Asar muallif tirikligida chop etilgan"
                  />
                  <AppInput
                    v-if="!form.publishedInLifetime"
                    v-model.number="form.firstPublishedYear"
                    type="number"
                    min="1000"
                    max="2100"
                    label="Birinchi nashr yili *"
                    hint="Vafotdan keyin chop etilgan asar nashrdan keyin 70 yil himoyalanadi"
                    class="q-mt-sm"
                    style="max-width: var(--field-w)"
                  />
                  <q-checkbox
                    v-model="form.noModernCommentary"
                    dense
                    class="q-mt-sm"
                    label="Faylda zamonaviy so'zboshi, izoh yoki sharh yo'q"
                  />
                </div>
                <!-- author's permission / licence: a permission in force -->
                <div
                  v-if="needsPermission(form.legalBasis)"
                  class="col-12 col-sm-8"
                >
                  <AppSelect
                    v-model="form.permissionId"
                    clearable
                    emit-value
                    map-options
                    :options="permissionOptions"
                    :disable="!authStore.isAdmin"
                    label="Ruxsatnoma *"
                    :hint="
                      authStore.isAdmin
                        ? 'Faqat amaldagi ruxsatnomalar'
                        : 'Ruxsatnomani administrator bog\'laydi'
                    "
                  />
                </div>
                <div class="col-12 row q-gutter-md">
                  <q-toggle
                    v-model="form.active"
                    label="Faol (botda ko'rinadi)"
                  />
                  <q-toggle
                    v-model="form.featured"
                    color="amber-8"
                    label="⭐ Maktab tavsiya qiladi"
                  />
                </div>
              </div>
            </div>
          </div>
        </q-card-section>

        <q-card-actions align="right" class="q-px-md q-pb-md">
          <AppButton
            variant="ghost"
            v-if="form.id && form.active"
            color="negative"
            icon="visibility_off"
            label="Nofaol qilish"
            @click="deactivate(form, true)"
          />
          <q-space />
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

    <!-- bulk upload -->
    <q-dialog v-model="bulkOpen" :persistent="bulkRunning">
      <q-card class="bulk-form">
        <q-card-section class="row items-center q-pb-none">
          <q-icon
            name="drive_folder_upload"
            color="primary"
            size="24px"
            class="q-mr-sm"
          />
          <div class="text-h6">Ommaviy yuklash</div>
          <q-space />
          <AppButton
            variant="ghost"
            icon="close"
            aria-label="Yopish"
            :disable="bulkRunning"
            v-close-popup
          />
        </q-card-section>
        <q-card-section>
          <q-banner dense rounded class="legal-banner q-mb-md">
            <template #avatar><q-icon name="gavel" color="warning" /></template>
            <b>{{ LEGAL_WARNING }}</b>
            Har bir PDF uchun qoralama kartochka yaratiladi (nomi fayl nomidan)
            — keyin to'ldirib saqlaysiz. Qoralamalar botda ko'rinmaydi.
          </q-banner>
          <div class="row q-col-gutter-sm">
            <div class="col-12 col-sm-6">
              <AppSelect
                v-model="bulk.legalBasis"
                emit-value
                map-options
                :options="legalBases"
                label="Huquqiy asos (hammasiga) *"
              />
            </div>
            <div class="col-12 col-sm-6">
              <AppInput v-model="bulk.legalNote" maxlength="500" label="Izoh" />
            </div>
            <div class="col-12 col-sm-4">
              <AppSelect
                v-model="bulk.categoryId"
                clearable
                emit-value
                map-options
                :options="categoryOptions"
                label="Bo'lim"
              />
            </div>
            <div class="col-6 col-sm-4">
              <AppSelect
                v-model="bulk.language"
                emit-value
                map-options
                :options="languages"
                label="Til"
              />
            </div>
            <div class="col-6 col-sm-4">
              <AppSelect
                v-model="bulk.ageGroup"
                emit-value
                map-options
                :options="ageGroups"
                label="Yosh toifasi"
              />
            </div>
            <div class="col-12 col-sm-6">
              <AppSelect
                v-model="bulk.authorId"
                clearable
                use-input
                emit-value
                map-options
                input-debounce="0"
                :options="authorOptionsFiltered"
                label="Muallif (hammasiga, faylda o'zgartirsa bo'ladi)"
                @filter="filterAuthors"
              />
            </div>
            <div
              v-if="needsPermission(bulk.legalBasis)"
              class="col-12 col-sm-6"
            >
              <AppSelect
                v-model="bulk.permissionId"
                clearable
                emit-value
                map-options
                :options="permissionOptions"
                :disable="!authStore.isAdmin"
                label="Ruxsatnoma *"
              />
            </div>
            <div
              v-if="bulk.legalBasis === 'JAMOAT_MULKI'"
              class="col-12 legal-box"
            >
              <q-checkbox
                v-model="bulk.publishedInLifetime"
                dense
                label="Asarlar muallif tirikligida chop etilgan"
              />
              <q-checkbox
                v-model="bulk.noModernCommentary"
                dense
                class="q-ml-md"
                label="Fayllarda zamonaviy so'zboshi, izoh yoki sharh yo'q"
              />
            </div>
          </div>
          <div
            class="drop-zone q-mt-md"
            :class="{ 'drop-zone--over': dragBulk }"
            role="button"
            tabindex="0"
            aria-label="PDF fayllarni tanlash"
            @click="bulkInput.click()"
            @keyup.enter="bulkInput.click()"
            @dragover.prevent="dragBulk = true"
            @dragleave.prevent="dragBulk = false"
            @drop.prevent="onBulkDrop"
          >
            <q-icon name="library_add" size="36px" color="primary" />
            <div class="text-caption">
              Bir nechta PDF ni shu yerga tashlang yoki bosing
            </div>
          </div>
          <input
            ref="bulkInput"
            type="file"
            multiple
            accept="application/pdf,.pdf"
            class="hidden"
            @change="e => addBulk(e.target.files)"
          />
          <q-list
            v-if="bulkFiles.length"
            dense
            separator
            class="q-mt-sm bulk-list"
          >
            <q-item v-for="(f, i) in bulkFiles" :key="i">
              <q-item-section avatar>
                <q-spinner
                  v-if="f.state === 'uploading'"
                  size="18px"
                  color="primary"
                />
                <q-icon
                  v-else
                  :name="bulkIcon(f.state)"
                  :color="bulkColor(f.state)"
                />
              </q-item-section>
              <q-item-section>
                <q-item-label class="ellipsis">{{ f.file.name }}</q-item-label>
                <q-item-label caption>
                  {{ formatSize(f.file.size) }}
                  <span v-if="f.error" class="text-negative">
                    · {{ f.error }}</span
                  >
                </q-item-label>
                <q-item-label
                  v-if="fileStatus(f)"
                  caption
                  class="author-status q-mt-xs"
                  :class="`author-status--${fileStatus(f).kind}`"
                  data-test="bulk-file-status"
                >
                  {{ fileStatus(f).text }}
                </q-item-label>
              </q-item-section>
              <q-item-section
                v-if="f.state === 'waiting' && !bulkRunning"
                side
                style="min-width: 180px"
              >
                <AppSelect
                  v-model="f.authorId"
                  clearable
                  use-input
                  emit-value
                  map-options
                  input-debounce="0"
                  :options="authorOptionsFiltered"
                  label="Muallif"
                  style="width: 180px"
                  @filter="filterAuthors"
                />
              </q-item-section>
              <q-item-section v-if="f.state === 'waiting' && !bulkRunning" side>
                <AppButton
                  variant="ghost"
                  size="sm"
                  icon="close"
                  aria-label="Olib tashlash"
                  @click="bulkFiles.splice(i, 1)"
                />
              </q-item-section>
            </q-item>
          </q-list>
        </q-card-section>
        <q-card-actions align="right" class="q-px-md q-pb-md">
          <AppButton
            variant="ghost"
            label="Yopish"
            :disable="bulkRunning"
            v-close-popup
          />
          <AppButton
            icon="cloud_upload"
            :label="`Yuklash (${bulkWaiting})`"
            :loading="bulkRunning"
            :disable="!bulkWaiting || !bulk.legalBasis"
            @click="runBulk"
          />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- Wikisource import (admin): public-domain classics -->
    <q-dialog v-model="wsOpen" :persistent="!!wsBusy">
      <q-card class="ws-form">
        <q-card-section class="row items-center q-pb-none">
          <div class="text-h6">📜 Wikisource dan qo'shish</div>
          <q-space />
          <AppButton
            variant="ghost"
            icon="close"
            aria-label="Yopish"
            :disable="!!wsBusy"
            v-close-popup
          />
        </q-card-section>
        <q-card-section>
          <q-banner dense rounded class="legal-banner q-mb-md">
            <template #avatar><q-icon name="gavel" color="warning" /></template>
            Faqat jamoat mulki bo'lgan mualliflar (vafotidan 70 yil o'tgan).
            Matn Wikisource'dan olinadi va bir marta chiroyli PDF ga
            aylantiriladi; muallif nomi saqlanadi, matn o'zgartirilmaydi.
          </q-banner>
          <div class="row q-col-gutter-sm items-start">
            <div class="col-12 col-sm-8">
              <AppSelect
                v-model="ws.authorId"
                use-input
                emit-value
                map-options
                input-debounce="0"
                :options="freeAuthorOptions"
                label="Muallif (jamoat mulki)"
                @filter="filterFreeAuthors"
              />
            </div>
            <div class="col-12 col-sm-4">
              <AppButton
                icon="travel_explore"
                label="Asarlarni topish"
                class="full-width"
                :loading="wsBusy === 'search'"
                :disable="!ws.authorId || !!wsBusy"
                @click="wsSearch"
              />
            </div>
            <div class="col-12">
              <q-checkbox
                v-model="ws.noModernCommentary"
                dense
                label="Tasdiqlayman: faylda zamonaviy so'zboshi, izoh yoki sharh bo'lmaydi"
              />
              <q-checkbox
                v-model="ws.publishedInLifetime"
                dense
                class="q-ml-md"
                label="Yili ko'rsatilmagan asarlar ham muallif tirikligida chop etilgan"
              />
            </div>
          </div>
          <div v-if="wsResult" class="q-mt-md">
            <div class="text-caption muted-text q-mb-xs">
              <a :href="wsResult.pageUrl" target="_blank" rel="noopener">{{
                wsResult.page || 'Sahifa topilmadi'
              }}</a>
              · {{ wsResult.works.length }} ta asar
              <span v-if="wsResult.message"> · {{ wsResult.message }}</span>
            </div>
            <AppInput
              v-model="wsFilter"
              clearable
              placeholder="Asar nomi bo'yicha"
              class="q-mb-xs"
            />
            <q-list dense separator class="ws-list">
              <q-item v-for="w in wsWorks" :key="w.title">
                <q-item-section>
                  <q-item-label>{{ w.label }}</q-item-label>
                  <q-item-label caption>
                    {{ w.section || ''
                    }}{{ w.year ? ' · ' + w.year : " · yili ko'rsatilmagan" }}
                    <span
                      v-if="wsState[w.title]?.message"
                      :class="
                        wsState[w.title].ok ? 'text-positive' : 'text-negative'
                      "
                    >
                      · {{ wsState[w.title].message }}</span
                    >
                  </q-item-label>
                </q-item-section>
                <q-item-section side>
                  <q-badge
                    v-if="w.imported || wsState[w.title]?.ok"
                    color="positive"
                    label="Kutubxonada"
                  />
                  <AppButton
                    variant="ghost"
                    v-else
                    color="primary"
                    icon="download"
                    label="Import"
                    :loading="wsBusy === w.title"
                    :disable="!!wsBusy"
                    @click="wsImport(w)"
                  />
                </q-item-section>
              </q-item>
            </q-list>
          </div>
        </q-card-section>
      </q-card>
    </q-dialog>
  </page-layout>
</template>

<script setup>
import AppButton from '@/components/app/AppButton.vue'
import AppInput from '@/components/app/AppInput.vue'
import AppSelect from '@/components/app/AppSelect.vue'
import { ref, reactive, computed, watch } from 'vue'
import { useQuasar } from 'quasar'
import { api } from '@/boot/axios'
import PageLayout from '@/components/PageLayout.vue'
import BookCover from '@/components/BookCover.vue'
import { useSchoolStore } from '@/stores/school'
import {
  legalBases,
  ageGroups,
  languages,
  labelOf,
  formatSize,
  sha256Hex,
  looksLikePdf,
  LEGAL_WARNING,
  authorStatusText,
  combinedAuthorStatus,
  NEEDS_PERMISSION
} from '@/utils/library'
import { useAuthStore } from '@/stores/auth'

const MAX_PDF_MB = 50
const MAX_COVER_MB = 2

const $q = useQuasar()
const schoolStore = useSchoolStore()
const authStore = useAuthStore()

// ------------------------------------------------------------------ authors, permissions

const authors = ref([])
const authorQuery = ref('')
const permissions = ref([])

const authorOptions = computed(() =>
  authors.value.map(a => ({
    value: a.id,
    label:
      a.fullName +
      (a.deathYear ? ` (${a.birthYear || '?'}–${a.deathYear})` : ''),
    author: a,
    search: `${a.fullName} ${a.aliases || ''}`.toLowerCase()
  }))
)
const authorOptionsFiltered = computed(() => {
  const q = authorQuery.value.trim().toLowerCase()
  return q
    ? authorOptions.value.filter(o => o.search.includes(q))
    : authorOptions.value
})
const freeAuthorOptions = computed(() =>
  authorOptionsFiltered.value.filter(o => o.author.status === 'FREE')
)
const permissionOptions = computed(() =>
  permissions.value
    .filter(p => p.status === 'ACTIVE' || p.status === 'ENDING')
    .map(p => ({
      value: p.id,
      label: `${p.grantorFullName} · ${p.validUntil ? p.validUntil + ' gacha' : 'muddatsiz'}`
    }))
)

function filterAuthors(text, update) {
  update(() => {
    authorQuery.value = text || ''
  })
}

function filterFreeAuthors(text, update) {
  filterAuthors(text, update)
}

function authorById(id) {
  return authors.value.find(a => a.id === id)
}

function needsPermission(basis) {
  return NEEDS_PERMISSION.includes(basis)
}

const selectedStatus = computed(() =>
  combinedAuthorStatus((form.authorIds || []).map(authorById).filter(Boolean))
)

async function loadAuthors() {
  try {
    authors.value = (await api.get('/api/library/authors')).data
  } catch (e) {
    notifyError(e)
  }
}

async function loadPermissions() {
  if (!authStore.isAdmin || !schoolStore.activeSchoolId) return
  try {
    permissions.value = (
      await api.get('/api/library/permissions', {
        params: { schoolId: schoolStore.activeSchoolId }
      })
    ).data
  } catch {
    permissions.value = []
  }
}

const view = ref(readView())
const books = ref([])
const total = ref(0)
const loading = ref(false)
const page = ref(1)
const pageSize = ref(24)
const categories = ref([])
const filters = reactive({
  q: '',
  categoryId: null,
  language: null,
  ageGroup: null,
  legalBasis: null,
  status: 'all'
})

const statusOptions = [
  { value: 'all', label: 'Hammasi' },
  { value: 'active', label: 'Faol' },
  { value: 'inactive', label: 'Nofaol' },
  { value: 'draft', label: 'Qoralama' }
]

const categoryOptions = computed(() =>
  categories.value.map(c => ({
    value: c.id,
    label: `${c.emoji ? c.emoji + ' ' : ''}${c.nameUz}${c.active ? '' : ' (yashirin)'}`
  }))
)

const pages = computed(() =>
  Math.max(1, Math.ceil(total.value / pageSize.value))
)

const columns = [
  { name: 'cover', label: '', field: 'id', align: 'left' },
  { name: 'title', label: 'Kitob', field: 'title', align: 'left' },
  {
    name: 'category',
    label: "Bo'lim",
    field: r =>
      r.categoryName ? `${r.categoryEmoji || ''} ${r.categoryName}` : '—',
    align: 'left'
  },
  {
    name: 'language',
    label: 'Til',
    field: r => labelOf(languages, r.language),
    align: 'left'
  },
  {
    name: 'age',
    label: 'Yosh',
    field: r => labelOf(ageGroups, r.ageGroup),
    align: 'left'
  },
  {
    name: 'legal',
    label: 'Huquqiy asos',
    field: r => labelOf(legalBases, r.legalBasis),
    align: 'left'
  },
  {
    name: 'downloads',
    label: 'Yuklashlar',
    field: 'downloadCount',
    align: 'right'
  },
  { name: 'status', label: 'Holat', field: 'active', align: 'left' },
  { name: 'actions', label: '', field: 'id', align: 'right' }
]

const tablePagination = ref({ page: 1, rowsPerPage: 24, rowsNumber: 0 })

// bumped after every save: a replaced cover is fetched again instead of the cached one
const coverEpoch = ref(0)
function coverVersion(b) {
  return `${b.pdfSha256}:${b.hasCover}:${coverEpoch.value}`
}

function readView() {
  try {
    return localStorage.getItem('libraryView') === 'table' ? 'table' : 'grid'
  } catch {
    return 'grid'
  }
}

watch(view, v => {
  try {
    localStorage.setItem('libraryView', v)
  } catch {
    // remembered for this session only
  }
})

function notifyError(error) {
  $q.notify({
    type: 'negative',
    message: error.friendlyMessage || 'Xatolik yuz berdi'
  })
}

async function loadCategories() {
  if (!schoolStore.activeSchoolId) return
  try {
    const res = await api.get('/api/library/categories', {
      params: { schoolId: schoolStore.activeSchoolId }
    })
    categories.value = res.data
  } catch (e) {
    notifyError(e)
  }
}

async function load() {
  if (!schoolStore.activeSchoolId) return
  loading.value = true
  try {
    const res = await api.get('/api/library/books', {
      params: {
        schoolId: schoolStore.activeSchoolId,
        q: filters.q || undefined,
        categoryId: filters.categoryId || undefined,
        language: filters.language || undefined,
        ageGroup: filters.ageGroup || undefined,
        legalBasis: filters.legalBasis || undefined,
        status: filters.status,
        page: page.value - 1,
        size: pageSize.value
      }
    })
    books.value = res.data.items
    total.value = res.data.total
    tablePagination.value = {
      ...tablePagination.value,
      page: page.value,
      rowsPerPage: pageSize.value,
      rowsNumber: res.data.total
    }
  } catch (e) {
    notifyError(e)
  } finally {
    loading.value = false
  }
}

function onTableRequest({ pagination }) {
  pageSize.value = pagination.rowsPerPage || 24
  if (page.value === pagination.page) load()
  else page.value = pagination.page
}

watch(
  () => ({ ...filters }),
  () => {
    if (page.value === 1) load()
    else page.value = 1
  },
  { deep: true }
)
watch(page, load)
watch(
  () => schoolStore.activeSchoolId,
  id => {
    if (!id) return
    page.value = 1
    loadCategories()
    loadAuthors()
    loadPermissions()
    load()
  },
  { immediate: true }
)

// ------------------------------------------------------------------ PDF

async function viewPdf(book) {
  // opened right away (still inside the click) so the browser does not block the tab
  const tab = window.open('', '_blank')
  try {
    const res = await api.get(`/api/library/books/${book.id}/pdf`, {
      responseType: 'blob'
    })
    const url = URL.createObjectURL(
      new Blob([res.data], { type: 'application/pdf' })
    )
    if (tab) tab.location.href = url
    else window.location.href = url
    setTimeout(() => URL.revokeObjectURL(url), 60_000)
  } catch (e) {
    if (tab) tab.close()
    notifyError(e)
  }
}

function deactivate(book, fromForm = false) {
  $q.dialog({
    title: 'Nofaol qilish',
    message: `«${book.title}» botda ko'rinmay qoladi. Yuklash tarixi saqlanadi. Davom etamizmi?`,
    cancel: { flat: true, noCaps: true, label: 'Bekor qilish' },
    ok: {
      color: 'negative',
      unelevated: true,
      noCaps: true,
      label: 'Nofaol qilish'
    }
  }).onOk(async () => {
    try {
      await api.delete(`/api/library/books/${book.id}`)
      $q.notify({ type: 'positive', message: 'Kitob nofaol qilindi' })
      if (fromForm) formOpen.value = false
      load()
    } catch (e) {
      notifyError(e)
    }
  })
}

// ------------------------------------------------------------------ form

const formOpen = ref(false)
const saving = ref(false)
const form = reactive({})
const pdfFile = ref(null)
const pdfSha = ref('')
const coverFile = ref(null)
const coverPreview = ref('')
const removeCover = ref(false)
const duplicates = ref([])
const dragPdf = ref(false)
const dragCover = ref(false)
const pdfInput = ref(null)
const coverInput = ref(null)

function resetFiles() {
  pdfFile.value = null
  pdfSha.value = ''
  coverFile.value = null
  if (coverPreview.value) URL.revokeObjectURL(coverPreview.value)
  coverPreview.value = ''
  removeCover.value = false
  duplicates.value = []
}

function openCreate() {
  resetFiles()
  Object.keys(form).forEach(k => delete form[k])
  Object.assign(form, {
    id: null,
    title: '',
    authorIds: [],
    permissionId: null,
    publishedInLifetime: true,
    firstPublishedYear: null,
    noModernCommentary: false,
    categoryId: filters.categoryId || null,
    language: 'uz',
    ageGroup: 'KATTALAR',
    pages: null,
    year: null,
    description: '',
    legalBasis: null,
    legalNote: '',
    active: true,
    featured: false,
    draft: false
  })
  formOpen.value = true
}

function openEdit(book) {
  resetFiles()
  Object.keys(form).forEach(k => delete form[k])
  Object.assign(form, {
    ...book,
    authorIds: (book.authors || []).map(a => a.id),
    publishedInLifetime: book.publishedInLifetime !== false,
    noModernCommentary: !!book.noModernCommentary,
    active: book.draft ? true : book.active
  })
  formOpen.value = true
}

async function pickPdf(file) {
  dragPdf.value = false
  if (pdfInput.value) pdfInput.value.value = ''
  if (!file) return
  if (file.size > MAX_PDF_MB * 1024 * 1024) {
    $q.notify({
      type: 'negative',
      message: `PDF juda katta: ko'pi bilan ${MAX_PDF_MB} MB`
    })
    return
  }
  if (!(await looksLikePdf(file))) {
    $q.notify({
      type: 'negative',
      message: 'Fayl PDF emas (faqat haqiqiy PDF qabul qilinadi)'
    })
    return
  }
  pdfFile.value = file
  if (!form.title)
    form.title = file.name
      .replace(/\.pdf$/i, '')
      .replace(/_+/g, ' ')
      .trim()
  duplicates.value = []
  try {
    pdfSha.value = await sha256Hex(file)
    const res = await api.get('/api/library/books/duplicates', {
      params: { schoolId: schoolStore.activeSchoolId, sha256: pdfSha.value },
      background: true
    })
    duplicates.value = res.data.filter(d => d.id !== form.id)
  } catch {
    // the server checks again on save
  }
}

function onPdfDrop(e) {
  pickPdf(e.dataTransfer.files[0])
}

function pickCover(file) {
  dragCover.value = false
  if (coverInput.value) coverInput.value.value = ''
  if (!file) return
  if (!['image/jpeg', 'image/png'].includes(file.type)) {
    $q.notify({
      type: 'negative',
      message: "Muqova faqat JPG yoki PNG bo'lishi mumkin"
    })
    return
  }
  if (file.size > MAX_COVER_MB * 1024 * 1024) {
    $q.notify({
      type: 'negative',
      message: `Muqova juda katta: ko'pi bilan ${MAX_COVER_MB} MB`
    })
    return
  }
  coverFile.value = file
  if (coverPreview.value) URL.revokeObjectURL(coverPreview.value)
  coverPreview.value = URL.createObjectURL(file)
  removeCover.value = false
}

function onCoverDrop(e) {
  pickCover(e.dataTransfer.files[0])
}

function warn(message) {
  $q.notify({ type: 'warning', message })
}

function payload() {
  return {
    title: (form.title || '').trim(),
    authorIds: form.authorIds || [],
    permissionId: needsPermission(form.legalBasis)
      ? form.permissionId || null
      : null,
    publishedInLifetime: !!form.publishedInLifetime,
    firstPublishedYear: form.publishedInLifetime
      ? null
      : form.firstPublishedYear || null,
    noModernCommentary: !!form.noModernCommentary,
    categoryId: form.categoryId || null,
    description: form.description || null,
    language: form.language || 'uz',
    ageGroup: form.ageGroup || 'KATTALAR',
    pages: form.pages || null,
    year: form.year || null,
    legalBasis: form.legalBasis,
    legalNote: form.legalNote || null,
    active: !!form.active,
    featured: !!form.featured
  }
}

async function save(allowDuplicate = false) {
  if (!form.title || !form.title.trim()) {
    $q.notify({ type: 'warning', message: 'Kitob nomini kiriting' })
    return
  }
  if (!form.legalBasis) {
    $q.notify({
      type: 'warning',
      message: `Huquqiy asosni tanlang. ${LEGAL_WARNING}`
    })
    return
  }
  if (!form.id && !pdfFile.value) {
    $q.notify({ type: 'warning', message: 'PDF faylni tanlang' })
    return
  }
  // the same rules the server checks — told before a 50 MB upload, not after
  if (form.legalBasis === 'JAMOAT_MULKI') {
    const s = selectedStatus.value
    if (!s) return warn('Jamoat mulki uchun muallifni tanlang')
    if (s.kind !== 'free') return warn(s.text)
    if (!form.publishedInLifetime && !form.firstPublishedYear) {
      return warn('Birinchi nashr yilini kiriting')
    }
    if (!form.noModernCommentary) {
      return warn(
        "«Zamonaviy so'zboshi, izoh yoki sharh yo'q» tasdig'i majburiy"
      )
    }
  }
  if (needsPermission(form.legalBasis) && !form.permissionId) {
    return warn("Amaldagi ruxsatnomani tanlang («Ruxsatnomalar» bo'limi)")
  }
  if (duplicates.value.length && allowDuplicate !== true) {
    $q.dialog({
      title: 'Bu PDF allaqachon bor',
      message: `Xuddi shu fayl «${duplicates.value[0].title}» kitobida yuklangan. Baribir saqlaymizmi?`,
      cancel: { flat: true, noCaps: true, label: "Yo'q" },
      ok: { unelevated: true, noCaps: true, label: 'Ha, saqlash' }
    }).onOk(() => save(true))
    return
  }
  const data = new FormData()
  data.append(
    'book',
    new Blob([JSON.stringify(payload())], { type: 'application/json' })
  )
  if (pdfFile.value) data.append('pdf', pdfFile.value)
  if (coverFile.value) data.append('cover', coverFile.value)
  saving.value = true
  try {
    const params = { allowDuplicate: allowDuplicate === true }
    if (form.id) {
      await api.put(`/api/library/books/${form.id}`, data, {
        params: { ...params, removeCover: removeCover.value }
      })
    } else {
      await api.post('/api/library/books', data, {
        params: { ...params, schoolId: schoolStore.activeSchoolId }
      })
    }
    $q.notify({
      type: 'positive',
      message: form.id ? 'Kitob saqlandi' : "Kitob qo'shildi"
    })
    formOpen.value = false
    coverEpoch.value++
    loadCategories()
    load()
  } catch (e) {
    if (e.response?.status === 409 && allowDuplicate !== true) {
      $q.dialog({
        title: 'Bu PDF allaqachon bor',
        message: `${e.friendlyMessage}. Baribir saqlaymizmi?`,
        cancel: { flat: true, noCaps: true, label: "Yo'q" },
        ok: { unelevated: true, noCaps: true, label: 'Ha, saqlash' }
      }).onOk(() => save(true))
    } else {
      notifyError(e)
    }
  } finally {
    saving.value = false
  }
}

// ------------------------------------------------------------------ bulk

const bulkOpen = ref(false)
const bulkRunning = ref(false)
const bulkFiles = ref([])
const dragBulk = ref(false)
const bulkInput = ref(null)
const bulk = reactive({
  legalBasis: null,
  legalNote: '',
  categoryId: null,
  language: 'uz',
  ageGroup: 'KATTALAR',
  authorId: null,
  permissionId: null,
  publishedInLifetime: true,
  noModernCommentary: false
})

// the batch author is every waiting file's author until a file gets its own
watch(
  () => bulk.authorId,
  id => {
    for (const f of bulkFiles.value) if (f.state === 'waiting') f.authorId = id
  }
)

/** The author's state for one file, and whether it may be saved with the chosen legal basis. */
function fileStatus(f) {
  const a = authorById(f.authorId)
  if (!a) return null
  const s = combinedAuthorStatus([a])
  if (s.kind !== 'free' && !needsPermission(bulk.legalBasis)) {
    return {
      kind: s.kind,
      text: `${a.fullName}: ${s.text} — ruxsatnomasiz saqlanmaydi`
    }
  }
  return { kind: s.kind, text: `${a.fullName}: ${s.text}` }
}
const bulkWaiting = computed(
  () => bulkFiles.value.filter(f => f.state === 'waiting').length
)

function openBulk() {
  bulkFiles.value = []
  bulkOpen.value = true
}

async function addBulk(list) {
  dragBulk.value = false
  const files = Array.from(list || [])
  if (bulkInput.value) bulkInput.value.value = ''
  for (const file of files) {
    // checked by content ("%PDF"), not by name — a renamed file is marked at once
    if (!(await looksLikePdf(file))) {
      bulkFiles.value.push({ file, state: 'error', error: 'PDF emas' })
    } else if (file.size > MAX_PDF_MB * 1024 * 1024) {
      bulkFiles.value.push({
        file,
        state: 'error',
        error: `${MAX_PDF_MB} MB dan katta`
      })
    } else {
      bulkFiles.value.push({
        file,
        state: 'waiting',
        error: '',
        authorId: bulk.authorId
      })
    }
  }
}

function onBulkDrop(e) {
  addBulk(e.dataTransfer.files)
}

function bulkIcon(state) {
  return (
    { waiting: 'schedule', done: 'check_circle', error: 'error' }[state] ||
    'schedule'
  )
}

function bulkColor(state) {
  return (
    { waiting: 'grey-6', done: 'positive', error: 'negative' }[state] ||
    'grey-6'
  )
}

async function runBulk() {
  bulkRunning.value = true
  let done = 0
  // one file per request, one after another: 50 MB files never pile up in one request
  for (const item of bulkFiles.value) {
    if (item.state !== 'waiting') continue
    if (!(await looksLikePdf(item.file))) {
      item.state = 'error'
      item.error = 'PDF emas'
      continue
    }
    // a protected author's file is never saved without a permission (the server refuses it too)
    const st = fileStatus(item)
    if (bulk.legalBasis === 'JAMOAT_MULKI' && (!st || st.kind !== 'free')) {
      item.state = 'error'
      item.error = st
        ? 'himoyadagi muallif — ruxsatnoma kerak'
        : 'muallif tanlanmagan'
      continue
    }
    if (needsPermission(bulk.legalBasis) && !bulk.permissionId) {
      item.state = 'error'
      item.error = 'ruxsatnoma tanlanmagan'
      continue
    }
    item.state = 'uploading'
    const data = new FormData()
    data.append('pdf', item.file)
    try {
      await api.post('/api/library/books/drafts', data, {
        params: {
          schoolId: schoolStore.activeSchoolId,
          legalBasis: bulk.legalBasis,
          legalNote: bulk.legalNote || undefined,
          categoryId: bulk.categoryId || undefined,
          language: bulk.language,
          ageGroup: bulk.ageGroup,
          authorId: item.authorId || undefined,
          permissionId: needsPermission(bulk.legalBasis)
            ? bulk.permissionId
            : undefined,
          publishedInLifetime: bulk.publishedInLifetime,
          noModernCommentary: bulk.noModernCommentary
        }
      })
      item.state = 'done'
      done++
    } catch (e) {
      item.state = 'error'
      item.error = e.friendlyMessage || 'Xatolik'
    }
  }
  bulkRunning.value = false
  if (done) {
    $q.notify({
      type: 'positive',
      message: `${done} ta qoralama yaratildi — to'ldirib saqlang`
    })
    filters.status = 'draft'
    load()
  }
}

// ------------------------------------------------------------------ Wikisource (admin)

const wsOpen = ref(false)
const wsBusy = ref('')
const wsResult = ref(null)
const wsFilter = ref('')
const wsState = reactive({})
const ws = reactive({
  authorId: null,
  noModernCommentary: false,
  publishedInLifetime: false
})

const wsWorks = computed(() => {
  const list = wsResult.value?.works || []
  const q = (wsFilter.value || '').trim().toLowerCase()
  return q ? list.filter(w => w.label.toLowerCase().includes(q)) : list
})

function openWikisource() {
  wsResult.value = null
  wsFilter.value = ''
  Object.keys(wsState).forEach(k => delete wsState[k])
  wsOpen.value = true
}

async function wsSearch() {
  wsBusy.value = 'search'
  wsResult.value = null
  try {
    wsResult.value = (
      await api.get('/api/library/wikisource/works', {
        params: { schoolId: schoolStore.activeSchoolId, authorId: ws.authorId }
      })
    ).data
  } catch (e) {
    notifyError(e)
  } finally {
    wsBusy.value = ''
  }
}

/** A work whose year is within the author's life counts as published in their lifetime. */
function wsLifetime(w) {
  const died = wsResult.value?.deathYear
  return (w.year && died && w.year <= died) || ws.publishedInLifetime
}

async function wsImport(w) {
  if (!ws.noModernCommentary) {
    return warn(
      "Avval tasdiqlang: faylda zamonaviy so'zboshi, izoh yoki sharh bo'lmaydi"
    )
  }
  if (!wsLifetime(w)) {
    return warn(
      "Asar yili ko'rsatilmagan: muallif tirikligida chop etilganini tasdiqlang"
    )
  }
  wsBusy.value = w.title
  try {
    const res = await api.post(
      '/api/library/wikisource/import',
      {
        authorId: ws.authorId,
        title: w.title,
        publishedInLifetime: true,
        noModernCommentary: true
      },
      { params: { schoolId: schoolStore.activeSchoolId } }
    )
    wsState[w.title] = {
      ok: true,
      message: `import qilindi · ${res.data.pages} bet`
    }
    coverEpoch.value++
    load()
  } catch (e) {
    wsState[w.title] = { ok: false, message: e.friendlyMessage || 'Xatolik' }
  } finally {
    wsBusy.value = ''
  }
}
</script>

<style scoped>
/* book covers: as many in a row as fit, none narrower than --grid-min-sm */
.book-grid {
  display: grid;
  grid-template-columns: repeat(
    auto-fill,
    minmax(min(var(--grid-min-sm), 100%), 1fr)
  );
  gap: var(--space-4);
}

.book-card {
  position: relative;
  overflow: hidden;
  height: 100%;
  transition:
    transform 0.15s ease,
    box-shadow 0.15s ease;
}
.book-card:hover,
.book-card:focus-visible {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.12);
}
.book-card--off .book-card__cover {
  opacity: 0.55;
}
.book-card__cover {
  position: relative;
  aspect-ratio: 3 / 4;
  padding: 8px 8px 0;
}
.book-card__badge {
  position: absolute;
  top: 14px;
  left: 14px;
}
.book-card__menu {
  position: absolute;
  top: 10px;
  right: 10px;
  background: rgba(15, 23, 42, 0.45);
  color: #fff;
}
.book-card__title {
  font-weight: 600;
  line-height: 1.25;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 2.5em;
}
.book-form {
  width: 920px;
  max-width: 96vw;
}
.bulk-form {
  width: 640px;
  max-width: 96vw;
}
.author-status {
  font-size: 12px;
  font-weight: 600;
  border-radius: var(--radius-sm);
  padding: 3px 8px;
  display: inline-block;
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
.legal-box {
  border: 1px dashed rgba(148, 163, 184, 0.6);
  border-radius: var(--radius-md);
  padding: 10px 12px;
}
.ws-form {
  width: 760px;
  max-width: 96vw;
}
.ws-list {
  max-height: var(--list-max-h);
  overflow-y: auto;
}
.status-filter {
  border: 1px solid rgba(148, 163, 184, 0.45);
}
.legal-banner {
  background: rgba(245, 158, 11, 0.12);
  border: 1px solid rgba(245, 158, 11, 0.35);
}
.drop-zone {
  border: 2px dashed rgba(148, 163, 184, 0.6);
  border-radius: var(--radius-lg);
  padding: 16px 10px;
  min-height: var(--dropzone-h);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  text-align: center;
  cursor: pointer;
  transition:
    border-color 0.15s ease,
    background 0.15s ease;
}
.drop-zone--cover {
  min-height: var(--dropzone-h-lg);
}
.drop-zone--over {
  border-color: var(--q-primary);
  background: rgba(99, 102, 241, 0.08);
}
.drop-zone--ok {
  border-style: solid;
  border-color: var(--q-positive);
}
.cover-preview {
  max-height: var(--thumb-lg);
  max-width: 100%;
  border-radius: var(--radius-sm);
}
.bulk-list {
  max-height: var(--list-max-h-sm);
  overflow-y: auto;
}
.muted-text {
  color: var(--brand-text-muted);
}
.hidden {
  display: none;
}
</style>
