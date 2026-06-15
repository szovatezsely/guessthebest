<script setup>
import { ref } from 'vue'
import { fetchQuestions, submitAnswer } from './api.js'
import StartScreen from './components/StartScreen.vue'
import QuestionView from './components/QuestionView.vue'
import ResultScreen from './components/ResultScreen.vue'

const QUESTION_COUNT = 10

const screen = ref('start') // 'start' | 'playing' | 'finished'
const questions = ref([])
const currentIndex = ref(0)
const score = ref(0)
const answered = ref(false)
const selectedIndex = ref(null)
const correctIndex = ref(null)
const error = ref(null)
const loading = ref(false)

async function start() {
  error.value = null
  loading.value = true
  try {
    questions.value = await fetchQuestions(QUESTION_COUNT)
    if (questions.value.length === 0) {
      error.value = 'Nincsenek elérhető kérdések.'
      return
    }
    currentIndex.value = 0
    score.value = 0
    resetQuestionState()
    screen.value = 'playing'
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function choose(index) {
  if (answered.value) return
  const question = questions.value[currentIndex.value]
  try {
    const result = await submitAnswer(question.id, index)
    selectedIndex.value = index
    correctIndex.value = result.correctIndex
    answered.value = true
    if (result.correct) score.value++
  } catch (e) {
    error.value = e.message
  }
}

function next() {
  if (currentIndex.value + 1 >= questions.value.length) {
    screen.value = 'finished'
    return
  }
  currentIndex.value++
  resetQuestionState()
}

function resetQuestionState() {
  answered.value = false
  selectedIndex.value = null
  correctIndex.value = null
}

function restart() {
  screen.value = 'start'
}
</script>

<template>
  <div class="app-shell">
    <div class="brand">
      <span class="brand-dot"></span>
      <span class="brand-name">GuessTheBest</span>
    </div>

    <StartScreen v-if="screen === 'start'" @start="start" />

    <QuestionView
      v-else-if="screen === 'playing'"
      :question="questions[currentIndex]"
      :index="currentIndex"
      :total="questions.length"
      :score="score"
      :answered="answered"
      :selected-index="selectedIndex"
      :correct-index="correctIndex"
      @choose="choose"
      @next="next"
    />

    <ResultScreen
      v-else
      :score="score"
      :total="questions.length"
      @restart="restart"
    />

    <p v-if="error" class="error">{{ error }}</p>
  </div>
</template>
