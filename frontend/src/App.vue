<template>
  <div id="app" class="min-h-screen bg-background">
    <router-view />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
import { useUserStore } from './stores/user';
import { useAccessLog } from './composables/useAccessLog';

const userStore = useUserStore();

// 启动访问日志监听(必须在 setup 顶层调用,不能在 onMounted 回调里:
// Vue 3 的 onMounted 只能在 setup 同步阶段注册,异步回调里再调是 no-op)
useAccessLog();

onMounted(async () => {
  // 恢复认证信息并加载用户数据
  await userStore.restoreAuth();
  await userStore.loadUsers();

  console.log('App initialized, current user:', userStore.currentUser);
});
</script>
