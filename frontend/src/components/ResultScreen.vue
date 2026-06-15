<script setup>
import { computed } from 'vue'

const props = defineProps({
  score: { type: Number, required: true },
  total: { type: Number, required: true },
})

defineEmits(['restart'])

const message = computed(() => {
  const ratio = props.score / props.total
  if (ratio === 1) return 'Tökéletes! Ön a legjobb!'
  if (ratio >= 0.7) return 'Nagyszerű teljesítmény!'
  if (ratio >= 0.4) return 'Szép munka, de van még hova fejlődni.'
  return 'Ne keseredjen el, próbálja újra!'
})
</script>

<template>
  <div class="card result">
    <h2>Vége a játéknak</h2>
    <div class="score-big">{{ score }} / {{ total }}</div>
    <p class="score-sub">{{ message }}</p>
    <button class="btn-ghost" @click="$emit('restart')">Új játék</button>
  </div>
</template>
