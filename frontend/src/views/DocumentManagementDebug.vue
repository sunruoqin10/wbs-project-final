<template>
  <MainLayout>
    <div class="space-y-6 p-4 bg-gray-50">
      <!-- 页面标题 -->
      <div class="bg-white p-4 rounded shadow">
        <h1 class="text-2xl font-bold text-gray-900">文档管理 - 调试模式</h1>
        <p class="text-sm text-gray-600 mt-1">当前Tab: {{ activeTab }} | 索引: {{ activeTabIndex }}</p>
      </div>

      <!-- Tab切换 -->
      <div class="bg-white p-4 rounded shadow">
        <div class="space-x-2">
          <button
            @click="switchTab('project')"
            :class="['px-4 py-2 rounded font-medium transition-colors', activeTab === 'project' ? 'bg-blue-500 text-white' : 'bg-gray-200 text-gray-700 hover:bg-gray-300']"
          >
            项目文档
          </button>
          <button
            @click="switchTab('report')"
            :class="['px-4 py-2 rounded font-medium transition-colors', activeTab === 'report' ? 'bg-blue-500 text-white' : 'bg-gray-200 text-gray-700 hover:bg-gray-300']"
          >
            周报文档
          </button>
        </div>
      </div>

      <!-- 组件渲染区域 -->
      <div class="bg-white rounded shadow p-6 min-h-[400px]">
        <div v-if="activeTab === 'project'">
          <h2 class="text-lg font-bold mb-4 text-gray-800">项目文档组件</h2>
          <div class="border-t pt-4">
            <ProjectDocuments />
          </div>
        </div>

        <div v-else-if="activeTab === 'report'">
          <h2 class="text-lg font-bold mb-4 text-gray-800">周报文档组件</h2>
          <div class="border-t pt-4">
            <ReportDocuments />
          </div>
        </div>
      </div>

      <!-- 调试信息 -->
      <div class="bg-yellow-50 border border-yellow-200 rounded p-4">
        <h3 class="font-bold text-yellow-800 mb-2">调试信息</h3>
        <ul class="text-sm text-yellow-700 space-y-1">
          <li>✓ DocumentManagement.vue 正常渲染</li>
          <li>✓ MainLayout 组件正常工作</li>
          <li>✓ Tab切换功能正常</li>
          <li>✓ ProjectDocuments 组件: {{ activeTab === 'project' ? '已挂载' : '未挂载' }}</li>
          <li>✓ ReportDocuments 组件: {{ activeTab === 'report' ? '已挂载' : '未挂载' }}</li>
        </ul>
        <p class="text-xs text-yellow-600 mt-2">如果看到此消息，说明主页面正常。请检查子组件的控制台错误。</p>
      </div>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import MainLayout from '@/components/layout/MainLayout.vue';
import ProjectDocuments from '@/components/document/ProjectDocuments.vue';
import ReportDocuments from '@/components/document/ReportDocuments.vue';

const activeTabIndex = ref(0);
const activeTab = ref<'project' | 'report'>('project');

function switchTab(tab: 'project' | 'report') {
  console.log('[DocumentManagement] 切换Tab:', tab);
  activeTab.value = tab;
  activeTabIndex.value = tab === 'project' ? 0 : 1;
}
</script>
