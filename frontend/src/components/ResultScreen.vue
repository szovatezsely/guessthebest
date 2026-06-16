<script setup>
import { computed } from 'vue'
import { standings } from '../game.js'

const props = defineProps({
  players: { type: Array, required: true },
})

defineEmits(['replay', 'newgame'])

const ranked = computed(() => standings(props.players))
const topScore = computed(() => Math.max(...props.players.map((p) => p.score), 0))
const winners = computed(() => ranked.value.filter((p) => p.position === 1 && p.score > 0))

const winnerText = computed(() => {
  if (winners.value.length === 0) return 'Senki sem szerzett pontot.'
  if (winners.value.length === 1) return `${winners.value[0].name} nyert! 🏆`
  return `Holtverseny: ${winners.value.map((w) => w.name).join(', ')} 🏆`
})
const medals = { 1: '🥇', 2: '🥈', 3: '🥉' }
</script>

<template>
  <div class="card result">
    <h2>Vége a játéknak!</h2>
    <p class="winner">{{ winnerText }}</p>

    <ul class="result-list">
      <li
        v-for="p in ranked"
        :key="p.index"
        class="result-row"
        :class="{ winner: p.position === 1 && p.score === topScore && topScore > 0 }"
      >
        <span class="result-pos">{{ medals[p.position] || p.position + '.' }}</span>
        <span class="result-name">{{ p.name }}</span>
        <span class="result-score">{{ p.score }} pont</span>
      </li>
    </ul>

    <div class="result-actions">
      <button class="btn-primary" @click="$emit('replay')">Újra (ugyanazok)</button>
      <button class="btn-ghost" @click="$emit('newgame')">Új játékosok</button>
    </div>
  </div>
</template>
