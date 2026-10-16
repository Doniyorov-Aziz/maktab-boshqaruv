// UI tests (npm test): design-system sizes and layout on every page.
// They open the built app (dist/spa, served here on UI_BASE) against a running
// backend (UI_API) and sign in as UI_USER / UI_PASS — see docs/ui-system/REPORT.md.
import { defineConfig } from '@playwright/test'

const base = process.env.UI_BASE || 'http://localhost:9100'

export default defineConfig({
  testDir: 'tests/ui',
  testMatch: '*.spec.js',
  timeout: 60_000,
  workers: 4,
  reporter: [['list']],
  use: { baseURL: base },
  webServer: {
    command: `npx vite preview --outDir dist/spa --port ${new URL(base).port} --strictPort`,
    url: base,
    reuseExistingServer: true
  }
})
