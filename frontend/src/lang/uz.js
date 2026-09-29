import uzLatn from 'quasar/lang/uz-Latn'

// Quasar's built-in uz-Latn pack already gives us Monday-first weeks and
// Uzbek month/day names; we only override the couple of labels the product
// spec calls out by exact wording ("Sahifada:", "1-10 / 25").
export default {
  ...uzLatn,
  table: {
    ...uzLatn.table,
    recordsPerPage: 'Sahifada:',
    pagination: (start, end, total) => `${start}-${end} / ${total}`
  }
}
