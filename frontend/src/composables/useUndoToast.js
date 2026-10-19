// ConfirmUndoToast: after a change made in one move (a lesson dragged, an event moved) a
// toast says what happened and offers "Bekor qilish" for 5 seconds; undo() puts it back.
import { Notify } from 'quasar'

export const UNDO_MS = 5000

export function confirmUndoToast(message, undo) {
  let done = false
  Notify.create({
    message,
    type: 'positive',
    icon: 'check_circle',
    timeout: UNDO_MS,
    progress: true,
    classes: 'undo-toast',
    attrs: { 'data-undo-toast': '' },
    actions: [
      {
        label: 'Bekor qilish',
        color: 'white',
        noCaps: true,
        attrs: { 'data-undo': '' },
        handler: async () => {
          if (done) return
          done = true
          try {
            await undo()
            Notify.create({ message: 'Bekor qilindi', type: 'info', timeout: 1800 })
          } catch (e) {
            Notify.create({
              message: e.friendlyMessage || "Bekor qilib bo'lmadi",
              type: 'negative'
            })
          }
        }
      }
    ]
  })
}
