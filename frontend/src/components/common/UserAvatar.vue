<template>
  <img
    :src="dataUri"
    :alt="name || 'avatar'"
    :title="name"
    :class="['flex-shrink-0 rounded-full object-cover', sizeClass]"
  />
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { createAvatar } from '@dicebear/core';
import { avataaars } from '@dicebear/collection';

interface Props {
  name?: string;
  size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl' | '2xl';
}

const props = withDefaults(defineProps<Props>(), {
  name: '',
  size: 'md'
});

const sizeClass = computed(() => {
  const map: Record<string, string> = {
    xs: 'h-5 w-5',
    sm: 'h-6 w-6',
    md: 'h-8 w-8',
    lg: 'h-9 w-9',
    xl: 'h-10 w-10',
    '2xl': 'h-20 w-20'
  };
  return map[props.size] || map.md;
});

const dataUri = computed(() => {
  const seed = props.name || 'default';
  const avatar = createAvatar(avataaars, { seed });
  return avatar.toDataUri();
});
</script>
