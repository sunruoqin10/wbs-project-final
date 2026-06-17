# 仪表盘按部门筛选 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** admin 角色进入 `/dashboard` 时,默认看到登录者所在部门的项目数据,可通过部门下拉 + 「含子部门」勾选实时切换,所有统计/图表/列表联动。

**Architecture:** 纯前端方案。在 `Dashboard.vue` 现有「先拉全量再前端聚合」基础上,叠加一层「按部门二次过滤」:`userProjects` 不动,新增 `deptFilteredProjects` 作为下游纯函数;任务、统计、近期项目、即将到期任务 全部从 `deptFilteredProjects` 派生。新增一个可复用 `OrgTreeSelect.vue` 树形下拉消费 `useOrgStore.tree`。后端零改动。

**Tech Stack:** Vue 3 `<script setup lang="ts">`、Pinia、Tailwind CSS、ECharts(已有)、vue-i18n(已有)。**无新依赖**。

---

## File Map

| 状态 | 路径 | 职责 |
|------|------|------|
| 新增 | `frontend/src/components/common/OrgTreeSelect.vue` | 树形下拉选择器,modelValue: string \| null(null=未分配) |
| 改   | `frontend/src/views/Dashboard.vue` | 新增 filter 状态、deptFilteredProjects 派生、filter 卡片 + Transition |
| 改   | `frontend/src/i18n/locales/zh.ts` | 新增 dashboard.departmentFilter.* 7 个 key |
| 改   | `frontend/src/i18n/locales/ko.ts` | 同上,韩语 |
| 改   | `frontend/src/i18n/locales/en.ts` | 同上,英语 |

---

## Task 1: 新增 i18n key(三语同步)

**Files:**
- Modify: `frontend/src/i18n/locales/zh.ts`
- Modify: `frontend/src/i18n/locales/ko.ts`
- Modify: `frontend/src/i18n/locales/en.ts`

- [ ] **Step 1: 在 `frontend/src/i18n/locales/zh.ts` 中定位 dashboard 命名空间**

用 Grep 工具搜索 `dashboard: {` 或 `dashboard:` 找到对象字面量,找到 `charts:` 或 `recentProjects:` 等同级 key 所在的层级。在 `dashboard` 对象内,找一个合适位置(紧跟 `welcome` 之后或同级末尾),新增 7 个 key。

- [ ] **Step 2: 在 zh.ts 中新增以下 7 个 key + `common.retry`**

```ts
departmentFilter: {
  label: '按部门查看',
  placeholder: '请选择部门',
  includeSubDepts: '含子部门',
  unassigned: '未分配',
  leafHint: '叶子部门,无需展开',
  emptyHint: '该部门暂无项目',
  deptMissing: '您的部门信息缺失,已默认展示未分配项目',
},
```

**位置说明**:在 dashboard 对象内(与 `welcome` / `stats` / `charts` / `recentProjects` / `upcomingTasks` 同级),具体放在最后一个属性后即可。

同时在 `common` 对象内(`loading` 之后)新增:
```ts
retry: '重试',
```

- [ ] **Step 3: 在 `frontend/src/i18n/locales/ko.ts` 中同步新增 7 个 key + `common.retry`**

```ts
departmentFilter: {
  label: '부서별 보기',
  placeholder: '부서를 선택하세요',
  includeSubDepts: '하위 부서 포함',
  unassigned: '미할당',
  leafHint: '하위 부서 없음, 펼치기 불필요',
  emptyHint: '이 부서에 프로젝트가 없습니다',
  deptMissing: '부서 정보가 없어 미할당 프로젝트를 표시합니다',
},
```

`common.retry`:
```ts
retry: '재시도',
```

- [ ] **Step 4: 在 `frontend/src/i18n/locales/en.ts` 中同步新增 7 个 key + `common.retry`**

```ts
departmentFilter: {
  label: 'Filter by Department',
  placeholder: 'Select a department',
  includeSubDepts: 'Include sub-departments',
  unassigned: 'Unassigned',
  leafHint: 'Leaf department, no children',
  emptyHint: 'No projects in this department',
  deptMissing: 'Department info missing, showing unassigned',
},
```

`common.retry`:
```ts
retry: 'Retry',
```

- [ ] **Step 5: 确认三个文件类型一致**

运行:
```bash
cd frontend
npx vue-tsc --noEmit
```

**预期输出**:无类型错误。如果报 `Property 'departmentFilter' does not exist on type ...` 表示 key 路径嵌套不对,检查是否放进了正确的对象层级。

- [ ] **Step 6: 留待用户手动 commit**

按 CLAUDE.md 规则,本任务完成不自动 commit。在所有任务结束后,会列出本任务及其他所有任务的改动文件清单,由用户手动 commit。

---

## Task 2: 新增 `OrgTreeSelect.vue` 组件

**Files:**
- Create: `frontend/src/components/common/OrgTreeSelect.vue`

- [ ] **Step 1: 创建组件骨架**

新建 `frontend/src/components/common/OrgTreeSelect.vue`,写入:

```vue
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
              :key="node.code"
            >
              <button
                type="button"
                class="flex w-full items-center gap-2 px-3 py-1 text-left text-sm
                       hover:bg-secondary-50"
                :class="{
                  'bg-primary-50 text-primary-700': modelValue === node.code
                }"
                :style="{ paddingLeft: `${(node.level || 0) * 12 + 24}px` }"
                @click="select(node.code)"
              >
                <span class="truncate">{{ node.name }}</span>
                <span v-if="node.code && node.children.length > 0"
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
 * 把根节点的 children(4 个公司层)转换为带显示名的公司分组
 * 服务端 OrgService 已经做了公司分组,这里只读取顶层 children
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
  // 在树中查找
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
 * 扁平化并按 searchText 过滤(递归展开所有匹配节点)
 */
function filterTree(nodes: OrgNode[], search: string): OrgNode[] {
  if (!search) return flatten(nodes, 0);
  const lower = search.toLowerCase();
  const result: OrgNode[] = [];
  function dfs(node: OrgNode, level: number) {
    if (node.name && node.name.toLowerCase().includes(lower)) {
      // 命中,把该节点(以及完整子树下所有节点)都加入结果以保留上下文
      result.push({ ...node, level });
    }
    for (const c of node.children || []) dfs(c, level + 1);
  }
  for (const n of nodes) dfs(n, 0);
  return result;
}

function flatten(nodes: OrgNode[], level: number, out: OrgNode[] = []): OrgNode[] {
  for (const n of nodes) {
    if (n.code) out.push({ ...n, level });
    if (n.children) flatten(n.children, level + 1, out);
  }
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
    loadError.value = '加载失败';
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
```

- [ ] **Step 2: 验证类型检查通过**

运行:
```bash
cd frontend
npx vue-tsc --noEmit
```

**预期输出**:无错误。如果报 `Property 'children' does not exist on type 'OrgNode'` 之类的错误,检查 `frontend/src/types/index.ts` 的 `OrgNode` 定义是否一致(本组件代码已用 `c.children || []` 兜底)。

- [ ] **Step 3: 留待用户手动 commit**

---

## Task 3: Dashboard.vue 新增 state + computed

**Files:**
- Modify: `frontend/src/views/Dashboard.vue`(仅 `<script setup>` 段)

- [ ] **Step 1: 读取文件当前状态**

确认 Dashboard.vue 当前的 `<script setup>` 中已有的 import / state:
- 已有: `useUserStore`, `useProjectStore`, `useTaskStore`, `usePermissionStore`, `useOrgStore` 需新增
- 已有: `currentUser`, `userProjects`, `userTasks`, `recentProjects`, `statistics`, `planVsActual` 等 computed
- 已有: `onMounted`, `onUnmounted`, `watch`

读取文件的第 280-340 行(imports 与 computed 定义区)作为基准。

- [ ] **Step 2: 在 imports 区域增加 useOrgStore / useI18n 引用**

定位到 `import { usePermissionStore } from '@/stores/permission';` 之后,新增:

```ts
import { useOrgStore } from '@/stores/org';
import { useI18n } from 'vue-i18n';
```

(如果 `useI18n` 已在顶部声明则跳过第二步此行)

- [ ] **Step 3: 在 usePermissionStore 之后增加 orgStore 实例**

定位到 `const permissionStore = usePermissionStore();` 之后,**只新增**以下一行(useI18n 已经在文件顶部声明过,不要重复):

```ts
const orgStore = useOrgStore();
```

> **注意**:Dashboard.vue 顶部已有 `const { t } = useI18n();`,**不要再调用 useI18n**,否则会冲突。模板里直接用 `{{ $t(...) }}` 即可。

- [ ] **Step 4: 在 userProjects 之前插入部门过滤相关 state 与 computed**

定位到第 300 行附近的 `// 根据角色过滤项目:...` 注释之前,新增以下整段(必须放在 `userProjects` 之前,因为后面要引用 `userProjects.value`):

```ts
// ============ 部门过滤(admin 专用) ============ //
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

/** 部门二次过滤后的项目列表(基于现有 userProjects) */
const deptFilteredProjects = computed(() => {
  return userProjects.value.filter(p =>
    effectiveDeptCodes.value.has(p.deptCode ?? null)
  );
});
```

**注意**:`OrgNode` 类型需要从 `@/types` 导入(如果之前没导入):

```ts
import type { OrgNode } from '@/types';
```

- [ ] **Step 5: 修改 userProjects(保持原状,仅确认未动)**

`userProjects` 计算属性**不要动**。它仍然按角色数据范围过滤,`deptFilteredProjects` 是其下游。

- [ ] **Step 6: 修改 userTasks — 改为基于 deptFilteredProjects**

定位到 `const userTasks = computed(() => { ... })`,把整个函数体替换为:

```ts
const userTasks = computed(() => {
  if (!currentUser.value) return [];
  const userProjectIds = new Set(deptFilteredProjects.value.map(p => p.id));
  return taskStore.tasks.filter(t => userProjectIds.has(t.projectId));
});
```

(原代码有 `if (admin/pm) return taskStore.tasks;` 早返回,删除该分支——所有角色统一口径)

- [ ] **Step 7: 修改 recentProjects — 改为基于 deptFilteredProjects**

定位到 `const recentProjects = computed(() => userProjects.value.slice(0, 5));`,替换为:

```ts
const recentProjects = computed(() => deptFilteredProjects.value.slice(0, 5));
```

- [ ] **Step 8: 修改 statistics 中 projects 来源**

定位到 `const statistics = computed(() => { ... })`,把 `const projects = userProjects.value;` 一行替换为:

```ts
const projects = deptFilteredProjects.value;
```

**tasks 那一行(`const tasks = userTasks.value;`)保持不变** —— `userTasks` 已经在 Step 6 改为基于 `deptFilteredProjects`,自动联动。

- [ ] **Step 9: 验证 vue-tsc 通过**

运行:
```bash
cd frontend
npx vue-tsc --noEmit
```

**预期输出**:无错误。如果报 `Cannot find name 'OrgNode'`,补 import。如果报 `collectDescendants` 未定义,检查 Step 4 整段是否完整插入。

- [ ] **Step 10: 留待用户手动 commit**

---

## Task 4: Dashboard.vue 模板 — 部门筛选行 + Transition 动画

**Files:**
- Modify: `frontend/src/views/Dashboard.vue`(仅 `<template>` 段)

- [ ] **Step 1: 定位模板锚点**

Dashboard.vue 模板顶部结构:
```
<MainLayout>
  <div class="space-y-6">
    <!-- Page Header -->
    <div class="flex items-center">
      ...<h1>{{ $t('routes.dashboard') }}</h1>
    </div>
    <!-- Stats Cards -->
    <div class="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
      ...
```

需要在「Page Header」之后、「Stats Cards」之前插入部门筛选 Card。

- [ ] **Step 2: 插入部门筛选 Card(包 Card + 切换遮罩 + v-if 限定 admin)**

在 `<!-- Page Header -->` 那个 `<div class="flex items-center">` **结束后**(即 `</div>` 闭合后)的下一行,`<!-- Stats Cards -->` 注释之前,插入:

```vue
      <!-- 部门筛选行(仅 admin 可见) -->
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
```

- [ ] **Step 3: 包下游内容为 keyed `<Transition>` 容器**

定位到 `<!-- Stats Cards -->` 那个 `<div class="grid grid-cols-1 ...">` 之前,新增:

```vue
      <Transition name="content-fade" mode="out-in">
        <div :key="`${selectedDeptCode}-${includeSubDepts}`">
```

并在「即将到期任务列表 Card」结束之后、「主 `<div class="space-y-6">` 闭合之前」新增对应的 `</div>` 和 `</Transition>` 闭合标签。

**重要**:必须保证 `</Transition>` 嵌套正确,以下是结构示意:

```vue
      <Transition name="content-fade" mode="out-in">
        <div :key="`${selectedDeptCode}-${includeSubDepts}`">

          <!-- 原本的 4 Stats Cards / 3 进度对比 / 2 图表 / 近期项目表 / 即将到期任务列表 -->
          ...原有内容保持原样...

        </div>
      </Transition>
```

具体做法:在原本「Stats Cards」开始之前加 `<Transition>...<div :key=...>`,在「即将到期任务列表」结束之后(`</Card>`)加 `</div></Transition>`。**不要修改原有 5 个区块的内部结构**,只包一层。

- [ ] **Step 4: 调整「近期项目表」的空态显示**

定位到「Recent Projects」Card 内部的 `<table>...</table>` 之后,新增空态行(放在 `</table>` 后,`</Card>` 前):

```vue
          <div
            v-if="recentProjects.length === 0"
            class="py-8 text-center text-sm text-secondary-400"
          >
            {{ $t('dashboard.departmentFilter.emptyHint') }}
          </div>
```

- [ ] **Step 5: 验证 vue-tsc 通过**

运行:
```bash
cd frontend
npx vue-tsc --noEmit
```

**预期输出**:无错误。重点检查 `<Transition>` 嵌套、`v-if` 中 `currentUser` 的可选链(`?.role`)。

- [ ] **Step 6: 留待用户手动 commit**

---

## Task 5: 添加 CSS 过渡样式

**Files:**
- Modify: `frontend/src/views/Dashboard.vue`(`<style scoped>` 段)

- [ ] **Step 1: 定位 style 段**

Dashboard.vue 文件底部是否有 `<style scoped>`?如果没有,新增;如果有,在已有内容尾部追加。

- [ ] **Step 2: 追加 fade 与 content-fade 过渡 CSS**

```css
.fade-enter-active,
.fade-leave-active {
  transition: opacity 100ms ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.content-fade-enter-active,
.content-fade-leave-active {
  transition: opacity 200ms ease, transform 200ms ease;
}
.content-fade-enter-from {
  opacity: 0;
  transform: translateY(4px);
}
.content-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
```

- [ ] **Step 3: 验证构建**

```bash
cd frontend
npx vue-tsc --noEmit
```

预期:0 错误。

- [ ] **Step 4: 留待用户手动 commit**

---

## Task 6: 构建验证

**Files:** 零代码改动,仅验证

- [ ] **Step 1: 完整类型检查**

```bash
cd frontend
npx vue-tsc --noEmit
```

预期:`Found 0 errors`。

- [ ] **Step 2: 完整构建**

```bash
cd frontend
npm run build
```

预期:`vite build` 成功,`vue-tsc` 0 错误,产物在 `frontend/dist/`。

- [ ] **Step 3: 后端基线确认(可选)**

```bash
cd backend
mvn clean install -DskipTests
```

预期:BUILD SUCCESS(后端无改动,作为 sanity check)。

---

## Task 7: 手动 smoke test(spec §7.2 全部 15 个场景)

**Files:** 零代码改动,需用户手动启动服务

**前置**(用户手动):
- 启动后端:`cd backend && mvn spring-boot:run`(8084)
- 启动前端:`cd frontend && npm run dev`(5173)
- 准备测试账号:1 个 admin(`deptCode` 非空)、1 个 dept-pm、1 个 pm、1 个 member
- 准备数据:4 个不同部门的项目(含 1 个 `deptCode=null` 的历史项目)

按 [spec §7.2](../../specs/2026-06-17-dashboard-dept-filter-design.md#72-前端手动验证清单) 15 个场景逐项验证,每项打勾。

- [ ] 场景 1:admin 进入 → 默认部门 + 数字正确
- [ ] 场景 2:切换叶子部门 → 100ms 后更新 + 遮罩闪现
- [ ] 场景 3:选中「未分配」 → checkbox 禁用
- [ ] 场景 4:中层 + 含子部门 → 递归聚合
- [ ] 场景 5:叶子部门勾选 → checkbox 始终 disabled
- [ ] 场景 6:无项目部门 → 统计归零 + emptyHint
- [ ] 场景 7:快速连点 5 次 → 最终态正确
- [ ] 场景 8:刷新 → 重置回默认
- [ ] 场景 9:切换语言 → 三语同步
- [ ] 场景 10:member 登录 → 筛选行不可见
- [ ] 场景 11:dept-pm 登录 → 筛选行不可见
- [ ] 场景 12:admin 无 deptCode → 降级「未分配」+ deptMissing
- [ ] 场景 13:组织树 500 → 失败重试 + 卡片不阻塞
- [ ] 场景 14:1280 / 1024 宽度 → 不破版
- [ ] 场景 15:Performance 面板 → 切换 < 100ms,无 console error

---

## Task 8: 收尾 — 列出 commit 文件清单

**Files:** 零代码改动,仅信息汇总

- [ ] **Step 1: 列出本次所有变更文件**

| 状态 | 路径 |
|------|------|
| 新增 | `frontend/src/components/common/OrgTreeSelect.vue` |
| 改   | `frontend/src/views/Dashboard.vue` |
| 改   | `frontend/src/i18n/locales/zh.ts` |
| 改   | `frontend/src/i18n/locales/ko.ts` |
| 改   | `frontend/src/i18n/locales/en.ts` |
| 新增 | `docs/superpowers/specs/2026-06-17-dashboard-dept-filter-design.md`(可选随代码一起提交) |
| 新增 | `docs/superpowers/plans/2026-06-17-dashboard-dept-filter.md`(可选随代码一起提交) |

- [ ] **Step 2: 由用户手动 commit**

按 CLAUDE.md 规则,本会话**不自动 commit**。请用户自行:

```bash
git add frontend/src/components/common/OrgTreeSelect.vue
git add frontend/src/views/Dashboard.vue
git add frontend/src/i18n/locales/zh.ts
git add frontend/src/i18n/locales/ko.ts
git add frontend/src/i18n/locales/en.ts
git commit -m "feat(dashboard): admin 按部门筛选项目数据"
```

可选追加:
```bash
git add docs/superpowers/specs/2026-06-17-dashboard-dept-filter-design.md
git add docs/superpowers/plans/2026-06-17-dashboard-dept-filter.md
git commit -m "docs: add dashboard dept-filter spec and plan"
```

---

## Self-Review

**1. Spec coverage**:
- §1 背景/目标 → Task 1-5 全部
- §2.1 部门匹配口径 → Task 3 Step 4-8
- §2.2 部门范围 → Task 2(未分配)+ Task 3(leaf 判定)
- §2.3 UI 控件 → Task 2 + Task 4(全部 7 个维度)
- §3 数据流 → 全部 Task 体现
- §4 组件拆分 → Task 2 + Task 3
- §5 UI/UX → Task 4 + Task 5
- §6 边界 A-H → Task 2(loadError 兜底)+ Task 3(deptMissing)+ Task 4(emptyHint)
- §7 测试 → Task 6 + Task 7
- §8 改动文件清单 → Task 8

**2. Placeholder scan**:
- 无 TBD / TODO / "类似" / 占位符。
- 步骤 4 插入的整段代码已经过完整定义。
- 步骤 5 的 CSS 已给完整代码。

**3. Type consistency**:
- `OrgNode` 在 Task 2 内部用 `node.children || []` 兜底,与 `frontend/src/types/index.ts` 的 OrgNode 定义一致。
- `modelValue: string | null` 在 Task 2、Task 3、Task 4 中一致使用。
- `selectedDeptCode`、`includeSubDepts`、`switching`、`isLeaf`、`effectiveDeptCodes`、`deptFilteredProjects` 在所有 task 中名字一致。
- `OrgTreeSelect` 组件名 Task 2 / Task 4 一致。

**4. 已修复**:
- Step 3 中,`useI18n` 重复声明问题已加注释提醒(只新增 orgStore 即可)。
- 切换遮罩 100ms 延迟,Task 3 Step 4 与 spec §5.3 一致。
