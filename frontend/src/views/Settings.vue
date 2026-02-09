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
                @click="activeSection = item.id"
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
                <img
                  :src="currentUser?.avatar"
                  :alt="currentUser?.name"
                  class="h-20 w-20 rounded-full border-4 border-secondary-200"
                />
                <div>
                  <Button variant="secondary" size="sm">{{ t('settings.profile.changeAvatar') }}</Button>
                  <p class="mt-1 text-xs text-secondary-500">{{ t('settings.profile.avatarHint') }}</p>
                </div>
              </div>

              <!-- Form -->
              <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <Input :label="t('settings.profile.name')" v-model="profileForm.name" />
                <Input :label="t('settings.profile.email')" v-model="profileForm.email" type="email" />
                <Input :label="t('settings.profile.department')" v-model="profileForm.department" />
                <Select :label="t('settings.profile.role')" v-model="profileForm.role">
                  <option value="admin">{{ t('roles.admin') }}</option>
                  <option value="project-manager">{{ t('roles.projectManager') }}</option>
                  <option value="member">{{ t('roles.member') }}</option>
                </Select>
              </div>

              <div class="flex justify-end">
                <Button variant="primary">{{ t('settings.profile.saveChanges') }}</Button>
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
                <Button variant="primary">{{ t('settings.notifications.saveChanges') }}</Button>
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
                <Button variant="primary">{{ t('settings.display.saveChanges') }}</Button>
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
                  <Input :label="t('settings.security.currentPassword')" type="password" />
                  <Input :label="t('settings.security.newPassword')" type="password" />
                  <Input :label="t('settings.security.confirmPassword')" type="password" />
                </div>
                <div class="mt-4">
                  <Button variant="primary">{{ t('settings.security.updatePassword') }}</Button>
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
        </div>
      </div>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useI18n } from 'vue-i18n';
import MainLayout from '@/components/layout/MainLayout.vue';
import Card from '@/components/common/Card.vue';
import Button from '@/components/common/Button.vue';
import Input from '@/components/common/Input.vue';
import Select from '@/components/common/Select.vue';
import { useUserStore } from '@/stores/user';

const { t } = useI18n();
const userStore = useUserStore();
const currentUser = computed(() => userStore.currentUser);

const activeSection = ref('profile');

const settingsNav = computed(() => [
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
]);

const profileForm = ref({
  name: currentUser.value?.name || '',
  email: currentUser.value?.email || '',
  department: currentUser.value?.department || '',
  role: currentUser.value?.role || 'member'
});

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

const themes = computed(() => [
  { label: t('settings.display.themes.light'), value: 'light', color: '#f8fafc' },
  { label: t('settings.display.themes.dark'), value: 'dark', color: '#1e293b' },
  { label: t('settings.display.themes.auto'), value: 'auto', color: '#64748b' }
]);
</script>
