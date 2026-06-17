<template>
  <MainLayout>
    <div class="space-y-6">
      <!-- Page Header -->
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-secondary-900">{{ $t('team.title') }}</h1>
          <p class="mt-1 text-sm text-secondary-600">{{ $t('team.subtitle') }}</p>
        </div>
        <!-- 成员由 HR 同步管理,无手动添加入口 -->
      </div>

      <!-- 部门筛选行(2026-06-17 新增,仅 admin 可见,与 Dashboard 保持一致) -->
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
            <p class="text-sm font-medium text-secondary-600">{{ $t('dashboard.stats.totalMembers') }}</p>
            <p class="mt-2 text-2xl font-semibold text-secondary-900">{{ memberCount }}</p>
          </div>
        </Card>
      </div>

      <!-- Workload Distribution -->
      <Card>
        <template #header>
          <h3 class="text-lg font-semibold text-secondary-900">{{ $t('team.workloadDistribution') }}</h3>
        </template>
        <div class="h-80" ref="workloadChartRef"></div>
      </Card>

      <!-- Tabs -->
      <Tabs v-model="activeTab" :tabs="tabs">
        <template #default="{ activeTab: currentTab }">
          <div v-if="currentTab === 0" class="space-y-6">
            <!-- 顶部全局筛选 -->
            <Card>
              <template #header>
                <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                  <h3 class="text-lg font-semibold text-secondary-900">按组织架构查看</h3>
                  <div class="flex items-center gap-3">
                    <input
                      v-model="memberSearch"
                      type="text"
                      placeholder="搜索姓名/工号/邮箱"
                      class="w-48 rounded-lg border border-secondary-300 px-3 py-1.5 text-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
                    />
                    <select
                      v-model="memberRoleFilter"
                      class="rounded-lg border border-secondary-300 px-3 py-1.5 text-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
                    >
                      <option value="">{{ $t('team.allMembers.filterRole') }}</option>
                      <option value="admin">{{ $t('team.allMembers.roleAdmin') }}</option>
                      <option value="dept-project-manager">{{ $t('team.allMembers.roleDeptProjectManager') }}</option>
                      <option value="project-manager">{{ $t('team.allMembers.roleProjectManager') }}</option>
                      <option value="member">{{ $t('team.allMembers.roleMember') }}</option>
                      <option value="viewer">{{ $t('team.allMembers.roleViewer') }}</option>
                    </select>
                    <select
                      v-model="companyFilter"
                      class="rounded-lg border border-secondary-300 px-3 py-1.5 text-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
                    >
                      <option value="">所有公司</option>
                      <option v-for="cd in availableCompanyCodes" :key="cd" :value="cd">
                        {{ cd }} {{ companyName(cd) }}
                      </option>
                    </select>
                  </div>
                </div>
              </template>

              <div v-if="!orgStore.tree" class="py-8 text-center text-sm text-secondary-500">
                正在加载组织架构…
              </div>

              <div v-else-if="filteredUsers.length === 0" class="py-8 text-center text-sm text-secondary-500">
                没有匹配的用户
              </div>

              <!-- 搜索模式：扁平结果 -->
              <div v-else-if="isSearchMode" class="space-y-4">
                <div class="flex items-center justify-between">
                  <h4 class="text-sm font-medium text-secondary-700">
                    搜索结果 ({{ filteredUsers.length }} 人)
                  </h4>
                  <button
                    type="button"
                    @click="memberSearch = ''"
                    class="text-xs text-primary-600 hover:text-primary-700"
                  >
                    清除搜索
                  </button>
                </div>
                <div class="grid grid-cols-1 gap-3 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4">
                  <div
                    v-for="user in filteredUsers"
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
                      <div class="mt-0.5 truncate text-xs text-secondary-500">
                        {{ user.email }}
                      </div>
                      <div class="mt-0.5 truncate text-xs text-secondary-500">
                        {{ user.companyCd }} · {{ user.department }}
                      </div>
                      <!-- 2026-06-16: 职级展示 (HR 同步字段,JPSTN_CD ∈ {BA,BF} 显示 JPSTN_NAM,否则 TL) -->
                      <div class="mt-0.5 truncate text-xs text-secondary-500">
                        <span class="text-secondary-400">{{ $t('team.position') }}:</span>
                        <span class="ml-1 font-medium text-secondary-700">{{ displayPosition(user) }}</span>
                      </div>
                      <div class="mt-1 flex items-center justify-between gap-2">
                        <div class="flex items-center gap-1">
                          <Badge :variant="roleBadgeVariant(user.role)">
                            {{ roleLabel(user.role) }}
                          </Badge>
                          <Badge
                            v-if="user.roleAutoInferred"
                            variant="info"
                            :title="user.roleInferredFromJpstn === 'BA'
                              ? $t('team.roleSource.fromBa')
                              : (user.roleInferredFromJpstn === 'BF'
                                ? $t('team.roleSource.fromBf')
                                : $t('team.roleSource.autoInferredHint'))"
                          >
                            {{ $t('team.roleSource.autoInferred') }}
                          </Badge>
                        </div>
                        <!-- 修改角色按钮:永远显示,仅 admin 可点;非 admin 显示灰色 + tooltip 提示 -->
                        <button
                          type="button"
                          :class="[
                            'rounded px-2 py-0.5 text-xs',
                            canChangeRoleFor(user)
                              ? 'text-primary-600 hover:bg-primary-50 hover:text-primary-700'
                              : 'cursor-not-allowed text-secondary-400'
                          ]"
                          :disabled="!canChangeRoleFor(user)"
                          :title="canChangeRoleFor(user)
                            ? (permissionStore.isAdmin() ? $t('team.roleChange.title') : '变更本部门用户角色')
                            : $t('team.roleChange.adminOnly')"
                          @click="canChangeRoleFor(user) && openRoleChangeDialog(user)"
                        >
                          {{ $t('team.roleChange.title') }}
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 浏览模式：按公司 + org 树分组 -->
              <div v-else class="space-y-3">
                <div
                  v-for="company in displayedCompanyNodes"
                  :key="company.code"
                  class="rounded-lg border-2 border-primary-200 bg-primary-50/30 p-3"
                >
                  <div class="mb-2 flex items-center gap-2 px-1">
                    <span class="inline-flex h-6 w-6 items-center justify-center rounded bg-primary-600 text-xs font-bold text-white">
                      {{ company.code }}
                    </span>
                    <span class="text-sm font-semibold text-secondary-900">{{ company.name }}</span>
                    <span class="text-xs text-secondary-500">({{ countCompanyMembers(company) }} 人)</span>
                  </div>
                  <div v-if="company.children.length === 0" class="px-1 py-2 text-xs text-secondary-400">
                    本公司在组织架构中暂无数据
                  </div>
                  <div v-else class="space-y-2">
                    <OrgGroup
                      v-for="top in company.children"
                      :key="top.code"
                      :node="top"
                      :members="filteredUsers"
                      @open-role-change="openRoleChangeDialog"
                    />
                  </div>
                </div>

                <!-- 未分配部门用户 -->
                <div
                  v-if="unassignedUsers.length > 0"
                  class="rounded-lg border-2 border-dashed border-secondary-300 bg-secondary-50"
                >
                  <button
                    type="button"
                    @click="unassignedExpanded = !unassignedExpanded"
                    class="flex w-full items-center justify-between gap-2 rounded-lg px-3 py-2.5 text-left transition-colors hover:bg-secondary-100"
                    :aria-expanded="unassignedExpanded"
                  >
                    <div class="flex items-center gap-2">
                      <span
                        class="inline-flex h-5 w-5 items-center justify-center text-secondary-400 transition-transform"
                        :class="{ 'rotate-90': unassignedExpanded }"
                      >
                        <svg viewBox="0 0 20 20" fill="currentColor" class="h-4 w-4">
                          <path fill-rule="evenodd" d="M7.21 14.77a.75.75 0 01.02-1.06L10.94 10 7.23 6.29a.75.75 0 111.04-1.08l4.25 4.25a.75.75 0 010 1.08l-4.25 4.25a.75.75 0 01-1.06-.02z" clip-rule="evenodd" />
                        </svg>
                      </span>
                      <span class="inline-flex h-6 w-6 items-center justify-center rounded bg-secondary-400 text-xs font-bold text-white">
                        ?
                      </span>
                      <span class="text-sm font-semibold text-secondary-900">未分配部门</span>
                      <span class="text-xs text-secondary-500">({{ unassignedUsers.length }} 人 · deptCode 为空)</span>
                    </div>
                  </button>

                  <div v-if="unassignedExpanded" class="border-t border-secondary-200 p-3">
                    <div class="grid grid-cols-1 gap-3 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4">
                      <div
                        v-for="user in visibleUnassignedUsers"
                        :key="user.id"
                        class="flex items-start gap-3 rounded-lg border border-secondary-200 bg-white p-3 transition-shadow hover:shadow-sm"
                      >
                        <UserAvatar :name="displayName(user)" :seed="user.avatar" size="md" />
                        <div class="min-w-0 flex-1">
                          <div class="truncate text-sm font-medium text-secondary-900">{{ displayName(user) }}</div>
                          <div v-if="user.chineseNam && user.chineseNam !== user.name" class="truncate text-xs text-secondary-500">
                            {{ user.name }}
                          </div>
                          <div class="mt-0.5 truncate font-mono text-xs text-secondary-500">{{ user.id }}</div>
                          <div v-if="user.email" class="mt-0.5 truncate text-xs text-secondary-500">{{ user.email }}</div>
                          <div class="mt-0.5 truncate text-xs text-secondary-500">
                            {{ user.companyCd || '-' }} · {{ user.department || '未分配部门' }}
                          </div>
                          <!-- 2026-06-16: 职级展示 (HR 同步字段,JPSTN_CD ∈ {BA,BF} 显示 JPSTN_NAM,否则 TL) -->
                          <div class="mt-0.5 truncate text-xs text-secondary-500">
                            <span class="text-secondary-400">{{ $t('team.position') }}:</span>
                            <span class="ml-1 font-medium text-secondary-700">{{ displayPosition(user) }}</span>
                          </div>
                          <div class="mt-1 flex items-center justify-between gap-2">
                            <Badge :variant="roleBadgeVariant(user.role)">
                              {{ roleLabel(user.role) }}
                            </Badge>
                            <button
                              type="button"
                              :class="[
                                'rounded px-2 py-0.5 text-xs',
                                canChangeRoleFor(user)
                                  ? 'text-primary-600 hover:bg-primary-50 hover:text-primary-700'
                                  : 'cursor-not-allowed text-secondary-400'
                              ]"
                              :disabled="!canChangeRoleFor(user)"
                              :title="canChangeRoleFor(user)
                                ? (permissionStore.isAdmin() ? $t('team.roleChange.title') : '变更本部门用户角色')
                                : $t('team.roleChange.adminOnly')"
                              @click="canChangeRoleFor(user) && openRoleChangeDialog(user)"
                            >
                              {{ $t('team.roleChange.title') }}
                            </button>
                          </div>
                        </div>
                      </div>
                    </div>
                    <div v-if="unassignedUsers.length > unassignedPageSize" class="mt-3 text-center">
                      <button
                        type="button"
                        @click="unassignedVisibleCount += unassignedPageSize"
                        class="text-xs font-medium text-primary-600 hover:text-primary-700"
                      >
                        加载更多 ({{ unassignedUsers.length - unassignedVisibleCount }} 人未显示)
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </Card>
          </div>

          <div v-else-if="currentTab === 1" class="space-y-6">
            <!-- 2026-06-17: 本部门成员(表格视图,基于当前登录用户 deptCode 过滤) -->
            <Card>
              <template #header>
                <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                  <div>
                    <h3 class="text-lg font-semibold text-secondary-900">{{ $t('team.currentDept.title') }}</h3>
                    <p class="mt-0.5 text-xs text-secondary-500">
                      {{ $t('team.currentDept.subtitle') }}
                      <span v-if="currentUser?.deptCode" class="ml-1 font-mono text-secondary-700">
                        ({{ currentUser.deptCode }})
                      </span>
                    </p>
                  </div>
                  <div class="flex items-center gap-2">
                    <input
                      v-model="currentDeptSearch"
                      type="text"
                      :placeholder="$t('team.currentDept.searchPlaceholder')"
                      class="w-56 rounded-lg border border-secondary-300 px-3 py-1.5 text-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
                    />
                  </div>
                </div>
              </template>

              <!-- 当前用户未关联部门 -->
              <div v-if="!currentUser?.deptCode" class="px-2 py-10 text-center text-sm text-secondary-500">
                {{ $t('team.currentDept.noDept') }}
              </div>

              <div v-else class="overflow-x-auto">
                <table class="min-w-full divide-y divide-secondary-200">
                  <thead class="bg-secondary-50">
                    <tr>
                      <th class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-secondary-500">
                        {{ $t('team.currentDept.memberName') }}
                      </th>
                      <th class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-secondary-500">
                        {{ $t('team.currentDept.userId') }}
                      </th>
                      <th class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-secondary-500">
                        {{ $t('team.currentDept.email') }}
                      </th>
                      <th class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-secondary-500">
                        {{ $t('team.currentDept.company') }}
                      </th>
                      <th class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-secondary-500">
                        {{ $t('team.currentDept.department') }}
                      </th>
                      <th class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-secondary-500">
                        {{ $t('team.currentDept.position') }}
                      </th>
                      <th class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-secondary-500">
                        {{ $t('team.currentDept.role') }}
                      </th>
                    </tr>
                  </thead>
                  <tbody class="divide-y divide-secondary-200 bg-white">
                    <tr
                      v-for="user in paginatedCurrentDeptUsers"
                      :key="user.id"
                      class="hover:bg-secondary-50"
                    >
                      <td class="whitespace-nowrap px-6 py-4">
                        <div class="flex items-center">
                          <UserAvatar :name="displayName(user)" :seed="user.avatar" size="md" />
                          <div class="ml-3">
                            <div class="text-sm font-medium text-secondary-900">{{ displayName(user) }}</div>
                            <div v-if="user.chineseNam && user.chineseNam !== user.name" class="text-xs text-secondary-500">
                              {{ user.name }}
                            </div>
                          </div>
                        </div>
                      </td>
                      <td class="whitespace-nowrap px-6 py-4 font-mono text-sm text-secondary-900">{{ user.id }}</td>
                      <td class="px-6 py-4 text-sm text-secondary-500">{{ user.email || '—' }}</td>
                      <td class="whitespace-nowrap px-6 py-4 text-sm text-secondary-900">
                        {{ user.companyCd || '—' }}
                      </td>
                      <td class="px-6 py-4 text-sm text-secondary-900">
                        {{ user.department || '—' }}
                      </td>
                      <td class="whitespace-nowrap px-6 py-4 text-sm text-secondary-700">
                        {{ displayPosition(user) }}
                      </td>
                      <td class="whitespace-nowrap px-6 py-4">
                        <div class="flex items-center gap-1">
                          <Badge :variant="roleBadgeVariant(user.role)">
                            {{ roleLabel(user.role) }}
                          </Badge>
                          <Badge
                            v-if="user.roleAutoInferred"
                            variant="info"
                            :title="user.roleInferredFromJpstn === 'BA'
                              ? $t('team.roleSource.fromBa')
                              : (user.roleInferredFromJpstn === 'BF'
                                ? $t('team.roleSource.fromBf')
                                : $t('team.roleSource.autoInferredHint'))"
                          >
                            {{ $t('team.roleSource.autoInferred') }}
                          </Badge>
                        </div>
                      </td>
                    </tr>
                    <tr v-if="filteredCurrentDeptUsers.length === 0">
                      <td colspan="7" class="px-6 py-12 text-center text-sm text-secondary-500">
                        {{ $t('team.currentDept.noData') }}
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <!-- Pagination -->
              <div
                v-if="filteredCurrentDeptUsers.length > 0"
                class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-t border-secondary-200 px-6 py-4"
              >
                <div class="flex items-center gap-2">
                  <span class="text-sm text-secondary-600">{{ $t('team.currentDept.itemsPerPage') }}</span>
                  <select
                    v-model="currentDeptItemsPerPage"
                    @change="currentDeptPage = 1"
                    class="rounded-lg border border-secondary-300 px-2 py-1 text-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
                  >
                    <option :value="10">10</option>
                    <option :value="20">20</option>
                    <option :value="50">50</option>
                  </select>
                </div>
                <div class="flex items-center gap-4">
                  <span class="text-sm text-secondary-600">
                    {{ $t('team.currentDept.page', { current: currentDeptPage, total: currentDeptTotalPages }) }}
                  </span>
                  <div class="flex gap-2">
                    <Button
                      variant="secondary"
                      size="sm"
                      :disabled="currentDeptPage === 1"
                      @click="currentDeptPage--"
                    >
                      {{ $t('team.currentDept.previous') }}
                    </Button>
                    <Button
                      variant="secondary"
                      size="sm"
                      :disabled="currentDeptPage === currentDeptTotalPages"
                      @click="currentDeptPage++"
                    >
                      {{ $t('team.currentDept.next') }}
                    </Button>
                  </div>
                </div>
              </div>
            </Card>
          </div>

          <div v-else-if="currentTab === 2" class="space-y-6">
            <!-- Task Assignment Table -->
            <Card>
              <template #header>
                <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                  <h3 class="text-lg font-semibold text-secondary-900">{{ $t('team.taskAssignment.title') }}</h3>
                  <div class="flex items-center gap-2">
                    <button
                      type="button"
                      @click="expandAll"
                      class="rounded-lg border border-secondary-300 px-2.5 py-1 text-xs text-secondary-700 hover:bg-secondary-50"
                    >
                      {{ $t('team.taskAssignment.expandAll', '全部展开') }}
                    </button>
                    <button
                      type="button"
                      @click="collapseAll"
                      class="rounded-lg border border-secondary-300 px-2.5 py-1 text-xs text-secondary-700 hover:bg-secondary-50"
                    >
                      {{ $t('team.taskAssignment.collapseAll', '全部折叠') }}
                    </button>
                    <select
                      v-model="sortBy" 
                      @change="handleSortChange"
                      class="rounded-lg border border-secondary-300 px-3 py-1.5 text-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
                    >
                      <option value="memberName">{{ $t('team.taskAssignment.memberName') }}</option>
                      <option value="taskName">{{ $t('team.taskAssignment.taskName') }}</option>
                      <option value="projectName">{{ $t('team.taskAssignment.projectName') }}</option>
                      <option value="status">{{ $t('team.taskAssignment.status') }}</option>
                      <option value="priority">{{ $t('team.taskAssignment.priority') }}</option>
                    </select>
                    <select 
                      v-model="sortOrder" 
                      @change="handleSortChange"
                      class="rounded-lg border border-secondary-300 px-3 py-1.5 text-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
                    >
                      <option value="asc">↑</option>
                      <option value="desc">↓</option>
                    </select>
                  </div>
                </div>
              </template>
              <div class="overflow-x-auto">
                <table class="min-w-full divide-y divide-secondary-200">
                  <thead class="bg-secondary-50">
                    <tr>
                      <th class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-secondary-500">
                        {{ $t('team.taskAssignment.memberName') }}
                      </th>
                      <th class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-secondary-500">
                        {{ $t('team.taskAssignment.userId') }}
                      </th>
                      <th class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-secondary-500">
                        {{ $t('team.taskAssignment.parentTask') }}
                      </th>
                      <th class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-secondary-500">
                        {{ $t('team.taskAssignment.taskName') }}
                      </th>
                      <th class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-secondary-500">
                        {{ $t('team.taskAssignment.projectName') }}
                      </th>
                      <th class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-secondary-500">
                        {{ $t('team.taskAssignment.status') }}
                      </th>
                      <th class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-secondary-500">
                        {{ $t('team.taskAssignment.priority') }}
                      </th>
                      <th class="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-secondary-500">
                        {{ $t('team.taskAssignment.progress') }}
                      </th>
                    </tr>
                  </thead>
                  <tbody class="divide-y divide-secondary-200 bg-white">
                    <tr v-for="item in paginatedTaskAssignments" :key="item.id" class="hover:bg-secondary-50">
                      <td class="whitespace-nowrap px-6 py-4">
                        <div class="flex items-center">
                          <UserAvatar :name="item.userName" :seed="item.userAvatar" size="md" />
                          <div class="ml-3">
                            <div class="text-sm font-medium text-secondary-900">{{ item.userName }}</div>
                          </div>
                        </div>
                      </td>
                      <td class="whitespace-nowrap px-6 py-4 text-sm font-mono text-secondary-900">
                        {{ item.userId }}
                      </td>
                      <td class="px-6 py-4 text-sm text-secondary-500">
                        <span v-if="item.parentTaskName">{{ item.parentTaskName }}</span>
                        <span v-else class="text-secondary-300">—</span>
                      </td>
                      <td class="px-6 py-4 text-sm text-secondary-900">
                        <div class="flex items-center gap-1" :style="{ paddingLeft: item.taskDepth * 1.25 + 'rem' }">
                          <!-- 折叠/展开 chevron:有子任务时显示,可点击 -->
                          <button
                            v-if="hasChildren(item.taskId)"
                            type="button"
                            @click="toggleExpand(item.taskId)"
                            class="flex h-5 w-5 flex-shrink-0 items-center justify-center rounded text-secondary-500 hover:bg-secondary-100 hover:text-secondary-700"
                            :title="isExpanded(item.taskId) ? '折叠子任务' : '展开子任务'"
                          >
                            <svg
                              v-if="isExpanded(item.taskId)"
                              class="h-3.5 w-3.5"
                              fill="none"
                              stroke="currentColor"
                              viewBox="0 0 24 24"
                            >
                              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
                            </svg>
                            <svg
                              v-else
                              class="h-3.5 w-3.5"
                              fill="none"
                              stroke="currentColor"
                              viewBox="0 0 24 24"
                            >
                              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
                            </svg>
                          </button>
                          <!-- 占位:无子任务时让对齐一致 -->
                          <span v-else class="inline-block h-5 w-5 flex-shrink-0"></span>
                          <span :class="item.taskDepth > 0 ? 'text-secondary-700' : 'font-medium text-secondary-900'">
                            {{ item.taskName }}
                          </span>
                        </div>
                      </td>
                      <td class="px-6 py-4 text-sm text-secondary-900">
                        {{ item.projectName }}
                      </td>
                      <td class="whitespace-nowrap px-6 py-4">
                        <Badge :variant="getStatusBadgeVariant(item.status)">
                          {{ getStatusLabel(item.status) }}
                        </Badge>
                      </td>
                      <td class="whitespace-nowrap px-6 py-4">
                        <Badge :variant="getPriorityBadgeVariant(item.priority)">
                          {{ getPriorityLabel(item.priority) }}
                        </Badge>
                      </td>
                      <td class="px-6 py-4">
                        <div class="w-32">
                          <div class="flex items-center">
                            <div class="flex-1 bg-secondary-200 rounded-full h-2">
                              <div 
                                class="bg-primary-600 h-2 rounded-full transition-all duration-300" 
                                :style="{ width: `${item.progress}%` }"
                              ></div>
                            </div>
                            <span class="ml-2 text-sm text-secondary-600">{{ item.progress }}%</span>
                          </div>
                        </div>
                      </td>
                    </tr>
                    <tr v-if="sortedTaskAssignments.length === 0">
                      <td colspan="8" class="px-6 py-12 text-center text-sm text-secondary-500">
                        {{ $t('team.taskAssignment.noData') }}
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <!-- Pagination -->
              <div v-if="sortedTaskAssignments.length > 0" class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-t border-secondary-200 px-6 py-4">
                <div class="flex items-center gap-2">
                  <span class="text-sm text-secondary-600">{{ $t('team.taskAssignment.itemsPerPage') }}</span>
                  <select 
                    v-model="itemsPerPage" 
                    @change="handleItemsPerPageChange"
                    class="rounded-lg border border-secondary-300 px-2 py-1 text-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
                  >
                    <option :value="5">5</option>
                    <option :value="10">10</option>
                    <option :value="20">20</option>
                    <option :value="50">50</option>
                  </select>
                </div>
                <div class="flex items-center gap-4">
                  <span class="text-sm text-secondary-600">
                    {{ $t('team.taskAssignment.page', { current: currentPage, total: totalPages }) }}
                  </span>
                  <div class="flex gap-2">
                    <Button 
                      variant="secondary" 
                      size="sm" 
                      :disabled="currentPage === 1"
                      @click="currentPage--"
                    >
                      {{ $t('team.taskAssignment.previous') }}
                    </Button>
                    <Button 
                      variant="secondary" 
                      size="sm" 
                      :disabled="currentPage === totalPages"
                      @click="currentPage++"
                    >
                      {{ $t('team.taskAssignment.next') }}
                    </Button>
                  </div>
                </div>
              </div>
            </Card>
          </div>
        </template>
      </Tabs>
    </div>

    <!-- Add Member Modal 已移除:成员由 HR 同步管理 -->

    <!-- 角色变更弹窗（仅 admin 可触发） -->
    <RoleChangeDialog
      v-if="roleChangeTarget"
      v-model:visible="roleChangeDialogVisible"
      :user-id="roleChangeTarget.id"
      :current-role="roleChangeTarget.role"
      :current-managed-dept-codes="roleChangeTarget.managedDeptCodes || []"
      :current-managed-company-cd="roleChangeTarget.managedCompanyCd || ''"
      :user-company-cd="roleChangeTarget.companyCd || ''"
      :user-dept-code="roleChangeTarget.deptCode || ''"
      :current-managed-project-ids="roleChangeTarget.managedProjectIds || []"
      :current-role-auto-inferred="roleChangeTarget.roleAutoInferred || false"
      @success="onRoleChangeSuccess"
    />
  </MainLayout>
</template>

  <script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue';
import { useI18n } from 'vue-i18n';
import * as echarts from 'echarts';
import MainLayout from '@/components/layout/MainLayout.vue';
import Card from '@/components/common/Card.vue';
import Button from '@/components/common/Button.vue';
import Badge from '@/components/common/Badge.vue';
import Tabs from '@/components/common/Tabs.vue';
import type { Tab } from '@/components/common/Tabs.vue';
import UserAvatar from '@/components/common/UserAvatar.vue';
import OrgGroup from '@/components/team/OrgGroup.vue';
import RoleChangeDialog from '@/components/team/RoleChangeDialog.vue';
import OrgTreeSelect from '@/components/common/OrgTreeSelect.vue';
import { useUserStore } from '@/stores/user';
import { useTaskStore } from '@/stores/task';
import { useProjectStore } from '@/stores/project';
import { useOrgStore } from '@/stores/org';
import { usePermissionStore } from '@/stores/permission';
// Modal 与 ApiError 已不再使用（Add Member 表单移除）
import type { User, Task, OrgNode } from '@/types';

interface TaskAssignment {
  id: string;
  userId: string;
  userName: string;
  userAvatar: string;
  taskId: string;
  taskName: string;
  projectId: string;
  projectName: string;
  status: Task['status'];
  priority: Task['priority'];
  progress: number;
  // 父子关系(2026-06-12)
  parentTaskName?: string;  // 父任务标题(无则为 undefined / 显示 "—")
  taskDepth: number;         // 0=顶级,1=子任务,2=孙任务 ...
}

const { t } = useI18n();
const userStore = useUserStore();
const taskStore = useTaskStore();
const projectStore = useProjectStore();
const orgStore = useOrgStore();
const permissionStore = usePermissionStore();

const currentUser = computed(() => userStore.currentUser);

// ============ 部门过滤(admin 专用,2026-06-17 新增,与 Dashboard 保持一致) ============ //
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

/**
 * 用户列表:admin 视角下先按部门过滤,再走 tab 内部筛选(search/role/company)
 * 非 admin 维持原样,不受顶部部门筛选影响
 */
const users = computed(() => {
  const all = userStore.users;
  if (currentUser.value?.role !== 'admin') return all;
  const codes = effectiveDeptCodes.value;
  return all.filter(u => {
    if (u.deptCode === null || u.deptCode === undefined || u.deptCode === '') {
      return codes.has(null);
    }
    return codes.has(u.deptCode);
  });
});
const workloadChartRef = ref<HTMLElement>();
let workloadChartInstance: echarts.ECharts | null = null;
const activeTab = ref(0);

const tabs = computed<Tab[]>(() => [
  {
    label: t('team.allMembers.title'),
    badge: users.value.length,
    value: 'members'
  },
  {
    // 2026-06-17: 本部门成员 tab（基于当前用户 deptCode 过滤,以表格展示）
    label: t('team.currentDept.title'),
    badge: currentDeptUsers.value.length,
    value: 'current-dept'
  },
  {
    label: t('team.taskAssignment.title'),
    badge: taskAssignments.value.length,
    value: 'tasks'
  }
]);

// (Add Member Modal 相关 state/functions 已移除:成员由 HR 同步管理,前端不再暴露手动 add/edit/delete 入口)


const adminCount = computed(() => users.value.filter(u => u.role?.replace(/_/g, '-') === 'admin').length);
const pmCount = computed(() => users.value.filter(u => u.role?.replace(/_/g, '-') === 'project-manager').length);
const deptPmCount = computed(() => users.value.filter(u => u.role?.replace(/_/g, '-') === 'dept-project-manager').length);
void deptPmCount; // 暂未在模板中使用,保留供后续"按角色统计"卡
// 统计当前视图(部门过滤后)的总成员数 —— 与 Dashboard 的 totalMembers 语义保持一致
const memberCount = computed(() => users.value.length);

// (getRoleLabel / getRoleBadgeVariant / formattedDate removed: org
// group view uses inline role/date formatting in OrgGroup.vue)

const initWorkloadChart = () => {
  if (!workloadChartRef.value) return;

  // 销毁旧实例
  if (workloadChartInstance) {
    workloadChartInstance.dispose();
    workloadChartInstance = null;
  }

  const chart = echarts.init(workloadChartRef.value);
  workloadChartInstance = chart;

  // 只统计叶子任务（没有子任务的任务），避免重复统计
  const allTaskIds = new Set(taskStore.tasks.map(t => t.id));
  const parentTaskIds = new Set(taskStore.tasks.filter(t => t.parentTaskId).map(t => t.parentTaskId));
  const leafTaskIds = new Set([...allTaskIds].filter(id => !parentTaskIds.has(id)));
  const leafTasks = taskStore.tasks.filter(t => leafTaskIds.has(t.id));

  // 按任务数量排序，取前8名
  const userData = users.value
    .map(user => {
      const taskCount = leafTasks.filter(t => t.assigneeId === user.id).length;
      return { name: user.name, value: taskCount };
    })
    .sort((a, b) => b.value - a.value)
    .slice(0, 8)
    .filter(u => u.value > 0);

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

// 筛选
const memberSearch = ref<string>('');
const memberRoleFilter = ref<string>('');
const companyFilter = ref<string>('');

// 角色变更弹窗状态
const roleChangeDialogVisible = ref(false);
const roleChangeTarget = ref<User | null>(null);
const openRoleChangeDialog = (user: User) => {
  roleChangeTarget.value = user;
  roleChangeDialogVisible.value = true;
};

/**
 * 能否修改该用户的角色(2026-06-12)
 * - admin: 任意
 * - dept-pm: 本部门内非 admin、非 dept-pm 用户
 */
const canChangeRoleFor = (user: User): boolean => {
  if (permissionStore.isAdmin()) return true;
  if (permissionStore.isDeptProjectManager()) {
    // dept-pm 不能动 admin 和 dept-pm（包括自己）
    if (user.role === 'admin' || user.role === 'dept-project-manager') return false;
    // 必须本部门内
    const actorCodes = permissionStore.managedDeptCodes;
    if (!user.deptCode || !actorCodes.includes(user.deptCode)) return false;
    return true;
  }
  return false;
};
const onRoleChangeSuccess = async () => {
  // 角色变更后,后端 tokenVersion + 1 → 目标用户旧 token 失效
  // 当前 admin 自己的 users 列表也要刷新
  try {
    await userStore.refreshUsers();
  } catch (e) {
    console.error('角色变更后刷新用户列表失败', e);
  }
};

/**
 * 按顶部筛选条件过滤用户
 * 用于驱动 org 分组视图
 */
const filteredUsers = computed<User[]>(() => {
  const search = memberSearch.value.trim().toLowerCase();
  return users.value.filter(user => {
    if (search) {
      const name = (user.chineseNam || user.name || '').toLowerCase();
      const id = (user.id || '').toLowerCase();
      const email = (user.email || '').toLowerCase();
      if (!name.includes(search) && !id.includes(search) && !email.includes(search)) return false;
    }
    const normalizedRole = user.role?.replace(/_/g, '-');
    if (memberRoleFilter.value && normalizedRole !== memberRoleFilter.value) return false;
    if (companyFilter.value && user.companyCd !== companyFilter.value) return false;
    return true;
  });
});

/**
 * 顶层节点 = 4 个公司（2700 / 8400 / 2710 / 9000）
 * 按 companyFilter 过滤
 */
const displayedCompanyNodes = computed<OrgNode[]>(() => {
  if (!orgStore.tree) return [];
  const all = orgStore.tree.children || [];
  if (!companyFilter.value) return all;
  return all.filter(n => n.code === companyFilter.value);
});

/**
 * 用户所有出现过的 company_cd（驱动公司下拉框）
 */
const availableCompanyCodes = computed<string[]>(() => {
  if (!orgStore.tree) return [];
  return (orgStore.tree.children || []).map(n => n.code).filter(Boolean);
});

function companyName(cd: string): string {
  if (!orgStore.tree) return cd;
  const node = (orgStore.tree.children || []).find(n => n.code === cd);
  return node?.name || cd;
}

/**
 * 搜索模式：搜索框有内容 → 扁平结果
 * 浏览模式：搜索框为空 → org 树分组
 */
const isSearchMode = computed(() => memberSearch.value.trim().length > 0);

/**
 * 收集 org tree 中所有 code（用于判断用户的 deptCode 是否在树里）
 */
const orgCodes = computed<Set<string>>(() => {
  const set = new Set<string>();
  if (!orgStore.tree) return set;
  const walk = (n: OrgNode) => {
    set.add(n.code);
    for (const c of n.children || []) walk(c);
  };
  walk(orgStore.tree);
  return set;
});

/**
 * deptCode 为空 或 deptCode 在 org tree 里找不到的用户
 */
const unassignedUsers = computed<User[]>(() => {
  return filteredUsers.value.filter(u => !u.deptCode || !orgCodes.value.has(u.deptCode));
});

/**
 * 未分配部门折叠/分页状态
 */
const unassignedExpanded = ref(false);
const unassignedVisibleCount = ref(12);
const unassignedPageSize = 12;
const visibleUnassignedUsers = computed<User[]>(() =>
  unassignedUsers.value.slice(0, unassignedVisibleCount.value)
);

/**
 * 统计某公司节点下所有成员数（含子组织）
 */
function countCompanyMembers(root: OrgNode): number {
  const codes = new Set<string>();
  const walk = (n: OrgNode) => {
    codes.add(n.code);
    for (const c of n.children || []) walk(c);
  };
  walk(root);
  return filteredUsers.value.filter(u => u.deptCode && codes.has(u.deptCode)).length;
}

function displayName(user: User): string {
  return user.chineseNam && user.chineseNam.trim() ? user.chineseNam : user.name;
}

// 2026-06-16: 职级展示 (HR 同步字段,JPSTN_CD ∈ {BA, BF} 时显示 JPSTN_NAM,否则显示 TL)
function displayPosition(user: User): string {
  if (user.jpstnCd === 'BA' || user.jpstnCd === 'BF') {
    return user.jpstnNam || 'TL';
  }
  return 'TL';
}

function roleLabel(role: string): string {
  const normalized = role?.replace(/_/g, '-');
  const map: Record<string, string> = {
    'admin': '管理员',
    'dept-project-manager': '部门项目负责人',
    'project-manager': '项目经理',
    'member': '项目人员',
    'viewer': '观察者',
  };
  return map[normalized] || role;
}

function roleBadgeVariant(role: string): 'default' | 'primary' | 'danger' | 'success' | 'warning' | 'info' {
  const normalized = role?.replace(/_/g, '-');
  const map: Record<string, 'danger' | 'warning' | 'primary' | 'default'> = {
    'admin': 'danger',
    'dept-project-manager': 'warning',
    'project-manager': 'warning',
    'member': 'primary',
    'viewer': 'default',
  };
  return map[normalized] || 'default';
}

// ============ 2026-06-17:本部门成员 tab(基于当前登录用户 deptCode 过滤) ============ //
const currentDeptSearch = ref<string>('');
const currentDeptPage = ref<number>(1);
const currentDeptItemsPerPage = ref<number>(10);

/** 当前部门成员:仅按当前用户的 deptCode 过滤,不依赖顶部 admin 部门筛选器 */
const currentDeptUsers = computed<User[]>(() => {
  const myDeptCode = currentUser.value?.deptCode;
  if (!myDeptCode) return [];
  return userStore.users.filter(u => u.deptCode === myDeptCode);
});

/** 当前部门成员 + 搜索过滤 */
const filteredCurrentDeptUsers = computed<User[]>(() => {
  const search = currentDeptSearch.value.trim().toLowerCase();
  if (!search) return currentDeptUsers.value;
  return currentDeptUsers.value.filter(u => {
    const name = (u.chineseNam || u.name || '').toLowerCase();
    const id = (u.id || '').toLowerCase();
    const email = (u.email || '').toLowerCase();
    return name.includes(search) || id.includes(search) || email.includes(search);
  });
});

/** 切换搜索时重置到第一页 */
watch(currentDeptSearch, () => {
  currentDeptPage.value = 1;
});

const currentDeptTotalPages = computed(() =>
  Math.ceil(filteredCurrentDeptUsers.value.length / currentDeptItemsPerPage.value) || 1
);

const paginatedCurrentDeptUsers = computed<User[]>(() => {
  const start = (currentDeptPage.value - 1) * currentDeptItemsPerPage.value;
  const end = start + currentDeptItemsPerPage.value;
  return filteredCurrentDeptUsers.value.slice(start, end);
});

// Task Assignment Table
const sortBy = ref<string>('memberName');
const sortOrder = ref<'asc' | 'desc'>('asc');
const currentPage = ref<number>(1);
const itemsPerPage = ref<number>(10);

// 计算任务分配数据
// 2026-06-12:简化显示规则——所有有 assignee 的任务都显示(包含子任务、中间任务、根任务),
// 之前的 shouldShow 把"有父 + 有子"的任务错误地隐藏,导致子任务/中间任务看不到
// 同时为每行计算父任务名 + 任务深度,用于表格里展示父子关系
const taskAssignments = computed<TaskAssignment[]>(() => {
  const assignments: TaskAssignment[] = [];
  const userMap = new Map(users.value.map(u => [u.id, u]));
  const projectMap = new Map(projectStore.projects.map(p => [p.id, p]));
  const taskMap = new Map(taskStore.tasks.map(t => [t.id, t]));

  const getDepth = (taskId: string): number => {
    let depth = 0;
    let cur = taskMap.get(taskId);
    while (cur?.parentTaskId) {
      depth++;
      cur = taskMap.get(cur.parentTaskId);
      if (depth > 32) break; // 防止环
    }
    return depth;
  };

  // 处理有负责人的任务
  const assignedTasks = taskStore.tasks.filter(t => t.assigneeId);

  assignedTasks.forEach(task => {
    const user = userMap.get(task.assigneeId!);
    const project = projectMap.get(task.projectId);
    if (!user || !project) return;

    const parent = task.parentTaskId ? taskMap.get(task.parentTaskId) : null;
    const depth = getDepth(task.id);

    assignments.push({
      id: `${user.id}-${task.id}`,
      userId: user.id,
      userName: user.name,
      userAvatar: user.avatar,
      taskId: task.id,
      taskName: task.title,
      projectId: project.id,
      projectName: project.name,
      status: task.status,
      priority: task.priority,
      progress: task.progress,
      parentTaskName: parent?.title,
      taskDepth: depth
    });
  });

  return assignments;
});

// ========== 2026-06-12:树状展开/折叠 ==========
// 当前展开的任务 ID 集合(展开后其直接子任务才会出现在可见列表里)
const expandedTaskIds = ref<Set<string>>(new Set());

// 排序比较函数(用于 sortBy / sortOrder)
const compareAssignments = (a: TaskAssignment, b: TaskAssignment): number => {
  let aVal: string | number = '';
  let bVal: string | number = '';
  switch (sortBy.value) {
    case 'memberName': aVal = a.userName; bVal = b.userName; break;
    case 'taskName': aVal = a.taskName; bVal = b.taskName; break;
    case 'projectName': aVal = a.projectName; bVal = b.projectName; break;
    case 'status': aVal = a.status; bVal = b.status; break;
    case 'priority': {
      const order = { urgent: 0, high: 1, medium: 2, low: 3 };
      aVal = order[a.priority]; bVal = order[b.priority];
      break;
    }
    default: aVal = a.userName; bVal = b.userName;
  }
  if (typeof aVal === 'string' && typeof bVal === 'string') {
    return sortOrder.value === 'asc'
      ? aVal.localeCompare(bVal, 'zh-CN')
      : bVal.localeCompare(aVal, 'zh-CN');
  }
  return sortOrder.value === 'asc'
    ? (aVal as number) - (bVal as number)
    : (bVal as number) - (aVal as number);
};

// 任务在已分配集合中的查找表
const assignmentByTaskId = computed(() => {
  return new Map(taskAssignments.value.map(a => [a.taskId, a]));
});

// 父任务 -> 子任务列表(仅含在已分配集合中的子任务)
const childrenByParent = computed(() => {
  const map = new Map<string, TaskAssignment[]>();
  for (const a of taskAssignments.value) {
    const task = taskStore.tasks.find(t => t.id === a.taskId);
    if (!task || !task.parentTaskId) continue;
    if (!assignmentByTaskId.value.has(task.parentTaskId)) continue;
    if (!map.has(task.parentTaskId)) map.set(task.parentTaskId, []);
    map.get(task.parentTaskId)!.push(a);
  }
  return map;
});

// 根任务:无父任务 或 父任务不在已分配集合中(默认显示为顶级)
const rootAssignments = computed(() => {
  return taskAssignments.value.filter(a => {
    const task = taskStore.tasks.find(t => t.id === a.taskId);
    if (!task || !task.parentTaskId) return true;
    return !assignmentByTaskId.value.has(task.parentTaskId);
  });
});

// 树状可见列表(尊重展开/折叠,深度按树中位置计算)
const sortedTaskAssignments = computed<TaskAssignment[]>(() => {
  const result: TaskAssignment[] = [];
  const visit = (a: TaskAssignment, depth: number) => {
    result.push({ ...a, taskDepth: depth });
    if (expandedTaskIds.value.has(a.taskId)) {
      const children = [...(childrenByParent.value.get(a.taskId) || [])].sort(compareAssignments);
      for (const child of children) {
        visit(child, depth + 1);
      }
    }
  };
  const sortedRoots = [...rootAssignments.value].sort(compareAssignments);
  for (const root of sortedRoots) {
    visit(root, 0);
  }
  return result;
});

// 展开/折叠助手
const hasChildren = (taskId: string): boolean => {
  return (childrenByParent.value.get(taskId) || []).length > 0;
};
const isExpanded = (taskId: string): boolean => {
  return expandedTaskIds.value.has(taskId);
};
const toggleExpand = (taskId: string) => {
  const next = new Set(expandedTaskIds.value);
  if (next.has(taskId)) next.delete(taskId);
  else next.add(taskId);
  expandedTaskIds.value = next;
};
const expandAll = () => {
  expandedTaskIds.value = new Set(taskAssignments.value.map(a => a.taskId));
};
const collapseAll = () => {
  expandedTaskIds.value = new Set();
};

// 分页后的任务分配数据
const paginatedTaskAssignments = computed<TaskAssignment[]>(() => {
  const start = (currentPage.value - 1) * itemsPerPage.value;
  const end = start + itemsPerPage.value;
  return sortedTaskAssignments.value.slice(start, end);
});

// 总页数
const totalPages = computed(() => {
  return Math.ceil(sortedTaskAssignments.value.length / itemsPerPage.value) || 1;
});

// 处理排序变化
const handleSortChange = () => {
  currentPage.value = 1;
};

// 处理每页条数变化
const handleItemsPerPageChange = () => {
  currentPage.value = 1;
};

// 状态标签样式
const getStatusBadgeVariant = (status: Task['status']) => {
  const variants: Record<Task['status'], 'default' | 'primary' | 'danger' | 'success' | 'warning' | 'info'> = {
    'todo': 'default',
    'in-progress': 'primary',
    'done': 'success'
  };
  return variants[status] || 'default';
};

// 状态标签文本
const getStatusLabel = (status: Task['status']) => {
  const statusMap: Record<string, string> = {
    'todo': t('taskStatus.todo'),
    'in-progress': t('taskStatus.inProgress'),
    'done': t('taskStatus.done')
  };
  return statusMap[status] || status;
};

// 优先级标签样式
const getPriorityBadgeVariant = (priority: Task['priority']) => {
  const variants: Record<Task['priority'], 'default' | 'primary' | 'danger' | 'success' | 'warning' | 'info'> = {
    'low': 'default',
    'medium': 'info',
    'high': 'warning',
    'urgent': 'danger'
  };
  return variants[priority] || 'default';
};

// 优先级标签文本
const getPriorityLabel = (priority: Task['priority']) => {
  return t(`priorities.${priority}`);
};

// 监听任务、用户、项目数据变化，重置当前页
watch([() => taskStore.tasks, () => userStore.users, () => projectStore.projects], () => {
  currentPage.value = 1;
});

// 监听数据加载完成，重新渲染图表
watch(() => taskStore.loaded, async (loaded) => {
  if (loaded && userStore.users.length > 0) {
    await nextTick();
    initWorkloadChart();
  }
}, { immediate: false });

// 部门筛选变化时,重新计算工作负载分布(2026-06-17)
watch(
  [selectedDeptCode, includeSubDepts, () => orgStore.tree],
  async () => {
    await nextTick();
    initWorkloadChart();
  }
);

// 窗口大小变化处理
const handleResize = () => {
  if (workloadChartInstance) {
    workloadChartInstance.resize();
  }
};

onMounted(async () => {
  // 强制刷新用户数据，确保获取最新数据
  await userStore.refreshUsers();

  // 加载组织架构树
  try {
    await orgStore.loadTree();
  } catch (error) {
    console.warn('加载组织架构失败:', error);
  }

  // 强制刷新项目数据（任务分配表格需要）
  try {
    await projectStore.refreshProjects();
  } catch (error) {
    console.warn('加载项目数据失败:', error);
  }

  // 强制刷新任务数据（工作负载分布图表和任务分配表格需要）
  try {
    await taskStore.refreshTasks();
  } catch (error) {
    console.warn('加载任务数据失败:', error);
  }

  // 初始化图表
  await nextTick();
  initWorkloadChart();

  // 监听窗口大小变化
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  if (workloadChartInstance) {
    workloadChartInstance.dispose();
    workloadChartInstance = null;
  }
});
</script>
