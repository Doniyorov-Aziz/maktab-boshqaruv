// Bridge to the Telegram Mini App runtime (https://core.telegram.org/bots/webapps).
//
// Inside Telegram, telegram-web-app.js exposes window.Telegram.WebApp with the
// signed initData and the user's theme; it also publishes the theme as CSS
// variables (--tg-theme-bg-color, ...), which the Mini App styles read.

const SDK_URL = 'https://telegram.org/js/telegram-web-app.js'

let sdkPromise = null

/** Loads the Telegram SDK once. Resolves (never rejects) — outside Telegram it simply is not there. */
export function loadTelegramSdk() {
  if (window.Telegram?.WebApp) return Promise.resolve(window.Telegram.WebApp)
  if (!sdkPromise) {
    sdkPromise = new Promise(resolve => {
      const s = document.createElement('script')
      s.src = SDK_URL
      s.async = true
      s.onload = () => resolve(window.Telegram?.WebApp || null)
      s.onerror = () => resolve(null)
      document.head.appendChild(s)
    })
  }
  return sdkPromise
}

export function webApp() {
  return window.Telegram?.WebApp || null
}

/**
 * Telegram-signed initData. Outside Telegram (local testing) it can be passed
 * once as ?initData=... and is then kept for the browser session.
 */
export function initData() {
  const fromTelegram = webApp()?.initData
  if (fromTelegram) return fromTelegram
  try {
    const fromQuery = new URLSearchParams(window.location.search).get(
      'initData'
    )
    if (fromQuery) sessionStorage.setItem('tgInitData', fromQuery)
    return fromQuery || sessionStorage.getItem('tgInitData') || ''
  } catch {
    return ''
  }
}

/** "light" | "dark" — Telegram's current theme, or the system preference outside Telegram. */
export function colorScheme() {
  const wa = webApp()
  if (wa?.colorScheme) return wa.colorScheme
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches
    ? 'dark'
    : 'light'
}

export function haptic(type = 'light') {
  try {
    webApp()?.HapticFeedback?.impactOccurred(type)
  } catch {
    // not available outside Telegram
  }
}

/** "success" | "warning" | "error" — a short vibration after an action finished. */
export function hapticNotify(type = 'success') {
  try {
    webApp()?.HapticFeedback?.notificationOccurred(type)
  } catch {
    // not available outside Telegram
  }
}

export function hapticSelect() {
  try {
    webApp()?.HapticFeedback?.selectionChanged()
  } catch {
    // not available outside Telegram
  }
}

let backHandler = null

/** Telegram's native ‹ Back button in the header: shown with a handler, hidden with null. */
export function setBackButton(handler) {
  const button = webApp()?.BackButton
  if (!button) return
  if (backHandler) button.offClick?.(backHandler)
  backHandler = handler
  if (handler) {
    button.onClick?.(handler)
    button.show?.()
  } else {
    button.hide?.()
  }
}

let mainHandler = null

/** Telegram's big bottom MainButton; null hides it. */
export function setMainButton(text, handler) {
  const button = webApp()?.MainButton
  if (!button) return
  if (mainHandler) button.offClick?.(mainHandler)
  mainHandler = handler
  if (text && handler) {
    button.setText?.(text)
    button.onClick?.(handler)
    button.show?.()
  } else {
    button.hide?.()
  }
}

export function mainButtonProgress(on) {
  const button = webApp()?.MainButton
  if (!button) return
  if (on) button.showProgress?.(false)
  else button.hideProgress?.()
}
