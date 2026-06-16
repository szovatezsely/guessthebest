<script setup>
import { pointsFor } from '../game.js'

defineProps({
  cell: { type: Object, required: true },
  playerName: { type: String, required: true },
})

defineEmits(['accept', 'cancel'])
</script>

<template>
  <div class="modal-overlay" @click.self="$emit('cancel')">
    <div class="modal confirm">
      <span class="confirm-turn">{{ playerName }} választása</span>
      <div class="confirm-badges">
        <span class="badge">{{ cell.category }}</span>
        <span class="badge diff">{{ cell.difficulty }}</span>
        <span class="badge points">{{ pointsFor(cell.difficulty) }} pont</span>
      </div>
      <p class="confirm-q">Felfeded ezt a kérdést?</p>
      <div class="confirm-actions">
        <button class="btn-primary" @click="$emit('accept')">Mehet!</button>
        <button class="btn-ghost" @click="$emit('cancel')">Mégse</button>
      </div>
    </div>
  </div>
</template>
