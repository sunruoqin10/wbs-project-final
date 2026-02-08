<template>
  <MainLayout>
    <div class="space-y-6">
      <!-- Page Header -->
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-4">
          <button @click="goBack" class="rounded-lg p-2 hover:bg-secondary-100">
            <svg class="h-5 w-5 text-secondary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
            </svg>
          </button>
          <div>
            <h1 class="text-2xl font-bold text-secondary-900">{{ project?.name }}</h1>
            <p class="mt-1 text-sm text-secondary-600">甘特图</p>
          </div>
        </div>
        <div class="flex items-center gap-3">
          <Select v-model="scale" class="w-32">
            <option value="day">日视图</option>
            <option value="week">周视图</option>
            <option value="month">月视图</option>
          </Select>
          <Button variant="secondary" @click="zoomIn">
            <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0zM10 7v3m0 0v3m0-3h3m-3 0H7" />
            </svg>
          </Button>
          <Button variant="secondary" @click="zoomOut">
            <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0zM13 10H7" />
            </svg>
          </Button>
        </div>
      </div>

      <!-- Gantt Chart -->
      <Card>
        <GanttChart :tasks="projectTasks" :project="project!" :scale="scale" />
      </Card>

      <!-- Legend -->
      <Card>
        <div class="flex items-center justify-between">
          <h3 class="font-semibold text-secondary-900">状态</h3>
          <div class="flex items-center gap-6">
            <div class="flex items-center gap-2">
              <div class="h-3 w-3 rounded" style="background-color: #95a5a6;"></div>
              <span class="text-sm text-secondary-600">待办</span>
            </div>
            <div class="flex items-center gap-2">
              <div class="h-3 w-3 rounded" style="background-color: #3498db;"></div>
              <span class="text-sm text-secondary-600">进行中</span>
            </div>
            <div class="flex items-center gap-2">
              <div class="h-3 w-3 rounded" style="background-color: #27ae60;"></div>
              <span class="text-sm text-secondary-600">已完成</span>
            </div>
          </div>
        </div>
      </Card>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import MainLayout from '@/components/layout/MainLayout.vue';
import GanttChart from '@/components/gantt/GanttChart.vue';
import Card from '@/components/common/Card.vue';
import Button from '@/components/common/Button.vue';
import Select from '@/components/common/Select.vue';
import { useProjectStore } from '@/stores/project';
import { useTaskStore } from '@/stores/task';

const route = useRoute();
const router = useRouter();
const projectStore = useProjectStore();
const taskStore = useTaskStore();

const projectId = route.params.id as string;
const scale = ref('day');

onMounted(() => {
  const project = projectStore.projectById(projectId);
  if (project) {
    projectStore.setCurrentProject(project);
  }
});

const project = computed(() => projectStore.projectById(projectId));
const projectTasks = computed(() => taskStore.tasksByProject(projectId));

const goBack = () => {
  router.push(`/projects/${projectId}`);
};

const zoomIn = () => {
  console.log('Zoom in');
};

const zoomOut = () => {
  console.log('Zoom out');
};
</script>
