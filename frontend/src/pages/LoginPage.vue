<template>
  <q-layout>
    <q-page-container>
      <q-page class="login">
        <!-- Brand panel: a school "katakli daftar" (squared notebook) page -->
        <section class="login__brand" aria-label="Maktab Boshqaruv haqida">
          <div class="login__brand-inner">
            <header class="login__logo">
              <span class="login__logo-mark" aria-hidden="true">
                <q-icon name="school" size="26px" />
              </span>
              <span class="login__logo-name">Maktab Boshqaruv</span>
            </header>

            <div class="login__hero">
              <h1 class="login__title"> Maktabingiz —<br />bir qarashda </h1>
              <p class="login__lead">
                Davomat, baholar, dars jadvali va ota-onalar bilan aloqa — bitta
                tizimda.
              </p>

              <!-- Static sample cards: nothing real is shown before login -->
              <div class="login__scene" aria-hidden="true">
                <div class="glass glass--attendance">
                  <svg class="ring" viewBox="0 0 64 64">
                    <circle class="ring__track" cx="32" cy="32" r="26" />
                    <circle
                      class="ring__value"
                      cx="32"
                      cy="32"
                      r="26"
                      :stroke-dasharray="ringLength"
                      :stroke-dashoffset="ringLength * (1 - 0.94)"
                    />
                  </svg>
                  <div>
                    <div class="glass__value">94%</div>
                    <div class="glass__label">Bugungi davomat</div>
                  </div>
                </div>

                <div class="glass glass--lesson">
                  <div class="glass__label">Hozirgi dars</div>
                  <div class="glass__chip">
                    <span class="glass__dot" />Matematika
                  </div>
                  <div class="glass__meta">5-A · 201-xona · 09:25–10:10</div>
                  <div class="glass__progress"><span /></div>
                </div>

                <div class="glass glass--grade">
                  <span class="glass__grade">5</span>
                  <div>
                    <div class="glass__title">Yangi baho</div>
                    <div class="glass__label">Ona tili</div>
                  </div>
                </div>

                <div class="glass glass--telegram">
                  <q-icon name="send" size="20px" class="glass__tg-icon" />
                  <div>
                    <div class="glass__title">Ota-onaga xabar ketdi</div>
                    <div class="glass__label">
                      Telegram · 08:31 <span class="glass__ticks">✓✓</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <ul class="login__features">
              <li v-for="f in features" :key="f.label">
                <q-icon :name="f.icon" size="18px" aria-hidden="true" />
                {{ f.label }}
              </li>
            </ul>

            <footer class="login__copyright">
              © 2026 Maktab Boshqaruv · v{{ version }}
            </footer>
          </div>
        </section>

        <!-- Form panel -->
        <section class="login__form-side">
          <q-btn
            flat
            round
            class="login__theme"
            :icon="$q.dark.isActive ? 'light_mode' : 'dark_mode'"
            :aria-label="
              $q.dark.isActive
                ? 'Yorug\' rejimga o\'tish'
                : 'Qorong\'i rejimga o\'tish'
            "
            @click="toggleTheme"
          >
            <q-tooltip>{{
              $q.dark.isActive ? "Yorug' rejim" : "Qorong'i rejim"
            }}</q-tooltip>
          </q-btn>

          <div class="login__form-wrap">
            <div
              class="login__card"
              :class="{ 'login__card--shake': shaking }"
              role="region"
              aria-labelledby="login-title"
            >
              <div
                v-if="backendDown"
                class="login__server"
                role="alert"
                aria-live="assertive"
              >
                <q-icon name="cloud_off" size="20px" aria-hidden="true" />
                <span class="login__server-text"
                  >Server bilan bog'lanib bo'lmadi</span
                >
                <q-btn
                  flat
                  dense
                  no-caps
                  label="Qayta tekshirish"
                  class="login__server-retry"
                  :loading="checking"
                  @click="checkHealth"
                />
              </div>

              <div class="login__greet" aria-hidden="true">
                <q-icon name="waving_hand" size="24px" />
              </div>
              <h2 id="login-title" class="login__card-title">Xush kelibsiz</h2>
              <p class="login__card-sub">
                Davom etish uchun hisobingizga kiring
              </p>

              <q-form class="login__form" @submit.prevent="onSubmit">
                <q-input
                  v-model="username"
                  label="Login"
                  outlined
                  autofocus
                  autocomplete="username"
                  class="login__field"
                  :error="authError ? true : undefined"
                  :rules="[val => !!val || 'Majburiy maydon']"
                  lazy-rules="ondemand"
                  @update:model-value="authError = false"
                >
                  <template v-slot:prepend>
                    <q-icon name="person_outline" aria-hidden="true" />
                  </template>
                </q-input>

                <q-input
                  v-model="password"
                  label="Parol"
                  :type="showPassword ? 'text' : 'password'"
                  outlined
                  autocomplete="current-password"
                  class="login__field"
                  :error="authError ? true : undefined"
                  :rules="[val => !!val || 'Majburiy maydon']"
                  lazy-rules="ondemand"
                  @update:model-value="authError = false"
                  @keydown="detectCaps"
                  @keyup="detectCaps"
                  @blur="capsLock = false"
                >
                  <template v-slot:prepend>
                    <q-icon name="lock_outline" aria-hidden="true" />
                  </template>
                  <template v-slot:append>
                    <q-btn
                      flat
                      round
                      dense
                      :icon="showPassword ? 'visibility_off' : 'visibility'"
                      :aria-label="
                        showPassword
                          ? 'Parolni yashirish'
                          : 'Parolni ko\'rsatish'
                      "
                      :aria-pressed="showPassword"
                      @click="showPassword = !showPassword"
                    />
                  </template>
                </q-input>

                <div v-if="capsLock" class="login__caps" role="status">
                  ⇪ Caps Lock yoqilgan
                </div>

                <div class="login__row">
                  <q-checkbox
                    v-model="rememberMe"
                    label="Eslab qolish"
                    color="primary"
                    dense
                  />
                  <button
                    type="button"
                    class="login__link"
                    @click="forgotOpen = true"
                  >
                    Parolni unutdingizmi?
                  </button>
                </div>

                <div
                  v-if="errorMessage && !backendDown"
                  class="login__error"
                  role="alert"
                  aria-live="assertive"
                >
                  <q-icon name="error_outline" size="20px" aria-hidden="true" />
                  {{ errorMessage }}
                </div>

                <q-btn
                  type="submit"
                  class="login__submit"
                  unelevated
                  no-caps
                  :loading="loading"
                  :disable="loading"
                  aria-label="Kirish"
                >
                  Kirish
                  <template v-slot:loading>
                    <q-spinner size="20px" class="q-mr-sm" />
                    Kirilmoqda...
                  </template>
                </q-btn>
              </q-form>
            </div>

            <p class="login__terms">
              Kirish bilan siz maktab ma'lumotlaridan foydalanish qoidalariga
              rozilik bildirasiz.
            </p>
          </div>
        </section>

        <q-dialog v-model="forgotOpen">
          <q-card class="login__dialog">
            <q-card-section class="row items-center no-wrap q-gutter-md">
              <q-icon name="key" size="28px" color="primary" />
              <div>
                <div class="text-subtitle1 text-weight-bold">
                  Parolni tiklash
                </div>
                <div class="login__dialog-text">
                  Parolni tiklash uchun maktab administratoriga murojaat qiling.
                </div>
              </div>
            </q-card-section>
            <q-card-actions align="right">
              <q-btn
                v-close-popup
                flat
                no-caps
                color="primary"
                label="Tushunarli"
              />
            </q-card-actions>
          </q-card>
        </q-dialog>
      </q-page>
    </q-page-container>
  </q-layout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { api } from '@/boot/axios'
import { useAuthStore } from '@/stores/auth'
import { version } from '../../package.json'

const $q = useQuasar()
const router = useRouter()
const authStore = useAuthStore()

const username = ref(localStorage.getItem('rememberedUsername') || '')
const password = ref('')
const loading = ref(false)
const rememberMe = ref(!!localStorage.getItem('rememberedUsername'))
const errorMessage = ref('')
const showPassword = ref(false)
const backendDown = ref(false)
const checking = ref(false)
const capsLock = ref(false)
const authError = ref(false)
const shaking = ref(false)
const forgotOpen = ref(false)

const ringLength = 2 * Math.PI * 26

const features = [
  { icon: 'fact_check', label: 'Davomat' },
  { icon: 'star_outline', label: 'Baholar' },
  { icon: 'calendar_view_week', label: 'Dars jadvali' },
  { icon: 'send', label: 'Telegram xabarnomalar' }
]

async function checkHealth() {
  checking.value = true
  try {
    await api.get('/api/health', { timeout: 4000 })
    backendDown.value = false
  } catch {
    backendDown.value = true
  } finally {
    checking.value = false
  }
}

onMounted(checkHealth)

function detectCaps(event) {
  if (typeof event.getModifierState === 'function') {
    capsLock.value = event.getModifierState('CapsLock')
  }
}

function toggleTheme() {
  $q.dark.toggle()
  try {
    localStorage.setItem('darkMode', String($q.dark.isActive))
  } catch {
    // storage unavailable — the choice lasts until reload
  }
}

function shake() {
  shaking.value = false
  requestAnimationFrame(() => {
    shaking.value = true
    setTimeout(() => (shaking.value = false), 320)
  })
}

async function onSubmit() {
  if (loading.value) return // guards against a duplicate submit firing (e.g. Enter + button both resolving)
  errorMessage.value = ''
  authError.value = false
  loading.value = true
  try {
    const response = await api.post(
      '/api/auth/login',
      { username: username.value, password: password.value },
      { responseType: 'text' }
    )
    backendDown.value = false
    authStore.setToken(response.data)
    if (rememberMe.value) {
      localStorage.setItem('rememberedUsername', username.value)
    } else {
      localStorage.removeItem('rememberedUsername')
    }
    await router.push('/')
  } catch (error) {
    errorMessage.value = error.friendlyMessage || 'Kirishda xatolik yuz berdi'
    if (!error.response) {
      backendDown.value = true
    } else {
      authError.value = true
      shake()
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* ---------------------------------------------------------------- layout */

.login {
  position: relative;
  display: grid;
  grid-template-columns: 55fr 45fr;
  min-height: 100vh;
  font-family: Inter, sans-serif;
}

/* ----------------------------------------------------------- brand panel */

.login__brand {
  position: relative;
  overflow: hidden;
  color: var(--login-brand-text);
  /* squared notebook: 24px grid lines over a soft indigo gradient */
  background-color: var(--login-brand-from);
  background-image:
    linear-gradient(var(--login-grid-line) 1px, transparent 1px),
    linear-gradient(90deg, var(--login-grid-line) 1px, transparent 1px),
    linear-gradient(
      160deg,
      var(--login-brand-from) 0%,
      var(--login-brand-to) 100%
    );
  background-size:
    24px 24px,
    24px 24px,
    100% 100%;
}

/* the notebook's red margin line */
.login__brand::before {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  left: 72px;
  width: 2px;
  background: var(--login-margin-line);
}

.login__brand-inner {
  position: relative;
  display: grid;
  grid-template-rows: auto 1fr auto auto;
  gap: 32px;
  height: 100%;
  min-height: 100vh;
  padding: 40px 56px 32px 104px;
  animation: loginRise 0.4s ease-out both;
}

.login__logo {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: var(--text-md);
  font-weight: 700;
  letter-spacing: 0.01em;
}

.login__logo-mark {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  background: var(--login-brand-text);
  color: var(--login-brand-to);
}

.login__hero {
  align-self: center;
  max-width: 640px;
}

.login__title {
  margin: 0;
  font-size: var(--text-display);
  line-height: 1.08;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.login__lead {
  margin: 20px 0 0;
  max-width: 520px;
  font-size: var(--text-lead);
  line-height: 1.55;
  color: var(--login-brand-muted);
}

/* ---- glass cards scene */

.login__scene {
  position: relative;
  height: 300px;
  margin-top: 40px;
  max-width: 600px;
}

.glass {
  position: absolute;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  border-radius: var(--radius-lg);
  background: var(--login-glass-bg);
  border: 1px solid var(--login-glass-border);
  box-shadow: 0 18px 40px rgba(10, 8, 40, 0.35);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  /* size to the content: a narrow panel must not squeeze the text into a column */
  width: max-content;
  white-space: nowrap;
  animation: loginFloat 7s ease-in-out infinite;
}

.glass--attendance {
  top: 8px;
  left: 0;
  --tilt: -4deg;
  animation-delay: 0s;
}

.glass--lesson {
  top: 0;
  left: 236px;
  width: 260px;
  flex-direction: column;
  align-items: stretch;
  gap: 8px;
  --tilt: 3deg;
  animation-delay: -2.2s;
  animation-duration: 8s;
}

.glass--grade {
  top: 150px;
  left: 56px;
  --tilt: 2deg;
  animation-delay: -4.1s;
  animation-duration: 6.5s;
}

.glass--telegram {
  top: 172px;
  left: 300px;
  --tilt: -3deg;
  animation-delay: -1.3s;
  animation-duration: 7.5s;
}

.ring {
  width: 56px;
  height: 56px;
  transform: rotate(-90deg);
}

.ring circle {
  fill: none;
  stroke-width: 7;
}

.ring__track {
  stroke: rgba(255, 255, 255, 0.16);
}

.ring__value {
  stroke: #34d399;
  stroke-linecap: round;
}

.glass__value {
  font-size: var(--text-xl);
  font-weight: 800;
  line-height: 1.1;
}

.glass__title {
  font-size: var(--text-base);
  font-weight: 700;
}

.glass__label {
  font-size: var(--text-sm);
  color: var(--login-brand-muted);
}

.glass__chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  align-self: flex-start;
  padding: 4px 12px;
  border-radius: 999px;
  background: rgba(251, 191, 36, 0.2);
  color: #fde68a;
  font-weight: 700;
  font-size: var(--text-base);
}

.glass__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #fbbf24;
}

.glass__meta {
  font-size: var(--text-sm);
  color: var(--login-brand-muted);
}

.glass__progress {
  height: 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  overflow: hidden;
}

.glass__progress span {
  display: block;
  width: 62%;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #a5b4fc, #f0abfc);
}

.glass__grade {
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background: #10b981;
  color: #fff;
  font-size: 24px;
  font-weight: 800;
}

.glass__tg-icon {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: #229ed9;
  color: #fff;
}

.glass__ticks {
  margin-left: 4px;
  color: #6ee7b7;
  font-weight: 700;
}

/* ---- features and footer */

.login__features {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 26px;
  margin: 0;
  padding: 0;
  list-style: none;
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--login-brand-muted);
}

.login__features li {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.login__features .q-icon {
  color: var(--login-brand-text);
}

.login__copyright {
  font-size: var(--text-xs);
  color: var(--login-brand-muted);
  opacity: 0.85;
}

/* ------------------------------------------------------------ form panel */

.login__form-side {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 32px;
  background: var(--login-form-bg);
}

.login__theme {
  position: absolute;
  top: 20px;
  right: 20px;
  color: var(--brand-text-muted);
}

.login__form-wrap {
  width: 100%;
  max-width: 420px;
  animation: loginRise 0.4s ease-out 0.06s both;
}

.login__card {
  padding: 40px;
  border-radius: var(--radius-xl);
  background: var(--login-card-bg);
  box-shadow: var(--login-card-shadow);
  color: var(--text-primary);
}

.login__card--shake {
  animation: loginShake 0.3s ease-in-out;
}

.login__server {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: -12px -12px 24px;
  padding: 10px 12px;
  border-radius: var(--radius-md);
  background: var(--login-warning-bg);
  color: var(--login-warning-text);
  font-size: var(--text-base);
  font-weight: 600;
}

.login__server-text {
  flex: 1;
}

.login__server-retry {
  color: inherit;
  font-weight: 700;
}

.login__greet {
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  margin-bottom: 16px;
  border-radius: var(--radius-md);
  background: rgba(79, 70, 229, 0.12);
  color: var(--color-brand);
}

.body--dark .login__greet {
  background: rgba(129, 140, 248, 0.16);
  color: #a5b4fc;
}

.login__card-title {
  margin: 0;
  font-size: var(--text-title);
  line-height: 1.2;
  font-weight: 800;
  letter-spacing: -0.01em;
}

.login__card-sub {
  margin: 8px 0 28px;
  font-size: var(--text-md);
  color: var(--brand-text-muted);
}

.login__form {
  display: grid;
  gap: 6px;
}

.login__field :deep(.q-field__control) {
  height: 52px;
  border-radius: var(--radius-md);
}

.login__field :deep(.q-field__marginal) {
  height: 52px;
}

/* QInput's root element carries both .login__field and .q-field--focused */
.login__field.q-field--focused :deep(.q-field__control) {
  box-shadow: 0 0 0 4px var(--login-input-ring);
}

.login__field.q-field--error :deep(.q-field__control) {
  box-shadow: 0 0 0 4px rgba(239, 68, 68, 0.18);
}

.login__caps {
  margin: -8px 0 4px;
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--color-warning);
}

.body--dark .login__caps {
  color: #fcd34d;
}

.login__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 2px 0 14px;
}

.login__link {
  padding: 4px 2px;
  border: 0;
  background: none;
  color: var(--color-brand);
  font: inherit;
  font-size: var(--text-base);
  font-weight: 600;
  cursor: pointer;
  border-radius: var(--radius-sm);
}

.body--dark .login__link {
  color: #a5b4fc;
}

.login__link:hover {
  text-decoration: underline;
}

.login__error {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding: 10px 12px;
  border-radius: var(--radius-md);
  background: rgba(239, 68, 68, 0.1);
  color: var(--color-danger);
  font-size: var(--text-base);
  font-weight: 600;
}

.body--dark .login__error {
  color: #fca5a5;
}

.login__submit {
  height: 52px;
  border-radius: var(--radius-md);
  background: var(--login-button-gradient);
  color: #fff;
  font-size: var(--text-md);
  font-weight: 700;
  box-shadow: 0 8px 20px rgba(79, 70, 229, 0.28);
  transition:
    transform 0.15s ease,
    box-shadow 0.15s ease;
}

.login__submit:hover:not(.disabled) {
  transform: translateY(-2px);
  box-shadow: 0 12px 26px rgba(79, 70, 229, 0.36);
}

.login__terms {
  margin: 20px 8px 0;
  text-align: center;
  font-size: var(--text-sm);
  line-height: 1.5;
  color: var(--brand-text-muted);
}

.login__dialog {
  max-width: 400px;
  border-radius: var(--radius-lg);
}

.login__dialog-text {
  margin-top: 4px;
  color: var(--brand-text-muted);
}

/* visible keyboard focus everywhere */
.login :focus-visible {
  outline: 3px solid var(--login-input-ring);
  outline-offset: 2px;
}

/* ------------------------------------------------------------ animations */

@keyframes loginRise {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes loginFloat {
  0%,
  100% {
    transform: translateY(0) rotate(var(--tilt, 0deg));
  }
  50% {
    transform: translateY(-10px) rotate(var(--tilt, 0deg));
  }
}

@keyframes loginShake {
  0%,
  100% {
    transform: translateX(0);
  }
  20%,
  60% {
    transform: translateX(-8px);
  }
  40%,
  80% {
    transform: translateX(8px);
  }
}

/* ------------------------------------------------------------ responsive */

/* Large screens: a bigger headline and card composition fill the panel. */
@media (min-width: 1600px) {
  .login__brand-inner {
    padding: 48px 72px 36px 120px;
  }

  .login__brand::before {
    left: 84px;
  }

  .login__hero {
    max-width: 780px;
  }

  .login__title {
    font-size: 56px;
  }

  .login__lead {
    max-width: 580px;
  }

  .login__scene {
    max-width: 780px;
    height: 400px;
    margin-top: 48px;
    transform: scale(1.28);
    transform-origin: left top;
  }
}

@media (max-width: 1279px) {
  .login {
    grid-template-columns: 1fr 1fr;
  }

  .login__brand-inner {
    padding: 32px 36px 28px 84px;
    gap: 24px;
  }

  .login__brand::before {
    left: 56px;
  }

  .login__title {
    font-size: 44px;
  }

  .login__scene {
    height: 250px;
    transform: scale(0.82);
    transform-origin: left top;
    margin-bottom: -40px;
  }
}

@media (min-width: 768px) and (max-width: 1099px) {
  .login__scene {
    height: 220px;
    transform: scale(0.68);
    margin-bottom: -70px;
  }
}

@media (max-width: 767px) {
  .login {
    grid-template-columns: 1fr;
  }

  .login__brand-inner {
    min-height: 0;
    grid-template-rows: auto auto;
    gap: 24px;
    padding: 24px 20px 28px 44px;
  }

  .login__brand::before {
    left: 26px;
  }

  .login__title {
    font-size: 32px;
  }

  .login__lead {
    margin-top: 10px;
    font-size: var(--text-md);
  }

  .login__scene,
  .login__features,
  .login__copyright {
    display: none;
  }

  /* static here, so the theme button is placed against the whole page —
     in the brand header's top-right corner, clear of the text */
  .login__form-side {
    position: static;
    align-items: flex-start;
    padding: 24px 16px 40px;
  }

  .login__theme {
    top: 22px;
    right: 12px;
    color: var(--login-brand-text);
  }

  .login__card {
    padding: 28px 22px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .login__brand-inner,
  .login__form-wrap,
  .glass,
  .login__card--shake {
    animation: none !important;
  }

  .login__submit {
    transition: none;
  }

  .glass {
    transform: rotate(var(--tilt, 0deg));
  }
}
</style>
