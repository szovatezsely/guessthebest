<script setup>
import { computed } from 'vue'
import { pointsFor } from '../game.js'

const props = defineProps({
  question: { type: Object, required: true },
  playerName: { type: String, required: true },
  answered: { type: Boolean, required: true },
  result: { type: Object, default: null }, // { correct, correctIndex, selectedIndex }
})

defineEmits(['choose', 'next'])

const letters = ['A', 'B', 'C', 'D']
const points = computed(() => pointsFor(props.question.difficulty))
const isCorrect = computed(() => props.result?.correct)

function answerClass(i) {
  if (!props.answered || !props.result) return ''
  if (i === props.result.correctIndex) return 'correct'
  if (i === props.result.selectedIndex) return 'wrong'
  return 'dim'
}
</script>

<template>
  <div class="modal-overlay">
    <div class="modal question">
      <div class="q-modal-top">
        <div class="badges">
          <span class="badge">{{ question.category }}</span>
          <span class="badge diff">{{ question.difficulty }}</span>
          <span class="badge points">{{ points }} pont</span>
        </div>
        <span class="q-player">{{ playerName }}</span>
      </div>

      <h2 class="q-text">{{ question.text }}</h2>

      <div class="answers">
        <button
          v-for="(answer, i) in question.answers"
          :key="i"
          class="answer"
          :class="answerClass(i)"
          :disabled="answered"
          @click="$emit('choose', i)"
        >
          <span class="letter">{{ letters[i] }}</span>
          <span>{{ answer }}</span>
        </button>
      </div>

      <div v-if="answered" class="q-feedback">
        <span class="msg" :class="isCorrect ? 'ok' : 'no'">
          {{ isCorrect ? `Helyes! +${points} pont` : 'Hibás válasz. A mező új témát kap.' }}
        </span>
        <button class="btn-primary" @click="$emit('next')">Következő játékos</button>
      </div>
    </div>
  </div>
</template>
