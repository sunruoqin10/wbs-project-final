<template>
  <div class="w-full">
    <div v-if="showLabel" class="mb-1 flex items-center justify-between">
      <span class="text-sm font-medium text-secondary-700">
        <slot name="label">{{ label }}</slot>
      </span>
      <span class="text-sm font-medium text-secondary-900">{{ percentage }}%</span>
    </div>
    <div class="overflow-hidden rounded-full bg-secondary-200">
      <div
        class="h-2 rounded-full transition-all duration-300 ease-out"
        :class="colorClass"
        :style="{ width: `${percentage}%` }"
      ></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

interface Props {
  value: number;
  max?: number;
  label?: string;
  showLabel?: boolean;
  color?: 'primary' | 'success' | 'warning' | 'danger';
}

const props = withDefaults(defineProps<Props>(), {
  max: 100,
  showLabel: false,
  color: 'primary'
});

const percentage = computed(() => {
  return Math.min(Math.round((props.value / props.max) * 100), 100);
});

const colorClass = computed(() => {
  const colors = {
    primary: 'bg-primary-600',
    success: 'bg-accent-600',
    warning: 'bg-warning-500',
    danger: 'bg-danger-600'
  };
  return colors[props.color] || colors.primary;
});
</script>
