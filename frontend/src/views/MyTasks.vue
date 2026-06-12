<template>
  <MainLayout>
    <div class="space-y-6">
      <!-- Page Header -->
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-secondary-900">{{ $t('myTasks.title', '我的任务') }}</h1>
          <p class="mt-1 text-sm text-secondary-600">
            {{ $t('myTasks.subtitle', '当前登录用户作为负责人的所有任务,跨项目聚合') }}
          </p>
        </div>
        <Button variant="secondary" @click="refresh">
          <svg class="mr-1.5 h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
          </svg>
          {{ $t('common.refresh', '刷新') }}
        </Button>
      </div>

      <!-- 顶部统计条:可点击作筛选 -->
      <div class="grid grid-cols-2 gap-4 md:grid-cols-4">
        <button
          v-for="card in statCards"
          :key="card.key"
          type="button"
          @click="toggleStatFilter(card.key)"
          :class="[
            'rounded-xl border p-4 text-left transition-all',
            statFilter === card.key
              ? 'border-primary-500 bg-primary-50 ring-2 ring-primary-200'
              : 'border-secondary-200 bg-white hover:border-secondary-300 hover:shadow-sm'
          ]"
        >
          <p class="text-xs font-medium uppercase tracking-wide text-secondary-500">{{ card.label }}</p>
          <p :class="['mt-2 text-2xl font-bold', card.color]">{{ card.value }}</p>
        </button>
      </div>

      <!-- 工具栏:筛选 / 排序 / 分组 -->
      <Card>
        <div class="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
          <div class="flex flex-wrap items-center gap-2">
            <Select v-model="statusFilter">
              <option value="all">{{ $t('myTasks.filter.allStatus', '全部状态') }}</option>
              <option value="todo">{{ $t('taskStatus.todo', '待办') }}</option>
              <option value="in-progress">{{ $t('taskStatus.inProgress', '进行中') }}</option>
              <option value="done">{{ $t('taskStatus.done', '已完成') }}</option>
            </Select>
            <Select v-model="priorityFilter">
              <option value="all">{{ $t('myTasks.filter.allPriority', '全部优先级') }}</option>
              <option value="urgent">{{ $t('priorities.urgent', '紧急') }}</option>
              <option value="high">{{ $t('priorities.high', '高') }}</option>
              <option value="medium">{{ $t('priorities.medium', '中') }}</option>
              <option value="low">{{ $t('priorities.low', '低') }}</option>
            </Select>
            <Select v-model="projectFilter">
              <option value="all">{{ $t('myTasks.filter.allProjects', '全部项目') }}</option>
              <option v-for="p in accessibleProjects" :key="p.id" :value="p.id">{{ p.name }}</option>
            </Select>
            <button
              v-if="statusFilter !== 'all' || priorityFilter !== 'all' || projectFilter !== 'all' || statFilter !== 'all'"
              type="button"
              @click="clearFilters"
              class="rounded-lg border border-secondary-300 px-3 py-1.5 text-sm text-secondary-700 hover:bg-secondary-50"
            >
              {{ $t('myTasks.filter.clear', '清除筛选') }}
            </button>
          </div>
          <div class="flex items-center gap-2">
            <Select v-model="sortBy" class="min-w-[10rem]">
              <option value="endDate">{{ $t('myTasks.sort.endDate', '按截止日期') }}</option>
              <option value="priority">{{ $t('myTasks.sort.priority', '按优先级') }}</option>
              <option value="project">{{ $t('myTasks.sort.project', '按项目') }}</option>
              <option value="status">{{ $t('myTasks.sort.status', '按状态') }}</option>
            </Select>
            <Select v-model="groupBy" class="min-w-[8rem]">
              <option value="none">{{ $t('myTasks.group.none', '平铺') }}</option>
              <option value="project">{{ $t('myTasks.group.project', '按项目分组') }}</option>
              <option value="status">{{ $t('myTasks.group.status', '按状态分组') }}</option>
            </Select>
          </div>
        </div>
      </Card>

      <!-- 任务列表 -->
      <div v-if="loading" class="py-12 text-center text-sm text-secondary-500">
        {{ $t('common.loading', '加载中...') }}
      </div>

      <div v-else-if="allTasks.length === 0" class="rounded-xl border border-dashed border-secondary-300 bg-secondary-50 p-12 text-center">
        <svg class="mx-auto h-12 w-12 text-secondary-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" />
        </svg>
        <p class="mt-4 text-sm text-secondary-500">{{ $t('myTasks.empty', '暂无任务') }}</p>
      </div>

      <template v-else>
        <!-- 平铺 - 树状 -->
        <div v-if="groupBy === 'none'" class="space-y-1">
          <template v-for="node in flatTreeNodes" :key="node.task.id">
            <div :style="{ paddingLeft: node.depth * 24 + 'px' }" class="tree-node-wrapper">
              <div class="flex items-start gap-1.5">
                <button
                  v-if="node.hasChildren"
                  type="button"
                  @click="toggleTreeExpand(node.task.id)"
                  class="tree-expand-btn"
                  :title="treeExpandedIds.has(node.task.id) ? '收起子任务' : '展开子任务'"
                >
                  <svg
                    class="h-3.5 w-3.5 transition-transform duration-200"
                    :class="{ 'rotate-90': treeExpandedIds.has(node.task.id) }"
                    fill="none" stroke="currentColor" viewBox="0 0 24 24"
                  >
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
                  </svg>
                </button>
                <span v-else class="inline-block w-5 flex-shrink-0" />
                <div v-if="node.depth > 0" class="tree-line-indicator" />
                <div class="flex-1 min-w-0">
                  <TaskCard :task="node.task" @click="openDetail" />
                </div>
              </div>
            </div>
          </template>
        </div>

        <!-- 按项目分组 - 树状 -->
        <div v-else-if="groupBy === 'project'" class="space-y-4">
          <Card v-for="group in groupedByProject" :key="group.projectId">
            <template #header>
              <div class="flex items-center justify-between">
                <h3 class="text-base font-semibold text-secondary-900">
                  <router-link
                    v-if="group.projectId !== '_none'"
                    :to="`/projects/${group.projectId}`"
                    class="hover:text-primary-600"
                  >
                    {{ group.projectName }}
                  </router-link>
                  <span v-else>{{ $t('myTasks.unassigned', '未关联项目') }}</span>
                </h3>
                <span class="text-xs text-secondary-500">{{ group.totalNodes }} {{ $t('myTasks.taskCount', '个任务') }}</span>
              </div>
            </template>
            <div class="space-y-1">
              <template v-for="node in group.nodes" :key="node.task.id">
                <div :style="{ paddingLeft: node.depth * 24 + 'px' }" class="tree-node-wrapper">
                  <div class="flex items-start gap-1.5">
                    <button
                      v-if="node.hasChildren"
                      type="button"
                      @click="toggleTreeExpand(node.task.id)"
                      class="tree-expand-btn"
                      :title="treeExpandedIds.has(node.task.id) ? '收起子任务' : '展开子任务'"
                    >
                      <svg
                        class="h-3.5 w-3.5 transition-transform duration-200"
                        :class="{ 'rotate-90': treeExpandedIds.has(node.task.id) }"
                        fill="none" stroke="currentColor" viewBox="0 0 24 24"
                      >
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
                      </svg>
                    </button>
                    <span v-else class="inline-block w-5 flex-shrink-0" />
                    <div v-if="node.depth > 0" class="tree-line-indicator" />
                    <div class="flex-1 min-w-0">
                      <TaskCard :task="node.task" @click="openDetail" />
                    </div>
                  </div>
                </div>
              </template>
            </div>
          </Card>
        </div>

        <!-- 按状态分组 - 树状 -->
        <div v-else class="space-y-4">
          <Card v-for="group in groupedByStatus" :key="group.status">
            <template #header>
              <div class="flex items-center justify-between">
                <h3 class="text-base font-semibold text-secondary-900">
                  <Badge :variant="group.variant">{{ group.label }}</Badge>
                </h3>
                <span class="text-xs text-secondary-500">{{ group.totalNodes }} {{ $t('myTasks.taskCount', '个任务') }}</span>
              </div>
            </template>
            <div class="space-y-1">
              <template v-for="node in group.nodes" :key="node.task.id">
                <div :style="{ paddingLeft: node.depth * 24 + 'px' }" class="tree-node-wrapper">
                  <div class="flex items-start gap-1.5">
                    <button
                      v-if="node.hasChildren"
                      type="button"
                      @click="toggleTreeExpand(node.task.id)"
                      class="tree-expand-btn"
                      :title="treeExpandedIds.has(node.task.id) ? '收起子任务' : '展开子任务'"
                    >
                      <svg
                        class="h-3.5 w-3.5 transition-transform duration-200"
                        :class="{ 'rotate-90': treeExpandedIds.has(node.task.id) }"
                        fill="none" stroke="currentColor" viewBox="0 0 24 24"
                      >
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
                      </svg>
                    </button>
                    <span v-else class="inline-block w-5 flex-shrink-0" />
                    <div v-if="node.depth > 0" class="tree-line-indicator" />
                    <div class="flex-1 min-w-0">
                      <TaskCard :task="node.task" @click="openDetail" />
                    </div>
                  </div>
                </div>
              </template>
            </div>
          </Card>
        </div>
      </template>
    </div>

    <!-- 任务详情抽屉 -->
    <Drawer
      :open="drawerOpen"
      @close="drawerOpen = false"
      size="2xl"
    >
      <TaskDetail
        v-if="selectedTask"
        :task="selectedTask"
        @edit="openEditModal"
        @delete="handleDelete"
        @createSubtask="handleCreateSubtask"
        @viewSubtask="viewSubtask"
        @close="drawerOpen = false"
      />
    </Drawer>

    <!-- 编辑/创建任务 Modal -->
    <TaskModal
      :open="modalOpen"
      :task="editingTask"
      :project-id="editingTask ? editingTask.projectId : currentProjectId"
      :parent-task-id="editingTask?.parentTaskId || currentParentTaskId || undefined"
      :parent-task-name="editingTask?.parentTaskId ? parentTaskName : currentParentTaskName"
      @close="closeEditModal"
      @save="handleSave"
    />
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import dayjs from 'dayjs';
import MainLayout from '@/components/layout/MainLayout.vue';
import Card from '@/components/common/Card.vue';
import Button from '@/components/common/Button.vue';
import Select from '@/components/common/Select.vue';
import Badge from '@/components/common/Badge.vue';
import TaskCard from '@/components/task/TaskCard.vue';
import TaskDetail from '@/components/task/TaskDetail.vue';
import TaskModal from '@/components/task/TaskModal.vue';
import Drawer from '@/components/common/Drawer.vue';
import apiService from '@/services/api';
import { useProjectStore } from '@/stores/project';
import { useTaskStore } from '@/stores/task';
import { useUserStore } from '@/stores/user';
import type { Task, Project } from '@/types';

const { t } = useI18n();
const projectStore = useProjectStore();
const taskStore = useTaskStore();
const userStore = useUserStore();

// ===== 数据 =====
const allTasks = ref<Task[]>([]);
const accessibleProjects = ref<Project[]>([]);
const loading = ref(false);
const drawerOpen = ref(false);
const selectedTask = ref<Task | null>(null);
const modalOpen = ref(false);
const editingTask = ref<Task | null>(null);
const isCreatingSubtask = ref(false);
const currentParentTaskId = ref<string | null>(null);
const currentParentTaskName = ref<string>('');
const currentProjectId = ref<string>('');
const parentTaskNameCache = ref<Record<string, string>>({});
const parentTaskName = computed<string | undefined>(() => {
  const pid = editingTask.value?.parentTaskId;
  if (!pid) return undefined;
  return parentTaskNameCache.value[pid];
});

// ===== 筛选 / 排序 / 分组 =====
const statusFilter = ref<string>('all');
const priorityFilter = ref<string>('all');
const projectFilter = ref<string>('all');
const statFilter = ref<string>('all');
const sortBy = ref<string>('endDate');
const groupBy = ref<'none' | 'project' | 'status'>('none');

// ===== 树状结构状态 =====
const treeExpandedIds = ref<Set<string>>(new Set());

// ----- 树工具函数 -----

interface FlatTreeNode {
  task: Task;
  depth: number;
  hasChildren: boolean;
}

/** 构建 childrenMap: parentTaskId → children（按标题排序） */
function buildChildrenMap(tasks: Task[]): Map<string, Task[]> {
  const map = new Map<string, Task[]>();
  for (const t of tasks) {
    const key = t.parentTaskId || '__root__';
    if (!map.has(key)) map.set(key, []);
    map.get(key)!.push(t);
  }
  for (const [, children] of map) {
    children.sort((a, b) => a.title.localeCompare(b.title));
  }
  return map;
}

/** 在给定 tasks 集合内查找根节点 */
function findRootsInSet(tasks: Task[], taskIdSet: Set<string>): Task[] {
  return tasks.filter(t => {
    if (!t.parentTaskId) return true;
    return !taskIdSet.has(t.parentTaskId);
  });
}

/** 平铺树：先序遍历，展开的节点递归子节点 */
function flattenTree(
  roots: Task[],
  childrenMap: Map<string, Task[]>,
  expandedIds: Set<string>
): FlatTreeNode[] {
  const result: FlatTreeNode[] = [];

  function walk(task: Task, depth: number) {
    const children = childrenMap.get(task.id) || [];
    result.push({ task, depth, hasChildren: children.length > 0 });
    if (expandedIds.has(task.id) && children.length > 0) {
      for (const child of children) {
        walk(child, depth + 1);
      }
    }
  }

  for (const root of roots) {
    walk(root, 0);
  }

  return result;
}

function toggleTreeExpand(taskId: string) {
  const next = new Set(treeExpandedIds.value);
  if (next.has(taskId)) {
    next.delete(taskId);
  } else {
    next.add(taskId);
  }
  treeExpandedIds.value = next;
}

// ----- 筛选 & 基础 computed -----

/** 当前用户的任务ID集合（用于统计卡计数，仅统计用户自己的任务） */
const myTaskIds = computed(() => {
  const uid = userStore.currentUser?.id;
  if (!uid) return new Set<string>();
  return new Set(allTasks.value.filter(t => t.assigneeId === uid).map(t => t.id));
});

/** 按筛选条件过滤后的任务 */
const filteredTasks = computed(() => {
  let list = [...allTasks.value];

  if (statusFilter.value !== 'all') {
    list = list.filter(t => t.status === statusFilter.value);
  }
  if (priorityFilter.value !== 'all') {
    list = list.filter(t => t.priority === priorityFilter.value);
  }
  if (projectFilter.value !== 'all') {
    list = list.filter(t => t.projectId === projectFilter.value);
  }
  if (statFilter.value === 'due-soon') {
    const now = dayjs();
    list = list.filter(t => {
      if (!t.endDate) return false;
      if (t.status === 'done') return false;
      const diff = dayjs(t.endDate).diff(now, 'day');
      return diff >= 0 && diff <= 3;
    });
  } else if (statFilter.value !== 'all') {
    list = list.filter(t => t.status === statFilter.value);
  }

  return list;
});

// ----- 平铺模式 -----

const flatTreeNodes = computed(() => {
  if (allTasks.value.length === 0) return [];
  const filtered = filteredTasks.value;
  const filteredIds = new Set(filtered.map(t => t.id));
  const roots = findRootsInSet(filtered, filteredIds);
  roots.sort((a, b) => a.title.localeCompare(b.title));
  const cMap = buildChildrenMap(filtered);
  return flattenTree(roots, cMap, treeExpandedIds.value);
});

// ----- 按项目分组 -----

const projectById = computed(() => {
  const m = new Map<string, Project>();
  for (const p of accessibleProjects.value) m.set(p.id, p);
  return m;
});

const groupedByProject = computed(() => {
  const map = new Map<string, { projectId: string; projectName: string; nodes: FlatTreeNode[]; totalNodes: number }>();
  const filtered = filteredTasks.value;
  const cMap = buildChildrenMap(filtered);

  // 按项目归类
  const byProject = new Map<string, Task[]>();
  for (const t of filtered) {
    const key = t.projectId || '_none';
    if (!byProject.has(key)) byProject.set(key, []);
    byProject.get(key)!.push(t);
  }

  for (const [projectId, projectTasks] of byProject) {
    const projectIds = new Set(projectTasks.map(t => t.id));
    const roots = findRootsInSet(projectTasks, projectIds);
    roots.sort((a, b) => a.title.localeCompare(b.title));
    const nodes = flattenTree(roots, cMap, treeExpandedIds.value);
    const p = projectId !== '_none' ? projectById.value.get(projectId) : null;
    map.set(projectId, {
      projectId,
      projectName: p?.name || projectId,
      nodes,
      totalNodes: nodes.length
    });
  }

  return Array.from(map.values());
});

// ----- 按状态分组 -----

const groupedByStatus = computed(() => {
  const statusOrder: Array<{ status: Task['status']; label: string; variant: 'default' | 'primary' | 'success' }> = [
    { status: 'todo', label: t('taskStatus.todo', '待办'), variant: 'default' },
    { status: 'in-progress', label: t('taskStatus.inProgress', '进行中'), variant: 'primary' },
    { status: 'done', label: t('taskStatus.done', '已完成'), variant: 'success' }
  ];

  const filtered = filteredTasks.value;
  const filteredIds = new Set(filtered.map(t => t.id));
  const cMap = buildChildrenMap(filtered);

  return statusOrder.map(g => {
    // 某个状态下的根节点：属于该状态 AND (无parent 或 parent不在filtered中)
    const statusTasks = filtered.filter(t => t.status === g.status);
    const roots = findRootsInSet(statusTasks, filteredIds);
    roots.sort((a, b) => a.title.localeCompare(b.title));
    const nodes = flattenTree(roots, cMap, treeExpandedIds.value);
    return {
      ...g,
      nodes,
      totalNodes: nodes.length
    };
  }).filter(g => g.nodes.length > 0);
});

// ===== 加载 =====
const loadData = async () => {
  loading.value = true;
  try {
    const [treeData, projects] = await Promise.all([
      apiService.getMyTaskTree(),
      projectStore.projects.length > 0
        ? Promise.resolve(projectStore.projects)
        : apiService.getProjects()
    ]);
    allTasks.value = treeData;
    accessibleProjects.value = projects;

    // 首次加载时自动展开根节点
    if (treeExpandedIds.value.size === 0 && treeData.length > 0) {
      const filteredIds = new Set(treeData.map(t => t.id));
      const rootIds = new Set<string>();
      for (const t of treeData) {
        if (!t.parentTaskId || !filteredIds.has(t.parentTaskId)) {
          rootIds.add(t.id);
        }
      }
      treeExpandedIds.value = rootIds;
    }
  } catch (err) {
    console.error('加载我的任务失败', err);
    allTasks.value = [];
  } finally {
    loading.value = false;
  }
};

const refresh = () => {
  treeExpandedIds.value = new Set();
  loadData();
};

onMounted(() => {
  loadData();
});

// ===== 顶部统计（仅统计用户自己的任务） =====

const myOwnTasks = computed(() =>
  allTasks.value.filter(t => myTaskIds.value.has(t.id))
);

/** 有子任务的任务ID集合——父任务的状态/日期由子任务自动推算，不应独立计入统计 */
const parentTaskIds = computed(() => {
  const ids = new Set<string>();
  for (const t of allTasks.value) {
    if (t.parentTaskId) ids.add(t.parentTaskId);
  }
  return ids;
});

/** 用户自己的、且非父任务的叶子任务（用于统计卡计数） */
const myOwnLeafTasks = computed(() =>
  myOwnTasks.value.filter(t => !parentTaskIds.value.has(t.id))
);

const dueSoonTasks = computed(() => {
  const now = dayjs();
  return myOwnLeafTasks.value.filter(t => {
    if (!t.endDate) return false;
    if (t.status === 'done') return false;
    const diff = dayjs(t.endDate).diff(now, 'day');
    return diff >= 0 && diff <= 3;
  });
});

const statCards = computed(() => [
  {
    key: 'todo',
    label: t('taskStatus.todo', '待办'),
    value: myOwnLeafTasks.value.filter(t => t.status === 'todo').length,
    color: 'text-secondary-700'
  },
  {
    key: 'in-progress',
    label: t('taskStatus.inProgress', '进行中'),
    value: myOwnLeafTasks.value.filter(t => t.status === 'in-progress').length,
    color: 'text-primary-600'
  },
  {
    key: 'done',
    label: t('taskStatus.done', '已完成'),
    value: myOwnLeafTasks.value.filter(t => t.status === 'done').length,
    color: 'text-success-600'
  },
  {
    key: 'due-soon',
    label: t('myTasks.stat.dueSoon', '即将到期(≤3天)'),
    value: dueSoonTasks.value.length,
    color: 'text-warning-600'
  }
]);

const toggleStatFilter = (key: string) => {
  if (statFilter.value === key) {
    statFilter.value = 'all';
  } else {
    statFilter.value = key;
  }
};

const clearFilters = () => {
  statusFilter.value = 'all';
  priorityFilter.value = 'all';
  projectFilter.value = 'all';
  statFilter.value = 'all';
};

// ===== 抽屉 / 编辑 =====
const openDetail = (task: Task) => {
  selectedTask.value = task;
  drawerOpen.value = true;
};

const ensureParentTaskLoaded = async (parentTaskId: string) => {
  if (!parentTaskId) return;
  if (parentTaskNameCache.value[parentTaskId]) return;
  const local = allTasks.value.find(t => t.id === parentTaskId);
  if (local) {
    parentTaskNameCache.value[parentTaskId] = local.title;
    return;
  }
  try {
    const task = await apiService.getTask(parentTaskId);
    parentTaskNameCache.value = {
      ...parentTaskNameCache.value,
      [parentTaskId]: task.title || ''
    };
  } catch (err) {
    console.warn('加载父任务失败:', parentTaskId, err);
  }
};

const openEditModal = async (task: Task) => {
  if (task.parentTaskId) {
    await ensureParentTaskLoaded(task.parentTaskId);
  }
  editingTask.value = task;
  modalOpen.value = true;
  drawerOpen.value = false;
};

const closeEditModal = () => {
  modalOpen.value = false;
  editingTask.value = null;
  isCreatingSubtask.value = false;
  currentParentTaskId.value = null;
  currentParentTaskName.value = '';
  currentProjectId.value = '';
};

const taskSaving = ref(false);
const handleSave = async (taskData: Partial<Task>) => {
  if (taskSaving.value) return;
  taskSaving.value = true;
  try {
    if (editingTask.value) {
      await taskStore.updateTask(editingTask.value.id, {
        ...taskData,
        status: taskData.status || editingTask.value.status
      });
    } else if (isCreatingSubtask.value && currentParentTaskId.value) {
      await taskStore.createTask({
        ...taskData,
        projectId: taskData.projectId || currentProjectId.value,
        parentTaskId: currentParentTaskId.value,
        status: taskData.status || 'todo'
      });
    } else {
      return;
    }
  } catch (err) {
    console.error('保存任务失败', err);
  } finally {
    taskSaving.value = false;
    closeEditModal();
    await loadData();
  }
};

const handleDelete = async (_task: Task) => {
  drawerOpen.value = false;
  await loadData();
};

const handleCreateSubtask = (parentId: string) => {
  const parentTask = selectedTask.value;
  if (!parentTask || parentTask.id !== parentId) return;
  drawerOpen.value = false;
  setTimeout(() => {
    isCreatingSubtask.value = true;
    currentParentTaskId.value = parentId;
    currentParentTaskName.value = parentTask.title;
    currentProjectId.value = parentTask.projectId;
    editingTask.value = null;
    modalOpen.value = true;
  }, 200);
};

const viewSubtask = (_subtask: Task) => {
  // 抽屉内部已切到子任务的详情
};
</script>

<style scoped>
/* === 树状视图样式 === */
.tree-expand-btn {
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  color: #6b7280;
  cursor: pointer;
  background: transparent;
  border: none;
  margin-top: 12px;
  transition: all 0.15s ease;
}
.tree-expand-btn:hover {
  background: #eff6ff;
  color: #2563eb;
}

.tree-line-indicator {
  flex-shrink: 0;
  width: 16px;
  height: 100%;
  min-height: 60px;
  margin-top: 4px;
  border-left: 2px solid #e5e7eb;
  border-bottom: 2px solid #e5e7eb;
  border-bottom-left-radius: 6px;
}
</style>
