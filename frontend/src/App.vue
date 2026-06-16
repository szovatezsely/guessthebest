<script setup>
import { ref, computed } from 'vue'
import { fetchMeta, drawQuestion, submitAnswer } from './api.js'
import { buildBoard, pointsFor, sample, BOARD_SIZE } from './game.js'
import PlayerSetup from './components/PlayerSetup.vue'
import RankingBar from './components/RankingBar.vue'
import Leaderboard from './components/Leaderboard.vue'
import QuizGrid from './components/QuizGrid.vue'
import ConfirmModal from './components/ConfirmModal.vue'
import QuestionModal from './components/QuestionModal.vue'
import ResultScreen from './components/ResultScreen.vue'

const phase = ref('setup') // 'setup' | 'playing' | 'finished'
const error = ref(null)

const players = ref([])
const currentIndex = ref(0)
const board = ref([])
const combos = ref([])
const usedIds = ref([])

const modal = ref(null) // null | 'confirm' | 'question'
const selectedCell = ref(null)
const activeQuestion = ref(null)
const answered = ref(false)
const result = ref(null)

const doneCount = computed(() => board.value.filter((c) => c.status === 'done').length)
const remaining = computed(() => BOARD_SIZE - doneCount.value)
const currentPlayer = computed(() => players.value[currentIndex.value] ?? null)
const selected = computed(() =>
  selectedCell.value === null ? null : board.value[selectedCell.value],
)
const modalOpen = computed(() => modal.value !== null)

async function startGame(names) {
  error.value = null
  try {
    const meta = await fetchMeta()
    combos.value = meta.combos
    players.value = names.map((name) => ({ name, score: 0 }))
    board.value = buildBoard(combos.value)
    usedIds.value = []
    currentIndex.value = 0
    resetModal()
    phase.value = 'playing'
  } catch (e) {
    error.value = e.message
  }
}

function pickCell(i) {
  if (modalOpen.value) return
  selectedCell.value = i
  modal.value = 'confirm'
}

function cancelConfirm() {
  resetModal()
}

async function acceptConfirm() {
  const cell = board.value[selectedCell.value]
  error.value = null
  try {
    const q = await drawQuestion(cell.category, cell.difficulty, usedIds.value)
    // Keep the card truthful to whatever was actually drawn (fallback can swap category).
    cell.category = q.category
    cell.difficulty = q.difficulty
    activeQuestion.value = q
    answered.value = false
    result.value = null
    modal.value = 'question'
  } catch (e) {
    error.value = e.message
    resetModal()
  }
}

async function chooseAnswer(index) {
  if (answered.value || !activeQuestion.value) return
  error.value = null
  try {
    const r = await submitAnswer(activeQuestion.value.id, index)
    result.value = { ...r, selectedIndex: index }
    answered.value = true
    usedIds.value.push(activeQuestion.value.id)

    if (r.correct) {
      const cell = board.value[selectedCell.value]
      players.value[currentIndex.value].score += pointsFor(cell.difficulty)
      cell.status = 'done'
      cell.doneBy = currentIndex.value
    }
  } catch (e) {
    error.value = e.message
  }
}

function nextTurn() {
  // Wrong answer: re-randomize the cell's topic & difficulty for whoever picks it next.
  if (result.value && !result.value.correct && selectedCell.value !== null) {
    const fresh = sample(combos.value)
    board.value[selectedCell.value] = {
      category: fresh.category,
      difficulty: fresh.difficulty,
      status: 'open',
      doneBy: null,
    }
  }

  resetModal()

  if (doneCount.value >= BOARD_SIZE) {
    phase.value = 'finished'
    return
  }
  currentIndex.value = (currentIndex.value + 1) % players.value.length
}

function resetModal() {
  modal.value = null
  selectedCell.value = null
  activeQuestion.value = null
  answered.value = false
  result.value = null
}

function replay() {
  players.value = players.value.map((p) => ({ name: p.name, score: 0 }))
  board.value = buildBoard(combos.value)
  usedIds.value = []
  currentIndex.value = 0
  resetModal()
  phase.value = 'playing'
}

function newGame() {
  phase.value = 'setup'
}
</script>

<template>
  <div class="app-shell" :class="{ wide: phase === 'playing' }">
    <div class="brand">
      <span class="brand-dot"></span>
      <span class="brand-name">GuessTheBest</span>
    </div>

    <PlayerSetup v-if="phase === 'setup'" @start="startGame" />

    <template v-else-if="phase === 'playing'">
      <RankingBar :players="players" />
      <div class="game-layout">
        <QuizGrid
          :board="board"
          :players="players"
          :disabled="modalOpen"
          @pick="pickCell"
        />
        <Leaderboard
          :players="players"
          :current-index="currentIndex"
          :remaining="remaining"
        />
      </div>
    </template>

    <ResultScreen
      v-else
      :players="players"
      @replay="replay"
      @newgame="newGame"
    />

    <ConfirmModal
      v-if="modal === 'confirm' && selected"
      :cell="selected"
      :player-name="currentPlayer?.name"
      @accept="acceptConfirm"
      @cancel="cancelConfirm"
    />
    <QuestionModal
      v-if="modal === 'question' && activeQuestion"
      :question="activeQuestion"
      :player-name="currentPlayer?.name"
      :answered="answered"
      :result="result"
      @choose="chooseAnswer"
      @next="nextTurn"
    />

    <p v-if="error" class="error">{{ error }}</p>
  </div>
</template>
