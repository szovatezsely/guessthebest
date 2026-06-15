// Builds backend/src/main/resources/questions.json from the hand-authored
// tools/questions-source.json.
//
// In the source file every question lists the CORRECT answer first
// (correctIndex: 0). This script validates each entry and deterministically
// shuffles the four answers so the correct one lands in a varied position,
// then recomputes correctIndex. Deterministic (seeded) => reproducible output.
//
// Run from the repo root:  node tools/build-questions.mjs

import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const SRC = path.join(ROOT, 'tools/questions-source.json')
const OUT = path.join(ROOT, 'backend/src/main/resources/questions.json')

function mulberry32(seed) {
  return function () {
    seed |= 0
    seed = (seed + 0x6d2b79f5) | 0
    let t = Math.imul(seed ^ (seed >>> 15), 1 | seed)
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
}

function shuffle(arr, rand) {
  const a = arr.slice()
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(rand() * (i + 1))
    ;[a[i], a[j]] = [a[j], a[i]]
  }
  return a
}

const source = JSON.parse(fs.readFileSync(SRC, 'utf8'))
const rand = mulberry32(0x6a17)
const out = []
const seenText = new Set()
const problems = []

source.forEach((q, i) => {
  const where = `#${i + 1} "${(q.text || '').slice(0, 50)}"`
  if (!q.text || !q.category || !q.difficulty) {
    problems.push(`${where}: missing field`)
    return
  }
  if (!Array.isArray(q.answers) || q.answers.length !== 4) {
    problems.push(`${where}: needs exactly 4 answers`)
    return
  }
  if (new Set(q.answers.map((a) => String(a).toLowerCase().trim())).size !== 4) {
    problems.push(`${where}: duplicate answers`)
    return
  }
  if (q.correctIndex < 0 || q.correctIndex > 3) {
    problems.push(`${where}: bad correctIndex`)
    return
  }
  if (seenText.has(q.text)) {
    problems.push(`${where}: duplicate question text`)
    return
  }
  seenText.add(q.text)

  const correct = q.answers[q.correctIndex]
  const shuffled = shuffle(q.answers, rand)
  out.push({
    category: q.category,
    difficulty: q.difficulty,
    text: q.text,
    answers: shuffled,
    correctIndex: shuffled.indexOf(correct),
  })
})

if (problems.length) {
  console.error(`Found ${problems.length} problem(s):`)
  problems.forEach((p) => console.error('  - ' + p))
  process.exit(1)
}

fs.writeFileSync(OUT, JSON.stringify(out, null, 2) + '\n')

const byCat = {}
const byDiff = {}
for (const q of out) {
  byCat[q.category] = (byCat[q.category] || 0) + 1
  byDiff[q.difficulty] = (byDiff[q.difficulty] || 0) + 1
}
console.log(`Wrote ${out.length} questions to ${path.relative(ROOT, OUT)}.`)
console.log('By category:', byCat)
console.log('By difficulty:', byDiff)
