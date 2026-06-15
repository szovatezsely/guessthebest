<script setup>
import { computed } from 'vue'

const props = defineProps({
  question: { type: Object, required: true },
  index: { type: Number, required: true },
  total: { type: Number, required: true },
  score: { type: Number, required: true },
  answered: { type: Boolean, required: true },
  selectedIndex: { type: Number, default: null },
  correctIndex: { type: Number, default: null },
})

defineEmits(['choose', 'next'])

const letters = ['A', 'B', 'C', 'D']
const progress = computed(() => Math.round((props.index / props.total) * 100))
const isCorrect = computed(
  () => props.answered && props.selectedIndex === props.correctIndex,
)

function answerClass(i) {
  if (!props.answered) return ''
  if (i === props.correctIndex) return 'correct'
  if (i === props.selectedIndex) return 'wrong'
  return 'dim'
}
</script>

<template>
  <div class="card">
    <div class="q-top">
      <span class="q-progress">Kérdés {{ index + 1 }} / {{ total }}</span>
      <span class="q-score">Pontszám: {{ score }}</span>
    </div>

    <div class="progress-bar">
      <div class="progress-bar-fill" :style="{ width: progress + '%' }"></div>
    </div>

    <div class="badges">
      <span class="badge">{{ question.category }}</span>
      <span class="badge diff">{{ question.difficulty }}</span>
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
        {{ isCorrect ? 'Helyes válasz!' : 'Sajnos nem talált.' }}
      </span>
      <button class="btn-primary" @click="$emit('next')">
        {{ index + 1 === total ? 'Eredmény' : 'Következő' }}
      </button>
    </div>
  </div>
</template>
