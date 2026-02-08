<template>
  <div class="w-full">
    <label v-if="label" :for="id" class="mb-1 block text-sm font-medium text-secondary-700">
      {{ label }}
      <span v-if="required" class="text-danger-500">*</span>
    </label>
    <div class="relative">
      <div v-if="$slots.prefix" class="absolute left-3 top-1/2 -translate-y-1/2 text-secondary-400">
        <slot name="prefix" />
      </div>
      <input
        :id="id"
        :type="type"
        :value="modelValue"
        :placeholder="placeholder"
        :disabled="disabled"
        :required="required"
        :class="inputClasses"
        @input="handleInput"
        @blur="handleBlur"
        @focus="handleFocus"
      />
    </div>
    <p v-if="error" class="mt-1 text-sm text-danger-600">{{ error }}</p>
    <p v-else-if="hint" class="mt-1 text-sm text-secondary-500">{{ hint }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, useSlots } from 'vue';

interface Props {
  modelValue?: string | number;
  type?: string;
  label?: string;
  placeholder?: string;
  error?: string;
  hint?: string;
  disabled?: boolean;
  required?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  type: 'text',
  disabled: false,
  required: false
});

const emit = defineEmits<{
  'update:modelValue': [value: string | number];
  blur: [event: FocusEvent];
  focus: [event: FocusEvent];
}>();

const slots = useSlots();
const focused = ref(false);
const id = computed(() => `input-${Math.random().toString(36).substr(2, 9)}`);

const inputClasses = computed(() => {
  const hasPrefix = !!slots.prefix;
  return [
    'w-full rounded-lg border transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-offset-0',
    hasPrefix ? 'pl-10 pr-4' : 'px-4',
    'py-2',
    props.error
      ? 'border-danger-300 focus:border-danger-500 focus:ring-danger-500/20'
      : 'border-secondary-200 focus:border-primary-500 focus:ring-primary-500/20',
    props.disabled ? 'bg-secondary-100 text-secondary-500 cursor-not-allowed' : 'bg-white text-secondary-900'
  ].join(' ');
});

const handleInput = (event: Event) => {
  const target = event.target as HTMLInputElement;
  emit('update:modelValue', target.value);
};

const handleBlur = (event: FocusEvent) => {
  focused.value = false;
  emit('blur', event);
};

const handleFocus = (event: FocusEvent) => {
  focused.value = true;
  emit('focus', event);
};
</script>
