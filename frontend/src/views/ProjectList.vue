<template>
  <MainLayout>
    <div class="space-y-6">
      <!-- Page Header -->
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-secondary-900">{{ $t('routes.projectList') }}</h1>
          <p class="mt-1 text-sm text-secondary-600">{{ $t('projectList.subtitle') }}</p>
        </div>
        <Button v-if="permissionStore.canCreateProject()" variant="primary" @click="createNewProject">
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

          <!-- View Toggle -->
          <div class="flex items-center gap-1 rounded-lg bg-secondary-100 p-1">
            <button
              @click="setViewMode('card')"
              :class="[
                'flex items-center gap-1.5 rounded-md px-3 py-1.5 text-sm font-medium transition-all',
                viewMode === 'card'
                  ? 'bg-white text-primary-600 shadow-sm'
                  : 'text-secondary-600 hover:text-secondary-900'
              ]"
              :title="$t('projectList.viewModes.card')"
            >
              <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z" />
              </svg>
              <span class="hidden sm:inline">{{ $t('projectList.viewModes.card') }}</span>
            </button>
            <button
              @click="setViewMode('list')"
              :class="[
                'flex items-center gap-1.5 rounded-md px-3 py-1.5 text-sm font-medium transition-all',
                viewMode === 'list'
                  ? 'bg-white text-primary-600 shadow-sm'
                  : 'text-secondary-600 hover:text-secondary-900'
              ]"
              :title="$t('projectList.viewModes.list')"
            >
              <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 10h16M4 14h16M4 18h16" />
              </svg>
              <span class="hidden sm:inline">{{ $t('projectList.viewModes.list') }}</span>
            </button>
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

      <!-- Projects Grid - Card View -->
      <Transition name="fade" mode="out-in">
        <div
          v-if="viewMode === 'card'"
          key="card-view"
          class="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3"
        >
          <ProjectCard
            v-for="project in filteredProjects"
            :key="project.id"
            :project="project"
          />
        </div>

        <!-- Projects List - List View -->
        <div
          v-else
          key="list-view"
          class="space-y-3"
        >
          <!-- List Header -->
          <div class="hidden rounded-lg bg-secondary-50 px-4 py-3 text-sm font-medium text-secondary-600 md:grid md:grid-cols-12 md:gap-4">
            <div class="col-span-3">{{ $t('projectList.listView.projectName') }}</div>
            <div class="col-span-2">{{ $t('projectList.listView.status') }}</div>
            <div class="col-span-2">{{ $t('projectList.listView.progress') }}</div>
            <div class="col-span-2">{{ $t('projectList.listView.members') }}</div>
            <div class="col-span-2">{{ $t('projectList.listView.dateRange') }}</div>
            <div class="col-span-1 text-right">{{ $t('projectList.listView.actions') }}</div>
          </div>

          <!-- List Items -->
          <div
            v-for="project in filteredProjects"
            :key="project.id"
            class="group cursor-pointer rounded-lg border border-secondary-200 bg-white p-4 transition-all hover:border-primary-300 hover:shadow-md md:grid md:grid-cols-12 md:gap-4 md:p-3"
            @click="goToProject(project.id)"
          >
            <!-- Project Name -->
            <div class="col-span-3 flex items-center gap-3">
              <div
                class="h-3 w-3 flex-shrink-0 rounded-full"
                :style="{ backgroundColor: project.color || '#3b82f6' }"
              ></div>
              <div class="min-w-0 flex-1">
                <div class="truncate font-medium text-secondary-900">{{ project.name }}</div>
                <div class="truncate text-xs text-secondary-500 md:hidden">{{ project.description }}</div>
              </div>
            </div>

            <!-- Status -->
            <div class="col-span-2 mt-2 flex items-center md:mt-0">
              <Badge :variant="getStatusVariant(project.status)">{{ getStatusLabel(project.status) }}</Badge>
              <Badge v-if="project.isDelayed || (project.delayedTasks || 0) > 0" :variant="getDelaySeverity(project)" size="sm" class="ml-2">
                {{ getDelayLabel(project) }}
              </Badge>
            </div>

            <!-- Progress -->
            <div class="col-span-2 mt-2 md:mt-0">
              <div class="flex items-center gap-2">
                <ProgressBar :value="project.progress" :show-label="true" class="flex-1" />
              </div>
            </div>

            <!-- Members -->
            <div class="col-span-2 mt-2 flex items-center md:mt-0">
              <div class="flex -space-x-2">
                <img
                  v-for="member in getMembers(project).slice(0, 3)"
                  :key="member.id"
                  :src="member.avatar"
                  :alt="member.name"
                  class="h-7 w-7 rounded-full border-2 border-white"
                  :title="member.name"
                />
                <div
                  v-if="getMemberCount(project) > 3"
                  class="flex h-7 w-7 items-center justify-center rounded-full border-2 border-white bg-secondary-200 text-xs font-medium text-secondary-600"
                >
                  +{{ getMemberCount(project) - 3 }}
                </div>
              </div>
            </div>

            <!-- Date Range -->
            <div class="col-span-2 mt-2 flex items-center gap-1 text-sm text-secondary-600 md:mt-0">
              <svg class="h-4 w-4 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
              </svg>
              <span class="truncate">{{ formatDateRange(project) }}</span>
            </div>

            <!-- Actions -->
            <div class="col-span-1 mt-3 flex items-center justify-end gap-2 md:mt-0">
              <button
                v-if="permissionStore.canEditProject(project)"
                @click.stop="openEditModal(project)"
                class="rounded p-1.5 text-secondary-400 hover:bg-secondary-100 hover:text-primary-600"
                :title="$t('common.edit')"
              >
                <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                </svg>
              </button>
              <button
                v-if="permissionStore.canDeleteProject(project)"
                @click.stop="confirmDelete(project)"
                class="rounded p-1.5 text-secondary-400 hover:bg-secondary-100 hover:text-danger-600"
                :title="$t('common.delete')"
              >
                <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                </svg>
              </button>
            </div>
          </div>
        </div>
      </Transition>

      <!-- Empty State -->
      <Card v-if="filteredProjects.length === 0" class="p-12">
        <div class="text-center">
          <svg class="mx-auto h-24 w-24 text-secondary-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
          </svg>
          <h3 class="mt-4 text-lg font-medium text-secondary-900">{{ $t('projectList.emptyState.title') }}</h3>
          <p class="mt-2 text-sm text-secondary-600">
            {{ searchQuery || selectedStatuses.length > 0 ? $t('projectList.emptyState.noResults') : $t('projectList.emptyState.createFirst') }}
          </p>
          <Button v-if="!searchQuery && selectedStatuses.length === 0 && permissionStore.canCreateProject()" variant="primary" class="mt-4" @click="openCreateModal">
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
import Badge from '@/components/common/Badge.vue';
import ProgressBar from '@/components/common/ProgressBar.vue';
import { useProjectStore } from '@/stores/project';
import { usePermissionStore } from '@/stores/permission';
import { useUserStore } from '@/stores/user';
import type { Project, User } from '@/types';
import dayjs from 'dayjs';
import 'dayjs/locale/zh-cn';

dayjs.locale('zh-cn');

const router = useRouter();
const { t } = useI18n();
const projectStore = useProjectStore();
const permissionStore = usePermissionStore();
const userStore = useUserStore();

const searchQuery = ref('');
const selectedStatuses = ref<string[]>([]);
const modalOpen = ref(false);
const editingProject = ref<Project | null>(null);

const STORAGE_KEY = 'wbs-project-view-mode';

const viewMode = ref<'card' | 'list'>(
  (localStorage.getItem(STORAGE_KEY) as 'card' | 'list') || 'card'
);

const setViewMode = (mode: 'card' | 'list') => {
  viewMode.value = mode;
  localStorage.setItem(STORAGE_KEY, mode);
};

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

  // 权限控制：管理员和项目经理可以看到所有项目，其他角色只能看到自己参与或负责的项目
  if (permissionStore.currentRole !== 'admin' && permissionStore.currentRole !== 'project-manager') {
    const currentUserId = userStore.currentUserId;
    if (currentUserId) {
      result = result.filter(project => {
        // 检查是否是项目负责人
        const isOwner = project.ownerId === currentUserId;
        // 检查是否是项目成员
        const isMember = project.memberIds?.includes(currentUserId) || false;
        return isOwner || isMember;
      });
    }
  }

  if (selectedStatuses.value.length > 0) {
    result = result.filter(p => selectedStatuses.value.includes(p.status));
  }

  if (searchQuery.value) {
    const search = searchQuery.value.toLowerCase();
    result = result.filter(p =>
      p.name.toLowerCase().includes(search) ||
      p.description.toLowerCase().includes(search) ||
      (p.tags && p.tags.some(tag => tag.toLowerCase().includes(search)))
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
    await projectStore.updateProject(editingProject.value.id, projectData);
  } else {
    const newProject = await projectStore.createProject(projectData);
    console.log('Created project:', newProject);
  }
};

const getMembers = (project: Project): User[] => {
  return userStore.getUsersByIds(project.memberIds);
};

const getMemberCount = (project: Project): number => {
  return getMembers(project).length;
};

const formatDateRange = (project: Project): string => {
  if (!project.startDate || !project.endDate) return '-';
  return `${dayjs(project.startDate).format('MM/DD')} - ${dayjs(project.endDate).format('MM/DD')}`;
};

const getStatusLabel = (status: string): string => {
  const labels: Record<string, string> = {
    planning: '计划中',
    active: '进行中',
    completed: '已完成',
    'on-hold': '已暂停'
  };
  return labels[status] || status;
};

const getStatusVariant = (status: string): string => {
  const variants: Record<string, string> = {
    planning: 'info',
    active: 'primary',
    completed: 'success',
    'on-hold': 'warning'
  };
  return variants[status] || 'info';
};

const getDelaySeverity = (project: Project): string => {
  const delayedTasks = project.delayedTasks || 0;
  const totalDelayedDays = project.totalDelayedDays || 0;
  if (delayedTasks >= 3 || totalDelayedDays >= 7) return 'danger';
  if (delayedTasks >= 1 || totalDelayedDays >= 3) return 'warning';
  return 'info';
};

const getDelayLabel = (project: Project): string => {
  const delayedTasks = project.delayedTasks || 0;
  const totalDelayedDays = project.totalDelayedDays || 0;
  if (delayedTasks >= 3 || totalDelayedDays >= 7) return '严重延期';
  if (delayedTasks >= 1 || totalDelayedDays >= 3) return '已延期';
  return '有延期';
};

const goToProject = (projectId: string) => {
  router.push(`/projects/${projectId}`);
};

const confirmDelete = async (project: Project) => {
  if (confirm(`确定要删除项目 "${project.name}" 吗？`)) {
    await projectStore.deleteProject(project.id);
  }
};
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.fade-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
