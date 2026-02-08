<template>
  <Transition name="modal">
    <div
      v-if="open"
      class="fixed inset-0 z-[60] overflow-y-auto"
      aria-labelledby="modal-title"
      role="dialog"
      aria-modal="true"
    >
      <!-- Background -->
      <div
        class="fixed inset-0 bg-secondary-900/50 backdrop-blur-sm transition-opacity"
        @click="handleClose"
      ></div>

      <!-- Modal Panel -->
      <div class="flex min-h-full items-center justify-center p-4">
        <Transition name="modal-panel">
          <div
            v-if="open"
            class="relative transform overflow-hidden rounded-xl bg-white shadow-xl transition-all sm:my-8 sm:w-full"
            :class="widthClass"
          >
            <!-- Header -->
            <div v-if="$slots.header || title" class="border-b border-secondary-200 px-6 py-4">
              <slot name="header">
                <h3 class="text-lg font-semibold text-secondary-900" id="modal-title">
                  {{ title }}
                </h3>
              </slot>
              <button
                v-if="showClose"
                @click="handleClose"
                class="absolute right-4 top-4 rounded-lg p-1 text-secondary-400 transition-colors hover:bg-secondary-100 hover:text-secondary-600"
              >
                <svg class="h-6 w-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            <!-- Body -->
            <div class="px-6 py-5">
              <slot />
            </div>

            <!-- Footer -->
            <div v-if="$slots.footer" class="border-t border-secondary-200 px-6 py-4">
              <slot name="footer" />
            </div>
          </div>
        </Transition>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue';

interface Props {
  open: boolean;
  title?: string;
  size?: 'sm' | 'md' | 'lg' | 'xl';
  showClose?: boolean;
  closeOnBackdrop?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  size: 'md',
  showClose: true,
  closeOnBackdrop: true
});

const emit = defineEmits<{
  close: [];
}>();

const widthClass = computed(() => {
  const sizes = {
    sm: 'sm:max-w-sm',
    md: 'sm:max-w-md',
    lg: 'sm:max-w-lg',
    xl: 'sm:max-w-xl'
  };
  return sizes[props.size];
});

const handleClose = () => {
  if (props.closeOnBackdrop) {
    emit('close');
  }
};

// Prevent body scroll when modal is open
watch(() => props.open, (isOpen) => {
  if (isOpen) {
    document.body.style.overflow = 'hidden';
  } else {
    document.body.style.overflow = '';
  }
});
</script>

<style scoped>
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.3s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-panel-enter-active,
.modal-panel-leave-active {
  transition: all 0.3s ease;
}

.modal-panel-enter-from,
.modal-panel-leave-to {
  opacity: 0;
  transform: scale(0.95) translateY(-10px);
}
</style>
