<template>
  <div id="app" class="min-h-screen bg-background">
    <router-view />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
import { useProjectStore } from './stores/project';
import { useUserStore } from './stores/user';

const projectStore = useProjectStore();
const userStore = useUserStore();

onMounted(() => {
  // Initialize stores with data
  console.log('App initialized');
});
</script>
