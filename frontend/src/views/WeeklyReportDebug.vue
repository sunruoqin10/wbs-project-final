<template>
  <div class="p-8">
    <h1 class="text-2xl font-bold mb-4">周报数据调试</h1>
    
    <div class="space-y-4">
      <div class="bg-white p-4 rounded-lg shadow">
        <h2 class="text-lg font-semibold mb-2">当前用户信息</h2>
        <pre class="bg-gray-100 p-2 rounded">{{ userStore.currentUser }}</pre>
        <p class="mt-2">currentUserId: {{ userStore.currentUserId }}</p>
        <p>token: {{ userStore.token ? '已设置' : '未设置' }}</p>
      </div>

      <div class="bg-white p-4 rounded-lg shadow">
        <h2 class="text-lg font-semibold mb-2">权限信息</h2>
        <p>当前角色: {{ permissionStore.currentRole }}</p>
      </div>

      <div class="bg-white p-4 rounded-lg shadow">
        <h2 class="text-lg font-semibold mb-2">周报数据</h2>
        <p>数量: {{ weeklyReportStore.reports.length }}</p>
        <pre class="bg-gray-100 p-2 rounded mt-2 max-h-96 overflow-auto">{{ JSON.stringify(weeklyReportStore.reports, null, 2) }}</pre>
      </div>

      <div class="bg-white p-4 rounded-lg shadow">
        <h2 class="text-lg font-semibold mb-2">用户列表</h2>
        <p>数量: {{ userStore.users.length }}</p>
        <pre class="bg-gray-100 p-2 rounded mt-2 max-h-96 overflow-auto">{{ JSON.stringify(userStore.users, null, 2) }}</pre>
      </div>

      <button 
        @click="reloadData" 
        class="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600"
      >
        重新加载数据
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
import { useUserStore } from '@/stores/user';
import { useWeeklyReportStore } from '@/stores/weeklyReport';
import { usePermissionStore } from '@/stores/permission';

const userStore = useUserStore();
const weeklyReportStore = useWeeklyReportStore();
const permissionStore = usePermissionStore();

const reloadData = async () => {
  await Promise.all([
    userStore.loadUsers(),
    weeklyReportStore.loadReports()
  ]);
};

onMounted(async () => {
  await reloadData();
});
</script>
