<template>
  <MainLayout>
    <div class="space-y-6">
      <!-- Page Header -->
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-secondary-900">{{ $t('team.title') }}</h1>
          <p class="mt-1 text-sm text-secondary-600">{{ $t('team.subtitle') }}</p>
        </div>
        <Button variant="primary" @click="showAddMemberModal = true">
          <svg class="mr-2 h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          {{ $t('team.addMember') }}
        </Button>
      </div>

      <!-- Stats -->
      <div class="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
        <Card>
          <div class="p-4">
            <p class="text-sm font-medium text-secondary-600">{{ $t('common.members') }}</p>
            <p class="mt-2 text-2xl font-semibold text-secondary-900">{{ users.length }}</p>
          </div>
        </Card>

        <Card>
          <div class="p-4">
            <p class="text-sm font-medium text-secondary-600">{{ $t('roles.admin') }}</p>
            <p class="mt-2 text-2xl font-semibold text-secondary-900">{{ adminCount }}</p>
          </div>
        </Card>

        <Card>
          <div class="p-4">
            <p class="text-sm font-medium text-secondary-600">{{ $t('roles.projectManager') }}</p>
            <p class="mt-2 text-2xl font-semibold text-secondary-900">{{ pmCount }}</p>
          </div>
        </Card>

        <Card>
          <div class="p-4">
            <p class="text-sm font-medium text-secondary-600">{{ $t('roles.member') }}</p>
            <p class="mt-2 text-2xl font-semibold text-secondary-900">{{ memberCount }}</p>
          </div>
        </Card>
      </div>

      <!-- Team Members List -->
      <Card>
        <template #header>
          <h3 class="text-lg font-semibold text-secondary-900">{{ $t('team.allMembers') }}</h3>
        </template>
        <div class="overflow-x-auto">
          <table class="min-w-full divide-y divide-secondary-200">
            <thead class="bg-secondary-50">
              <tr>
                <th class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-secondary-500">
                  {{ $t('team.name') }}
                </th>
                <th class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-secondary-500">
                  {{ $t('team.role') }}
                </th>
                <th class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-secondary-500">
                  {{ $t('team.department') }}
                </th>
                <th class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-secondary-500">
                  {{ $t('team.skills') }}
                </th>
                <th class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-secondary-500">
                  {{ $t('team.joinedAt') }}
                </th>
                <th class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-secondary-500">
                  {{ $t('team.actions') }}
                </th>
              </tr>
            </thead>
            <tbody class="divide-y divide-secondary-200 bg-white">
              <tr v-for="user in users" :key="user.id" class="hover:bg-secondary-50">
                <td class="whitespace-nowrap px-6 py-4">
                  <div class="flex items-center">
                    <img :src="user.avatar" :alt="user.name" class="h-10 w-10 rounded-full" />
                    <div class="ml-4">
                      <div class="text-sm font-medium text-secondary-900">{{ user.name }}</div>
                      <div class="text-sm text-secondary-600">{{ user.email }}</div>
                    </div>
                  </div>
                </td>
                <td class="whitespace-nowrap px-6 py-4">
                  <Badge :variant="getRoleBadgeVariant(user.role)">
                    {{ getRoleLabel(user.role) }}
                  </Badge>
                </td>
                <td class="whitespace-nowrap px-6 py-4 text-sm text-secondary-900">
                  {{ user.department }}
                </td>
                <td class="px-6 py-4">
                  <div class="flex flex-wrap gap-1">
                    <template v-if="user.skills && user.skills.length > 0">
                      <span
                        v-for="skill in user.skills.slice(0, 2)"
                        :key="skill"
                        class="rounded bg-secondary-100 px-2 py-0.5 text-xs text-secondary-600"
                      >
                        {{ skill }}
                      </span>
                      <span
                        v-if="user.skills.length > 2"
                        class="rounded bg-secondary-100 px-2 py-0.5 text-xs text-secondary-600"
                      >
                        +{{ user.skills.length - 2 }}
                      </span>
                    </template>
                    <span v-else class="text-xs text-secondary-400">-</span>
                  </div>
                </td>
                <td class="whitespace-nowrap px-6 py-4 text-sm text-secondary-600">
                  {{ formattedDate(user.joinedAt) }}
                </td>
                <td class="whitespace-nowrap px-6 py-4 text-sm">
                  <button
                    @click="openEditModal(user)"
                    class="text-primary-600 hover:text-primary-900 mr-3"
                  >
                    {{ $t('team.edit') }}
                  </button>
                  <button
                    @click="handleDeleteMember(user.id)"
                    class="text-red-600 hover:text-red-900"
                  >
                    {{ $t('team.delete') }}
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </Card>

      <!-- Workload Distribution -->
      <Card>
        <template #header>
          <h3 class="text-lg font-semibold text-secondary-900">{{ $t('team.workloadDistribution') }}</h3>
        </template>
        <div class="h-80" ref="workloadChartRef"></div>
      </Card>
    </div>

    <!-- Add/Edit Member Modal -->
    <Modal
      v-model="showAddMemberModal"
      :title="isEditMode ? $t('team.editMember') : $t('team.addNewMember')"
      size="lg"
      @close="closeModal"
    >
      <form @submit.prevent="handleSaveMember" class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-secondary-700 mb-1">{{ $t('team.form.nameLabel') }} *</label>
          <input
            v-model="newMember.name"
            type="text"
            required
            class="w-full rounded-lg border border-secondary-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
            :placeholder="$t('team.form.namePlaceholder')"
          />
        </div>

        <div>
          <label class="block text-sm font-medium text-secondary-700 mb-1">{{ $t('team.form.emailLabel') }} *</label>
          <input
            v-model="newMember.email"
            type="email"
            required
            class="w-full rounded-lg border border-secondary-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
            :placeholder="$t('team.form.emailPlaceholder')"
          />
        </div>

        <div>
          <label class="block text-sm font-medium text-secondary-700 mb-1">{{ $t('team.form.roleLabel') }} *</label>
          <select
            v-model="newMember.role"
            required
            class="w-full rounded-lg border border-secondary-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
          >
            <option value="">{{ $t('team.form.rolePlaceholder') }}</option>
            <option value="member">{{ $t('roles.member') }}</option>
            <option value="project-manager">{{ $t('roles.projectManager') }}</option>
            <option value="admin">{{ $t('roles.admin') }}</option>
          </select>
        </div>

        <div>
          <label class="block text-sm font-medium text-secondary-700 mb-1">{{ $t('team.form.departmentLabel') }} *</label>
          <input
            v-model="newMember.department"
            type="text"
            required
            class="w-full rounded-lg border border-secondary-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
            :placeholder="$t('team.form.departmentPlaceholder')"
          />
        </div>

        <div>
          <label class="block text-sm font-medium text-secondary-700 mb-1">{{ $t('team.form.skillsLabel') }}</label>
          <input
            v-model="skillsInput"
            type="text"
            class="w-full rounded-lg border border-secondary-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
            :placeholder="$t('team.form.skillsPlaceholder')"
          />
          <p class="mt-1 text-xs text-secondary-500">{{ $t('team.form.skillsHint') }}</p>
        </div>
      </form>

      <template #footer>
        <div class="flex justify-end gap-3">
          <Button variant="secondary" @click="closeModal">{{ $t('common.cancel') }}</Button>
          <Button variant="primary" @click="handleSaveMember">
            {{ isEditMode ? $t('team.buttons.saveChanges') : $t('team.buttons.addMember') }}
          </Button>
        </div>
      </template>
    </Modal>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue';
import { useI18n } from 'vue-i18n';
import * as echarts from 'echarts';
import MainLayout from '@/components/layout/MainLayout.vue';
import Card from '@/components/common/Card.vue';
import Button from '@/components/common/Button.vue';
import Badge from '@/components/common/Badge.vue';
import Modal from '@/components/common/Modal.vue';
import { useUserStore } from '@/stores/user';
import { useTaskStore } from '@/stores/task';
import type { User } from '@/types';
import dayjs from 'dayjs';

const { t } = useI18n();
const userStore = useUserStore();
const taskStore = useTaskStore();

const users = computed(() => userStore.users);
const workloadChartRef = ref<HTMLElement>();

// Add Member Modal
const showAddMemberModal = ref(false);
const isEditMode = ref(false);
const editingUserId = ref<string | null>(null);
const skillsInput = ref('');

const newMember = reactive<Omit<User, 'id' | 'avatar' | 'joinedAt'>>({
  name: '',
  email: '',
  role: 'member',
  department: '',
  skills: []
});

const resetNewMember = () => {
  newMember.name = '';
  newMember.email = '';
  newMember.role = 'member';
  newMember.department = '';
  newMember.skills = [];
  skillsInput.value = '';
};

const closeModal = () => {
  showAddMemberModal.value = false;
  isEditMode.value = false;
  editingUserId.value = null;
  resetNewMember();
};

const openEditModal = (user: User) => {
  isEditMode.value = true;
  editingUserId.value = user.id;
  newMember.name = user.name;
  newMember.email = user.email;
  // 标准化角色：将下划线转换为连字符，确保与表单选项匹配
  const normalizedRole = user.role?.replace(/_/g, '-') || 'member';
  newMember.role = normalizedRole as User['role'];
  newMember.department = user.department;
  // 确保 skills 是数组
  newMember.skills = Array.isArray(user.skills) ? [...user.skills] : [];
  skillsInput.value = Array.isArray(user.skills) ? user.skills.join(', ') : '';
  showAddMemberModal.value = true;
};

const handleSaveMember = async () => {
  if (!newMember.name || !newMember.email || !newMember.department) {
    alert(t('team.messages.requiredFields'));
    return;
  }

  // Parse skills from comma-separated input
  const skillsArray = skillsInput.value
    .split(',')
    .map(s => s.trim())
    .filter(s => s.length > 0);

  try {
    if (isEditMode.value && editingUserId.value) {
      // Update existing user
      await userStore.updateUser(editingUserId.value, {
        name: newMember.name,
        email: newMember.email,
        role: newMember.role,
        department: newMember.department,
        skills: skillsArray
      });
      alert(t('team.messages.updateSuccess'));
    } else {
      // Add new user
      const avatar = `https://api.dicebear.com/7.x/avataaars/svg?seed=${encodeURIComponent(newMember.name)}`;
      await userStore.addUser({
        ...newMember,
        skills: skillsArray,
        avatar,
        joinedAt: new Date().toISOString()
      });
      alert(t('team.messages.createSuccess'));
    }

    // Close modal and reset form
    closeModal();

    // Refresh chart after a short delay
    setTimeout(() => {
      initWorkloadChart();
    }, 200);
  } catch (error) {
    console.error('Failed to save member:', error);
    alert(t('team.messages.operationFailed'));
  }
};

const handleDeleteMember = async (userId: string) => {
  const user = users.value.find(u => u.id === userId);
  if (!user) return;

  if (confirm(t('team.messages.deleteConfirm', { name: user.name }))) {
    try {
      await userStore.deleteUser(userId);
      alert(t('team.messages.deleteSuccess'));

      // Refresh chart after a short delay
      setTimeout(() => {
        initWorkloadChart();
      }, 200);
    } catch (error) {
      console.error('Failed to delete member:', error);
      alert(t('team.messages.deleteFailed'));
    }
  }
};

const adminCount = computed(() => users.value.filter(u => u.role?.replace(/_/g, '-') === 'admin').length);
const pmCount = computed(() => users.value.filter(u => u.role?.replace(/_/g, '-') === 'project-manager').length);
const memberCount = computed(() => users.value.filter(u => u.role?.replace(/_/g, '-') === 'member').length);

const getRoleLabel = (role: string) => {
  // 标准化角色名称：处理下划线和连字符的兼容性
  const normalizedRole = role?.replace(/_/g, '-');

  const labels: Record<string, string> = {
    admin: 'roles.admin',
    'project-manager': 'roles.projectManager',
    member: 'roles.member'
  };
  const key = labels[normalizedRole];
  return key ? t(key) : role;
};

const getRoleBadgeVariant = (role: string) => {
  // 标准化角色名称：处理下划线和连字符的兼容性
  const normalizedRole = role?.replace(/_/g, '-');

  const variants: Record<string, string> = {
    admin: 'danger',
    'project-manager': 'warning',
    member: 'primary'
  };
  return variants[normalizedRole] || 'default';
};

const formattedDate = (date: string) => {
  // Use localized date format based on current locale
  const locale = t('common.locale') || 'zh';
  if (locale === 'ko') {
    return dayjs(date).format('YYYY년 MM월');
  } else {
    return dayjs(date).format('YYYY年MM月');
  }
};

const initWorkloadChart = () => {
  if (!workloadChartRef.value) return;

  const chart = echarts.init(workloadChartRef.value);

  // 只统计叶子任务（没有子任务的任务），避免重复统计
  const allTaskIds = new Set(taskStore.tasks.map(t => t.id));
  const parentTaskIds = new Set(taskStore.tasks.filter(t => t.parentTaskId).map(t => t.parentTaskId));
  const leafTaskIds = new Set([...allTaskIds].filter(id => !parentTaskIds.has(id)));
  const leafTasks = taskStore.tasks.filter(t => leafTaskIds.has(t.id));

  const userData = users.value.slice(0, 8).map(user => {
    const taskCount = leafTasks.filter(t => t.assigneeId === user.id).length;
    return {
      name: user.name,
      value: taskCount
    };
  });

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: userData.map(u => u.name),
      axisLabel: {
        interval: 0,
        rotate: 30
      }
    },
    yAxis: {
      type: 'value',
      name: t('team.taskCount')
    },
    series: [
      {
        name: '任务数',
        type: 'bar',
        data: userData.map(u => u.value),
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#3b82f6' },
            { offset: 1, color: '#1d4ed8' }
          ]),
          borderRadius: [4, 4, 0, 0]
        },
        barWidth: '60%'
      }
    ]
  };

  chart.setOption(option);
};

onMounted(async () => {
  // 确保用户数据已加载
  await userStore.loadUsers();

  // 确保任务数据已加载（工作负载分布图表需要）
  try {
    await taskStore.loadTasks();
  } catch (error) {
    console.warn('加载任务数据失败:', error);
  }

  // 初始化图表
  setTimeout(() => {
    initWorkloadChart();
  }, 100);
});
</script>
