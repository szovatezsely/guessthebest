const BASE = '/api'

/** Fetch `count` random questions (without the correct answer). */
export async function fetchQuestions(count = 10) {
  const res = await fetch(`${BASE}/questions?count=${count}`)
  if (!res.ok) throw new Error('Nem sikerült betölteni a kérdéseket.')
  return res.json()
}

/** Submit a chosen answer; the server returns whether it was correct. */
export async function submitAnswer(questionId, selectedIndex) {
  const res = await fetch(`${BASE}/questions/${questionId}/answer`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ selectedIndex }),
  })
  if (!res.ok) throw new Error('Nem sikerült ellenőrizni a választ.')
  return res.json()
}
