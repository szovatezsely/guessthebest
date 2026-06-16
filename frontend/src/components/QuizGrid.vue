<script setup>
import { pointsFor } from '../game.js'

const props = defineProps({
  board: { type: Array, required: true },
  players: { type: Array, required: true },
  disabled: { type: Boolean, default: false },
})

const emit = defineEmits(['pick'])

const diffClass = (d) =>
  ({ könnyű: 'easy', közepes: 'medium', nehéz: 'hard' })[d] || 'medium'

function pick(i) {
  if (props.disabled || props.board[i].status === 'done') return
  emit('pick', i)
}
</script>

<template>
  <div class="quiz-grid">
    <button
      v-for="(cell, i) in board"
      :key="i"
      class="cell"
      :class="[diffClass(cell.difficulty), { done: cell.status === 'done', locked: disabled }]"
      :disabled="cell.status === 'done' || disabled"
      @click="pick(i)"
    >
      <template v-if="cell.status === 'done'">
        <span class="cell-check">✓</span>
        <span class="cell-done-by">{{ players[cell.doneBy]?.name }}</span>
      </template>
      <template v-else>
        <span class="cell-points">{{ pointsFor(cell.difficulty) }} pont</span>
        <span class="cell-cat">{{ cell.category }}</span>
        <span class="cell-diff">{{ cell.difficulty }}</span>
      </template>
    </button>
  </div>
</template>
