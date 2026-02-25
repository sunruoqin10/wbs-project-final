<template>
  <div class="w-full">
    <div class="border-b border-secondary-200">
      <nav class="flex space-x-8 -mb-px overflow-x-auto" role="tablist">
        <button
          v-for="(tab, index) in tabs"
          :key="index"
          :ref="(el: any) => tabRefs[index] = el"
          role="tab"
          :aria-selected="activeTab === index"
          :class="[
            tabClasses(index),
            'whitespace-nowrap'
          ]"
          @click="handleTabClick(index)"
        >
          <svg v-if="tab.icon" class="h-5 w-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="tab.icon" />
          </svg>
          {{ tab.label }}
          <span
            v-if="tab.badge !== undefined"
            :class="[
              'ml-2 inline-flex items-center justify-center px-2 py-0.5 text-xs rounded-full',
              activeTab === index
                ? 'bg-primary-200 text-primary-800'
                : 'bg-secondary-100 text-secondary-600'
            ]"
          >
            {{ tab.badge }}
          </span>
        </button>
      </nav>
    </div>
    <div class="mt-4">
      <slot :activeTab="activeTab" :activeTabData="tabs[activeTab]" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';

export interface Tab {
  label: string;
  icon?: string;
  badge?: number;
  value?: any;
}

interface Props {
  tabs: Tab[];
  modelValue?: number;
  variant?: 'default' | 'pills';
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: 0,
  variant: 'default'
});

const emit = defineEmits<{
  'update:modelValue': [value: number];
  'change': [tab: Tab, index: number];
}>();

const activeTab = ref(props.modelValue);
const tabRefs = ref<(HTMLElement | null)[]>([]);

const tabClasses = (index: number) => {
  const base = 'px-4 py-2 text-sm font-medium transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500';
  
  if (props.variant === 'pills') {
    return [
      base,
      'rounded-lg',
      activeTab.value === index
        ? 'bg-primary-600 text-white'
        : 'bg-secondary-100 text-secondary-700 hover:bg-secondary-200'
    ].join(' ');
  }
  
  return [
    base,
    activeTab.value === index
      ? 'border-b-2 border-primary-600 text-primary-600'
      : 'text-secondary-500 hover:text-secondary-700 hover:border-secondary-300 border-b-2 border-transparent'
  ].join(' ');
};

const handleTabClick = (index: number) => {
  activeTab.value = index;
  emit('update:modelValue', index);
  emit('change', props.tabs[index], index);
};

defineExpose({
  activeTab,
  setTab: (index: number) => {
    activeTab.value = index;
  }
});
</script>