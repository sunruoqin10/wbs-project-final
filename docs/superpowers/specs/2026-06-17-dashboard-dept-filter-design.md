# 仪表盘按部门筛选 — 设计

> 日期:2026-06-17
> 状态:已设计,待提交 + spec review
> 前置依赖:无
> 后端改动:无

## 1. 背景与目标

`Dashboard.vue` 当前基于角色数据范围展示项目/任务统计(admin 看全公司,dept-pm 看所管部门,member/viewer 看参与项目)。**所有这些视角都是「按我」**,admin 想要"按部门"查看不同部门下的项目概览时,只能离开 Dashboard 跳到 `ProjectList.vue` 用列表筛,体验差。

**诉求**:
- admin 进入 `/dashboard`,默认看到**登录者所在部门**的项目数据
- admin 可在部门下拉中切换到任意部门(支持搜索 + 树形浏览)
- 可选「含子部门」勾选,选中中层部门时递归包含其下所有子部门的项目
- 提供「未分配」节点,展示 `deptCode IS NULL` 的历史项目
- 切换部门时,4 张统计卡 + 3 张进度对比卡 + 2 个图表 + 近期项目表 + 即将到期任务列表 全部实时联动
- 非 admin 角色**不展示**部门筛选行(回归现有 Dashboard 行为)

**目标**:
- 仅前端改动,零后端改动、零数据库改动
- 不持久化筛选状态(每次进入 /dashboard 重置为登录者部门)
- 切换部门时 100ms 内完成 + 柔和淡入淡出过渡动画
- i18n 三语(zh / ko / en)同步

## 2. 核心规则(决策表)

### 2.1 部门匹配口径

| 维度 | 决定 | 理由 |
|------|------|------|
| 字段 | `project.deptCode` ↔ `user.deptCode` | 项目表已有 `dept_code` 字段(对应 mdm_if_or_a.ORG_CD),HR 同步已回填 |
| 默认值 | `currentUser.deptCode` | 登录者的"叶子部门"最贴近项目归属 |
| `deptCode` 归一化 | `p.deptCode ?? null` | 统一 `null` 表示未分配,避免 `''`/`undefined` 混杂 |
| 任务过滤 | 跟随项目过滤(`task.projectId IN filteredProjectIds`) | "项目属于哪个部门"口径一致,避免"任务跨部门"统计异常 |
| 成员计数 | 维持现有语义 = `deptFilteredProjects` 中 `owner + memberIds` 去重数 | 不是"部门内员工数";改后者是另一个需求,本次不动 |

### 2.2 部门范围

| 维度 | 决定 | 理由 |
|------|------|------|
| 精确 vs 递归 | **可选**(checkbox 控制,默认不勾) | 用户已确认"双向" |
| 叶子部门时 | checkbox disabled + 提示"叶子部门,无需展开" | 避免无意义操作 |
| 「未分配」 | 独立节点(顶部固定),`selectedDeptCode = null` | 与具体部门互斥 |
| 全量视图 | 不提供(用户没要求) | admin 想看全量可去 ProjectList |
| 选中已删除部门 | 静默处理,显示空态 | HR 同步删除 deptCode 极少出现 |

### 2.3 UI 控件

| 维度 | 决定 | 理由 |
|------|------|------|
| 选择器形态 | 树形下拉(可搜索) | 用户已确认 |
| 数据源 | 复用 `useOrgStore.tree`(`/api/orgs/tree`) | 已有缓存,不重复请求 |
| 包装 | 包 `Card` | 用户已确认"视觉分组更强" |
| 过渡动画 | 100ms 延迟 + Vue `<Transition>` 淡入淡出 | 用户已确认 |
| 切换遮罩 | 只覆盖筛选行 Card(不覆盖全 Dashboard) | 用户已确认 |
| 持久化 | 不持久化(每次进入重置) | 用户已确认 |
| 角色限定 | `v-if="currentUser?.role === 'admin'"` | 用户已确认"限定只 admin 可见" |

## 3. 架构与数据流

```
┌──────────────────────────────────────────────────────────────┐
│  浏览器 (admin 登录)                                          │
│                                                              │
│   user 进入 /dashboard                                       │
│      ↓                                                       │
│   onMounted:                                                 │
│     1. projectStore.loadProjects() → 全量项目(admin 视角)     │
│     2. taskStore.loadTasks()     → 全量任务                   │
│     3. userStore.loadUsers()     → 全量用户                   │
│     4. orgStore.loadTree()       → 组织树(OrgTreeSelect 内触发)│
│      ↓                                                       │
│   初始化 selectedDeptCode = currentUser.deptCode ?? null     │
│   初始化 includeSubDepts  = false                            │
│      ↓                                                       │
│   effectiveDeptCodes (computed) = 解析后的 Set<string|null>  │
│     - selectedDeptCode = null         → Set([null])          │
│     - 叶子 + includeSubDepts=false    → Set([code])          │
│     - 中层 + includeSubDepts=true     → Set([code, ...desc]) │
│      ↓                                                       │
│   deptFilteredProjects = userProjects.filter(p =>            │
│     effectiveDeptCodes.has(p.deptCode ?? null)               │
│   )                                                          │
│      ↓                                                       │
│   deptFilteredTasks / statistics / recentProjects /          │
│   upcomingTasks 全部从 deptFilteredProjects 派生              │
│      ↓                                                       │
│   <Transition name="content-fade">                          │
│     Stats Cards / 进度对比 / 图表 / 列表 全部包在 keyed 容器  │
│   </Transition>                                              │
│      ↓                                                       │
│   模板渲染                                                    │
└──────────────────────────────────────────────────────────────┘
```

**关键不变量**:
- `userProjects` 不动(基于角色数据范围);`deptFilteredProjects` 是其下游的纯函数
- `taskStore.loadTasks()` 仍拉全量;部门过滤只看项目归属,不看任务 assignee
- 不新建后端表、不改后端 API、不改 `Result<T>` 契约

## 4. 组件拆分与状态结构

### 4.1 新增文件

#### ① `frontend/src/components/common/OrgTreeSelect.vue`(可复用下拉)

- **Props**:
  - `modelValue: string | null` —— 双向绑定;传 `null` 表示选中「未分配」
  - `placeholder?: string`
- **Emit**: `update:modelValue`
- **行为**:
  - 内部用 `useOrgStore.loadTree()` 拉组织树(已有缓存)
  - 把 OrgService 返回的 4 个公司顶层节点渲染成 4 个折叠组(沿用 `OrgNode` 类型)
  - 每组下递归渲染节点,缩进表达层级
  - 顶部固定一项「未分配」节点(特殊 icon 标识),选中后 `emit('update:modelValue', null)`
  - 搜索框(v-model 内置 `searchText`):对节点 name 做大小写不敏感包含匹配;命中节点自动展开父链;公司组永远展开
  - 选中后关闭 dropdown
- **样式**: 用现有 Tailwind 主题色 + 下拉浮层用 `relative/absolute`(不引新 UI 库)
- **为什么不复用 OrgGroup.vue**: OrgGroup 是只读展示组件,无 modelValue / 搜索 / 单选;基于扁平 `OrgNode[]` 入参渲染。OrgTreeSelect 直接消费 `useOrgStore.tree`,更聚焦。

### 4.2 Dashboard.vue 内部状态(不抽组件)

```ts
// 部门过滤状态
const selectedDeptCode = ref<string | null>(currentUser.value?.deptCode ?? null);
const includeSubDepts = ref(false);
const orgStore = useOrgStore();

// 切换过渡
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

// 部门树叶子判断(用于「含子部门」checkbox enabled 控制)
const isLeaf = computed(() => {
  if (selectedDeptCode.value === null) return true;
  return !hasChildrenInTree(orgStore.tree, selectedDeptCode.value);
});

// 有效部门集合
const effectiveDeptCodes = computed<Set<string | null>>(() => {
  if (selectedDeptCode.value === null) return new Set([null]);
  const codes = new Set<string | null>([selectedDeptCode.value]);
  if (includeSubDepts.value && orgStore.tree) {
    collectDescendants(orgStore.tree, selectedDeptCode.value).forEach(c => codes.add(c));
  }
  return codes;
});

// 工具函数(纯函数,放 <script setup> 顶部)
function collectDescendants(root: OrgNode, targetCode: string): string[] {
  const result: string[] = [];
  function dfs(node: OrgNode): OrgNode | null {
    if (node.code === targetCode) return node;
    for (const c of node.children) {
      const hit = dfs(c);
      if (hit) return hit;
    }
    return null;
  }
  const target = dfs(root);
  if (!target) return result;
  function walk(n: OrgNode) {
    for (const c of n.children) {
      if (c.code) result.push(c.code);
      walk(c);
    }
  }
  walk(target);
  return result;
}

function hasChildrenInTree(root: OrgNode | null, code: string): boolean {
  if (!root) return false;
  function dfs(node: OrgNode): boolean {
    if (node.code === code) return node.children.length > 0;
    for (const c of node.children) if (dfs(c)) return true;
    return false;
  }
  return dfs(root);
}

// 按部门二次过滤
const deptFilteredProjects = computed(() => {
  return userProjects.value.filter(p =>
    effectiveDeptCodes.value.has(p.deptCode ?? null)
  );
});

// 现有的 recentProjects / statistics 中涉及 projects 的部分改为基于 deptFilteredProjects
// (userTasks 已统一口径,upcomingTasks / statistics 中 tasks 部分自动跟随)
const recentProjects = computed(() => deptFilteredProjects.value.slice(0, 5));
// statistics / planVsActual 内部 const projects = userProjects.value → 改为 deptFilteredProjects.value

// 现有 userTasks 改为基于 deptFilteredProjects(所有角色统一口径,避免 admin/pm 看到跨部门任务)
const userTasks = computed(() => {
  if (!currentUser.value) return [];
  const userProjectIds = new Set(deptFilteredProjects.value.map(p => p.id));
  return taskStore.tasks.filter(t => userProjectIds.has(t.projectId));
});
```

**关键不变量**:
- `userProjects` 不被改动;`deptFilteredProjects` 是其下游的纯函数
- `includeSubDepts` 在叶子部门时 disabled(UI 控制)+ title 提示
- 进入 /dashboard 重新挂载组件 → ref 重新初始化 → 默认选中登录者部门 + `includeSubDepts=false`
- 「未分配」节点用 `null` 表示;匹配规则 `p.deptCode ?? null === null`

## 5. UI/UX 布局

### 5.1 整体结构

```
┌──────────────────────────────────────────────────────────────┐
│  Header: 仪表盘 / 欢迎语                                      │
├──────────────────────────────────────────────────────────────┤
│  [Card: 部门筛选行]                                          │  ← 新增,仅 admin 可见
│   label | OrgTreeSelect | ☑ 含子部门                        │
│   (切换中遮罩覆盖此 Card)                                     │
├──────────────────────────────────────────────────────────────┤
│  <TransitionGroup / Transition>                               │
│  [Stats Cards × 4]                                           │
│  [进度对比 × 3]                                               │
│  [任务状态饼图]  [项目进度柱图]                                │
│  [近期项目表]                                                 │
│  [即将到期任务列表]                                            │
│  </Transition>                                               │
└──────────────────────────────────────────────────────────────┘
```

### 5.2 部门筛选行(包 Card)

- 整行一个 `Card`(用户已确认)
- 左侧 label:「按部门查看」/ `$t('dashboard.departmentFilter.label')`
- 中间 `OrgTreeSelect`:宽度 `w-72` 左右,按钮显示当前选中节点全路径(公司 / 一级部门 / 二级部门),右侧 ▼ 图标
- 右侧 checkbox:「含子部门」
  - 仅当 `selectedDeptCode` 非 null 且对应节点是「非叶子」时启用
  - 叶子节点时 disabled, title 提示「叶子部门,无需展开」
- 「未分配」选中时,checkbox 自动 disabled 并隐藏
- 切换部门时所有下游 computed 实时刷新

### 5.3 切换过渡

```ts
function onDeptChange(newCode: string | null) {
  switching.value = true;
  if (switchTimer) window.clearTimeout(switchTimer);
  switchTimer = window.setTimeout(() => {
    selectedDeptCode.value = newCode;
    switching.value = false;
  }, 100);
}
```

**模板**:

```vue
<Card class="relative">
  <div class="flex items-center gap-4">
    <span class="text-sm text-secondary-600">
      {{ $t('dashboard.departmentFilter.label') }}
    </span>
    <OrgTreeSelect
      v-model="selectedDeptCode"
      @update:modelValue="onDeptChange" />
    <label class="flex items-center gap-2 text-sm"
           :class="{ 'opacity-50 cursor-not-allowed': isLeaf }">
      <input type="checkbox"
             v-model="includeSubDepts"
             :disabled="isLeaf" />
      {{ $t('dashboard.departmentFilter.includeSubDepts') }}
    </label>
  </div>
  <Transition name="fade">
    <div v-if="switching"
         class="pointer-events-none absolute inset-0 flex items-center
                justify-center rounded-lg bg-white/60 backdrop-blur-sm">
      <svg class="h-6 w-6 animate-spin text-primary-600" .../>
    </div>
  </Transition>
</Card>

<Transition name="content-fade" mode="out-in">
  <div :key="`${selectedDeptCode}-${includeSubDepts}`">
    <!-- 原有 4 Stats / 3 进度对比 / 2 图表 / 近期 / 即将到期 全部放这里 -->
  </div>
</Transition>
```

**CSS 过渡**(写在组件 `<style scoped>`):

```css
.fade-enter-active, .fade-leave-active { transition: opacity 100ms ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

.content-fade-enter-active, .content-fade-leave-active {
  transition: opacity 200ms ease, transform 200ms ease;
}
.content-fade-enter-from { opacity: 0; transform: translateY(4px); }
.content-fade-leave-to { opacity: 0; transform: translateY(-4px); }
```

**关键细节**:
- `onDeptChange` 是 debounced 提交,避免快速连点造成中间态闪烁
- ECharts 实例在 keyed 容器变化时会被销毁重建 → initProjectChart / initTaskChart 在新容器上重新跑(已有 `onUnmounted` 清理逻辑,安全)
- 部门筛选行 Card 用 `relative` 定位,遮罩才能正确覆盖

### 5.4 空态 / 错误态

- 当前选中部门无项目时:
  - 4 张统计卡片数字归零
  - 任务饼图、项目柱图走现有 empty state(已有 `dashboard.charts.noTaskData` / `noProjectData`)
  - 「近期项目表」改为显示一行 `dashboard.departmentFilter.emptyHint` 文字提示
- 加载组织树失败:OrgTreeSelect 显示「加载失败,点击重试」并打回 onError
- 当前登录 admin 用户 `deptCode` 为空:回退到「未分配」节点, 并在 OrgTreeSelect 上方显示 warning 文案「您的部门信息缺失,已默认展示未分配项目」

### 5.5 i18n 新增 key(三语同步)

```
dashboard.departmentFilter.label               按部门查看 / 부서별 보기 / Filter by Department
dashboard.departmentFilter.placeholder         请选择部门 / 부서를 선택하세요 / Select a department
dashboard.departmentFilter.includeSubDepts     含子部门 / 하위 부서 포함 / Include sub-departments
dashboard.departmentFilter.unassigned          未分配 / 미할당 / Unassigned
dashboard.departmentFilter.leafHint            叶子部门,无需展开 / 하위 부서 없음, 펼치기 불필요 / Leaf department, no children
dashboard.departmentFilter.emptyHint           该部门暂无项目 / 이 부서에 프로젝트가 없습니다 / No projects in this department
dashboard.departmentFilter.deptMissing         您的部门信息缺失,已默认展示未分配项目 / 부서 정보가 없어 미할당 프로젝트를 표시합니다 / Department info missing, showing unassigned
```

(其他已有 i18n key 不动)

## 6. 边界情况与错误处理

| # | 场景 | 行为 |
|---|------|------|
| A | 用户 `deptCode` 为空(HR 同步失败) | 降级为「未分配」; 显示 deptMissing 提示; 不抛错 |
| B | 组织树加载失败 | OrgTreeSelect 显示「加载失败 + 重试按钮」; 其他卡片继续用全表数据(降级); 不阻塞 Dashboard |
| C | 选中的部门节点在树中已不存在(HR 整列重导) | 静默处理, 收集后代集合为空, 显示 emptyHint |
| D | 选中叶子但绕过 disabled 勾上「含子部门」 | computed 走 collectDescendants → children=[] → 等同精确匹配, 静默 |
| E | 切换部门时多次快速连点 | `clearTimeout` + 重置, 只提交最后一次; switching 期间下拉被遮罩拦截 |
| F | 当前部门下没有项目 | 统计归零; 图表走空态; 近期项目表显示 emptyHint; 不报错 |
| G | 非 admin 登录 | 部门筛选行 `v-if` 不渲染(回归现有 Dashboard 行为) |
| H | 项目 `deptCode` 为 `null/''/undefined` 混杂 | 统一 `p.deptCode ?? null` 归一化, Set 里 `null` 只匹配 null |

## 7. 测试与验证策略

项目无前端测试框架,本节列**手动验证清单**和**构建验证**。

### 7.1 构建验证

| 命令 | 预期 |
|------|------|
| `npx vue-tsc` | 0 错误 |
| `npm run build` | 构建成功 (vue-tsc + vite build 全过) |
| `mvn clean install -DskipTests` | 后端不需改动, 可选 baseline 验证 |

### 7.2 前端手动验证清单

**前置**:
- 启动后端 `mvn spring-boot:run`(用户手动)
- 启动前端 `npm run dev`(用户手动)
- 准备至少 1 个 admin 账号(`currentUser.deptCode` 不为空)
- 准备至少 1 个 dept-project-manager / project-manager / member 账号(用于 §G 验证)
- 准备测试数据: 4 个不同部门的项目(含 1 个 `deptCode=null` 的老项目)

| # | 场景 | 预期 |
|---|------|------|
| 1 | admin 登录, 进入 /dashboard | 部门筛选行可见, 默认选中 admin 自己的部门; `includeSubDepts` 未勾; 统计数字 = 该部门项目数 |
| 2 | admin 切换到另一叶子部门 | 100ms 后统计 + 图表 + 列表全部更新; 筛选行遮罩短暂出现 |
| 3 | admin 选中「未分配」 | 「含子部门」checkbox 隐藏/disabled; 统计 = `deptCode=null` 的项目数 |
| 4 | admin 选中带子部门的中层部门, 勾「含子部门」 | 统计 = 该部门 + 所有后代部门项目之和 |
| 5 | admin 选中叶子部门, 尝试勾「含子部门」 | checkbox 始终 disabled |
| 6 | admin 切换到无项目的部门 | 统计归零; 饼图/柱图走空态; 近期项目表显示 emptyHint |
| 7 | admin 连续快速切换 5 个部门 | 最终态 = 第 5 个部门; 中间态不残留; 无卡顿报错 |
| 8 | admin 刷新页面 | 部门筛选重置回自己部门 + `includeSubDepts=false`(不持久化) |
| 9 | admin 切换语言 zh → ko → en | 部门筛选行所有文字三语同步 |
| 10 | 非 admin (member / viewer) 登录 | 部门筛选行不可见(`v-if` 不渲染); 现有 Dashboard 行为完全保留 |
| 11 | dept-project-manager 登录 | 部门筛选行不可见 |
| 12 | admin 的 `deptCode` 为空 | 默认降级到「未分配」; 提示文案可见 |
| 13 | 手动断网 / 后端关掉, `/api/orgs/tree` 返回 500 | OrgTreeSelect 显示「加载失败 + 重试」; 其他卡片继续用全表数据 |
| 14 | 部门筛选行 Card 在 1280px / 1024px 宽度 | 排版不破; OrgTreeSelect 宽度自适应 |
| 15 | 切换部门时打开 DevTools Performance | 切换耗时 < 100ms; 无 console.error / warning |

### 7.3 回归检查

**算法层面**(不应变化):
- 统计卡 / 进度对比 / 图表 / 列表 的计算公式(总项目 = length, 总任务 = leaf 数, 总成员 = owner + memberIds 去重 等)与改动前完全一致
- ECharts 实例化 / resize / dispose 逻辑未变

**数据源层面**(预期变化, 非回归):
- admin 视角下, 4 张统计卡的数字现在 = 选中部门的项目数 (改动前 = 全公司项目数), 这是本次需求
- 非 admin 角色: 数字逻辑不变 (dept 筛选对非 admin 不可见, 不影响)
- 项目柱图 / 任务饼图 改为从 deptFilteredProjects / 过滤后 userTasks 派生

**行为层面**:
- 近期项目表 / 即将到期任务列表项点击跳转路由正常
- ECharts 实例在部门切换时正确销毁重建, 无内存泄漏(DevTools Memory 多次切换 heap 稳定)
- `useOrgStore.loadTree()` 缓存仍生效(不重复请求 `/api/orgs/tree`)

## 8. 改动文件清单

| 状态 | 文件 | 说明 |
|------|------|------|
| 新增 | `frontend/src/components/common/OrgTreeSelect.vue` | 树形下拉组件 |
| 改   | `frontend/src/views/Dashboard.vue` | 加筛选行 + deptFilteredProjects 派生 + Transition 动画 |
| 改   | `frontend/src/i18n/locales/zh.ts` | 新增 7 个 key |
| 改   | `frontend/src/i18n/locales/ko.ts` | 新增 7 个 key |
| 改   | `frontend/src/i18n/locales/en.ts` | 新增 7 个 key |

**后端**: 零改动
**数据库**: 零改动
**配置文件**: 零改动
**SQL**: 零改动
**不自动 commit**(按 CLAUDE.md 规则, 等待用户明确确认)
