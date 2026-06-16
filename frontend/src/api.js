const BASE = '/api'

async function getJson(url) {
  const res = await fetch(url)
  if (!res.ok) throw new Error('Hálózati hiba (' + res.status + ').')
  return res.json()
}

/** Topic+difficulty combos and lists the board is built from. */
export async function fetchMeta() {
  return getJson(`${BASE}/meta`)
}

/** Draw one question (without the correct answer) for a cell, skipping already-used ids. */
export async function drawQuestion(category, difficulty, excludeIds = []) {
  const params = new URLSearchParams({ category, difficulty })
  if (excludeIds.length) params.set('exclude', excludeIds.join(','))
  return getJson(`${BASE}/question?${params.toString()}`)
}

/** Submit an answer; the server returns whether it was correct. */
export async function submitAnswer(questionId, selectedIndex) {
  const res = await fetch(`${BASE}/questions/${questionId}/answer`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ selectedIndex }),
  })
  if (!res.ok) throw new Error('Nem sikerült ellenőrizni a választ.')
  return res.json()
}
