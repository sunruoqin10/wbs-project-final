<template>
  <MainLayout>
    <div class="space-y-6">
      <!-- Page Header -->
      <div>
        <h1 class="text-2xl font-bold text-secondary-900">{{ t('settings.title') }}</h1>
        <p class="mt-1 text-sm text-secondary-600">{{ t('settings.subtitle') }}</p>
      </div>

      <div class="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <!-- Settings Navigation -->
        <div class="lg:col-span-1">
          <Card>
            <nav class="space-y-1">
              <button
                v-for="item in settingsNav"
                :key="item.id"
                @click="handleSectionChange(item.id)"
                :class="[
                  'w-full flex items-center gap-3 rounded-lg px-4 py-3 text-left transition-colors',
                  activeSection === item.id
                    ? 'bg-primary-50 text-primary-700'
                    : 'text-secondary-700 hover:bg-secondary-50'
                ]"
              >
                <span v-html="item.icon" class="h-5 w-5"></span>
                <span class="font-medium">{{ item.label }}</span>
              </button>
            </nav>
          </Card>
        </div>

        <!-- Settings Content -->
        <div class="lg:col-span-2">
          <!-- Profile Settings -->
          <Card v-if="activeSection === 'profile'">
            <template #header>
              <h3 class="text-lg font-semibold text-secondary-900">{{ t('settings.profile.title') }}</h3>
            </template>
            <div class="space-y-6">
              <!-- Avatar -->
              <div class="flex items-center gap-6">
              <UserAvatar
                  :name="currentUser?.name"
                  :seed="currentUser?.avatar"
                  size="2xl"
                  class="border-4 border-secondary-200"
                />
                <div>
                  <Button variant="secondary" size="sm" @click="openAvatarPicker">{{ t('settings.profile.changeAvatar') }}</Button>
                  <p class="mt-1 text-xs text-secondary-500">{{ t('settings.profile.avatarHint') }}</p>
                </div>
              </div>

              <!-- Form -->
              <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <Input :label="t('settings.profile.name')" v-model="profileForm.name" />
                <Input :label="t('settings.profile.email')" v-model="profileForm.email" type="email" />
                <Input :label="t('settings.profile.department')" v-model="profileForm.department" />
                <Select :label="t('settings.profile.role')" v-model="profileForm.role" :disabled="isMemberRole">
                  <option value="admin">{{ t('roles.admin') }}</option>
                  <option value="project-manager">{{ t('roles.projectManager') }}</option>
                  <option value="member">{{ t('roles.member') }}</option>
                </Select>
              </div>

              <div class="flex justify-end">
                <Button variant="primary" @click="handleSaveProfile">{{ t('settings.profile.saveChanges') }}</Button>
              </div>
            </div>
          </Card>

          <!-- Notification Settings -->
          <Card v-if="activeSection === 'notifications'">
            <template #header>
              <h3 class="text-lg font-semibold text-secondary-900">{{ t('settings.notifications.title') }}</h3>
            </template>
            <div class="space-y-4">
              <div class="flex items-center justify-between rounded-lg border border-secondary-200 p-4">
                <div>
                  <p class="font-medium text-secondary-900">{{ t('settings.notifications.emailNotifications') }}</p>
                  <p class="text-sm text-secondary-600">{{ t('settings.notifications.emailNotificationsDesc') }}</p>
                </div>
                <button
                  @click="notificationSettings.email = !notificationSettings.email"
                  class="relative inline-flex h-6 w-11 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2"
                  :class="notificationSettings.email ? 'bg-primary-600' : 'bg-secondary-200'"
                >
                  <span
                    class="pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out"
                    :class="notificationSettings.email ? 'translate-x-5' : 'translate-x-0'"
                  ></span>
                </button>
              </div>

              <div class="flex items-center justify-between rounded-lg border border-secondary-200 p-4">
                <div>
                  <p class="font-medium text-secondary-900">{{ t('settings.notifications.browserNotifications') }}</p>
                  <p class="text-sm text-secondary-600">{{ t('settings.notifications.browserNotificationsDesc') }}</p>
                </div>
                <button
                  @click="notificationSettings.browser = !notificationSettings.browser"
                  class="relative inline-flex h-6 w-11 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2"
                  :class="notificationSettings.browser ? 'bg-primary-600' : 'bg-secondary-200'"
                >
                  <span
                    class="pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out"
                    :class="notificationSettings.browser ? 'translate-x-5' : 'translate-x-0'"
                  ></span>
                </button>
              </div>

              <div class="flex items-center justify-between rounded-lg border border-secondary-200 p-4">
                <div>
                  <p class="font-medium text-secondary-900">{{ t('settings.notifications.taskReminders') }}</p>
                  <p class="text-sm text-secondary-600">{{ t('settings.notifications.taskRemindersDesc') }}</p>
                </div>
                <button
                  @click="notificationSettings.taskReminder = !notificationSettings.taskReminder"
                  class="relative inline-flex h-6 w-11 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2"
                  :class="notificationSettings.taskReminder ? 'bg-primary-600' : 'bg-secondary-200'"
                >
                  <span
                    class="pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out"
                    :class="notificationSettings.taskReminder ? 'translate-x-5' : 'translate-x-0'"
                  ></span>
                </button>
              </div>

              <div class="flex justify-end">
                <Button variant="primary" @click="handleSaveNotifications">{{ t('settings.notifications.saveChanges') }}</Button>
              </div>
            </div>
          </Card>

          <!-- Display Settings -->
          <Card v-if="activeSection === 'display'">
            <template #header>
              <h3 class="text-lg font-semibold text-secondary-900">{{ t('settings.display.title') }}</h3>
            </template>
            <div class="space-y-6">
              <div>
                <label class="block text-sm font-medium text-secondary-700">{{ t('settings.display.theme') }}</label>
                <div class="mt-2 grid grid-cols-3 gap-3">
                  <button
                    v-for="theme in themes"
                    :key="theme.value"
                    @click="displaySettings.theme = theme.value"
                    :class="[
                      'rounded-lg border-2 p-4 text-center transition-all',
                      displaySettings.theme === theme.value
                        ? 'border-primary-500 bg-primary-50'
                        : 'border-secondary-200 hover:border-secondary-300'
                    ]"
                  >
                    <div class="mx-auto mb-2 h-8 w-8 rounded-full" :style="{ backgroundColor: theme.color }"></div>
                    <span class="text-sm font-medium text-secondary-900">{{ theme.label }}</span>
                  </button>
                </div>
              </div>

              <div>
                <label class="block text-sm font-medium text-secondary-700">{{ t('settings.display.language') }}</label>
                <Select v-model="displaySettings.language" class="mt-2">
                  <option value="zh-CN">{{ t('lang.name') }} {{ t('lang.flag') }}</option>
                  <option value="ko">{{ t('lang.name', { locale: 'ko' }) }} {{ t('lang.flag', { locale: 'ko' }) }}</option>
                </Select>
              </div>

              <div>
                <label class="block text-sm font-medium text-secondary-700">{{ t('settings.display.dateFormat') }}</label>
                <Select v-model="displaySettings.dateFormat" class="mt-2">
                  <option value="YYYY-MM-DD">2024-01-31</option>
                  <option value="YYYY/MM/DD">2024/01/31</option>
                  <option value="DD/MM/YYYY">31/01/2024</option>
                  <option value="MM/DD/YYYY">01/31/2024</option>
                </Select>
              </div>

              <div class="flex justify-end">
                <Button variant="primary" @click="handleSaveDisplay">{{ t('settings.display.saveChanges') }}</Button>
              </div>
            </div>
          </Card>

          <!-- Security Settings -->
          <Card v-if="activeSection === 'security'">
            <template #header>
              <h3 class="text-lg font-semibold text-secondary-900">{{ t('settings.security.title') }}</h3>
            </template>
            <div class="space-y-6">
              <div>
                <h4 class="text-sm font-medium text-secondary-900">{{ t('settings.security.changePassword') }}</h4>
                <p class="text-sm text-secondary-600">{{ t('settings.security.changePasswordDesc') }}</p>
                <div class="mt-4 space-y-4">
                <Input :label="t('settings.security.currentPassword')" type="password" v-model="passwordForm.currentPassword" />
                <Input :label="t('settings.security.newPassword')" type="password" v-model="passwordForm.newPassword" />
                <Input :label="t('settings.security.confirmPassword')" type="password" v-model="passwordForm.confirmPassword" />
              </div>
              <div class="mt-4">
                <Button variant="primary" @click="handleUpdatePassword">{{ t('settings.security.updatePassword') }}</Button>
              </div>
              </div>

              <hr class="border-secondary-200" />

              <div>
                <h4 class="text-sm font-medium text-secondary-900">{{ t('settings.security.twoFactor') }}</h4>
                <p class="text-sm text-secondary-600">{{ t('settings.security.twoFactorDesc') }}</p>
                <div class="mt-4">
                  <Button variant="secondary">{{ t('settings.security.enableTwoFactor') }}</Button>
                </div>
              </div>
            </div>
          </Card>

          <!-- HR Sync Settings -->
          <Card v-if="activeSection === 'hrSync' && canAccessHrSync">
            <template #header>
              <h3 class="text-lg font-semibold text-secondary-900">{{ t('settings.hrSync.title') }}</h3>
            </template>
            <div class="space-y-4">
              <p class="text-sm text-secondary-600">{{ t('settings.hrSync.description') }}</p>

              <div class="rounded-lg border border-secondary-200 p-4 bg-secondary-50">
                <p class="text-sm text-secondary-700">{{ t('settings.hrSync.syncNote') }}</p>
              </div>

              <div v-if="syncResult !== null" :class="[
                'rounded-lg border p-4',
                syncResult.success ? 'border-green-300 bg-green-50' : 'border-red-300 bg-red-50'
              ]">
                <p :class="[
                  'text-sm font-medium',
                  syncResult.success ? 'text-green-700' : 'text-red-700'
                ]">
                  {{ syncResult.message }}
                </p>
              </div>

              <div class="flex justify-end">
                <Button
                  variant="primary"
                  :disabled="syncing"
                  @click="handleSyncHr"
                >
                  <span v-if="syncing" class="flex items-center gap-2">
                    <svg class="animate-spin h-4 w-4" fill="none" viewBox="0 0 24 24">
                      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                    </svg>
                    {{ t('settings.hrSync.syncing') }}
                  </span>
                  <span v-else>{{ t('settings.hrSync.syncButton') }}</span>
                </Button>
              </div>
            </div>
          </Card>
        </div>
      </div>
    </div>

    <Modal :open="showAvatarPicker" :title="t('settings.profile.chooseAvatar')" size="2xl" @close="showAvatarPicker = false">
      <div class="max-h-[65vh] overflow-y-auto space-y-5">
        <div v-for="group in avatarGroups" :key="group.style">
          <h4 class="mb-2 text-sm font-semibold text-secondary-500">{{ group.label }}</h4>
          <div class="grid grid-cols-5 gap-2 sm:grid-cols-6 md:grid-cols-7">
            <button
              v-for="seed in group.seeds"
              :key="seed"
              @click="selectAvatar(seed)"
              class="flex flex-col items-center gap-1 rounded-lg p-2 border-2 border-secondary-200 hover:border-primary-400 hover:bg-primary-50 transition-all"
            >
              <UserAvatar :seed="seed" size="xl" />
            </button>
          </div>
        </div>
      </div>
    </Modal>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';
import MainLayout from '@/components/layout/MainLayout.vue';
import Card from '@/components/common/Card.vue';
import Button from '@/components/common/Button.vue';
import Input from '@/components/common/Input.vue';
import Modal from '@/components/common/Modal.vue';
import Select from '@/components/common/Select.vue';
import UserAvatar from '@/components/common/UserAvatar.vue';
import { useUserStore } from '@/stores/user';
import { usePermissionStore } from '@/stores/permission';
import apiService from '@/services/api';

const { t } = useI18n();
const userStore = useUserStore();
const permissionStore = usePermissionStore();
const currentUser = computed(() => userStore.currentUser);
const isMemberRole = computed(() => currentUser.value?.role === 'member');
const route = useRoute();
const router = useRouter();

const canAccessHrSync = computed(() => permissionStore.canAccessHrSync());
const activeSection = ref('profile');

// 处理 URL 参数并设置相应的选项卡
const setActiveSectionFromRoute = () => {
  const tab = route.query.tab as string;
  // 根据权限筛选可用的选项卡
  const allowedTabs = ['profile', 'notifications', 'display', 'security'];
  if (canAccessHrSync.value) {
    allowedTabs.push('hrSync');
  }
  // 只在允许的选项卡中设置
  if (tab && allowedTabs.includes(tab)) {
    activeSection.value = tab;
  }
};

// 组件挂载时检查 URL 参数并设置默认选项卡
onMounted(() => {
  setActiveSectionFromRoute();
});

// 监听路由参数变化，当用户通过Header组件导航时更新选项卡
watch(
  () => route.query.tab,
  () => {
    setActiveSectionFromRoute();
  }
);

// 切换选项卡，同时更新URL
const handleSectionChange = (sectionId: string) => {
  activeSection.value = sectionId;
  // 更新URL，使用replace避免添加新的历史记录
  router.replace({
    name: 'Settings',
    query: { tab: sectionId }
  });
};

// 监听权限变化，如果用户切换到无权访问的选项卡，自动切回默认选项卡
watch(() => [canAccessHrSync.value, activeSection.value], ([hasHrAccess, section]) => {
  if (section === 'hrSync' && !hasHrAccess) {
    activeSection.value = 'profile';
  }
});

const settingsNav = computed(() => {
  const navItems = [
    {
      id: 'profile',
      label: t('settings.navigation.profile'),
      icon: '<svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" /></svg>'
    },
    {
      id: 'notifications',
      label: t('settings.navigation.notifications'),
      icon: '<svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" /></svg>'
    },
    {
      id: 'display',
      label: t('settings.navigation.display'),
      icon: '<svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" /></svg>'
    },
    {
      id: 'security',
      label: t('settings.navigation.security'),
      icon: '<svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" /></svg>'
    }
  ];
  
  // 只有管理员才能看到人事同步选项卡
  if (canAccessHrSync.value) {
    navItems.push({
      id: 'hrSync',
      label: t('settings.navigation.hrSync'),
      icon: '<svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" /></svg>'
    });
  }
  
  return navItems;
});

const profileForm = ref({
  name: currentUser.value?.name || '',
  email: currentUser.value?.email || '',
  department: currentUser.value?.department || '',
  role: currentUser.value?.role || 'member'
});

interface AvatarGroup {
  style: string;
  label: string;
  seeds: string[];
}

const avatarStyles: { key: string; label: string }[] = [
  { key: 'avataaars', label: '卡通人物' },
  { key: 'bottts', label: '机器人' },
  { key: 'micah', label: '插画风格' },
  { key: 'lorelei', label: '圆润可爱' },
  { key: 'pixelArt', label: '像素艺术' },
  { key: 'identicon', label: '几何图案' },
  { key: 'thumbs', label: '趣味搞怪' },
  { key: 'notionists', label: '简约线条' },
  { key: 'funEmoji', label: '表情符号' },
  { key: 'bigSmile', label: '笑脸' },
  { key: 'croodles', label: '涂鸦风格' },
  { key: 'openPeeps', label: '人物头像' },
  { key: 'personas', label: '个性肖像' },
  { key: 'adventurer', label: '冒险角色' },
  { key: 'miniavs', label: '迷你头像' }
];

const seedsPerStyle = 7;

function generateSeed(): string {
  return Math.random().toString(36).substring(2, 10);
}

const showAvatarPicker = ref(false);
const avatarGroups = ref<AvatarGroup[]>([]);

const openAvatarPicker = () => {
  avatarGroups.value = avatarStyles.map(({ key, label }) => ({
    style: key,
    label,
    seeds: Array.from({ length: seedsPerStyle }, () => `${key}:${generateSeed()}`)
  }));
  showAvatarPicker.value = true;
};

const selectAvatar = async (value: string) => {
  showAvatarPicker.value = false;
  const userId = currentUser.value?.id;
  if (!userId) return;
  try {
    await userStore.updateUser(userId, { avatar: value });
  } catch (error) {
    console.error('Failed to update avatar:', error);
  }
};

const notificationSettings = ref({
  email: true,
  browser: false,
  taskReminder: true
});

const displaySettings = ref({
  theme: 'light',
  language: 'zh-CN',
  dateFormat: 'YYYY-MM-DD'
});

const passwordForm = ref({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
});

const handleUpdatePassword = async () => {
  const { currentPassword, newPassword, confirmPassword } = passwordForm.value;

  if (!currentPassword) {
    alert(t('settings.security.msg.currentPasswordRequired'));
    return;
  }
  if (!newPassword) {
    alert(t('settings.security.msg.newPasswordRequired'));
    return;
  }
  if (newPassword !== confirmPassword) {
    alert(t('settings.security.msg.passwordMismatch'));
    return;
  }

  const userId = currentUser.value?.id;
  if (!userId) {
    alert(t('settings.security.msg.notLoggedIn'));
    return;
  }

  try {
    await apiService.changePassword(userId, currentPassword, newPassword);
    alert(t('settings.security.msg.updateSuccess'));
    passwordForm.value = { currentPassword: '', newPassword: '', confirmPassword: '' };
  } catch (error: any) {
    const message = error?.message || t('settings.security.msg.updateFailed');
    alert(message);
  }
};

const handleSaveProfile = async () => {
  const userId = currentUser.value?.id;
  if (!userId) {
    alert(t('settings.security.msg.notLoggedIn'));
    return;
  }
  try {
    await userStore.updateUser(userId, {
      name: profileForm.value.name,
      email: profileForm.value.email,
      department: profileForm.value.department,
      role: profileForm.value.role
    });
    alert(t('settings.profile.saveSuccess'));
  } catch (error: any) {
    const message = error?.message || t('settings.security.msg.updateFailed');
    alert(message);
  }
};

const handleSaveNotifications = () => {
  localStorage.setItem('notificationSettings', JSON.stringify(notificationSettings.value));
  alert(t('settings.notifications.saveSuccess'));
};

const handleSaveDisplay = () => {
  localStorage.setItem('displaySettings', JSON.stringify(displaySettings.value));
  if (displaySettings.value.language === 'ko') {
    location.reload();
  }
  alert(t('settings.display.saveSuccess'));
};

const themes = computed(() => [
  { label: t('settings.display.themes.light'), value: 'light', color: '#f8fafc' },
  { label: t('settings.display.themes.dark'), value: 'dark', color: '#1e293b' },
  { label: t('settings.display.themes.auto'), value: 'auto', color: '#64748b' }
]);

// HR Sync
const syncing = ref(false);
const syncResult = ref<{ success: boolean; message: string } | null>(null);

const handleSyncHr = async () => {
  syncing.value = true;
  syncResult.value = null;
  try {
    const result = await apiService.syncHrUsers();
    syncResult.value = {
      success: true,
      message: t('settings.hrSync.syncSuccessWithCount', { inserted: result.inserted, updated: result.updated })
    };
  } catch (error: any) {
    syncResult.value = {
      success: false,
      message: error?.message || t('settings.hrSync.syncFailed')
    };
  } finally {
    syncing.value = false;
  }
};
</script>
