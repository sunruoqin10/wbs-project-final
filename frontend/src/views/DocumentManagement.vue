<template>
  <MainLayout>
    <div class="space-y-6">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-secondary-900">{{ $t('documents.title') }}</h1>
          <p class="mt-1 text-sm text-secondary-600">{{ $t('documents.subtitle') }}</p>
        </div>
      </div>

      <Tabs
        v-model="activeTabIndex"
        :tabs="[
          { label: $t('documents.tabs.projectDocuments') },
          { label: $t('documents.tabs.reportDocuments') }
        ]"
        @change="handleTabChange"
        class="mb-6"
      />

      <div class="bg-white rounded-lg shadow-sm p-6">
        <ProjectDocuments v-if="activeTab === 'project'" />
        <ReportDocuments v-else-if="activeTab === 'report'" />
      </div>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import MainLayout from '@/components/layout/MainLayout.vue';
import Tabs from '@/components/common/Tabs.vue';
import type { Tab } from '@/components/common/Tabs.vue';
import ProjectDocuments from '@/components/document/ProjectDocuments.vue';
import ReportDocuments from '@/components/document/ReportDocuments.vue';

const activeTabIndex = ref(0);
const activeTab = ref<'project' | 'report'>('project');

function handleTabChange(tab: Tab, index: number) {
  if (index === 0) {
    activeTab.value = 'project';
  } else if (index === 1) {
    activeTab.value = 'report';
  }
}
</script>
