// ESLint runs one rule oxlint cannot check (it does not read Vue templates):
// pages, layouts and components use the App* wrappers, never q-input / q-select /
// q-btn directly. The wrappers themselves live in src/components/app/.
import vue from 'eslint-plugin-vue'
import vueParser from 'vue-eslint-parser'

const message = 'Use the design-system component from src/components/app/'

export default [
  {
    files: ['src/**/*.vue'],
    ignores: ['src/components/app/**'],
    languageOptions: { parser: vueParser, sourceType: 'module', ecmaVersion: 'latest' },
    plugins: { vue },
    rules: {
      'vue/no-restricted-html-elements': [
        'error',
        { element: ['q-input', 'QInput'], message: message + ' (AppInput / AppDateInput)' },
        { element: ['q-select', 'QSelect'], message: message + ' (AppSelect)' },
        { element: ['q-btn', 'QBtn'], message: message + ' (AppButton)' }
      ]
    }
  }
]
