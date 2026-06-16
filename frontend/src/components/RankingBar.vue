<script setup>
import { computed } from 'vue'
import { standings } from '../game.js'

const props = defineProps({
  players: { type: Array, required: true },
})

const ranked = computed(() => standings(props.players))
const medals = { 1: '🥇', 2: '🥈', 3: '🥉' }
</script>

<template>
  <div class="ranking">
    <span class="ranking-label">Állás:</span>
    <ol class="ranking-list">
      <li v-for="p in ranked" :key="p.index" class="rank-item">
        <span class="rank-pos">{{ medals[p.position] || p.position + '.' }}</span>
        <span class="rank-name">{{ p.name }}</span>
        <span class="rank-score">{{ p.score }}</span>
      </li>
    </ol>
  </div>
</template>
