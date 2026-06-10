<template>
  <div :class="['rounded-lg border border-secondary-200 bg-white', isLeaf ? '' : 'shadow-sm']">
    <!-- 节点头部：name + count + 展开按钮 -->
    <button
      type="button"
      @click="toggle"
      class="flex w-full items-center justify-between gap-3 rounded-lg px-4 py-2.5 text-left transition-colors hover:bg-secondary-50"
      :aria-expanded="expanded"
    >
      <div class="flex items-center gap-2 min-w-0">
        <span
          class="inline-flex h-5 w-5 flex-shrink-0 items-center justify-center text-secondary-400 transition-transform"
          :class="{ 'rotate-90': expanded }"
          v-if="!isLeaf"
        >
          <svg viewBox="0 0 20 20" fill="currentColor" class="h-4 w-4">
            <path fill-rule="evenodd" d="M7.21 14.77a.75.75 0 01.02-1.06L10.94 10 7.23 6.29a.75.75 0 111.04-1.08l4.25 4.25a.75.75 0 010 1.08l-4.25 4.25a.75.75 0 01-1.06-.02z" clip-rule="evenodd" />
          </svg>
        </span>
        <span v-else class="inline-block h-5 w-5 flex-shrink-0" />

        <span
          class="inline-flex h-5 w-5 flex-shrink-0 items-center justify-center rounded text-xs font-medium"
          :class="badgeClass"
          v-if="!isLeaf"
        >
          L{{ node.level }}
        </span>

        <span class="truncate text-sm font-medium text-secondary-900">
          {{ node.name }}
        </span>

        <span class="text-xs text-secondary-400 font-mono">{{ node.code }}</span>
      </div>

      <span class="flex-shrink-0 rounded-full bg-secondary-100 px-2 py-0.5 text-xs font-medium text-secondary-600">
        {{ totalCount }} 人
      </span>
    </button>

    <!-- 展开内容 -->
    <div v-if="expanded" :class="isLeaf ? 'border-t border-secondary-200 p-4' : 'border-t border-secondary-200 p-2 space-y-2'">
      <!-- 非叶子节点：递归子组织 -->
      <div v-if="!isLeaf" class="ml-4 space-y-2">
        <OrgGroup
          v-for="child in node.children"
          :key="child.code"
          :node="child"
          :members="members"
        />
      </div>

      <!-- 叶子节点：成员卡片网格 -->
      <div v-else>
        <div v-if="leafMembers.length === 0" class="py-4 text-center text-xs text-secondary-400">
          暂无成员
        </div>
        <div v-else>
          <div class="grid grid-cols-1 gap-3 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4">
            <div
              v-for="user in visibleMembers"
              :key="user.id"
              class="flex items-start gap-3 rounded-lg border border-secondary-200 bg-white p-3 transition-shadow hover:shadow-sm"
            >
              <UserAvatar :name="displayName(user)" :seed="user.avatar" size="md" />
              <div class="min-w-0 flex-1">
                <div class="truncate text-sm font-medium text-secondary-900">
                  {{ displayName(user) }}
                </div>
                <div v-if="user.chineseNam && user.chineseNam !== user.name" class="truncate text-xs text-secondary-500">
                  {{ user.name }}
                </div>
                <div class="mt-0.5 truncate font-mono text-xs text-secondary-500">
                  {{ user.id }}
                </div>
                <div class="mt-1">
                  <Badge :variant="roleBadgeVariant(user.role)">
                    {{ roleLabel(user.role) }}
                  </Badge>
                </div>
              </div>
            </div>
          </div>

          <div v-if="leafMembers.length > pageSize" class="mt-3 text-center">
            <button
              type="button"
              @click="loadMore"
              class="text-xs font-medium text-primary-600 hover:text-primary-700"
            >
              加载更多 ({{ leafMembers.length - visibleCount }} 人未显示)
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import type { OrgNode, User } from '@/types';
import UserAvatar from '@/components/common/UserAvatar.vue';
import Badge from '@/components/common/Badge.vue';

const props = defineProps<{
  /** 当前组织节点 */
  node: OrgNode;
  /** 全量用户列表（按 deptCode 索引） */
  members: User[];
}>();

const expanded = ref(false);
const visibleCount = ref(12);
const pageSize = 12;

const isLeaf = computed(() => !props.node.children || props.node.children.length === 0);

const membersByCode = computed<Record<string, User[]>>(() => {
  const map: Record<string, User[]> = {};
  for (const u of props.members) {
    const code = u.deptCode;
    if (!code) continue;
    if (!map[code]) map[code] = [];
    map[code].push(u);
  }
  return map;
});

const leafMembers = computed<User[]>(() => membersByCode.value[props.node.code] || []);

const visibleMembers = computed<User[]>(() => leafMembers.value.slice(0, visibleCount.value));

const totalCount = computed(() => {
  if (isLeaf.value) return leafMembers.value.length;
  // 非叶子：递归统计所有后代 + 当前节点的成员
  return countTreeMembers(props.node);
});

function countTreeMembers(n: OrgNode): number {
  let sum = (membersByCode.value[n.code] || []).length;
  for (const c of n.children || []) {
    sum += countTreeMembers(c);
  }
  return sum;
}

function toggle() {
  expanded.value = !expanded.value;
}

function loadMore() {
  visibleCount.value += pageSize;
}

const badgeClass = computed(() => {
  const lvl = props.node.level;
  if (lvl <= 2) return 'bg-primary-100 text-primary-700';
  if (lvl <= 4) return 'bg-info-100 text-info-700';
  return 'bg-secondary-100 text-secondary-600';
});

function displayName(user: User): string {
  // 优先用中文名
  return user.chineseNam && user.chineseNam.trim() ? user.chineseNam : user.name;
}

function roleLabel(role: string): string {
  const normalized = role?.replace(/_/g, '-');
  const map: Record<string, string> = {
    'admin': '管理员',
    'project-manager': '项目经理',
    'member': '成员',
    'viewer': '观察者',
  };
  return map[normalized] || role;
}

function roleBadgeVariant(role: string): 'default' | 'primary' | 'danger' | 'success' | 'warning' | 'info' {
  const normalized = role?.replace(/_/g, '-');
  const map: Record<string, 'danger' | 'warning' | 'primary' | 'default'> = {
    'admin': 'danger',
    'project-manager': 'warning',
    'member': 'primary',
    'viewer': 'default',
  };
  return map[normalized] || 'default';
}
</script>
