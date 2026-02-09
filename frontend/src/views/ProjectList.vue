<template>
  <MainLayout>
    <div class="space-y-6">
      <!-- Page Header -->
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-secondary-900">{{ $t('routes.projectList') }}</h1>
          <p class="mt-1 text-sm text-secondary-600">{{ $t('projectList.subtitle') }}</p>
        </div>
        <Button variant="primary" @click="createNewProject">
          <svg class="mr-2 h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          {{ $t('projectList.newProject') }}
        </Button>
      </div>

      <!-- Filters -->
      <Card class="p-4">
        <div class="flex flex-wrap items-center gap-4">
          <!-- Search -->
          <div class="flex-1 min-w-[200px]">
            <Input
              v-model="searchQuery"
              :placeholder="$t('projectList.searchPlaceholder')"
              type="text"
            >
              <template #prefix>
                <svg class="h-5 w-5 text-secondary-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
              </template>
            </Input>
          </div>

          <!-- Status Filter -->
          <div class="flex items-center gap-2">
            <span class="text-sm font-medium text-secondary-700">{{ $t('common.status') }}:</span>
            <div class="flex gap-2">
              <button
                v-for="status in statusOptions"
                :key="status.value"
                @click="toggleStatus(status.value)"
                :class="[
                  'rounded-lg px-3 py-1.5 text-sm font-medium transition-colors',
                  selectedStatuses.includes(status.value)
                    ? 'bg-primary-600 text-white'
                    : 'bg-secondary-100 text-secondary-700 hover:bg-secondary-200'
                ]"
              >
                {{ status.label }}
              </button>
            </div>
          </div>

          <!-- Clear Filters -->
          <Button
            v-if="selectedStatuses.length > 0 || searchQuery"
            variant="ghost"
            size="sm"
            @click="clearFilters"
          >
            {{ $t('projectList.clearFilters') }}
          </Button>
        </div>
      </Card>

      <!-- Projects Grid -->
      <div v-if="filteredProjects.length > 0" class="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
        <ProjectCard
          v-for="project in filteredProjects"
          :key="project.id"
          :project="project"
        />
      </div>

      <!-- Empty State -->
      <Card v-else class="p-12">
        <div class="text-center">
          <svg class="mx-auto h-24 w-24 text-secondary-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
          </svg>
          <h3 class="mt-4 text-lg font-medium text-secondary-900">{{ $t('projectList.emptyState.title') }}</h3>
          <p class="mt-2 text-sm text-secondary-600">
            {{ searchQuery || selectedStatuses.length > 0 ? $t('projectList.emptyState.noResults') : $t('projectList.emptyState.createFirst') }}
          </p>
          <Button v-if="!searchQuery && selectedStatuses.length === 0" variant="primary" class="mt-4" @click="openCreateModal">
            {{ $t('projectList.emptyState.createButton') }}
          </Button>
        </div>
      </Card>

      <!-- Project Modal -->
      <ProjectModal
        :open="modalOpen"
        :project="editingProject"
        @close="closeModal"
        @save="handleSaveProject"
      />
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import MainLayout from '@/components/layout/MainLayout.vue';
import ProjectCard from '@/components/project/ProjectCard.vue';
import ProjectModal from '@/components/project/ProjectModal.vue';
import Card from '@/components/common/Card.vue';
import Button from '@/components/common/Button.vue';
import Input from '@/components/common/Input.vue';
import { useProjectStore } from '@/stores/project';
import type { Project } from '@/types';

const router = useRouter();
const { t } = useI18n();
const projectStore = useProjectStore();

const searchQuery = ref('');
const selectedStatuses = ref<string[]>([]);
const modalOpen = ref(false);
const editingProject = ref<Project | null>(null);

// 计算单个项目的预估工时
const calculateProjectHours = (project: Project): number => {
  const start = new Date(project.startDate);
  const end = new Date(project.endDate);

  if (end < start) return 0;

  let workDays = 0;
  let current = new Date(start);

  while (current <= end) {
    const dayOfWeek = current.getDay();
    if (dayOfWeek !== 0 && dayOfWeek !== 6) {
      workDays++;
    }
    current.setDate(current.getDate() + 1);
  }

  return workDays * 8;
};

// 初始化时检查并更新所有缺少预估工时的项目
onMounted(async () => {
  // 确保项目已加载
  await projectStore.loadProjects();

  // 检查所有项目，为缺少预估工时的项目自动计算
  const projects = projectStore.projects;
  for (const project of projects) {
    if (!project.estimatedHours || project.estimatedHours === 0) {
      const hours = calculateProjectHours(project);
      if (hours > 0) {
        await projectStore.updateProject(project.id, { estimatedHours: hours });
      }
    }
  }
});

const statusOptions = computed(() => [
  { label: t('projectList.statuses.all'), value: '' },
  { label: t('projectList.statuses.planning'), value: 'planning' },
  { label: t('projectList.statuses.active'), value: 'active' },
  { label: t('projectList.statuses.completed'), value: 'completed' },
  { label: t('projectList.statuses.onHold'), value: 'on-hold' },
  { label: t('projectList.statuses.cancelled'), value: 'cancelled' }
]);

const filteredProjects = computed(() => {
  let result = projectStore.projects;

  if (selectedStatuses.value.length > 0) {
    result = result.filter(p => selectedStatuses.value.includes(p.status));
  }

  if (searchQuery.value) {
    const search = searchQuery.value.toLowerCase();
    result = result.filter(p =>
      p.name.toLowerCase().includes(search) ||
      p.description.toLowerCase().includes(search) ||
      p.tags.some(tag => tag.toLowerCase().includes(search))
    );
  }

  return result;
});

const toggleStatus = (status: string) => {
  if (status === '') {
    selectedStatuses.value = [];
  } else {
    const index = selectedStatuses.value.indexOf(status);
    if (index === -1) {
      selectedStatuses.value.push(status);
    } else {
      selectedStatuses.value.splice(index, 1);
    }
  }
};

const clearFilters = () => {
  searchQuery.value = '';
  selectedStatuses.value = [];
};

const createNewProject = () => {
  openCreateModal();
};

const openCreateModal = () => {
  editingProject.value = null;
  modalOpen.value = true;
};

const openEditModal = (project: Project) => {
  editingProject.value = project;
  modalOpen.value = true;
};

const closeModal = () => {
  modalOpen.value = false;
  editingProject.value = null;
};

const handleSaveProject = async (projectData: Partial<Project>) => {
  if (editingProject.value) {
    // Update existing project
    await projectStore.updateProject(editingProject.value.id, projectData);
  } else {
    // Create new project
    const newProject = await projectStore.createProject(projectData);
    console.log('Created project:', newProject);
  }
};
</script>
