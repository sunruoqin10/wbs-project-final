<template>
  <div class="min-h-screen bg-[#f8fafc]">
    <!-- 简化的侧边栏 -->
    <aside class="fixed left-0 top-0 h-64 w-64 bg-secondary-800 text-white p-4">
      <h2 class="text-xl font-bold">WBS 系统</h2>
      <nav class="mt-4 space-y-2">
        <router-link to="/dashboard" class="block py-2 hover:bg-secondary-700 rounded px-3">仪表盘</router-link>
        <router-link to="/projects" class="block py-2 hover:bg-secondary-700 rounded px-3">项目列表</router-link>
        <router-link to="/team" class="block py-2 hover:bg-secondary-700 rounded px-3">团队</router-link>
        <router-link to="/reports" class="block py-2 hover:bg-secondary-700 rounded px-3">报表</router-link>
      </nav>
    </aside>

    <!-- 主内容 -->
    <main class="ml-64 p-8">
      <h1 class="text-3xl font-bold text-secondary-900 mb-6">仪表盘</h1>

      <!-- 统计卡片 -->
      <div class="grid grid-cols-4 gap-6 mb-8">
        <div class="bg-white p-6 rounded-xl shadow-md">
          <p class="text-sm text-secondary-600">总项目数</p>
          <p class="text-3xl font-bold text-secondary-900 mt-2">{{ projects.length }}</p>
        </div>
        <div class="bg-white p-6 rounded-xl shadow-md">
          <p class="text-sm text-secondary-600">进行中</p>
          <p class="text-3xl font-bold text-primary-600 mt-2">{{ activeProjects }}</p>
        </div>
        <div class="bg-white p-6 rounded-xl shadow-md">
          <p class="text-sm text-secondary-600">总任务数</p>
          <p class="text-3xl font-bold text-secondary-900 mt-2">{{ tasks.length }}</p>
        </div>
        <div class="bg-white p-6 rounded-xl shadow-md">
          <p class="text-sm text-secondary-600">已完成</p>
          <p class="text-3xl font-bold text-accent-600 mt-2">{{ completedTasks }}</p>
        </div>
      </div>

      <!-- 项目列表 -->
      <div class="bg-white rounded-xl shadow-md p-6">
        <h2 class="text-xl font-bold text-secondary-900 mb-4">最近项目</h2>
        <div class="space-y-4">
          <div v-for="project in projects.slice(0, 5)" :key="project.id" class="border-b border-secondary-200 pb-4 last:border-0">
            <div class="flex items-center justify-between">
              <div>
                <h3 class="font-semibold text-secondary-900">{{ project.name }}</h3>
                <p class="text-sm text-secondary-600">{{ project.description }}</p>
              </div>
              <div class="flex items-center gap-4">
                <span class="px-3 py-1 rounded-full text-sm" :class="getStatusClass(project.status)">
                  {{ getStatusLabel(project.status) }}
                </span>
                <span class="text-sm text-secondary-600">{{ project.progress }}%</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useProjectStore } from '@/stores/project';
import { useTaskStore } from '@/stores/task';

const projectStore = useProjectStore();
const taskStore = useTaskStore();

const projects = computed(() => projectStore.projects);
const tasks = computed(() => taskStore.tasks);

const activeProjects = computed(() => projects.value.filter(p => p.status === 'active').length);
const completedTasks = computed(() => tasks.value.filter(t => t.status === 'done').length);

const getStatusLabel = (status: string) => {
  const labels: Record<string, string> = {
    planning: '计划中',
    active: '进行中',
    completed: '已完成',
    'on-hold': '已暂停'
  };
  return labels[status] || status;
};

const getStatusClass = (status: string) => {
  const classes: Record<string, string> = {
    planning: 'bg-info-100 text-info-700',
    active: 'bg-primary-100 text-primary-700',
    completed: 'bg-accent-100 text-accent-700',
    'on-hold': 'bg-warning-100 text-warning-700'
  };
  return classes[status] || 'bg-secondary-100 text-secondary-700';
};

onMounted(async () => {
  await Promise.all([
    projectStore.loadProjects(),
    taskStore.loadTasks()
  ]);
});
</script>
