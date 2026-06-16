<script setup>
import { computed } from 'vue'
import { standings } from '../game.js'

const props = defineProps({
  players: { type: Array, required: true },
  currentIndex: { type: Number, required: true },
  remaining: { type: Number, required: true },
})

const ranked = computed(() => standings(props.players))
const currentName = computed(() => props.players[props.currentIndex]?.name ?? '')
</script>

<template>
  <aside class="leaderboard">
    <div class="turn-box">
      <span class="turn-label">Soron következik</span>
      <span class="turn-name">{{ currentName }}</span>
    </div>

    <h3 class="lb-title">Eredménytábla</h3>
    <ul class="lb-list">
      <li
        v-for="p in ranked"
        :key="p.index"
        class="lb-row"
        :class="{ active: p.index === currentIndex }"
      >
        <span class="lb-pos">{{ p.position }}.</span>
        <span class="lb-name">{{ p.name }}</span>
        <span class="lb-score">{{ p.score }} pont</span>
      </li>
    </ul>

    <div class="lb-remaining">Hátralévő mezők: <strong>{{ remaining }}</strong></div>
  </aside>
</template>
