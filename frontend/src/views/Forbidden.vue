<template>
  <div class="flex min-h-screen items-center justify-center bg-secondary-50">
    <div class="text-center">
      <div class="mb-8">
        <svg class="mx-auto h-24 w-24 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
        </svg>
      </div>
      <h1 class="mb-4 text-6xl font-bold text-secondary-900">403</h1>
      <h2 class="mb-4 text-2xl font-semibold text-secondary-700">访问被拒绝</h2>
      <p class="mb-8 text-secondary-600">抱歉，您没有权限访问此页面。请联系管理员获取相应权限。</p>
      <div class="flex justify-center gap-4">
        <button
          @click="goBack"
          class="rounded-lg border border-secondary-300 bg-white px-6 py-2 text-secondary-700 hover:bg-secondary-50"
        >
          返回上一页
        </button>
        <button
          @click="goHome"
          class="rounded-lg bg-primary-600 px-6 py-2 text-white hover:bg-primary-700"
        >
          返回首页
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';

const router = useRouter();

const goBack = () => {
  router.back();
};

const goHome = () => {
  router.push('/dashboard');
};
</script>
