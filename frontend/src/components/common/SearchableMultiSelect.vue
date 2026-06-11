<template>
  <div class="relative" ref="containerRef">
    <!-- 触发器 -->
    <div
      @click="toggleOpen"
      :class="[
        'min-h-[42px] w-full rounded-lg border px-3 py-2 text-sm cursor-pointer transition-colors flex items-center gap-1 flex-wrap',
        isOpen
          ? 'border-primary-500 ring-2 ring-primary-500/20'
          : 'border-secondary-200 hover:border-secondary-300'
      ]"
    >
      <!-- 已选标签 -->
      <span
        v-for="user in selectedUsers"
        :key="user.id"
        class="inline-flex items-center gap-1 rounded-md bg-primary-50 border border-primary-200 px-1.5 py-0.5 text-xs text-primary-700 max-w-[120px]"
      >
        <UserAvatar :name="user.name" size="xs" />
        <span class="truncate">{{ user.name }}</span>
        <button
          type="button"
          @click.stop="removeUser(user.id)"
          class="ml-0.5 flex-shrink-0 text-primary-400 hover:text-danger-500"
        >
          <svg class="h-3 w-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </span>
      <span v-if="selectedUsers.length === 0" class="text-secondary-400">{{ placeholder }}</span>
      <!-- 下拉箭头 -->
      <svg class="ml-auto h-4 w-4 flex-shrink-0 text-secondary-400 transition-transform" :class="{ 'rotate-180': isOpen }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
      </svg>
    </div>

    <!-- 下拉面板 -->
    <Transition name="dropdown">
      <div
        v-if="isOpen"
        class="absolute z-50 mt-1 w-full rounded-lg border border-secondary-200 bg-white shadow-lg"
      >
        <!-- 搜索框 -->
        <div class="border-b border-secondary-100 px-2 py-1.5">
          <div class="relative">
            <svg class="absolute left-2.5 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-secondary-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            <input
              ref="searchInputRef"
              v-model="searchKeyword"
              type="text"
              :placeholder="searchPlaceholder"
              class="w-full rounded-md border border-secondary-200 py-1.5 pl-8 pr-3 text-xs focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500/20"
              @keydown.escape="closeDropdown"
            />
          </div>
        </div>

        <!-- 结果列表 -->
        <div class="max-h-52 overflow-y-auto py-1" ref="listRef" @scroll="onListScroll">
          <div v-if="filteredResults.length === 0" class="py-4 text-center text-xs text-secondary-400">
            {{ searchKeyword ? '未找到匹配的用户' : '暂无用户' }}
          </div>

          <!-- 选项列表 - 限制显示数量 -->
          <template v-else>
            <label
              v-for="user in displayedResults"
              :key="user.id"
              :class="[
                'flex items-center gap-2 px-3 py-2 text-sm cursor-pointer transition-colors',
                isSelected(user.id) ? 'bg-primary-50' : 'hover:bg-secondary-50'
              ]"
            >
              <input
                type="checkbox"
                :checked="isSelected(user.id)"
                @change="toggleUser(user.id)"
                class="h-3.5 w-3.5 rounded border-secondary-300 text-primary-600 focus:ring-primary-500 flex-shrink-0"
              />
              <UserAvatar :name="user.name" size="xs" />
              <span class="text-xs text-secondary-700 truncate">{{ user.name }}</span>
              <span class="ml-auto text-[10px] text-secondary-400 flex-shrink-0">{{ user.id }}</span>
            </label>
            <div
              v-if="filteredResults.length > showLimit"
              class="border-t border-secondary-100 px-3 py-2 text-center text-xs text-secondary-400"
            >
              显示 {{ Math.min(showLimit, filteredResults.length) }} / 共 {{ filteredResults.length }} 个，请搜索筛选
            </div>
          </template>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import UserAvatar from '@/components/common/UserAvatar.vue';
import { useUserStore } from '@/stores/user';

interface Props {
  modelValue: string[];
  placeholder?: string;
  searchPlaceholder?: string;
}

const props = withDefaults(defineProps<Props>(), {
  placeholder: '请选择团队成员',
  searchPlaceholder: '搜索姓名或ID...'
});

const emit = defineEmits<{
  'update:modelValue': [value: string[]];
}>();

const userStore = useUserStore();

const isOpen = ref(false);
const searchKeyword = ref('');
const showLimit = ref(50);
const containerRef = ref<HTMLElement>();
const searchInputRef = ref<HTMLInputElement>();
const listRef = ref<HTMLElement>();

// 全部用户来自 store,仅显示与当前用户同部门的成员
const allUsers = computed(() => {
  const currentDept = userStore.currentUser?.deptCode;
  if (!currentDept) return userStore.users;
  return userStore.users.filter(u => u.deptCode === currentDept);
});

// 前端搜索过滤
const filteredResults = computed(() => {
  const kw = searchKeyword.value.trim().toLowerCase();
  if (!kw) return allUsers.value;
  return allUsers.value.filter(u =>
    u.name.toLowerCase().includes(kw) ||
    u.id.toLowerCase().includes(kw)
  );
});

// 限制显示数量，避免渲染太多 DOM
const displayedResults = computed(() => {
  return filteredResults.value.slice(0, showLimit.value);
});

// 已选用户（从全量 store 中查找，确保已选标签始终可见）
const selectedUsers = computed(() => {
  if (props.modelValue.length === 0) return [];
  const userMap = new Map(userStore.users.map(u => [u.id, u]));
  return props.modelValue
    .filter(id => userMap.has(id))
    .map(id => userMap.get(id)!);
});

const isSelected = (id: string) => props.modelValue.includes(id);

const toggleOpen = () => {
  if (isOpen.value) {
    closeDropdown();
  } else {
    openDropdown();
  }
};

const openDropdown = () => {
  isOpen.value = true;
  searchKeyword.value = '';
  showLimit.value = 50;
  setTimeout(() => {
    searchInputRef.value?.focus();
  }, 100);
};

const closeDropdown = () => {
  isOpen.value = false;
};

const toggleUser = (id: string) => {
  const newValue = [...props.modelValue];
  const index = newValue.indexOf(id);
  if (index === -1) {
    newValue.push(id);
  } else {
    newValue.splice(index, 1);
  }
  emit('update:modelValue', newValue);
};

const removeUser = (id: string) => {
  emit('update:modelValue', props.modelValue.filter(uid => uid !== id));
};

// 列表滚动到底部时加载更多
const onListScroll = () => {
  const el = listRef.value;
  if (!el) return;
  if (el.scrollHeight - el.scrollTop - el.clientHeight < 20) {
    showLimit.value += 50;
  }
};

// 点击外部关闭
const handleClickOutside = (e: MouseEvent) => {
  if (containerRef.value && !containerRef.value.contains(e.target as Node)) {
    closeDropdown();
  }
};

onMounted(() => {
  document.addEventListener('click', handleClickOutside);
});

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside);
});
</script>

<style scoped>
.dropdown-enter-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}
.dropdown-leave-active {
  transition: opacity 0.1s ease, transform 0.1s ease;
}
.dropdown-enter-from {
  opacity: 0;
  transform: translateY(-4px);
}
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
