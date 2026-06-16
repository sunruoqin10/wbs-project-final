<template>
  <div
    v-if="visible"
    class="absolute top-0 left-0 rounded bg-red-500 px-2 py-0.5 text-xs text-white shadow"
  >
    {{ $t('project.needsHandoverBadge') }}
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useUserStore } from '@/stores/user';

const props = defineProps<{ needsHandover: boolean }>();

const userStore = useUserStore();

/**
 * 仅 admin / dept-project-manager 可见 needsHandover 红角标
 * (2026-06-16 PM/Dept-PM 变更方案 — NeedsHandoverBadge.vue)
 */
const visible = computed(
  () =>
    props.needsHandover &&
    (userStore.currentUser?.role === 'admin' ||
      userStore.currentUser?.role === 'dept-project-manager')
);
</script>