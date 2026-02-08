<template>
  <div class="min-h-screen bg-background">
    <Sidebar />

    <main
      class="mt-16 transition-all duration-300"
      :class="uiStore.sidebarCollapsed ? 'ml-20' : 'ml-64'"
    >
      <Header />
      <div class="p-6 min-h-[calc(100vh-4rem)]">
        <slot />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { watch, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import Sidebar from './Sidebar.vue';
import Header from './Header.vue';
import { useUiStore } from '@/stores/ui';

const route = useRoute();
const uiStore = useUiStore();

// Initialize sidebar state on mount
onMounted(() => {
  if (window.innerWidth < 1024) {
    uiStore.setSidebarCollapsed(true);
  } else {
    uiStore.setSidebarCollapsed(false);
  }
});

watch(() => route.path, () => {
  // Scroll to top on route change
  window.scrollTo(0, 0);
});
</script>
