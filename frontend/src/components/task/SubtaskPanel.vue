<template>
  <div class="subtask-panel">
    <div class="subtask-list">
      <SubtaskItem
        v-for="subtask in subtasks"
        :key="subtask.id"
        :task="subtask"
        @click="handleSubtaskClick"
      />
    </div>

    <!-- 快速添加子任务按钮 -->
    <button
      @click.stop="$emit('add-subtask')"
      class="add-subtask-btn"
    >
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
      </svg>
      添加子任务
    </button>
  </div>
</template>

<script setup lang="ts">
import SubtaskItem from './SubtaskItem.vue';
import type { Task } from '@/types';

interface Props {
  subtasks: Task[];
}

interface Emits {
  (e: 'click-subtask', task: Task): void;
  (e: 'add-subtask'): void;
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();

const handleSubtaskClick = (task: Task) => {
  emit('click-subtask', task);
};
</script>

<style scoped>
.subtask-panel {
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-top: none;
  border-bottom-left-radius: 8px;
  border-bottom-right-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 12px;
}

.subtask-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.add-subtask-btn {
  width: 100%;
  padding: 8px;
  margin-top: 8px;
  border: 2px dashed #d1d5db;
  border-radius: 6px;
  background: transparent;
  color: #6b7280;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.2s;
}

.add-subtask-btn:hover {
  border-color: #3b82f6;
  color: #3b82f6;
  background: #eff6ff;
}

.add-subtask-btn svg {
  width: 16px;
  height: 16px;
}
</style>
