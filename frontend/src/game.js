// Shared game rules / helpers.

export const BOARD_SIZE = 25 // 5 x 5

/** Points awarded per difficulty. */
export const POINTS = { könnyű: 1, közepes: 2, nehéz: 3 }
export const pointsFor = (difficulty) => POINTS[difficulty] ?? 1

/** A short English-free label is not needed; difficulties are already Hungarian. */
export function sample(arr) {
  return arr[Math.floor(Math.random() * arr.length)]
}

/** Build a 25-cell board from the available combos (duplicates across cells are fine). */
export function buildBoard(combos) {
  return Array.from({ length: BOARD_SIZE }, () => {
    const c = sample(combos)
    return { category: c.category, difficulty: c.difficulty, status: 'open', doneBy: null }
  })
}

/**
 * Players ranked by score (desc). Ties share a position. Each entry keeps its original
 * `index` so callers can highlight the player whose turn it is.
 */
export function standings(players) {
  const ranked = players
    .map((p, index) => ({ ...p, index }))
    .sort((a, b) => b.score - a.score)
  let position = 0
  let prevScore = null
  return ranked.map((p, i) => {
    if (prevScore === null || p.score !== prevScore) {
      position = i + 1
      prevScore = p.score
    }
    return { ...p, position }
  })
}
