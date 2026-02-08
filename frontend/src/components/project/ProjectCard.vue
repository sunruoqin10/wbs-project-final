<template>
  <Card
    :clickable="true"
    @click="handleClick"
    class="relative overflow-hidden"
  >
    <!-- Color Bar -->
    <div class="absolute left-0 top-0 bottom-0 w-1.5" :style="{ backgroundColor: project.color || '#3b82f6' }"></div>

    <!-- Header -->
    <div class="pl-4">
      <div class="mb-3 flex items-start justify-between">
        <div class="flex-1">
          <div class="flex items-center gap-2">
            <h3 class="text-lg font-semibold text-secondary-900">{{ project.name }}</h3>
            <Badge :variant="statusVariant">{{ statusLabel }}</Badge>
            <svg class="h-5 w-5" :class="priorityColor" fill="currentColor" viewBox="0 0 20 20">
              <path d="M5 3a2 2 0 00-2 2v2a2 2 0 002 2h2a2 2 0 002-2V5a2 2 0 00-2-2H5zM5 11a2 2 0 00-2 2v2a2 2 0 002 2h2a2 2 0 002-2v-2a2 2 0 00-2-2H5zM11 5a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V5zM11 13a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z" />
            </svg>
          </div>
          <p class="mt-1 text-sm text-secondary-600 line-clamp-2">{{ project.description }}</p>
        </div>
      </div>

      <!-- Progress -->
      <div class="mb-3">
        <ProgressBar :value="project.progress" :show-label="true" />
      </div>

      <!-- Footer -->
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-2">
          <!-- Team Members -->
          <div class="flex -space-x-2">
            <img
              v-for="member in members.slice(0, 3)"
              :key="member.id"
              :src="member.avatar"
              :alt="member.name"
              class="h-7 w-7 rounded-full border-2 border-white"
              :title="member.name"
            />
            <div
              v-if="memberCount > 3"
              class="flex h-7 w-7 items-center justify-center rounded-full border-2 border-white bg-secondary-200 text-xs font-medium text-secondary-600"
            >
              +{{ memberCount - 3 }}
            </div>
          </div>
        </div>

        <div class="flex items-center gap-3">
          <div class="text-xs text-secondary-500">
            <svg class="mr-1 inline h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"
              />
            </svg>
            {{ formattedDate }}
          </div>
        </div>
      </div>

      <!-- Tags -->
      <div v-if="project.tags && project.tags.length > 0" class="mt-3 flex flex-wrap gap-1.5">
        <span
          v-for="tag in project.tags.slice(0, 3)"
          :key="tag"
          class="rounded-full bg-secondary-100 px-2 py-0.5 text-xs text-secondary-600"
        >
          #{{ tag }}
        </span>
      </div>
    </div>
  </Card>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import Card from '@/components/common/Card.vue';
import Badge from '@/components/common/Badge.vue';
import ProgressBar from '@/components/common/ProgressBar.vue';
import type { Project } from '@/types';
import { useUserStore } from '@/stores/user';
import dayjs from 'dayjs';
import 'dayjs/locale/zh-cn';

dayjs.locale('zh-cn');

interface Props {
  project: Project;
}

const props = defineProps<Props>();
const router = useRouter();
const userStore = useUserStore();

const members = computed(() => {
  return userStore.getUsersByIds(props.project.memberIds);
});

const memberCount = computed(() => members.value.length);

const formattedDate = computed(() => {
  return `${dayjs(props.project.startDate).format('MM/DD')} - ${dayjs(props.project.endDate).format('MM/DD')}`;
});

const statusLabel = computed(() => {
  const labels: Record<string, string> = {
    planning: '计划中',
    active: '进行中',
    completed: '已完成',
    'on-hold': '已暂停'
  };
  return labels[props.project.status];
});

const statusVariant = computed(() => {
  const variants: Record<string, string> = {
    planning: 'info',
    active: 'primary',
    completed: 'success',
    'on-hold': 'warning'
  };
  return variants[props.project.status];
});

const priorityColor = computed(() => {
  const colors: Record<string, string> = {
    low: 'text-secondary-400',
    medium: 'text-info-500',
    high: 'text-warning-500',
    critical: 'text-danger-500'
  };
  return colors[props.project.priority] || colors.medium;
});

const handleClick = () => {
  router.push(`/projects/${props.project.id}`);
};
</script>
