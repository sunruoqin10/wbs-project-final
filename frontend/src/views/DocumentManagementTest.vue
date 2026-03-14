<template>
  <MainLayout>
    <div class="space-y-6">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-secondary-900">文档管理测试页面</h1>
          <p class="mt-1 text-sm text-secondary-600">测试组件是否正常渲染</p>
        </div>
      </div>

      <div class="bg-white rounded-lg shadow-sm p-6">
        <div class="mb-4">
          <p class="text-lg font-medium">当前Tab: {{ activeTab }}</p>
          <div class="mt-4 space-x-2">
            <button
              @click="activeTab = 'project'"
              :class="['px-4 py-2 rounded', activeTab === 'project' ? 'bg-primary-500 text-white' : 'bg-gray-200']"
            >
              项目文档
            </button>
            <button
              @click="activeTab = 'report'"
              :class="['px-4 py-2 rounded', activeTab === 'report' ? 'bg-primary-500 text-white' : 'bg-gray-200']"
            >
              周报文档
            </button>
          </div>
        </div>

        <hr class="my-4">

        <div v-if="activeTab === 'project'">
          <h2 class="text-xl font-bold mb-4">项目文档组件</h2>
          <ProjectDocuments />
        </div>

        <div v-else-if="activeTab === 'report'">
          <h2 class="text-xl font-bold mb-4">周报文档组件</h2>
          <ReportDocuments />
        </div>
      </div>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import MainLayout from '@/components/layout/MainLayout.vue';
import ProjectDocuments from '@/components/document/ProjectDocuments.vue';
import ReportDocuments from '@/components/document/ReportDocuments.vue';

const activeTab = ref<'project' | 'report'>('project');
</script>
