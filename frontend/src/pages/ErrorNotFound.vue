<template>
  <q-layout>
    <q-page-container>
      <q-page
        class="flex flex-center column text-center q-pa-md"
        style="min-height: 100vh"
      >
        <q-avatar
          size="88px"
          class="brand-gradient q-mb-lg"
          text-color="white"
          icon="explore_off"
        />
        <div class="text-h2 text-weight-bold text-grey-9">404</div>
        <div class="text-h6 text-grey-7 q-mt-sm q-mb-lg">
          Bunday sahifa topilmadi
        </div>
        <q-btn
          color="primary"
          icon="home"
          label="Bosh sahifaga qaytish"
          unelevated
          no-caps
          class="q-px-md"
          to="/"
        />
      </q-page>
    </q-page-container>
  </q-layout>
</template>
