<template>
  <div class="min-h-screen bg-white p-8">
    <h1 class="text-3xl font-bold text-blue-600">测试页面</h1>
    <p class="mt-4 text-lg">如果你能看到这个页面，说明路由工作正常。</p>
    <div class="mt-8 space-y-4">
      <div class="p-4 bg-gray-100 rounded">
        <p>项目数量: {{ projects.length }}</p>
      </div>
      <div class="p-4 bg-gray-100 rounded">
        <p>用户数量: {{ users.length }}</p>
      </div>
      <div class="p-4 bg-gray-100 rounded">
        <p>任务数量: {{ tasks.length }}</p>
      </div>
    </div>
    <button class="mt-6 px-4 py-2 bg-blue-500 text-white rounded" @click="testClick">
      点击测试
    </button>
    <p v-if="clicked" class="mt-4 text-green-600">按钮点击成功！</p>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useProjectStore } from '@/stores/project';
import { useTaskStore } from '@/stores/task';
import { useUserStore } from '@/stores/user';

const projectStore = useProjectStore();
const taskStore = useTaskStore();
const userStore = useUserStore();

const projects = computed(() => projectStore.projects);
const users = computed(() => userStore.users);
const tasks = computed(() => taskStore.tasks);

const clicked = ref(false);

const testClick = () => {
  clicked.value = true;
  console.log('Test button clicked!');
  console.log('Projects:', projects.value.length);
  console.log('Users:', users.value.length);
  console.log('Tasks:', tasks.value.length);
};

onMounted(async () => {
  await Promise.all([
    projectStore.loadProjects(),
    taskStore.loadTasks(),
    userStore.loadUsers()
  ]);
});
</script>
