<template>
  <MainLayout>
    <div class="p-6 bg-white min-h-screen">
      <h1 class="text-3xl font-bold text-blue-600 mb-4">文档组件测试页面</h1>

      <div class="mb-6 p-4 bg-gray-100 rounded">
        <p class="text-lg">测试状态: <span class="font-bold text-green-600">{{ testStatus }}</span></p>
        <button
          @click="showProjectDoc = !showProjectDoc"
          class="mt-2 px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600"
        >
          {{ showProjectDoc ? '隐藏' : '显示' }}项目文档组件
        </button>
        <button
          @click="showReportDoc = !showReportDoc"
          class="mt-2 ml-2 px-4 py-2 bg-green-500 text-white rounded hover:bg-green-600"
        >
          {{ showReportDoc ? '隐藏' : '显示' }}周报文档组件
        </button>
      </div>

      <div class="grid grid-cols-1 gap-6">
        <!-- 项目文档组件测试 -->
        <div v-if="showProjectDoc" class="p-4 border-2 border-blue-300 rounded">
          <h2 class="text-xl font-bold text-blue-800 mb-2">项目文档组件 (ProjectDocuments)</h2>
          <div class="bg-white p-4 rounded border">
            <ProjectDocuments />
          </div>
        </div>

        <!-- 周报文档组件测试 -->
        <div v-if="showReportDoc" class="p-4 border-2 border-green-300 rounded">
          <h2 class="text-xl font-bold text-green-800 mb-2">周报文档组件 (ReportDocuments)</h2>
          <div class="bg-white p-4 rounded border">
            <ReportDocuments />
          </div>
        </div>
      </div>

      <!-- 调试信息 -->
      <div class="mt-6 p-4 bg-yellow-50 border border-yellow-300 rounded">
        <h3 class="font-bold text-yellow-800">调试信息</h3>
        <ul class="list-disc list-inside text-sm text-yellow-700 mt-2">
          <li>主页面渲染: ✓</li>
          <li>MainLayout组件: ✓</li>
          <li>项目文档组件: {{ showProjectDoc ? '✓ 显示中' : '○ 隐藏' }}</li>
          <li>周报文档组件: {{ showReportDoc ? '✓ 显示中' : '○ 隐藏' }}</li>
        </ul>
        <p class="text-xs text-yellow-600 mt-2">
          如果看到这个黄色框，说明主页面工作正常。<br>
          如果上方组件区域是空的，请检查浏览器控制台的错误信息。
        </p>
      </div>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import MainLayout from '@/components/layout/MainLayout.vue';
import ProjectDocuments from '@/components/document/ProjectDocuments.vue';
import ReportDocuments from '@/components/document/ReportDocuments.vue';

const showProjectDoc = ref(true);
const showReportDoc = ref(false);

const testStatus = ref('页面已加载');
</script>
