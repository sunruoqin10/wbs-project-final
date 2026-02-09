<template>
  <MainLayout>
    <div class="space-y-6">
      <!-- Page Header -->
      <div v-if="project" class="flex items-center justify-between">
        <div class="flex items-center gap-4">
          <button @click="goBack" class="rounded-lg p-2 hover:bg-secondary-100">
            <svg class="h-5 w-5 text-secondary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
            </svg>
          </button>
          <div>
            <h1 class="text-2xl font-bold text-secondary-900">{{ project.name }}</h1>
            <p class="mt-1 text-sm text-secondary-600">{{ project.description }}</p>
          </div>
        </div>
        <div class="flex items-center gap-3">
          <Button variant="secondary" @click="editProject">{{ $t('projectDetail.editProject') }}</Button>
          <Button variant="danger" @click="deleteProject">{{ $t('projectDetail.deleteProject') }}</Button>
        </div>
      </div>

      <template v-if="project">
        <!-- Project Info Cards -->
        <div class="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-5">
          <Card>
            <div class="p-4">
              <p class="text-sm font-medium text-secondary-600">{{ $t('projectDetail.status') }}</p>
              <p class="mt-2 text-lg font-semibold text-secondary-900">{{ getStatusLabel(project.status) }}</p>
            </div>
          </Card>

          <Card>
            <div class="p-4">
              <p class="text-sm font-medium text-secondary-600">{{ $t('projectDetail.priority') }}</p>
              <p class="mt-2 text-lg font-semibold text-secondary-900">{{ getPriorityLabel(project.priority) }}</p>
            </div>
          </Card>

          <Card>
            <div class="p-4">
              <p class="text-sm font-medium text-secondary-600">{{ $t('projectDetail.progress') }}</p>
              <p class="mt-2 text-lg font-semibold text-secondary-900">{{ project.progress }}%</p>
            </div>
          </Card>

          <Card>
            <div class="p-4">
              <p class="text-sm font-medium text-secondary-600">{{ $t('projectDetail.estimatedHours') }}</p>
              <p class="mt-2 text-lg font-semibold text-secondary-900">{{ displayEstimatedHours }} {{ $t('projectDetail.hours') }}</p>
            </div>
          </Card>

          <Card>
            <div class="p-4">
              <p class="text-sm font-medium text-secondary-600">{{ $t('projectDetail.members') }}</p>
              <p class="mt-2 text-lg font-semibold text-secondary-900">{{ members.length }} {{ $t('projectDetail.people') }}</p>
            </div>
          </Card>
        </div>

        <!-- Progress Section -->
        <Card>
          <template #header>
            <h3 class="text-lg font-semibold text-secondary-900">{{ $t('projectDetail.projectProgress') }}</h3>
          </template>
          <div class="space-y-4">
            <ProgressBar :value="project.progress" :show-label="true" />
            <div class="grid grid-cols-2 gap-4 text-sm">
              <div>
                <span class="text-secondary-600">{{ $t('projectDetail.startDate') }}：</span>
                <span class="font-medium text-secondary-900">{{ formattedDate(project.startDate) }}</span>
              </div>
              <div>
                <span class="text-secondary-600">{{ $t('projectDetail.endDate') }}：</span>
                <span class="font-medium text-secondary-900">{{ formattedDate(project.endDate) }}</span>
              </div>
            </div>
          </div>
        </Card>

        <!-- Team Members -->
        <Card>
          <template #header>
            <h3 class="text-lg font-semibold text-secondary-900">{{ $t('projectDetail.members') }}</h3>
          </template>
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <div
              v-for="member in members"
              :key="member.id"
              class="flex items-center gap-3 rounded-lg border border-secondary-200 p-3"
            >
              <img :src="member.avatar" :alt="member.name" class="h-10 w-10 rounded-full" />
              <div>
                <p class="font-medium text-secondary-900">{{ member.name }}</p>
                <p class="text-sm text-secondary-600">{{ getRoleLabel(member.role) }}</p>
              </div>
            </div>
          </div>
        </Card>

        <!-- Tags -->
        <Card v-if="project.tags && project.tags.length > 0">
          <template #header>
            <h3 class="text-lg font-semibold text-secondary-900">{{ $t('projectDetail.tags') }}</h3>
          </template>
          <div class="flex flex-wrap gap-2">
            <span
              v-for="tag in project.tags"
              :key="tag"
              class="rounded-full bg-secondary-100 px-3 py-1 text-sm text-secondary-700"
            >
              #{{ tag }}
            </span>
          </div>
        </Card>

        <!-- Quick Actions -->
        <Card>
          <template #header>
            <h3 class="text-lg font-semibold text-secondary-900">{{ $t('projectDetail.quickActions') }}</h3>
          </template>
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <button
              @click="goToTasks"
              :disabled="project?.status === 'on-hold'"
              :class="[
                'flex items-center gap-3 rounded-lg border border-secondary-200 p-4 text-left transition-colors',
                project?.status === 'on-hold'
                  ? 'opacity-50 cursor-not-allowed'
                  : 'hover:bg-secondary-50'
              ]"
            >
              <div class="rounded-lg bg-primary-100 p-2">
                <svg class="h-6 w-6 text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="2"
                    d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"
                  />
                </svg>
              </div>
              <div>
                <p class="font-medium text-secondary-900">{{ $t('projectDetail.viewTasks') }}</p>
                <p class="text-sm text-secondary-600">{{ projectTasks.length }} {{ $t('projectDetail.tasksCount') }}</p>
              </div>
            </button>

            <button
              @click="goToGantt"
              class="flex items-center gap-3 rounded-lg border border-secondary-200 p-4 text-left transition-colors hover:bg-secondary-50"
            >
              <div class="rounded-lg bg-accent-100 p-2">
                <svg class="h-6 w-6 text-accent-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="2"
                    d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"
                  />
                </svg>
              </div>
              <div>
                <p class="font-medium text-secondary-900">{{ $t('projectDetail.ganttChart') }}</p>
                <p class="text-sm text-secondary-600">{{ $t('projectDetail.timelineView') }}</p>
              </div>
            </button>

            <button
              @click="createNewTask"
              :disabled="project?.status === 'on-hold'"
              :class="[
                'flex items-center gap-3 rounded-lg border border-secondary-200 p-4 text-left transition-colors',
                project?.status === 'on-hold'
                  ? 'opacity-50 cursor-not-allowed'
                  : 'hover:bg-secondary-50'
              ]"
            >
              <div class="rounded-lg bg-info-100 p-2">
                <svg class="h-6 w-6 text-info-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
                </svg>
              </div>
              <div>
                <p class="font-medium text-secondary-900">{{ $t('projectDetail.newTask') }}</p>
                <p class="text-sm text-secondary-600">{{ $t('projectDetail.addNewTask') }}</p>
              </div>
            </button>
          </div>
        </Card>
      </template>

      <!-- Empty State -->
      <Card v-else class="p-12">
        <div class="text-center">
          <svg class="mx-auto h-24 w-24 text-secondary-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
          </svg>
          <h3 class="mt-4 text-lg font-medium text-secondary-900">{{ $t('projectDetail.emptyState.title') }}</h3>
          <p class="mt-2 text-sm text-secondary-600">{{ $t('projectDetail.emptyState.message') }}</p>
          <Button variant="primary" class="mt-4" @click="goBack">{{ $t('projectDetail.emptyState.backToList') }}</Button>
        </div>
      </Card>

      <!-- Project Edit Modal -->
      <ProjectModal
        :open="modalOpen"
        :project="project"
        @close="closeModal"
        @save="handleSaveProject"
      />
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import MainLayout from '@/components/layout/MainLayout.vue';
import Card from '@/components/common/Card.vue';
import Button from '@/components/common/Button.vue';
import ProgressBar from '@/components/common/ProgressBar.vue';
import ProjectModal from '@/components/project/ProjectModal.vue';
import { useProjectStore } from '@/stores/project';
import { useTaskStore } from '@/stores/task';
import type { Project } from '@/types';
import { useUserStore } from '@/stores/user';
import dayjs from 'dayjs';

const route = useRoute();
const router = useRouter();
const { t } = useI18n();
const projectStore = useProjectStore();
const taskStore = useTaskStore();
const userStore = useUserStore();

const projectId = ref(route.params.id as string);
const modalOpen = ref(false);
const editingProject = ref(false);

onMounted(() => {
  if (route.params.id) {
    projectId.value = route.params.id as string;
    const project = projectStore.projectById(projectId.value);
    if (project) {
      projectStore.setCurrentProject(project);
      // 如果项目没有预估工时，自动计算并更新
      if (!project.estimatedHours || project.estimatedHours === 0) {
        const hours = calculateEstimatedHours();
        if (hours > 0) {
          projectStore.updateProject(projectId.value, { estimatedHours: hours });
        }
      }
    }
  }
  // 加载任务数据
  taskStore.loadTasks(projectId.value);
});

const project = computed(() => projectStore.projectById(projectId.value));
const projectTasks = computed(() => taskStore.tasksByProject(projectId.value));
const members = computed(() => {
  return project.value ? userStore.getUsersByIds(project.value.memberIds) : [];
});

// 计算并显示预估工时（基于所有没有父任务的任务的预估工时之和）
const displayEstimatedHours = computed(() => {
  // 如果项目有预估工时且大于0，直接使用项目的值
  if (project.value?.estimatedHours && project.value.estimatedHours > 0) {
    return project.value.estimatedHours;
  }

  // 否则计算所有没有父任务的预估工时之和
  const tasks = projectTasks.value;
  if (tasks.length === 0) {
    return 0;
  }

  // 只计算没有父任务的任务（顶层任务）
  const parentTasks = tasks.filter(task => !task.parentTaskId);

  const total = parentTasks.reduce((sum, task) => {
    return sum + (task.estimatedHours || 0);
  }, 0);

  return total;
});

// 监听项目变化，如果缺少预估工时则自动计算
watch(project, async (newProject) => {
  if (newProject && (!newProject.estimatedHours || newProject.estimatedHours === 0)) {
    // 基于所有没有父任务的预估工时之和计算
    const tasks = projectTasks.value;
    if (tasks.length > 0) {
      const parentTasks = tasks.filter(task => !task.parentTaskId);
      const total = parentTasks.reduce((sum, task) => {
        return sum + (task.estimatedHours || 0);
      }, 0);

      if (total > 0) {
        await projectStore.updateProject(projectId.value, { estimatedHours: total });
      }
    }
  }
});

// 计算项目进度（基于所有叶子任务中已完成任务的比例）
const computeProjectProgress = (): number => {
  const tasks = projectTasks.value;

  // 如果没有任务，进度为 0
  if (tasks.length === 0) {
    return 0;
  }

  // 只计算叶子任务（没有子任务的任务）
  // 通过检查是否有其他任务的 parentTaskId 指向当前任务来判断
  const leafTasks = tasks.filter(task => {
    return !tasks.some(otherTask => otherTask.parentTaskId === task.id);
  });

  if (leafTasks.length === 0) {
    return 0;
  }

  // 计算叶子任务中已完成任务的数量
  const completedTasks = leafTasks.filter(t => t.status === 'done').length;

  // 返回已完成任务的百分比
  return Math.round((completedTasks / leafTasks.length) * 100);
};

// 计算项目应该的状态（基于任务状态）
const computeProjectStatus = (): Project['status'] => {
  // 如果当前项目状态是已暂停，保持不变（除非手动修改）
  if (project.value?.status === 'on-hold') {
    return 'on-hold';
  }

  const tasks = projectTasks.value;

  // 如果没有任务，返回计划中
  if (tasks.length === 0) {
    return 'planning';
  }

  // 获取所有任务的状态
  const taskStatuses = tasks.map(t => t.status);

  // 如果所有任务都是已完成，项目状态为已完成
  if (taskStatuses.every(status => status === 'done')) {
    return 'completed';
  }

  // 如果有任务在进行中，项目状态为进行中
  if (taskStatuses.some(status => status === 'in-progress')) {
    return 'active';
  }

  // 如果所有任务都是待办，项目状态为计划中
  if (taskStatuses.every(status => status === 'todo')) {
    return 'planning';
  }

  // 默认返回进行中（混合状态）
  return 'active';
};

// 计算预估工时（基于所有没有父任务的任务的预估工时之和）
const calculateEstimatedHours = (): number => {
  const tasks = projectTasks.value;
  if (tasks.length === 0) {
    return 0;
  }

  // 只计算没有父任务的任务（顶层任务）
  const parentTasks = tasks.filter(task => !task.parentTaskId);

  const total = parentTasks.reduce((sum, task) => {
    return sum + (task.estimatedHours || 0);
  }, 0);

  return total;
};

// 监听任务变化，自动更新项目状态、进度和日期
watch(projectTasks, async () => {
  if (!project.value) return;

  const updateData: Partial<Project> = {};

  // 更新项目进度（基于已完成任务的比例）
  const newProgress = computeProjectProgress();
  if (newProgress !== project.value.progress) {
    updateData.progress = newProgress;
  }

  // 更新项目状态（如果不是已暂停或废弃状态）
  if (project.value.status !== 'on-hold' && project.value.status !== 'cancelled') {
    const newStatus = computeProjectStatus();
    if (newStatus !== project.value.status) {
      updateData.status = newStatus;
    }
  }

  // 根据任务自动计算开始日期和结束日期
  if (projectTasks.value.length > 0) {
    const startDates = projectTasks.value.map(t => new Date(t.startDate).getTime());
    const endDates = projectTasks.value.map(t => new Date(t.endDate).getTime());

    const earliestStart = new Date(Math.min(...startDates));
    const latestEnd = new Date(Math.max(...endDates));

    const newStartDate = earliestStart.toISOString().split('T')[0];
    const newEndDate = latestEnd.toISOString().split('T')[0];

    // 检查是否需要更新日期
    if (newStartDate !== project.value.startDate) {
      updateData.startDate = newStartDate;
    }
    if (newEndDate !== project.value.endDate) {
      updateData.endDate = newEndDate;
    }
  }

  // 如果有任务更新，重新计算预估工时（只计算顶层任务）
  const parentTasks = projectTasks.value.filter(task => !task.parentTaskId);
  const newEstimatedHours = parentTasks.reduce((sum, task) => {
    return sum + (task.estimatedHours || 0);
  }, 0);

  if (newEstimatedHours !== project.value.estimatedHours) {
    updateData.estimatedHours = newEstimatedHours;
  }

  // 如果有需要更新的数据，调用API更新
  if (Object.keys(updateData).length > 0) {
    await projectStore.updateProject(projectId.value, updateData);
  }
}, { deep: true });

const formattedDate = (date: string) => {
  // 使用本地化日期格式
  const locale = t('common.locale') || 'zh';
  if (locale === 'ko') {
    return dayjs(date).format('YYYY년 MM월 DD일');
  } else {
    return dayjs(date).format('YYYY年MM月DD日');
  }
};

const getStatusLabel = (status: string) => {
  const labelMap: Record<string, string> = {
    planning: 'projectStatus.planning',
    active: 'projectStatus.active',
    completed: 'projectStatus.completed',
    'on-hold': 'projectStatus.onHold',
    cancelled: 'projectStatus.cancelled'
  };
  const key = labelMap[status];
  return key ? t(key) : status;
};

const getPriorityLabel = (priority: string) => {
  const labelMap: Record<string, string> = {
    low: 'priorities.low',
    medium: 'priorities.medium',
    high: 'priorities.high',
    critical: 'priorities.critical',
    urgent: 'priorities.urgent'
  };
  const key = labelMap[priority];
  return key ? t(key) : priority;
};

const getRoleLabel = (role: string) => {
  // 标准化角色名称：处理下划线和连字符的兼容性
  const normalizedRole = role?.replace(/_/g, '-');

  const labelMap: Record<string, string> = {
    admin: 'roles.admin',
    'project-manager': 'roles.projectManager',
    member: 'roles.member'
  };
  const key = labelMap[normalizedRole];
  return key ? t(key) : role;
};

const goBack = () => {
  router.push('/projects');
};

const goToTasks = () => {
  router.push(`/projects/${projectId.value}/tasks`);
};

const goToGantt = () => {
  router.push(`/projects/${projectId.value}/gantt`);
};

const createNewTask = () => {
  // Navigate to task board page
  router.push(`/projects/${projectId.value}/tasks`);
};

const editProject = () => {
  editingProject.value = true;
  modalOpen.value = true;
};

const closeModal = () => {
  modalOpen.value = false;
  editingProject.value = false;
};

const handleSaveProject = async (projectData: Partial<Project>) => {
  if (editingProject.value) {
    // Update existing project
    await projectStore.updateProject(projectId.value, projectData);
  }
};

const deleteProject = () => {
  if (confirm(t('projectDetail.deleteConfirm'))) {
    projectStore.deleteProject(projectId.value);
    router.push('/projects');
  }
};
</script>
