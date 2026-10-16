// Finds sizes written straight into the code instead of the design tokens:
// max-width / border-radius / (min-|max-)height in px, and pixel sizes on avatars.
// Media query breakpoints are not sizes of anything (and CSS variables cannot be
// used there), so @media lines are skipped.
import { readdirSync, readFileSync, statSync } from 'node:fs'
import { join, relative } from 'node:path'

const RULES = [
  { name: 'max-width', re: /max-width:\s*\d+px/g },
  {
    name: 'border-radius',
    re: /border(?:-[a-z]+)*-radius:[^;]*?\d+(?:\.\d+)?(?:px|%)/g
  },
  { name: 'height', re: /(?<![-\w])(?:min-|max-)?height:\s*\d+px/g }
]
const AVATAR = /<q-avatar\b[^>]*?\bsize="(\d+px)"/g

// the one place raw values are allowed: the token definitions
const ALLOWED = [
  /src[\\/]css[\\/]tokens\.scss$/,
  /src[\\/]css[\\/]quasar\.variables\.scss$/
]

function walk(dir, files = []) {
  for (const name of readdirSync(dir)) {
    const p = join(dir, name)
    if (statSync(p).isDirectory()) walk(p, files)
    else if (/\.(vue|scss|css)$/.test(name)) files.push(p)
  }
  return files
}

export function findHardcoded(root) {
  const hits = []
  for (const file of walk(join(root, 'src'))) {
    const rel = relative(root, file).replace(/\\/g, '/')
    if (ALLOWED.some(a => a.test(rel))) continue
    const text = readFileSync(file, 'utf8')
    for (const m of text.matchAll(AVATAR)) {
      hits.push({
        file: rel,
        line: text.slice(0, m.index).split('\n').length,
        rule: 'avatar size',
        text: m[1]
      })
    }
    text.split('\n').forEach((line, i) => {
      if (line.trim().startsWith('@media')) return
      for (const r of RULES) {
        for (const m of line.matchAll(r.re))
          hits.push({ file: rel, line: i + 1, rule: r.name, text: m[0] })
      }
    })
  }
  return hits
}
