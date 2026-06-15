<script setup>
defineEmits(['start'])
</script>

<template>
  <div class="card start">
    <h1>Legyen Ön is a <span class="accent">legjobb!</span></h1>
    <p>
      Tíz kérdés, négy lehetséges válasz, mindössze egy helyes. Lássuk, mennyit
      tud! Készen áll?
    </p>
    <button class="btn-primary" @click="$emit('start')">Játék indítása</button>
  </div>
</template>
