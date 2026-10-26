// The public demo visitor (role GUEST) sees everything and changes nothing. AppButton disables
// the buttons that change data by itself (isWriteAction); the few gestures that are not buttons
// (marking attendance, dragging a lesson, opening the grade picker) ask guestBlocked() first.
import { Notify } from 'quasar'
import { GUEST_READ_ONLY, isGuestSession } from '@/boot/axios'

export { GUEST_READ_ONLY, isGuestSession }

/** True (and a short notice) when the demo visitor tries to change something. */
export function guestBlocked() {
  if (!isGuestSession()) return false
  Notify.create({
    message: GUEST_READ_ONLY,
    icon: 'visibility',
    color: 'grey-8',
    group: 'guest-read-only'
  })
  return true
}

// Icons and words of the buttons that add, edit, delete, save, send, export, mark or move.
const WRITE_ICONS = new Set([
  'add',
  'add_link',
  'add_circle',
  'add_circle_outline',
  'person_add',
  'group_add',
  'post_add',
  'playlist_add',
  'edit',
  'edit_note',
  'delete',
  'delete_outline',
  'delete_sweep',
  'delete_forever',
  'save',
  'check',
  'done_all',
  'task_alt',
  'smart_toy',
  'cloud_upload',
  'drive_folder_upload',
  'upload',
  'upload_file',
  'download',
  'file_download',
  'table_view',
  'picture_as_pdf',
  'description',
  'history_edu',
  'autorenew',
  'link_off',
  'event_available',
  'replay',
  'play_arrow',
  'history',
  'auto_fix_high',
  'visibility_off',
  'block',
  'arrow_upward',
  'arrow_downward',
  'attach_file',
  'send',
  'sms',
  'person_off',
  'ios_share',
  'qr_code_2'
])
const WRITE_WORDS = [
  "qo'shish",
  "qo'yish",
  'saqlash',
  "o'chirish",
  'tahrirlash',
  'yuklash',
  'yuklab olish',
  'eksport',
  'excel',
  'pdf',
  'import',
  'yuborish',
  'tasdiqlash',
  'rad etish',
  'belgilash',
  "ko'chirish",
  'yaratish',
  "to'ldirish",
  'band qilish',
  'nofaol qilish',
  'faollashtirish',
  'uzish',
  'ga xabar'
]

/** Does a button with this icon / label change data (and so is off for the demo visitor)? */
export function isWriteAction(icon, label) {
  if (icon && WRITE_ICONS.has(icon)) return true
  if (label === null || label === undefined || label === '') return false
  const text = String(label)
    .toLowerCase()
    .replace(/[‘’ʻʼ`]/g, "'")
  return WRITE_WORDS.some(w => text.includes(w))
}
