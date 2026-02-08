<template>
  <div :class="cardClasses" v-bind="$attrs">
    <div v-if="$slots.header" class="border-b border-secondary-200 px-6 py-4">
      <slot name="header" />
    </div>
    <div :class="bodyClasses">
      <slot />
    </div>
    <div v-if="$slots.footer" class="border-t border-secondary-200 px-6 py-4">
      <slot name="footer" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

interface Props {
  padding?: 'none' | 'sm' | 'md' | 'lg';
  hover?: boolean;
  clickable?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  padding: 'md',
  hover: false,
  clickable: false
});

const cardClasses = computed(() => {
  return [
    'bg-white rounded-xl shadow-md transition-shadow duration-200',
    props.hover && 'hover:shadow-lg',
    props.clickable && 'cursor-pointer hover:shadow-lg'
  ].filter(Boolean).join(' ');
});

const bodyClasses = computed(() => {
  const paddings = {
    none: '',
    sm: 'px-4 py-3',
    md: 'px-6 py-5',
    lg: 'px-8 py-6'
  };
  return paddings[props.padding];
});
</script>
