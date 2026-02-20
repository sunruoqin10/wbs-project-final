<template>
  <MainLayout>
    <div class="space-y-6">
      <!-- Page Header -->
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-4">
          <button @click="goBack" class="rounded-lg p-2 hover:bg-secondary-100">
            <svg class="h-5 w-5 text-secondary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
            </svg>
          </button>
          <div>
            <h1 class="text-2xl font-bold text-secondary-900">{{ project?.name }}</h1>
            <p class="mt-1 text-sm text-secondary-600">{{ $t('taskBoard.title') }}</p>
          </div>
        </div>
        <Button variant="primary" @click="openCreateModal">
          <svg class="mr-2 h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          {{ $t('taskBoard.newTask') }}
        </Button>
      </div>

      <!-- 延期筛选器 -->
      <div class="flex items-center gap-4 rounded-lg bg-secondary-50 p-4">
        <span class="text-sm font-medium text-secondary-700">延期筛选：</span>
        <label class="flex items-center gap-2 cursor-pointer">
          <input type="checkbox" v-model="showDelayedOnly" class="rounded border-secondary-300 text-primary-600 focus:ring-primary-500" />
          <span class="text-sm text-secondary-600">仅显示延期任务</span>
        </label>
        <Select v-model="delayFilter" class="w-40">
          <option value="all">全部</option>
          <option value="delayed">已延期</option>
          <option value="critical">严重延期（≥7天）</option>
          <option value="warning">中度延期（3-6天）</option>
        </Select>
      </div>

      <!-- Task Board -->
      <div class="grid grid-cols-1 gap-6 md:grid-cols-3">
        <!-- Todo Column -->
        <div class="rounded-xl bg-secondary-50 p-4">
          <div class="mb-4 flex items-center justify-between">
            <h3 class="font-semibold text-secondary-900">{{ $t('taskBoard.columns.todo') }}</h3>
            <Badge variant="default">{{ todoTasks.length }}</Badge>
          </div>
          <draggable
            v-model="todoTasksList"
            :animation="200"
            item-key="id"
            class="space-y-3 min-h-[200px]"
            @change="onDragChange('todo', $event)"
          >
            <template #item="{ element: task }">
              <div>
                <!-- 任务卡片 -->
                <TaskCard
                  :task="task"
                  :summary="getSubtaskSummary(task.id)"
                  :is-expanded="isTaskExpanded(task.id)"
                  @click="openTaskDetail(task)"
                  @toggle-expand="toggleExpand(task.id)"
                />

                <!-- 子任务面板（展开时显示） -->
                <SubtaskPanel
                  v-if="isTaskExpanded(task.id) && hasSubtasks(task.id)"
                  :subtasks="getDirectSubtasks(task.id)"
                  @click-subtask="openTaskDetail"
                  @add-subtask="openCreateSubtaskModal(task.id)"
                />
              </div>
            </template>
          </draggable>
          <button
            @click="openCreateModal('todo')"
            class="mt-3 flex w-full items-center justify-center rounded-lg border-2 border-dashed border-secondary-300 py-3 text-sm font-medium text-secondary-600 transition-colors hover:border-secondary-400 hover:bg-secondary-100"
          >
            <svg class="mr-2 h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
            </svg>
            {{ $t('taskBoard.addTask') }}
          </button>
        </div>

        <!-- In Progress Column -->
        <div class="rounded-xl bg-secondary-50 p-4">
          <div class="mb-4 flex items-center justify-between">
            <h3 class="font-semibold text-secondary-900">{{ $t('taskBoard.columns.inProgress') }}</h3>
            <Badge variant="primary">{{ inProgressTasksList.length }}</Badge>
          </div>
          <draggable
            v-model="inProgressTasksList"
            :animation="200"
            item-key="id"
            class="space-y-3 min-h-[200px]"
            @change="onDragChange('in-progress', $event)"
          >
            <template #item="{ element: task }">
              <div>
                <!-- 任务卡片 -->
                <TaskCard
                  :task="task"
                  :summary="getSubtaskSummary(task.id)"
                  :is-expanded="isTaskExpanded(task.id)"
                  @click="openTaskDetail(task)"
                  @toggle-expand="toggleExpand(task.id)"
                />

                <!-- 子任务面板（展开时显示） -->
                <SubtaskPanel
                  v-if="isTaskExpanded(task.id) && hasSubtasks(task.id)"
                  :subtasks="getDirectSubtasks(task.id)"
                  @click-subtask="openTaskDetail"
                  @add-subtask="openCreateSubtaskModal(task.id)"
                />
              </div>
            </template>
          </draggable>
          <button
            @click="openCreateModal('in-progress')"
            class="mt-3 flex w-full items-center justify-center rounded-lg border-2 border-dashed border-secondary-300 py-3 text-sm font-medium text-secondary-600 transition-colors hover:border-secondary-400 hover:bg-secondary-100"
          >
            <svg class="mr-2 h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
            </svg>
            {{ $t('taskBoard.addTask') }}
          </button>
        </div>

        <!-- Done Column -->
        <div class="rounded-xl bg-secondary-50 p-4">
          <div class="mb-4 flex items-center justify-between">
            <h3 class="font-semibold text-secondary-900">{{ $t('taskBoard.columns.done') }}</h3>
            <Badge variant="success">{{ doneTasksList.length }}</Badge>
          </div>
          <draggable
            v-model="doneTasksList"
            :animation="200"
            item-key="id"
            class="space-y-3 min-h-[200px]"
            @change="onDragChange('done', $event)"
          >
            <template #item="{ element: task }">
              <div>
                <!-- 任务卡片 -->
                <TaskCard
                  :task="task"
                  :summary="getSubtaskSummary(task.id)"
                  :is-expanded="isTaskExpanded(task.id)"
                  @click="openTaskDetail(task)"
                  @toggle-expand="toggleExpand(task.id)"
                />

                <!-- 子任务面板（展开时显示） -->
                <SubtaskPanel
                  v-if="isTaskExpanded(task.id) && hasSubtasks(task.id)"
                  :subtasks="getDirectSubtasks(task.id)"
                  @click-subtask="openTaskDetail"
                  @add-subtask="openCreateSubtaskModal(task.id)"
                />
              </div>
            </template>
          </draggable>
          <button
            @click="openCreateModal('done')"
            class="mt-3 flex w-full items-center justify-center rounded-lg border-2 border-dashed border-secondary-300 py-3 text-sm font-medium text-secondary-600 transition-colors hover:border-secondary-400 hover:bg-secondary-100"
          >
            <svg class="mr-2 h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
            </svg>
            {{ $t('taskBoard.addTask') }}
          </button>
        </div>
      </div>

      <!-- Task Modal -->
      <TaskModal
        :open="modalOpen"
        :task="editingTask"
        :project-id="projectId"
        :initial-status="defaultStatus"
        :show-parent-task-selector="isCreatingSubtask"
        :parent-task-id="currentParentTaskId"
        @close="closeModal"
        @save="handleSaveTask"
      />

      <!-- Task Detail Drawer -->
      <Drawer
        :open="drawerOpen"
        :title="$t('taskBoard.taskDetail')"
        @close="drawerOpen = false"
      >
        <TaskDetail
          v-if="selectedTask"
          :task="selectedTask"
          @edit="openEditModal"
          @delete="handleDeleteTask"
          @createSubtask="openCreateSubtaskModal"
          @viewSubtask="handleViewSubtask"
        />
      </Drawer>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import draggable from 'vuedraggable';
import MainLayout from '@/components/layout/MainLayout.vue';
import TaskCard from '@/components/task/TaskCard.vue';
import TaskModal from '@/components/task/TaskModal.vue';
import TaskDetail from '@/components/task/TaskDetail.vue';
import SubtaskPanel from '@/components/task/SubtaskPanel.vue';
import Drawer from '@/components/common/Drawer.vue';
import Button from '@/components/common/Button.vue';
import Badge from '@/components/common/Badge.vue';
import Select from '@/components/common/Select.vue';
import { useProjectStore } from '@/stores/project';
import { useTaskStore } from '@/stores/task';
import type { Task } from '@/types';

const route = useRoute();
const router = useRouter();
const { t } = useI18n();
const projectStore = useProjectStore();
const taskStore = useTaskStore();

const projectId = route.params.id as string;
const modalOpen = ref(false);
const drawerOpen = ref(false);
const selectedTask = ref<Task | null>(null);
const editingTask = ref<Task | null>(null);
const defaultStatus = ref<Task['status'] | undefined>();
const isCreatingSubtask = ref(false); // 是否正在创建子任务
const currentParentTaskId = ref<string | null>(null); // 当前父任务ID（创建子任务时使用）

// 展开状态管理
const expandedTasks = ref<Set<string>>(new Set());

// 延期筛选
const showDelayedOnly = ref(false);
const delayFilter = ref('all');

onMounted(async () => {
  // 加载当前项目
  const project = projectStore.projectById(projectId);
  if (project) {
    projectStore.setCurrentProject(project);
  }

  // 加载任务数据
  await taskStore.loadTasks(projectId);
});

const project = computed(() => projectStore.projectById(projectId));
const projectTasks = computed(() => taskStore.tasksByProject(projectId));

// 应用延期筛选
const filteredTasks = computed(() => {
  let tasks = projectTasks.value;

  if (showDelayedOnly.value || delayFilter.value !== 'all') {
    tasks = tasks.filter(task => {
      const isDelayed = task.isDelayed || (task.delayedDays || 0) > 0;
      if (!isDelayed) return false;

      const days = task.delayedDays || 0;
      switch (delayFilter.value) {
        case 'critical': return days >= 7;
        case 'warning': return days >= 3 && days < 7;
        default: return true;
      }
    });
  }

  return tasks;
});

// 只获取顶级任务（没有父任务的任务）
const topLevelTasks = computed(() => {
  return filteredTasks.value.filter(t => t && !t.parentTaskId);
});

// Draggable lists
const todoTasksList = computed({
  get: () => topLevelTasks.value.filter(t => t && t.status === 'todo'),
  set: (value) => updateTaskOrders(value, 'todo')
});

const inProgressTasksList = computed({
  get: () => topLevelTasks.value.filter(t => t && t.status === 'in-progress'),
  set: (value) => updateTaskOrders(value, 'in-progress')
});

const doneTasksList = computed({
  get: () => topLevelTasks.value.filter(t => t && t.status === 'done'),
  set: (value) => updateTaskOrders(value, 'done')
});

const todoTasks = computed(() => topLevelTasks.value.filter(t => t && t.status === 'todo'));
const inProgressTasks = computed(() => topLevelTasks.value.filter(t => t && t.status === 'in-progress'));
const doneTasks = computed(() => topLevelTasks.value.filter(t => t && t.status === 'done'));

// Handle drag changes
const onDragChange = (status: Task['status'], event: any) => {
  if (event.added) {
    const task = event.added.element;
    updateTaskStatus(task.id, status);
  }
};

// Update task orders within same column
const updateTaskOrders = (tasks: Task[], status: Task['status']) => {
  // In a real app, you would update the order in the backend
  console.log(`Updated order for ${status} tasks:`, tasks.map(t => t.id));
};

// 新增：辅助方法
const hasSubtasks = (taskId: string) => {
  return taskStore.getSubtasks(taskId).length > 0;
};

const getSubtaskSummary = (taskId: string) => {
  return taskStore.getSubtaskSummary(taskId);
};

const getDirectSubtasks = (taskId: string) => {
  return taskStore.getSubtasks(taskId);
};

const isTaskExpanded = (taskId: string) => {
  return expandedTasks.value.has(taskId);
};

const toggleExpand = (taskId: string) => {
  if (expandedTasks.value.has(taskId)) {
    expandedTasks.value.delete(taskId);
  } else {
    expandedTasks.value.add(taskId);
  }
  // 触发响应式更新
  expandedTasks.value = new Set(expandedTasks.value);
};

// Update task status
const updateTaskStatus = (taskId: string, status: Task['status']) => {
  taskStore.updateTaskStatus(taskId, status);
};

// Open create modal
const openCreateModal = (status?: Task['status']) => {
  defaultStatus.value = status;
  editingTask.value = null;
  isCreatingSubtask.value = false;
  modalOpen.value = true;
};

// Open create subtask modal
const openCreateSubtaskModal = (parentTaskId: string) => {
  defaultStatus.value = 'todo';
  editingTask.value = null;
  isCreatingSubtask.value = true;
  currentParentTaskId.value = parentTaskId;
  // 创建子任务时，需要设置 parentTaskId
  // 我们可以通过在 TaskModal 组件中添加一个 prop 来传递
  // 或者直接在保存时处理
  // 这里使用一个临时变量来存储 parentTaskId
  (window as any).__creatingSubtaskParentId = parentTaskId;
  modalOpen.value = true;
};

// Handle view subtask
const handleViewSubtask = (subtask: Task) => {
  // 关闭当前抽屉，打开子任务的详情
  drawerOpen.value = false;
  setTimeout(() => {
    selectedTask.value = subtask;
    drawerOpen.value = true;
  }, 100);
};

// Open edit modal
const openEditModal = (task: Task) => {
  editingTask.value = task;
  modalOpen.value = true;
  // 关闭抽屉，避免遮挡编辑弹窗
  drawerOpen.value = false;
};

// Close modal
const closeModal = () => {
  modalOpen.value = false;
  editingTask.value = null;
  defaultStatus.value = undefined;
  isCreatingSubtask.value = false;
  currentParentTaskId.value = null;
  // Clear the temporary parent task ID variable
  delete (window as any).__creatingSubtaskParentId;
};

// Open task detail drawer
const openTaskDetail = (task: Task) => {
  // 从 store 中获取最新任务数据，确保显示的是更新后的数据
  const latestTask = taskStore.getTaskById(task.id);
  selectedTask.value = latestTask || task;
  drawerOpen.value = true;
};

// Handle delete task
const handleDeleteTask = async (task: Task) => {
  console.log('TaskBoard: 准备删除任务', task);
  try {
    await taskStore.deleteTask(task.id);
    drawerOpen.value = false;
    console.log('TaskBoard: 任务删除成功，抽屉已关闭');
  } catch (error) {
    console.error('TaskBoard: 删除任务失败', error);
    alert(t('taskBoard.messages.deleteFailed') + (error as Error).message);
  }
};

// Handle save task
const handleSaveTask = async (taskData: Partial<Task>) => {
  if (editingTask.value) {
    // Update existing task
    await taskStore.updateTask(editingTask.value.id, {
      ...taskData,
      status: taskData.status || editingTask.value.status
    });
  } else {
    // Check if creating a subtask
    const parentTaskId = (window as any).__creatingSubtaskParentId;
    if (parentTaskId) {
      // Creating a subtask
      const newTask = await taskStore.createTask({
        ...taskData,
        projectId: taskData.projectId || projectId,
        status: defaultStatus.value || taskData.status || 'todo',
        parentTaskId: parentTaskId
      });
      console.log('Created subtask:', newTask);
      // Clear the temporary variable
      delete (window as any).__creatingSubtaskParentId;
    } else {
      // Create new task - ensure projectId is set
      const newTask = await taskStore.createTask({
        ...taskData,
        projectId: taskData.projectId || projectId,
        status: defaultStatus.value || taskData.status || 'todo'
      });
      console.log('Created task:', newTask);
    }
  }

  // Close modal after saving
  closeModal();
};

const goBack = () => {
  router.push('/projects');
};
</script>

