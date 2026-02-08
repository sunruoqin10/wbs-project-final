<template>
  <div class="w-full">
    <label v-if="label" :for="id" class="mb-1 block text-sm font-medium text-secondary-700">
      {{ label }}
      <span v-if="required" class="text-danger-500">*</span>
    </label>
    <select
      :id="id"
      :value="modelValue"
      :disabled="disabled"
      :required="required"
      :class="selectClasses"
      @change="handleChange"
    >
      <option v-if="placeholder" value="" disabled selected>{{ placeholder }}</option>
      <slot />
    </select>
    <p v-if="error" class="mt-1 text-sm text-danger-600">{{ error }}</p>
    <p v-else-if="hint" class="mt-1 text-sm text-secondary-500">{{ hint }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

interface Props {
  modelValue?: string | number;
  label?: string;
  placeholder?: string;
  error?: string;
  hint?: string;
  disabled?: boolean;
  required?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false,
  required: false
});

const emit = defineEmits<{
  'update:modelValue': [value: string | number];
}>();

const id = computed(() => `select-${Math.random().toString(36).substr(2, 9)}`);

const selectClasses = computed(() => {
  return [
    'w-full rounded-lg border px-4 py-2 pr-8 transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-offset-0 appearance-none bg-no-repeat bg-right',
    props.error
      ? 'border-danger-300 focus:border-danger-500 focus:ring-danger-500/20'
      : 'border-secondary-200 focus:border-primary-500 focus:ring-primary-500/20',
    props.disabled ? 'bg-secondary-100 text-secondary-500 cursor-not-allowed' : 'bg-white text-secondary-900',
    "bg-[url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIGZpbGw9Im5vbmUiIHZpZXdCb3g9IjAgMCAyMCAyMCI+PHBhdGggc3Ryb2tlPSIjNmQ3YjkwIiBzdHJva2UtbGluZWNhcD0icm91bmQiIHN0cm9rZS1saW5lam9pbj0icm91bmQiIHN0cm9rZS13aWR0aD0iMS41IiBkPSJNNiA4bDQgNCA0LTQiLz48L3N2Zz4=')]"
  ].join(' ');
});

const handleChange = (event: Event) => {
  const target = event.target as HTMLSelectElement;
  emit('update:modelValue', target.value);
};
</script>
