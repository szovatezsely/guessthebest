<script setup>
import { ref, computed } from 'vue'

const emit = defineEmits(['start'])

const MAX = 10
const names = ref(['', ''])

const canAdd = computed(() => names.value.length < MAX)
const canRemove = computed(() => names.value.length > 1)

function addPlayer() {
  if (canAdd.value) names.value.push('')
}
function removePlayer(i) {
  if (canRemove.value) names.value.splice(i, 1)
}

function start() {
  const cleaned = names.value.map((n, i) => n.trim() || `${i + 1}. játékos`)
  emit('start', cleaned)
}
</script>

<template>
  <div class="card setup">
    <h1>Kvíz<span class="accent">rács</span></h1>
    <p>
      5×5 mező, mindegyiken egy téma és nehézség. Adj hozzá 1–10 játékost, majd
      kattints egy mezőre! A helyes válasz pontot ér (könnyű 1, közepes 2, nehéz 3),
      és kipipálja a mezőt. A játék 25 helyes válasz után ér véget.
    </p>

    <div class="players-setup">
      <div v-for="(name, i) in names" :key="i" class="player-row">
        <span class="player-num">{{ i + 1 }}.</span>
        <input
          v-model="names[i]"
          type="text"
          class="player-input"
          :placeholder="`${i + 1}. játékos`"
          maxlength="24"
          @keyup.enter="start"
        />
        <button
          class="icon-btn"
          :disabled="!canRemove"
          title="Eltávolítás"
          @click="removePlayer(i)"
        >
          ✕
        </button>
      </div>
    </div>

    <button class="btn-ghost add-btn" :disabled="!canAdd" @click="addPlayer">
      + Játékos hozzáadása
    </button>

    <button class="btn-primary start-btn" @click="start">Játék indítása</button>
  </div>
</template>
