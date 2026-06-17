<template>
  <MainLayout>
    <div class="space-y-6">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-secondary-900">{{ $t('documents.title') }}</h1>
          <p class="mt-1 text-sm text-secondary-600">{{ $t('documents.subtitle') }}</p>
        </div>
      </div>

      <!-- 部门筛选行(2026-06-17 新增,仅 admin 可见,与 Dashboard / ProjectList 保持一致) -->
      <Card v-if="currentUser?.role === 'admin'" class="relative">
        <div class="flex items-center gap-4">
          <span class="text-sm font-medium text-secondary-600">
            {{ $t('dashboard.departmentFilter.label') }}
          </span>
          <OrgTreeSelect
            v-model="selectedDeptCode"
            @update:modelValue="onDeptChange"
          />
          <label
            class="flex items-center gap-2 text-sm text-secondary-600"
            :class="{ 'cursor-not-allowed opacity-50': isLeaf }"
            :title="isLeaf ? $t('dashboard.departmentFilter.leafHint') : ''"
          >
            <input
              type="checkbox"
              v-model="includeSubDepts"
              :disabled="isLeaf"
              class="h-4 w-4 rounded border-secondary-300 text-primary-600
                     focus:ring-primary-500 disabled:opacity-50"
            />
            <span>{{ $t('dashboard.departmentFilter.includeSubDepts') }}</span>
          </label>
        </div>
        <!-- deptMissing 提示:当 admin 没有 deptCode 时显示 -->
        <p
          v-if="currentUser && !currentUser.deptCode"
          class="mt-2 text-xs text-warning-600"
        >
          {{ $t('dashboard.departmentFilter.deptMissing') }}
        </p>
        <!-- 切换中遮罩 -->
        <Transition name="fade">
          <div
            v-if="switching"
            class="pointer-events-none absolute inset-0 flex items-center
                   justify-center rounded-lg bg-white/60 backdrop-blur-sm"
          >
            <svg
              class="h-6 w-6 animate-spin text-primary-600"
              fill="none" viewBox="0 0 24 24"
            >
              <circle
                class="opacity-25" cx="12" cy="12" r="10"
                stroke="currentColor" stroke-width="4"
              />
              <path
                class="opacity-75" fill="currentColor"
                d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"
              />
            </svg>
          </div>
        </Transition>
      </Card>

      <Tabs
        v-model="activeTabIndex"
        :tabs="[
          { label: $t('documents.tabs.projectDocuments') },
          { label: $t('documents.tabs.reportDocuments') }
        ]"
        @change="handleTabChange"
        class="mb-6"
      />

      <div class="bg-white rounded-lg shadow-sm p-6">
        <ProjectDocuments v-if="activeTab === 'project'" />
        <ReportDocuments v-else-if="activeTab === 'report'" />
      </div>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed, provide, onMounted } from 'vue';
import MainLayout from '@/components/layout/MainLayout.vue';
import Tabs from '@/components/common/Tabs.vue';
import type { Tab } from '@/components/common/Tabs.vue';
import Card from '@/components/common/Card.vue';
import OrgTreeSelect from '@/components/common/OrgTreeSelect.vue';
import ProjectDocuments from '@/components/document/ProjectDocuments.vue';
import ReportDocuments from '@/components/document/ReportDocuments.vue';
import { useUserStore } from '@/stores/user';
import { useProjectStore } from '@/stores/project';
import { useOrgStore } from '@/stores/org';
import type { OrgNode } from '@/types';

const activeTabIndex = ref(0);
const activeTab = ref<'project' | 'report'>('project');

function handleTabChange(tab: Tab, index: number) {
  if (index === 0) {
    activeTab.value = 'project';
  } else if (index === 1) {
    activeTab.value = 'report';
  }
}

// ============ 部门过滤(admin 专用,与 Dashboard / ProjectList 保持一致,2026-06-17 新增) ============ //
const userStore = useUserStore();
const projectStore = useProjectStore();
const orgStore = useOrgStore();

const currentUser = computed(() => userStore.currentUser);

const selectedDeptCode = ref<string | null>(currentUser.value?.deptCode ?? null);
const includeSubDepts = ref(false);
const switching = ref(false);
let switchTimer: number | null = null;

function onDeptChange(newCode: string | null) {
  switching.value = true;
  if (switchTimer) window.clearTimeout(switchTimer);
  switchTimer = window.setTimeout(() => {
    selectedDeptCode.value = newCode;
    switching.value = false;
  }, 100);
}

/** 工具:从 org 树中找到目标 code 节点,DFS 收集其所有后代 code */
function collectDescendants(root: OrgNode, targetCode: string): string[] {
  const result: string[] = [];
  function find(node: OrgNode): OrgNode | null {
    if (node.code === targetCode) return node;
    for (const c of node.children || []) {
      const hit = find(c);
      if (hit) return hit;
    }
    return null;
  }
  const target = find(root);
  if (!target) return result;
  function walk(n: OrgNode) {
    for (const c of n.children || []) {
      if (c.code) result.push(c.code);
      walk(c);
    }
  }
  walk(target);
  return result;
}

/** 工具:判断指定 code 在树中是否有子节点(用于「含子部门」checkbox enabled) */
function hasChildrenInTree(root: OrgNode | null, code: string): boolean {
  if (!root) return false;
  function dfs(node: OrgNode): boolean {
    if (node.code === code) return (node.children || []).length > 0;
    for (const c of node.children || []) if (dfs(c)) return true;
    return false;
  }
  return dfs(root);
}

const isLeaf = computed(() => {
  if (selectedDeptCode.value === null) return true;
  return !hasChildrenInTree(orgStore.tree, selectedDeptCode.value);
});

const effectiveDeptCodes = computed<Set<string | null>>(() => {
  if (selectedDeptCode.value === null) return new Set([null]);
  const codes = new Set<string | null>([selectedDeptCode.value]);
  if (includeSubDepts.value && orgStore.tree) {
    collectDescendants(orgStore.tree, selectedDeptCode.value).forEach(c => codes.add(c));
  }
  return codes;
});

/** 部门过滤后的项目 ID 集合(Set<string | null>):
 *  - 未选中部门(null)→ 视为「未分配」,只保留 deptCode 为空的项目
 *  - 选中某部门 → 保留 deptCode 命中 effectiveDeptCodes 的项目
 *  非 admin 角色(provide 不会被消费)此值由子组件忽略。
 * 注意:这里返回 Set,子组件做 O(1) 查表。 */
const deptFilteredProjectIds = computed<Set<string | null>>(() => {
  if (currentUser.value?.role !== 'admin') {
    // 非 admin:传 null,子组件忽略该注入,不过滤
    return new Set();
  }
  const ids = new Set<string | null>();
  for (const p of projectStore.projects) {
    if (effectiveDeptCodes.value.has(p.deptCode ?? null)) {
      ids.add(p.id);
    }
  }
  return ids;
});

/** 部门过滤是否生效(用于子组件判断是否要应用该过滤) */
const deptFilterActive = computed(() => currentUser.value?.role === 'admin');

/** 通过 provide 注入,子组件通过 inject 读取。
 * 键名加上组件前缀以避免与项目中其它 provide 冲突。 */
provide('docsDeptFilteredProjectIds', deptFilteredProjectIds);
provide('docsDeptFilterActive', deptFilterActive);

onMounted(async () => {
  // 确保项目和组织树已加载,以便计算部门过滤后的项目 ID
  await Promise.all([
    projectStore.loadProjects(),
    orgStore.loadTree()
  ]);
});
</script>

<style scoped>
/* 部门筛选行切换遮罩淡入淡出(2026-06-17) */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 100ms ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>