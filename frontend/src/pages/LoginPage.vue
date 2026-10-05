<template>
  <q-layout>
    <q-page-container>
      <q-page class="login" :lang="lang === 'ru' ? 'ru' : 'uz'">
        <!-- Brand panel: a school "katakli daftar" (squared notebook) page -->
        <section
          ref="brandEl"
          class="login__brand"
          :aria-label="t.brandAria"
          @mousemove="onParallax"
          @mouseleave="resetParallax"
        >
          <div class="login__vignette" aria-hidden="true" />

          <header class="login__logo">
            <span class="login__logo-mark" aria-hidden="true">
              <q-icon name="school" size="26px" />
            </span>
            <span class="login__logo-name">Maktab Boshqaruv</span>
          </header>

          <div class="login__brand-body">
            <div class="login__intro">
              <div class="login__text">
                <h1 class="login__title">
                  {{ t.title1 }}<br />{{ t.title2 }}
                </h1>
                <p class="login__lead">{{ t.lead }}</p>
                <ul class="login__features">
                  <li v-for="(label, i) in t.features" :key="label">
                    <q-icon
                      :name="featureIcons[i]"
                      size="16px"
                      aria-hidden="true"
                    />
                    {{ label }}
                  </li>
                </ul>
              </div>

              <!-- Static sample cards: nothing real is shown before login -->
              <div class="login__scene" aria-hidden="true">
                <div class="scene">
                  <div class="scene__glow" />

                  <div
                    class="scene__slot scene__slot--timetable"
                    style="--d: 0.35"
                  >
                    <div class="glass glass--back">
                      <div class="glass__title">{{ t.timetableTitle }}</div>
                      <div class="week">
                        <div
                          v-for="(day, i) in t.days"
                          :key="day"
                          class="week__day"
                        >
                          <span class="week__label">{{ day }}</span>
                          <span
                            v-for="(c, j) in week[i]"
                            :key="j"
                            class="week__cell"
                            :style="{ background: c }"
                          />
                        </div>
                      </div>
                    </div>
                  </div>

                  <div
                    class="scene__slot scene__slot--attendance"
                    style="--d: 1"
                  >
                    <div class="glass glass--front">
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
                        <div class="glass__title">{{ t.attendanceTitle }}</div>
                        <div class="glass__label">{{ t.attendanceNote }}</div>
                      </div>
                    </div>
                  </div>

                  <div
                    class="scene__slot scene__slot--lesson"
                    style="--d: 0.85"
                  >
                    <div class="glass glass--front glass--column">
                      <div class="glass__label">{{ t.lessonTitle }}</div>
                      <div class="glass__chip">
                        <span class="glass__dot" />{{ t.lessonSubject }}
                      </div>
                      <div class="glass__meta">
                        <span>{{ t.lessonMeta }}</span>
                        <span>09:25–10:10</span>
                      </div>
                      <div class="glass__progress"><span /></div>
                    </div>
                  </div>

                  <div class="scene__slot scene__slot--grade" style="--d: 0.6">
                    <div class="glass glass--mid">
                      <span class="glass__grade">5</span>
                      <div>
                        <div class="glass__title">{{ t.gradeTitle }}</div>
                        <div class="glass__label">{{ t.gradeSubject }}</div>
                      </div>
                    </div>
                  </div>

                  <div
                    class="scene__slot scene__slot--telegram"
                    style="--d: 0.7"
                  >
                    <div class="glass glass--mid">
                      <span class="glass__tg" aria-hidden="true">
                        <q-icon name="send" size="18px" />
                      </span>
                      <div>
                        <div class="glass__title">{{ t.telegramTitle }}</div>
                        <div class="glass__label">
                          Telegram · 08:31 <span class="glass__ticks">✓✓</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div class="login__bottom">
              <p class="login__copyright">
                © 2026 Maktab Boshqaruv · v{{ version }}
              </p>
            </div>
          </div>
        </section>

        <!-- Top-right controls: language and theme -->
        <div class="login__controls">
          <div class="login__lang" role="group" :aria-label="t.language">
            <button
              v-for="l in ['uz', 'ru']"
              :key="l"
              type="button"
              class="login__lang-btn"
              :class="{ 'login__lang-btn--active': lang === l }"
              :aria-pressed="lang === l"
              @click="setLang(l)"
            >
              {{ l === 'uz' ? "O'Z" : 'RU' }}
            </button>
          </div>
          <q-btn
            flat
            round
            class="login__theme"
            :icon="$q.dark.isActive ? 'light_mode' : 'dark_mode'"
            :aria-label="$q.dark.isActive ? t.lightMode : t.darkMode"
            @click="toggleTheme"
          >
            <q-tooltip>{{
              $q.dark.isActive ? t.lightMode : t.darkMode
            }}</q-tooltip>
          </q-btn>
        </div>

        <!-- Form panel -->
        <section class="login__form-side">
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
                <span class="login__server-text">{{ t.serverDown }}</span>
                <q-btn
                  flat
                  dense
                  no-caps
                  :label="t.retry"
                  class="login__server-retry"
                  :loading="checking"
                  @click="checkHealth"
                />
              </div>

              <div class="login__card-head">
                <span class="login__card-logo" aria-hidden="true">
                  <q-icon name="school" size="26px" />
                </span>
                <span class="login__badge">{{ t.badge }}</span>
              </div>
              <h2 id="login-title" class="login__card-title">{{
                t.welcome
              }}</h2>
              <p class="login__card-sub">{{ t.subtitle }}</p>

              <q-form class="login__form" @submit.prevent="onSubmit">
                <label for="login-username" class="login__label">{{
                  t.login
                }}</label>
                <q-input
                  v-model="username"
                  for="login-username"
                  :placeholder="t.loginPlaceholder"
                  outlined
                  autofocus
                  autocomplete="username"
                  class="login__field"
                  :error="authError ? true : undefined"
                  :rules="[val => !!val || t.required]"
                  lazy-rules="ondemand"
                  @update:model-value="authError = false"
                >
                  <template v-slot:prepend>
                    <q-icon name="person_outline" aria-hidden="true" />
                  </template>
                </q-input>

                <label for="login-password" class="login__label">{{
                  t.password
                }}</label>
                <q-input
                  v-model="password"
                  for="login-password"
                  :placeholder="t.passwordPlaceholder"
                  :type="showPassword ? 'text' : 'password'"
                  outlined
                  autocomplete="current-password"
                  class="login__field"
                  :error="authError ? true : undefined"
                  :rules="[val => !!val || t.required]"
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
                        showPassword ? t.hidePassword : t.showPassword
                      "
                      :aria-pressed="showPassword"
                      @click="showPassword = !showPassword"
                    />
                  </template>
                </q-input>

                <div v-if="capsLock" class="login__caps" role="status">
                  {{ t.capsLock }}
                </div>

                <div class="login__row">
                  <q-checkbox
                    v-model="rememberMe"
                    :label="t.remember"
                    color="primary"
                    dense
                  />
                  <button
                    type="button"
                    class="login__link"
                    @click="forgotOpen = true"
                  >
                    {{ t.forgot }}
                  </button>
                </div>

                <div
                  v-if="errorMessage && !backendDown"
                  class="login__error"
                  role="alert"
                  aria-live="assertive"
                >
                  <q-icon name="error_outline" size="20px" aria-hidden="true" />
                  {{ shownError }}
                </div>

                <q-btn
                  type="submit"
                  class="login__submit"
                  :class="{ 'login__submit--success': success }"
                  unelevated
                  no-caps
                  :loading="loading"
                  :disable="loading || success"
                  :aria-label="t.submit"
                >
                  <template v-if="success">
                    <q-icon name="check" size="22px" class="q-mr-xs" />
                    {{ t.success }}
                  </template>
                  <template v-else>{{ t.submit }}</template>
                  <template v-slot:loading>
                    <q-spinner size="20px" class="q-mr-sm" />
                    {{ t.submitting }}
                  </template>
                </q-btn>

                <p class="login__terms">{{ t.terms }}</p>
              </q-form>
            </div>

            <ul class="login__trust">
              <li v-for="(label, i) in t.trust" :key="label">
                <q-icon :name="trustIcons[i]" size="15px" aria-hidden="true" />
                {{ label }}
              </li>
            </ul>
          </div>

          <p class="login__help">{{ t.help }}</p>
        </section>

        <q-dialog v-model="forgotOpen">
          <q-card class="login__dialog">
            <q-card-section class="row items-center no-wrap q-gutter-md">
              <q-icon name="key" size="28px" color="primary" />
              <div>
                <div class="text-subtitle1 text-weight-bold">{{
                  t.forgotTitle
                }}</div>
                <div class="login__dialog-text">{{ t.forgotText }}</div>
              </div>
            </q-card-section>
            <q-card-actions align="right">
              <q-btn
                v-close-popup
                flat
                no-caps
                color="primary"
                :label="t.forgotOk"
              />
            </q-card-actions>
          </q-card>
        </q-dialog>
      </q-page>
    </q-page-container>
  </q-layout>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { api } from '@/boot/axios'
import { useAuthStore } from '@/stores/auth'
import { loginTexts, LOGIN_LANG_KEY } from '@/i18n/login'
import { version } from '../../package.json'

const $q = useQuasar()
const router = useRouter()
const authStore = useAuthStore()

const username = ref(localStorage.getItem('rememberedUsername') || '')
const password = ref('')
const loading = ref(false)
const rememberMe = ref(!!localStorage.getItem('rememberedUsername'))
const errorMessage = ref('')
const errorStatus = ref(null)
const showPassword = ref(false)
const backendDown = ref(false)
const checking = ref(false)
const capsLock = ref(false)
const authError = ref(false)
const shaking = ref(false)
const forgotOpen = ref(false)
const success = ref(false)
const brandEl = ref(null)

const lang = ref(readLang())
const t = computed(() => loginTexts[lang.value])

const ringLength = 2 * Math.PI * 26
const featureIcons = [
  'fact_check',
  'star_outline',
  'calendar_view_week',
  'send'
]
const trustIcons = ['lock', 'verified_user', 'phone_iphone']

// Sample week: 5 days x 3 lessons, each a subject color.
const C = {
  math: '#818cf8',
  lang: '#f472b6',
  sci: '#34d399',
  art: '#fbbf24',
  pe: '#38bdf8'
}
const week = [
  [C.math, C.lang, C.sci],
  [C.lang, C.math, C.art],
  [C.sci, C.pe, C.math],
  [C.math, C.art, C.lang],
  [C.pe, C.sci, C.math]
]

/** Error text in the page language: the API message is Uzbek, so known cases are re-worded for RU. */
const shownError = computed(() => {
  if (errorStatus.value === 401) return t.value.badCredentials
  if (lang.value === 'uz') return errorMessage.value
  return t.value.genericError
})

function readLang() {
  try {
    return localStorage.getItem(LOGIN_LANG_KEY) === 'ru' ? 'ru' : 'uz'
  } catch {
    return 'uz'
  }
}

function setLang(l) {
  lang.value = l
  try {
    localStorage.setItem(LOGIN_LANG_KEY, l)
  } catch {
    // storage unavailable — the choice lasts until reload
  }
}

watch(
  () => t.value.pageTitle,
  title => {
    document.title = title
  },
  { immediate: true }
)

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

// Mouse parallax: desktop pointers only, never with reduced motion.
let parallaxOn = false
onMounted(() => {
  checkHealth()
  parallaxOn =
    window.matchMedia('(hover: hover) and (pointer: fine)').matches &&
    !window.matchMedia('(prefers-reduced-motion: reduce)').matches
})

onBeforeUnmount(() => {
  document.title = 'Maktab Boshqaruv'
})

function onParallax(event) {
  if (!parallaxOn || !brandEl.value) return
  const r = brandEl.value.getBoundingClientRect()
  const x = ((event.clientX - r.left) / r.width) * 2 - 1
  const y = ((event.clientY - r.top) / r.height) * 2 - 1
  brandEl.value.style.setProperty('--px', x.toFixed(3))
  brandEl.value.style.setProperty('--py', y.toFixed(3))
}

function resetParallax() {
  if (!brandEl.value) return
  brandEl.value.style.setProperty('--px', '0')
  brandEl.value.style.setProperty('--py', '0')
}

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

const pause = ms => new Promise(resolve => setTimeout(resolve, ms))

async function onSubmit() {
  if (loading.value || success.value) return // guards against a duplicate submit firing (e.g. Enter + button both resolving)
  errorMessage.value = ''
  errorStatus.value = null
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
    // A short green "✓" before leaving the page.
    loading.value = false
    success.value = true
    await pause(300)
    await router.push('/')
  } catch (error) {
    errorMessage.value = error.friendlyMessage || 'Kirishda xatolik yuz berdi'
    errorStatus.value = error.response ? error.response.status : null
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
  --px: 0;
  --py: 0;
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  overflow: hidden;
  color: var(--login-brand-text);
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

/* notebook margin line */
.login__brand::before {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  left: 72px;
  width: 2px;
  background: var(--login-margin-line);
}

/* soft hairline where the panels meet */
.login__brand::after {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  width: 1px;
  background: rgba(255, 255, 255, 0.1);
}

.login__vignette {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: radial-gradient(
    ellipse 85% 80% at 50% 50%,
    transparent 55%,
    rgba(8, 6, 30, 0.38) 100%
  );
}

.login__logo {
  position: absolute;
  top: 32px;
  left: 96px;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: var(--text-md);
  font-weight: 700;
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

/* one cohesive block, centred vertically below the logo */
.login__brand-body {
  position: relative;
  z-index: 1;
  display: flex;
  flex: 1;
  flex-direction: column;
  justify-content: center;
  gap: 32px;
  padding: 100px 40px 28px 96px;
  animation: loginRise 400ms var(--ease-out) both;
}

.login__intro {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 28px;
}

.login__text {
  max-width: 520px;
}

.login__title {
  margin: 0;
  font-size: var(--text-display);
  line-height: 1.08;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.login__lead {
  margin: 18px 0 0;
  max-width: 420px;
  font-size: var(--text-lead);
  line-height: 1.55;
  color: var(--login-brand-muted);
  text-wrap: balance;
}

/* ---- the card scene. Designed in px and zoomed to fit: zoom (unlike a
   scale transform) re-lays out the text, so it stays crisp at any size. */

.login__scene {
  --scene-zoom: 1;
  position: relative;
  min-width: 0;
}

.scene {
  position: relative;
  width: 500px;
  height: 416px;
  zoom: var(--scene-zoom);
}

.scene__glow {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 380px;
  height: 320px;
  transform: translate(-50%, -50%);
  border-radius: 50%;
  background: radial-gradient(
    circle,
    var(--login-scene-glow) 0%,
    rgba(79, 70, 229, 0.25) 45%,
    transparent 70%
  );
  filter: blur(80px);
}

.scene__slot {
  position: absolute;
  /* parallax: deeper cards move less */
  transform: translate3d(
    calc(var(--px) * var(--d) * 8px),
    calc(var(--py) * var(--d) * 6px),
    0
  );
  transition: transform var(--dur) var(--ease-out);
  will-change: transform;
}

/* landscape arrangement (under the text) — integer positions only */
.scene__slot--timetable {
  top: 0;
  left: 280px;
  z-index: 1;
}

.scene__slot--attendance {
  top: 36px;
  left: 0;
  z-index: 4;
}

.scene__slot--lesson {
  top: 168px;
  left: 150px;
  z-index: 5;
}

.scene__slot--grade {
  top: 300px;
  left: 0;
  z-index: 3;
}

.scene__slot--telegram {
  top: 336px;
  left: 228px;
  z-index: 3;
}

.glass {
  display: flex;
  align-items: center;
  gap: 14px;
  width: max-content;
  padding: 16px 18px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--login-glass-border);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
  white-space: nowrap;
  transform: translateZ(0);
  will-change: transform;
  animation: loginFloat 8s var(--ease-out) infinite;
  -webkit-font-smoothing: antialiased;
}

.glass--column {
  flex-direction: column;
  align-items: stretch;
  gap: 10px;
}

/* depth through size, shadow and opacity — front cards are almost opaque,
   so a card behind never shows through their text */
.glass--back {
  flex-direction: column;
  align-items: stretch;
  gap: 10px;
  padding: 14px 16px;
  background: rgba(255, 255, 255, 0.08);
  opacity: 0.72;
  box-shadow: 0 10px 24px rgba(10, 8, 40, 0.25);
  animation-duration: 9s;
  animation-delay: -3s;
  --tilt: 1deg;
}

.glass--mid {
  background: rgba(46, 40, 140, 0.78);
  box-shadow: 0 16px 32px rgba(10, 8, 40, 0.32);
  animation-duration: 7.5s;
  animation-delay: -1.5s;
  --tilt: -1deg;
}

.glass--front {
  background: rgba(52, 46, 158, 0.9);
  border-color: rgba(255, 255, 255, 0.22);
  box-shadow:
    0 24px 48px rgba(10, 8, 40, 0.45),
    inset 0 1px 0 rgba(255, 255, 255, 0.14);
  animation-duration: 8.5s;
}

.scene__slot--lesson .glass {
  min-width: 300px;
  animation-delay: -4.5s;
  --tilt: 0.5deg;
}

.scene__slot--attendance .glass {
  --tilt: -0.5deg;
}

.scene__slot--telegram .glass {
  animation-delay: -6s;
}

.ring {
  width: 60px;
  height: 60px;
  transform: rotate(-90deg);
  flex: none;
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

.scene__slot--attendance .glass__label {
  font-size: var(--text-xs);
}

.glass__chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  align-self: flex-start;
  padding: 5px 12px;
  border-radius: 999px;
  /* white on indigo-900: contrast ≈ 12:1 */
  background: #312e81;
  color: #ffffff;
  font-weight: 700;
  font-size: var(--text-base);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.16);
}

.glass__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #fbbf24;
}

.glass__meta {
  display: flex;
  justify-content: space-between;
  gap: 16px;
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
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: #059669;
  color: #ffffff;
  font-size: 22px;
  font-weight: 800;
}

.glass__tg {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #229ed9;
  color: #ffffff;
}

.glass__ticks {
  margin-left: 4px;
  color: #6ee7b7;
  font-weight: 700;
}

.week {
  display: grid;
  grid-template-columns: repeat(5, 30px);
  gap: 8px;
}

.week__day {
  display: grid;
  gap: 5px;
  justify-items: center;
}

.week__label {
  font-size: var(--text-xs);
  font-weight: 600;
  color: var(--login-brand-muted);
}

.week__cell {
  width: 30px;
  height: 14px;
  border-radius: 4px;
  opacity: 0.9;
}

/* ---- features and footer */

.login__bottom {
  display: grid;
  gap: 14px;
}

.login__features {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 28px 0 0;
  padding: 0;
  list-style: none;
}

.login__features li {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 7px 14px;
  border-radius: 999px;
  background: var(--login-chip-bg);
  border: 1px solid rgba(255, 255, 255, 0.08);
  font-size: 14px;
  font-weight: 600;
  color: var(--login-brand-muted);
}

.login__features .q-icon {
  color: var(--login-brand-text);
}

.login__copyright {
  margin: 0;
  font-size: var(--text-xs);
  color: var(--login-brand-faint);
}

/* ------------------------------------------------------------ controls */

.login__controls {
  position: absolute;
  top: 24px;
  right: 24px;
  z-index: 5;
  display: flex;
  align-items: center;
  gap: 10px;
}

.login__lang {
  display: inline-flex;
  padding: 3px;
  border-radius: 999px;
  background: var(--login-card-bg);
  border: 1px solid var(--login-card-border);
}

.login__lang-btn {
  min-width: 38px;
  height: 30px;
  padding: 0 10px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: var(--brand-text-muted);
  font: inherit;
  font-size: var(--text-sm);
  font-weight: 700;
  cursor: pointer;
  transition:
    background var(--dur-fast) var(--ease-out),
    color var(--dur-fast) var(--ease-out);
}

.login__lang-btn--active {
  background: var(--color-brand);
  color: #ffffff;
}

.login__theme {
  width: 40px;
  height: 40px;
  min-height: 40px;
  background: var(--login-card-bg);
  border: 1px solid var(--login-card-border);
  color: var(--brand-text-muted);
}

/* ------------------------------------------------------------ form panel */

.login__form-side {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 88px 32px 72px;
  background:
    radial-gradient(
      560px circle at 50% 46%,
      var(--login-form-glow),
      transparent 70%
    ),
    var(--login-form-bg);
}

/* 40px fade from the brand panel, so the seam is not a hard edge */
.login__form-side::before {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  width: 40px;
  background: linear-gradient(90deg, var(--login-edge), transparent);
  pointer-events: none;
}

.login__form-wrap {
  position: relative;
  width: 100%;
  max-width: 440px;
  animation: loginRise 400ms var(--ease-out) 60ms both;
}

.login__card {
  padding: 44px;
  border-radius: var(--radius-xl);
  background: var(--login-card-bg);
  border: 1px solid var(--login-card-border);
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
  margin: -16px -16px 24px;
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

.login__card-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.login__card-logo {
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  color: #ffffff;
  box-shadow: 0 8px 18px rgba(79, 70, 229, 0.3);
}

.login__badge {
  padding: 5px 12px;
  border-radius: 999px;
  background: var(--login-badge-bg);
  color: var(--login-badge-text);
  font-size: var(--text-sm);
  font-weight: 700;
}

.login__card-title {
  margin: 0;
  font-size: var(--text-title);
  line-height: 1.2;
  font-weight: 800;
  letter-spacing: -0.01em;
}

.login__card-sub {
  margin: 8px 0 26px;
  font-size: var(--text-md);
  color: var(--brand-text-muted);
}

.login__form {
  display: grid;
}

.login__label {
  margin: 0 0 8px 2px;
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--text-primary);
}

.login__field {
  margin-bottom: 6px;
}

.login__field :deep(.q-field__control) {
  height: 52px;
  border-radius: var(--radius-md);
  transition: box-shadow var(--dur) var(--ease-out);
}

.login__field :deep(.q-field__marginal) {
  height: 52px;
}

.login__field :deep(.q-field__native::placeholder) {
  color: var(--brand-text-muted);
  opacity: 0.8;
}

/* QInput's root element carries both .login__field and .q-field--focused */
.login__field.q-field--focused :deep(.q-field__control) {
  box-shadow:
    0 0 0 4px var(--login-input-ring),
    0 0 24px var(--login-input-glow);
}

.login__field.q-field--error :deep(.q-field__control) {
  box-shadow: 0 0 0 4px rgba(239, 68, 68, 0.18);
}

.login__caps {
  margin: -6px 0 6px;
  font-size: var(--text-sm);
  font-weight: 600;
  color: #b45309;
}

.body--dark .login__caps {
  color: #fcd34d;
}

.login__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 4px 0 18px;
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
  margin-bottom: 14px;
  padding: 10px 12px;
  border-radius: var(--radius-md);
  background: rgba(239, 68, 68, 0.1);
  color: #b91c1c;
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
  background-size: 200% 100%;
  background-position: 0% 0;
  color: #ffffff;
  font-size: var(--text-md);
  font-weight: 700;
  box-shadow: 0 8px 20px rgba(79, 70, 229, 0.28);
  transition:
    transform var(--dur) var(--ease-out),
    box-shadow var(--dur) var(--ease-out),
    background-position var(--dur) var(--ease-out),
    background-color var(--dur) var(--ease-out);
}

.login__submit:hover:not(.disabled) {
  transform: translateY(-1px);
  background-position: 100% 0;
  box-shadow: 0 12px 26px rgba(79, 70, 229, 0.36);
}

.login__submit:active:not(.disabled) {
  transform: scale(0.98);
  transition-duration: var(--dur-fast);
}

.login__submit--success,
.login__submit--success.disabled {
  background: #059669 !important;
  opacity: 1 !important;
  box-shadow: 0 8px 20px rgba(5, 150, 105, 0.3);
}

.login__terms {
  margin: 16px 0 0;
  text-align: center;
  font-size: var(--text-xs);
  line-height: 1.5;
  color: var(--brand-text-muted);
}

.login__trust {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px 20px;
  margin: 22px 0 0;
  padding: 0;
  list-style: none;
  font-size: var(--text-sm);
  color: var(--brand-text-muted);
}

.login__trust li {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.login__help {
  position: absolute;
  left: 24px;
  right: 24px;
  bottom: 24px;
  margin: 0;
  text-align: center;
  font-size: var(--text-sm);
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
    transform: translateZ(0) translateY(0) rotate(var(--tilt, 0deg));
  }
  50% {
    transform: translateZ(0) translateY(-5px) rotate(var(--tilt, 0deg));
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

/* ≥1440: text on the left and a tall card scene filling the right half.
   Column widths and the scene zoom are sized so nothing reaches the edge. */
@media (min-width: 1440px) {
  .login {
    grid-template-columns: 60fr 40fr;
  }

  .login__brand-body {
    padding: 96px 24px 24px 96px;
  }

  .login__intro {
    grid-template-columns: minmax(0, 330px) minmax(0, 1fr);
    align-items: center;
    gap: 24px;
  }

  .login__title {
    font-size: 44px;
  }

  .login__lead {
    font-size: var(--text-md);
  }

  .login__scene {
    --scene-zoom: 0.8;
    justify-self: end;
  }

  .scene {
    width: 480px;
    height: 530px;
  }

  .scene__slot--timetable {
    top: 0;
    left: 222px;
  }

  .scene__slot--attendance {
    top: 100px;
    left: 0;
  }

  .scene__slot--lesson {
    top: 250px;
    left: 120px;
  }

  .scene__slot--grade {
    top: 420px;
    left: 0;
  }

  .scene__slot--telegram {
    top: 452px;
    left: 190px;
  }
}

@media (min-width: 1680px) {
  .login__intro {
    grid-template-columns: minmax(0, 400px) minmax(0, 1fr);
    gap: 32px;
  }

  .login__title {
    font-size: 52px;
  }

  .login__lead {
    font-size: var(--text-lead);
  }

  .login__scene {
    --scene-zoom: 0.92;
  }
}

/* wide screens: a larger text column so both columns are about as tall */
@media (min-width: 1880px) {
  .login__intro {
    grid-template-columns: minmax(0, 440px) minmax(0, 1fr);
  }

  .login__title {
    font-size: 60px;
  }

  .login__lead {
    max-width: 440px;
    font-size: 20px;
  }

  .login__features {
    margin-top: 32px;
  }

  .login__features li {
    padding: 9px 16px;
    font-size: 15px;
  }

  .login__scene {
    --scene-zoom: 1.15;
  }
}

/* 1280–1439: the scene sits under the text */
@media (min-width: 1280px) and (max-width: 1439px) {
  .login__title {
    font-size: 46px;
  }

  .login__scene {
    --scene-zoom: 0.8;
  }
}

@media (max-width: 1279px) {
  .login {
    grid-template-columns: 1fr 1fr;
  }

  .login__brand-body {
    gap: 24px;
    padding: 96px 28px 28px 84px;
  }

  .login__logo {
    left: 84px;
  }

  .login__brand::before {
    left: 56px;
  }

  .login__title {
    font-size: 40px;
  }

  .login__intro {
    gap: 22px;
  }

  .login__scene {
    --scene-zoom: 0.74;
  }

  .login__card {
    padding: 36px;
  }
}

@media (max-width: 1023px) {
  .login__scene {
    --scene-zoom: 0.62;
  }
}

/* phone: compact header, the controls get their own row above the form */
@media (max-width: 767px) {
  .login {
    grid-template-columns: 1fr;
  }

  .login__brand {
    min-height: 0;
  }

  .login__brand::after {
    display: none;
  }

  .login__logo {
    position: relative;
    top: auto;
    left: auto;
    padding: 24px 20px 0 44px;
  }

  .login__brand::before {
    left: 26px;
  }

  .login__brand-body {
    gap: 0;
    padding: 20px 20px 28px 44px;
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
  .login__bottom {
    display: none;
  }

  .login__controls {
    position: static;
    justify-content: flex-end;
    padding: 16px 16px 0;
    background: var(--login-form-bg);
  }

  .login__form-side {
    padding: 16px 16px 72px;
    justify-content: flex-start;
  }

  .login__form-side::before {
    display: none;
  }

  .login__card {
    padding: 28px 22px;
  }

  .login__trust {
    gap: 8px 14px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .login__brand-body,
  .login__form-wrap,
  .glass,
  .login__card--shake {
    animation: none !important;
  }

  .scene__slot,
  .login__submit,
  .login__field :deep(.q-field__control),
  .login__lang-btn {
    transition: none !important;
  }

  .scene__slot {
    transform: none !important;
  }

  .glass {
    transform: rotate(var(--tilt, 0deg));
  }
}
</style>
