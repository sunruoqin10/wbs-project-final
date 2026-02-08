<template>
  <Teleport to="body">
    <!-- Overlay -->
    <Transition
      enter-active-class="transition-opacity duration-300"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition-opacity duration-300"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div
        v-if="open"
        class="fixed inset-0 z-40 bg-black/50"
        @click="handleClose"
      ></div>
    </Transition>

    <!-- Drawer -->
    <Transition
      enter-active-class="transition-transform duration-300"
      enter-from-class="translate-x-full"
      enter-to-class="translate-x-0"
      leave-active-class="transition-transform duration-300"
      leave-from-class="translate-x-0"
      leave-to-class="translate-x-full"
    >
      <div
        v-if="open"
        class="fixed right-0 top-0 z-50 h-screen w-full max-w-md bg-white shadow-xl"
        @click.stop
      >
        <!-- Header -->
        <div class="flex items-center justify-between border-b border-secondary-200 p-6">
          <h2 class="text-xl font-semibold text-secondary-900">{{ title }}</h2>
          <button
            @click="handleClose"
            class="rounded-lg p-2 hover:bg-secondary-100 transition-colors"
            aria-label="Close drawer"
          >
            <svg class="h-5 w-5 text-secondary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <!-- Content -->
        <div class="h-[calc(100vh-73px)] overflow-y-auto p-6">
          <slot></slot>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { watch, onMounted, onUnmounted } from 'vue';

interface Props {
  open: boolean;
  title?: string;
}

interface Emits {
  (e: 'close'): void;
}

const props = withDefaults(defineProps<Props>(), {
  title: ''
});

const emit = defineEmits<Emits>();

const handleClose = () => {
  emit('close');
};

// Handle ESC key press
const handleEscape = (event: KeyboardEvent) => {
  if (event.key === 'Escape' && props.open) {
    handleClose();
  }
};

// Prevent body scroll when drawer is open
watch(() => props.open, (isOpen) => {
  if (isOpen) {
    document.body.style.overflow = 'hidden';
  } else {
    document.body.style.overflow = '';
  }
}, { immediate: true });

onMounted(() => {
  document.addEventListener('keydown', handleEscape);
});

onUnmounted(() => {
  document.removeEventListener('keydown', handleEscape);
  document.body.style.overflow = '';
});
</script>
