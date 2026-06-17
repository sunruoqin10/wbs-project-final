<template>
  <div class="relative" ref="rootRef">
    <!-- 触发按钮 -->
    <button
      type="button"
      class="flex w-72 items-center justify-between gap-2 rounded-md
             border border-secondary-300 bg-white px-3 py-1.5 text-sm
             text-secondary-700 hover:border-primary-400 focus:outline-none
             focus:ring-2 focus:ring-primary-500"
      :class="{ 'opacity-60 cursor-not-allowed': loading }"
      :disabled="loading"
      @click="open = !open"
    >
      <span class="truncate">{{ displayLabel }}</span>
      <svg
        class="h-4 w-4 shrink-0 text-secondary-400 transition-transform"
        :class="{ 'rotate-180': open }"
        fill="none" stroke="currentColor" viewBox="0 0 24 24"
      >
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
      </svg>
    </button>

    <!-- 下拉浮层 -->
    <div
      v-if="open"
      class="absolute left-0 top-full z-20 mt-1 w-80 rounded-md
             border border-secondary-200 bg-white shadow-lg"
    >
      <!-- 搜索框 -->
      <div class="border-b border-secondary-100 p-2">
        <input
          v-model="searchText"
          type="text"
          :placeholder="placeholder || $t('dashboard.departmentFilter.placeholder')"
          class="w-full rounded border border-secondary-200 px-2 py-1 text-sm
                 focus:border-primary-400 focus:outline-none"
        />
      </div>

      <!-- 列表 -->
      <div class="max-h-80 overflow-y-auto py-1">
        <!-- 未分配 节点 -->
        <button
          type="button"
          class="flex w-full items-center gap-2 px-3 py-1.5 text-left text-sm
                 hover:bg-secondary-50"
          :class="{ 'bg-primary-50 text-primary-700': modelValue === null }"
          @click="select(null)"
        >
          <svg class="h-4 w-4 text-secondary-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01" />
          </svg>
          <span>{{ $t('dashboard.departmentFilter.unassigned') }}</span>
        </button>

        <div v-if="loading" class="px-3 py-2 text-xs text-secondary-400">
          {{ $t('common.loading') }}
        </div>

        <div v-else-if="loadError" class="px-3 py-2 text-xs">
          <div class="text-danger-600">{{ loadError }}</div>
          <button
            type="button"
            class="mt-1 text-primary-600 hover:underline"
            @click="retryLoad"
          >
            {{ $t('common.retry') }}
          </button>
        </div>

        <template v-else>
          <div
            v-for="company in companyGroups"
            :key="company.code"
            class="border-t border-secondary-50 first:border-t-0"
          >
            <div class="px-3 py-1 text-xs font-semibold text-secondary-500">
              {{ company.name }}
            </div>
            <div
              v-for="node in filterTree(company.children, searchText)"
              :key="node.code || node.name"
            >
              <button
                type="button"
                class="flex w-full items-center gap-2 px-3 py-1 text-left text-sm
                       hover:bg-secondary-50"
                :class="{
                  'bg-primary-50 text-primary-700': modelValue === node.code
                }"
                :style="{ paddingLeft: `${((node.level ?? 0) - minLevel) * 12 + 24}px` }"
                @click="select(node.code)"
              >
                <span class="truncate">{{ node.name }}</span>
                <span v-if="node.code && node.children && node.children.length > 0"
                      class="text-xs text-secondary-400">
                  ({{ node.children.length }})
                </span>
              </button>
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useOrgStore } from '@/stores/org';
import type { OrgNode } from '@/types';

interface CompanyGroup {
  code: string;
  name: string;
  children: OrgNode[];
}

const props = defineProps<{
  modelValue: string | null;
  placeholder?: string;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', v: string | null): void;
}>();

const { t } = useI18n();
const orgStore = useOrgStore();

const open = ref(false);
const searchText = ref('');
const loading = ref(false);
const loadError = ref<string | null>(null);
const rootRef = ref<HTMLElement | null>(null);

/**
 * 公司分组:4 个公司顶层节点
 * 服务端 OrgService 已经按公司码分组,这里只读取顶层 children
 */
const companyGroups = computed<CompanyGroup[]>(() => {
  const root = orgStore.tree;
  if (!root || !root.children) return [];
  return root.children.map((c) => ({
    code: c.code || c.companyCd || c.name,
    name: c.name,
    children: c.children || [],
  }));
});

const displayLabel = computed(() => {
  if (props.modelValue === null) {
    return t('dashboard.departmentFilter.unassigned');
  }
  const found = findNode(orgStore.tree, props.modelValue);
  return found?.name || props.modelValue;
});

function findNode(root: OrgNode | null, code: string): OrgNode | null {
  if (!root) return null;
  if (root.code === code) return root;
  for (const c of root.children || []) {
    const hit = findNode(c, code);
    if (hit) return hit;
  }
  return null;
}

/**
 * 计算最小 level(用于平铺缩进归一化)
 */
const minLevel = computed(() => {
  let min = Number.POSITIVE_INFINITY;
  function walk(n: OrgNode) {
    if (n.code && n.level < min) min = n.level;
    for (const c of n.children || []) walk(c);
  }
  if (orgStore.tree) walk(orgStore.tree);
  return min === Number.POSITIVE_INFINITY ? 0 : min;
});

/**
 * 扁平化(无搜索)或按 searchText 过滤(有搜索)
 */
function filterTree(nodes: OrgNode[], search: string): OrgNode[] {
  if (!search) return flatten(nodes);
  const lower = search.toLowerCase();
  const result: OrgNode[] = [];
  function dfs(node: OrgNode) {
    if (node.code && node.name && node.name.toLowerCase().includes(lower)) {
      result.push(node);
    }
    for (const c of node.children || []) dfs(c);
  }
  for (const n of nodes) dfs(n);
  return result;
}

function flatten(nodes: OrgNode[]): OrgNode[] {
  const out: OrgNode[] = [];
  function walk(n: OrgNode) {
    if (n.code) out.push(n);
    for (const c of n.children || []) walk(c);
  }
  for (const n of nodes) walk(n);
  return out;
}

function select(code: string | null) {
  emit('update:modelValue', code);
  open.value = false;
  searchText.value = '';
}

async function loadTree() {
  loading.value = true;
  loadError.value = null;
  try {
    await orgStore.loadTree(true);
  } catch (e: any) {
    loadError.value = t('dashboard.departmentFilter.loadFailed') || '加载失败';
  } finally {
    loading.value = false;
  }
}

async function retryLoad() {
  await loadTree();
}

function handleClickOutside(e: MouseEvent) {
  if (!open.value) return;
  if (rootRef.value && !rootRef.value.contains(e.target as Node)) {
    open.value = false;
  }
}

onMounted(async () => {
  if (!orgStore.tree) {
    await loadTree();
  }
  document.addEventListener('click', handleClickOutside);
});

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside);
});

watch(open, (v) => {
  if (!v) searchText.value = '';
});
</script>
