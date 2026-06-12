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

      <div v-else-if="filteredAndSortedTasks.length === 0" class="rounded-xl border border-dashed border-secondary-300 bg-secondary-50 p-12 text-center">
        <svg class="mx-auto h-12 w-12 text-secondary-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" />
        </svg>
        <p class="mt-4 text-sm text-secondary-500">{{ $t('myTasks.empty', '暂无任务') }}</p>
      </div>

      <template v-else>
        <!-- 平铺 -->
        <div v-if="groupBy === 'none'" class="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-3">
          <TaskCard
            v-for="task in filteredAndSortedTasks"
            :key="task.id"
            :task="task"
            @click="openDetail"
          />
        </div>

        <!-- 按项目分组 -->
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
                <span class="text-xs text-secondary-500">{{ group.tasks.length }} {{ $t('myTasks.taskCount', '个任务') }}</span>
              </div>
            </template>
            <div class="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-3">
              <TaskCard
                v-for="task in group.tasks"
                :key="task.id"
                :task="task"
                @click="openDetail"
              />
            </div>
          </Card>
        </div>

        <!-- 按状态分组 -->
        <div v-else class="space-y-4">
          <Card v-for="group in groupedByStatus" :key="group.status">
            <template #header>
              <div class="flex items-center justify-between">
                <h3 class="text-base font-semibold text-secondary-900">
                  <Badge :variant="group.variant">{{ group.label }}</Badge>
                </h3>
                <span class="text-xs text-secondary-500">{{ group.tasks.length }} {{ $t('myTasks.taskCount', '个任务') }}</span>
              </div>
            </template>
            <div class="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-3">
              <TaskCard
                v-for="task in group.tasks"
                :key="task.id"
                :task="task"
                @click="openDetail"
              />
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

    <!-- 编辑任务 Modal -->
    <TaskModal
      v-if="editingTask"
      :open="modalOpen"
      :task="editingTask"
      :project-id="editingTask.projectId"
      :parent-task-id="editingTask.parentTaskId || undefined"
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
import type { Task, Project } from '@/types';

const { t } = useI18n();
const projectStore = useProjectStore();

// ===== 数据 =====
const allTasks = ref<Task[]>([]);
const accessibleProjects = ref<Project[]>([]);
const loading = ref(false);
const drawerOpen = ref(false);
const selectedTask = ref<Task | null>(null);
const modalOpen = ref(false);
const editingTask = ref<Task | null>(null);

// ===== 筛选 / 排序 / 分组 =====
const statusFilter = ref<string>('all');
const priorityFilter = ref<string>('all');
const projectFilter = ref<string>('all');
const statFilter = ref<string>('all'); // 顶部统计卡筛选:all / todo / in-progress / done / due-soon
const sortBy = ref<string>('endDate');
const groupBy = ref<'none' | 'project' | 'status'>('none');

// ===== 加载 =====
const loadData = async () => {
  loading.value = true;
  try {
    const [myTasks, projects] = await Promise.all([
      apiService.getMyTasks(),
      // 任务可能引用 projectId 列表里没的项目,这里也单独拉项目列表供分组
      projectStore.projects.length > 0
        ? Promise.resolve(projectStore.projects)
        : apiService.getProjects()
    ]);
    allTasks.value = myTasks;
    accessibleProjects.value = projects;
  } catch (err) {
    console.error('加载我的任务失败', err);
    allTasks.value = [];
  } finally {
    loading.value = false;
  }
};

const refresh = () => {
  loadData();
};

onMounted(() => {
  loadData();
});

// ===== 顶部统计 =====
const PRIORITY_ORDER: Record<string, number> = { urgent: 0, high: 1, medium: 2, low: 3 };

const dueSoonTasks = computed(() => {
  const now = dayjs();
  return allTasks.value.filter(t => {
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
    value: allTasks.value.filter(t => t.status === 'todo').length,
    color: 'text-secondary-700'
  },
  {
    key: 'in-progress',
    label: t('taskStatus.inProgress', '进行中'),
    value: allTasks.value.filter(t => t.status === 'in-progress').length,
    color: 'text-primary-600'
  },
  {
    key: 'done',
    label: t('taskStatus.done', '已完成'),
    value: allTasks.value.filter(t => t.status === 'done').length,
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
  // 同样的 stat 再次点击取消,否则切到该 stat
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

// ===== 主筛选 =====
const filteredAndSortedTasks = computed(() => {
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

  // 排序
  list.sort((a, b) => {
    if (sortBy.value === 'priority') {
      const pa = PRIORITY_ORDER[a.priority] ?? 99;
      const pb = PRIORITY_ORDER[b.priority] ?? 99;
      return pa - pb;
    }
    if (sortBy.value === 'status') {
      return (a.status || '').localeCompare(b.status || '');
    }
    if (sortBy.value === 'project') {
      const ap = a.projectId || '';
      const bp = b.projectId || '';
      if (ap !== bp) return ap.localeCompare(bp);
      // 同一项目内按截止日期升序
      return (a.endDate || '').localeCompare(b.endDate || '');
    }
    // 默认:按截止日期升序(已完成排后面,无截止日期排最末)
    const aDone = a.status === 'done' ? 1 : 0;
    const bDone = b.status === 'done' ? 1 : 0;
    if (aDone !== bDone) return aDone - bDone;
    const aDate = a.endDate || '9999-12-31';
    const bDate = b.endDate || '9999-12-31';
    return aDate.localeCompare(bDate);
  });

  return list;
});

// ===== 分组 =====
const projectById = computed(() => {
  const m = new Map<string, Project>();
  for (const p of accessibleProjects.value) m.set(p.id, p);
  return m;
});

const groupedByProject = computed(() => {
  const map = new Map<string, { projectId: string; projectName: string; tasks: Task[] }>();
  for (const t of filteredAndSortedTasks.value) {
    const key = t.projectId || '_none';
    if (!map.has(key)) {
      const p = t.projectId ? projectById.value.get(t.projectId) : null;
      map.set(key, {
        projectId: key,
        projectName: p?.name || t.projectId || '',
        tasks: []
      });
    }
    map.get(key)!.tasks.push(t);
  }
  return Array.from(map.values());
});

const groupedByStatus = computed(() => {
  const order: Array<{ status: Task['status']; label: string; variant: 'default' | 'primary' | 'success' }> = [
    { status: 'todo', label: t('taskStatus.todo', '待办'), variant: 'default' },
    { status: 'in-progress', label: t('taskStatus.inProgress', '进行中'), variant: 'primary' },
    { status: 'done', label: t('taskStatus.done', '已完成'), variant: 'success' }
  ];
  return order.map(g => ({
    ...g,
    tasks: filteredAndSortedTasks.value.filter(t => t.status === g.status)
  })).filter(g => g.tasks.length > 0);
});

// ===== 抽屉 / 编辑 =====
const openDetail = (task: Task) => {
  selectedTask.value = task;
  drawerOpen.value = true;
};

const openEditModal = (task: Task) => {
  editingTask.value = task;
  modalOpen.value = true;
  drawerOpen.value = false;
};

const closeEditModal = () => {
  modalOpen.value = false;
  editingTask.value = null;
};

const handleSave = async (_task: Partial<Task>) => {
  // TaskModal 内部已调 store 完成保存;此处仅刷新本视图
  closeEditModal();
  await loadData();
};

const handleDelete = async (_task: Task) => {
  drawerOpen.value = false;
  await loadData();
};

const handleCreateSubtask = (_parentId: string) => {
  // 子任务的创建受父任务负责人 + 父任务 assignee 限制,在 ProjectTask 模态中处理
  // 这里先关闭抽屉,让用户跳到项目内去创建(保持职责单一)
  drawerOpen.value = false;
};

const viewSubtask = (_subtask: Task) => {
  // 抽屉内部已切到子任务的详情;不重置 selectedTask,让 TaskDetail 自己处理
};
</script>
