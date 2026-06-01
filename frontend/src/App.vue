<template>
  <div id="app" class="min-h-screen bg-background">
    <router-view />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
import { useUserStore } from './stores/user';

const userStore = useUserStore();

onMounted(async () => {
  // 恢复认证信息并加载用户数据
  await userStore.restoreAuth();
  await userStore.loadUsers();
  
  console.log('App initialized, current user:', userStore.currentUser);
});
</script>
