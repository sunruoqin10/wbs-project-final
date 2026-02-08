<template>
  <header
    class="fixed right-0 top-0 z-30 h-16 bg-white shadow-sm transition-all duration-300"
    :style="{ left: sidebarWidth }"
  >
    <div class="flex h-full items-center justify-between px-6">
      <!-- Page Title -->
      <div class="flex items-center gap-4">
        <h1 class="text-xl font-semibold text-secondary-800">{{ pageTitle }}</h1>
      </div>

      <!-- Right Actions -->
      <div class="flex items-center gap-4">
        <!-- Search -->
        <div class="relative">
          <input
            v-model="searchQuery"
            type="text"
            placeholder="搜索..."
            class="w-64 rounded-lg border border-secondary-200 px-4 py-2 pl-10 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20"
          />
          <svg
            class="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-secondary-400"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
            />
          </svg>
        </div>

        <!-- Notifications -->
        <button
          class="relative rounded-lg p-2 text-secondary-600 transition-colors hover:bg-secondary-100"
        >
          <svg class="h-6 w-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"
            />
          </svg>
          <span
            class="absolute right-1 top-1 h-2 w-2 rounded-full bg-danger-500"
          ></span>
        </button>

        <!-- User Menu -->
        <div class="relative" ref="userMenuRef">
          <button
            @click="toggleUserMenu"
            class="flex items-center gap-3 rounded-lg p-1 transition-colors hover:bg-secondary-100"
          >
            <img
              :src="currentUser?.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=default'"
              :alt="currentUser?.name || '用户头像'"
              class="h-9 w-9 rounded-full border-2 border-secondary-200 object-cover"
            />
            <div class="hidden lg:block text-left">
              <p class="text-sm font-medium text-secondary-800">
                {{ currentUser?.name || '加载中...' }}
              </p>
              <p class="text-xs text-secondary-500">{{ roleLabel }}</p>
            </div>
            <svg
              class="hidden h-4 w-4 text-secondary-400 lg:block"
              :class="{ 'rotate-180': showUserMenu }"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M19 9l-7 7-7-7"
              />
            </svg>
          </button>

          <!-- Dropdown Menu -->
          <transition
            enter-active-class="transition ease-out duration-200"
            enter-from-class="transform opacity-0 scale-95"
            enter-to-class="transform opacity-100 scale-100"
            leave-active-class="transition ease-in duration-150"
            leave-from-class="transform opacity-100 scale-100"
            leave-to-class="transform opacity-0 scale-95"
          >
            <div
              v-if="showUserMenu"
              class="absolute right-0 mt-2 w-56 origin-top-right rounded-lg bg-white py-2 shadow-lg ring-1 ring-black ring-opacity-5 focus:outline-none"
            >
              <!-- User Info -->
              <div class="border-b border-secondary-100 px-4 py-3">
                <p class="text-sm font-medium text-secondary-800">
                  {{ currentUser?.name }}
                </p>
                <p class="text-xs text-secondary-500">{{ currentUser?.email }}</p>
                <p class="mt-1 text-xs text-primary-600">{{ roleLabel }}</p>
              </div>

              <!-- Menu Items -->
              <div class="py-1">
                <a
                  href="#"
                  class="flex items-center gap-3 px-4 py-2 text-sm text-secondary-700 transition-colors hover:bg-secondary-50"
                  @click.prevent="handleMenuClick('profile')"
                >
                  <svg class="h-5 w-5 text-secondary-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      stroke-width="2"
                      d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"
                    />
                  </svg>
                  个人中心
                </a>
                <a
                  href="#"
                  class="flex items-center gap-3 px-4 py-2 text-sm text-secondary-700 transition-colors hover:bg-secondary-50"
                  @click.prevent="handleMenuClick('settings')"
                >
                  <svg class="h-5 w-5 text-secondary-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      stroke-width="2"
                      d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"
                    />
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      stroke-width="2"
                      d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
                    />
                  </svg>
                  账号设置
                </a>
              </div>

              <!-- Logout -->
              <div class="border-t border-secondary-100 py-1">
                <a
                  href="#"
                  class="flex items-center gap-3 px-4 py-2 text-sm text-danger-600 transition-colors hover:bg-danger-50"
                  @click.prevent="handleLogout"
                >
                  <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      stroke-width="2"
                      d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"
                    />
                  </svg>
                  退出系统
                </a>
              </div>
            </div>
          </transition>
        </div>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import { useUiStore } from '@/stores/ui';

const router = useRouter();
const userStore = useUserStore();
const uiStore = useUiStore();

const searchQuery = ref('');
const showUserMenu = ref(false);
const userMenuRef = ref<HTMLElement | null>(null);

const currentUser = computed(() => userStore.currentUser);
const sidebarWidth = computed(() => (uiStore.sidebarCollapsed ? '5rem' : '16rem'));

const pageTitle = computed(() => {
  return router.currentRoute.value.meta.title as string || 'WBS项目管理系统';
});

const roleLabel = computed(() => {
  const labels = {
    admin: '管理员',
    'project-manager': '项目经理',
    member: '成员',
    viewer: '观察者'
  };
  return currentUser.value ? labels[currentUser.value.role] : '';
});

// 切换用户菜单
const toggleUserMenu = () => {
  showUserMenu.value = !showUserMenu.value;
};

// 处理菜单项点击
const handleMenuClick = (action: string) => {
  showUserMenu.value = false;
  switch (action) {
    case 'profile':
      // TODO: 导航到个人中心页面
      console.log('导航到个人中心');
      break;
    case 'settings':
      // TODO: 导航到设置页面
      console.log('导航到设置页面');
      break;
  }
};

// 处理退出登录
const handleLogout = () => {
  showUserMenu.value = false;
  userStore.logout();
  router.push('/login');
};

// 点击外部关闭菜单
const handleClickOutside = (event: MouseEvent) => {
  if (userMenuRef.value && !userMenuRef.value.contains(event.target as Node)) {
    showUserMenu.value = false;
  }
};

// 初始化当前用户
onMounted(async () => {
  // 加载用户数据
  await userStore.loadUsers();

  // 如果没有设置当前用户，默认设置为第一个用户（演示用）
  if (!userStore.currentUserId && userStore.users.length > 0) {
    userStore.setCurrentUser(userStore.users[0]);
  }

  // 添加全局点击事件监听器
  document.addEventListener('click', handleClickOutside);
});

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside);
});
</script>
