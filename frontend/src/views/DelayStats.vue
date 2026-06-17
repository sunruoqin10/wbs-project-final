<template>
  <MainLayout>
    <div class="space-y-6">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-secondary-900">{{ $t('delayStats.title') }}</h1>
          <p class="mt-1 text-sm text-secondary-600">{{ $t('delayStats.subtitle') }}</p>
        </div>
        <div class="flex items-center gap-2">
          <Button variant="secondary" @click="handleRefresh" :loading="loading">
            <svg class="mr-2 h-5 w-5" :class="{ 'animate-spin': loading }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            {{ $t('delayStats.refresh') }}
          </Button>
        </div>
      </div>

      <div class="border-b border-secondary-200">
        <nav class="-mb-px flex space-x-8">
          <button
            @click="activeTab = 'personal'"
            :class="[
              activeTab === 'personal'
                ? 'border-primary-500 text-primary-600'
                : 'border-transparent text-secondary-500 hover:border-secondary-300 hover:text-secondary-700',
              'whitespace-nowrap border-b-2 py-4 px-1 text-sm font-medium'
            ]"
          >
            <div class="flex items-center gap-2">
              <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
              </svg>
              {{ $t('delayStats.personalTab') }}
            </div>
          </button>
          <button
            @click="activeTab = 'team'"
            :class="[
              activeTab === 'team'
                ? 'border-primary-500 text-primary-600'
                : 'border-transparent text-secondary-500 hover:border-secondary-300 hover:text-secondary-700',
              'whitespace-nowrap border-b-2 py-4 px-1 text-sm font-medium'
            ]"
          >
            <div class="flex items-center gap-2">
              <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
              </svg>
              {{ $t('delayStats.teamTab') }}
            </div>
          </button>
        </nav>
      </div>

      <div v-if="loading" class="flex items-center justify-center py-12">
        <div class="text-center">
          <svg class="mx-auto h-8 w-8 animate-spin text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
          </svg>
          <p class="mt-2 text-secondary-500">{{ $t('delayStats.loading') }}</p>
        </div>
      </div>

      <div v-else-if="activeTab === 'personal'" class="space-y-6">
        <div v-if="personalTasks.length === 0" class="flex items-center justify-center py-12">
          <div class="text-center">
            <svg class="mx-auto h-16 w-16 text-secondary-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
            </svg>
            <h3 class="mt-2 text-lg font-medium text-secondary-900">{{ $t('delayStats.empty.noTasks') }}</h3>
            <p class="mt-1 text-sm text-secondary-500">{{ $t('delayStats.empty.noData') }}</p>
          </div>
        </div>

        <div v-else class="space-y-6">
          <div class="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
            <Card>
              <div class="flex items-center">
                <div class="rounded-lg bg-blue-100 p-3">
                  <svg class="h-6 w-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                  </svg>
                </div>
                <div class="ml-4">
                  <p class="text-sm font-medium text-secondary-600">{{ $t('delayStats.stats.totalTasks') }}</p>
                  <p class="text-2xl font-semibold text-blue-600">{{ personalStats.totalTasks }}</p>
                </div>
              </div>
            </Card>

            <Card>
              <div class="flex items-center">
                <div class="rounded-lg bg-danger-100 p-3">
                  <svg class="h-6 w-6 text-danger-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                </div>
                <div class="ml-4">
                  <p class="text-sm font-medium text-secondary-600">{{ $t('delayStats.stats.delayedTasks') }}</p>
                  <p class="text-2xl font-semibold text-danger-600">{{ personalStats.delayedTasks }}</p>
                </div>
              </div>
            </Card>

            <Card>
              <div class="flex items-center">
                <div class="rounded-lg bg-info-100 p-3">
                  <svg class="h-6 w-6 text-info-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                  </svg>
                </div>
                <div class="ml-4">
                  <p class="text-sm font-medium text-secondary-600">{{ $t('delayStats.stats.delayRate') }}</p>
                  <p class="text-2xl font-semibold text-info-600">{{ Math.round(personalStats.delayRate) }}%</p>
                </div>
              </div>
            </Card>

            <Card>
              <div class="flex items-center">
                <div class="rounded-lg bg-warning-100 p-3">
                  <svg class="h-6 w-6 text-warning-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
                  </svg>
                </div>
                <div class="ml-4">
                  <p class="text-sm font-medium text-secondary-600">{{ $t('delayStats.stats.totalDelayedDays') }}</p>
                  <p class="text-2xl font-semibold text-warning-600">{{ personalStats.totalDelayedDays }}</p>
                </div>
              </div>
            </Card>
          </div>

          <div class="grid grid-cols-1 gap-6 lg:grid-cols-2">
            <Card>
              <template #header>
                <h3 class="text-lg font-semibold text-secondary-900">{{ $t('delayStats.charts.delayDurationDistribution') }}</h3>
              </template>
              <div class="h-80" ref="personalDelayDistributionChartRef"></div>
            </Card>

            <Card>
              <template #header>
                <h3 class="text-lg font-semibold text-secondary-900">{{ $t('delayStats.charts.projectDelayDistribution') }}</h3>
              </template>
              <div class="h-80" ref="personalProjectDelayChartRef"></div>
            </Card>
          </div>

          <Card>
            <template #header>
              <h3 class="text-lg font-semibold text-secondary-900">{{ $t('delayStats.list.delayedTaskList') }}</h3>
            </template>
            <div class="overflow-x-auto">
              <table class="min-w-full divide-y divide-secondary-200">
                <thead class="bg-secondary-50">
                  <tr>
                    <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">{{ $t('delayStats.list.taskName') }}</th>
                    <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">{{ $t('delayStats.list.projectName') }}</th>
                    <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">{{ $t('delayStats.list.delayedDays') }}</th>
                    <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">{{ $t('delayStats.list.severity') }}</th>
                    <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">{{ $t('delayStats.list.endDate') }}</th>
                    <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">{{ $t('delayStats.list.status') }}</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-secondary-200 bg-white">
                  <tr v-if="personalDelayedTasks.length === 0">
                    <td colspan="6" class="px-4 py-8 text-center text-sm text-secondary-500">
                      {{ $t('delayStats.list.noDelayedTasks') }}
                    </td>
                  </tr>
                  <tr v-for="task in personalDelayedTasks" :key="task.id" class="hover:bg-secondary-50">
                    <td class="whitespace-nowrap px-4 py-3 text-sm font-medium text-secondary-900">{{ task.title }}</td>
                    <td class="whitespace-nowrap px-4 py-3 text-sm text-secondary-600">{{ getProjectName(task.projectId) }}</td>
                    <td class="whitespace-nowrap px-4 py-3 text-sm font-semibold" :class="getDelayTextClass(task.delayedDays || 0)">
                      {{ task.delayedDays || 0 }} {{ $t('delayStats.common.days') }}
                    </td>
                    <td class="whitespace-nowrap px-4 py-3 text-sm">
                      <span :class="getDelayBadgeClass(task.delayedDays || 0)" class="inline-flex rounded-full px-2 py-1 text-xs font-medium">
                        {{ getDelaySeverityLabel(task.delayedDays || 0) }}
                      </span>
                    </td>
                    <td class="whitespace-nowrap px-4 py-3 text-sm text-secondary-600">{{ formatDate(task.endDate) }}</td>
                    <td class="whitespace-nowrap px-4 py-3 text-sm">
                      <span :class="getStatusBadgeClass(task.status)" class="inline-flex rounded-full px-2 py-1 text-xs font-medium">
                        {{ getStatusLabel(task.status) }}
                      </span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </Card>
        </div>
      </div>

      <div v-else class="space-y-6">
        <div v-if="teamMembers.length === 0" class="flex items-center justify-center py-12">
          <div class="text-center">
            <svg class="mx-auto h-16 w-16 text-secondary-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
            </svg>
            <h3 class="mt-2 text-lg font-medium text-secondary-900">{{ $t('delayStats.empty.noTeamMembers') }}</h3>
            <p class="mt-1 text-sm text-secondary-500">{{ $t('delayStats.empty.noTeamMembersDesc') }}</p>
          </div>
        </div>

        <div v-else class="space-y-6">
          <!-- 部门筛选行(2026-06-17 新增,仅 admin 可见) -->
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

          <!--
            注意:不要给 <Transition> 加 mode="out-in",否则旧 div 的 200ms leave 动画期间
            新的图表容器还没被创建,ref 仍指向旧 div,watch 里 init 出来的
            图表会挂到旧 div 上,等 leave 结束旧 div 被销毁后,新 div 上就没有图表了。
          -->
          <Transition name="content-fade">
            <div :key="`${selectedDeptCode}-${includeSubDepts}`" class="space-y-6">
          <div class="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
            <Card>
              <div class="flex items-center">
                <div class="rounded-lg bg-blue-100 p-3">
                  <svg class="h-6 w-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                  </svg>
                </div>
                <div class="ml-4">
                  <p class="text-sm font-medium text-secondary-600">{{ $t('delayStats.charts.teamTotalTasks') }}</p>
                  <p class="text-2xl font-semibold text-blue-600">{{ teamStats.totalTasks }}</p>
                </div>
              </div>
            </Card>

            <Card>
              <div class="flex items-center">
                <div class="rounded-lg bg-danger-100 p-3">
                  <svg class="h-6 w-6 text-danger-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                </div>
                <div class="ml-4">
                  <p class="text-sm font-medium text-secondary-600">{{ $t('delayStats.charts.teamDelayedTasks') }}</p>
                  <p class="text-2xl font-semibold text-danger-600">{{ teamStats.delayedTasks }}</p>
                </div>
              </div>
            </Card>

            <Card>
              <div class="flex items-center">
                <div class="rounded-lg bg-info-100 p-3">
                  <svg class="h-6 w-6 text-info-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                  </svg>
                </div>
                <div class="ml-4">
                  <p class="text-sm font-medium text-secondary-600">{{ $t('delayStats.charts.teamDelayRate') }}</p>
                  <p class="text-2xl font-semibold text-info-600">{{ Math.round(teamStats.delayRate) }}%</p>
                </div>
              </div>
            </Card>

            <Card>
              <div class="flex items-center">
                <div class="rounded-lg bg-purple-100 p-3">
                  <svg class="h-6 w-6 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                  </svg>
                </div>
                <div class="ml-4">
                  <p class="text-sm font-medium text-secondary-600">{{ $t('delayStats.charts.teamMemberCount') }}</p>
                  <p class="text-2xl font-semibold text-purple-600">{{ teamMembers.length }}</p>
                </div>
              </div>
            </Card>
          </div>

          <div class="grid grid-cols-1 gap-6 lg:grid-cols-2">
            <Card>
              <template #header>
                <h3 class="text-lg font-semibold text-secondary-900">{{ $t('delayStats.charts.memberDelayRateComparison') }}</h3>
              </template>
              <div class="h-80" ref="teamMemberDelayRateChartRef"></div>
            </Card>

            <Card>
              <template #header>
                <h3 class="text-lg font-semibold text-secondary-900">{{ $t('delayStats.charts.memberDelayedTasksComparison') }}</h3>
              </template>
              <div class="h-80" ref="teamMemberDelayCountChartRef"></div>
            </Card>
          </div>

          <Card>
            <template #header>
              <h3 class="text-lg font-semibold text-secondary-900">{{ $t('delayStats.charts.teamMemberDelayDetails') }}</h3>
            </template>
            <div class="overflow-x-auto">
              <table class="min-w-full divide-y divide-secondary-200">
                <thead class="bg-secondary-50">
                  <tr>
                    <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">{{ $t('delayStats.list.member') }}</th>
                    <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">{{ $t('delayStats.list.totalTasks') }}</th>
                    <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">{{ $t('delayStats.list.delayedTasks') }}</th>
                    <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">{{ $t('delayStats.list.delayRate') }}</th>
                    <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">{{ $t('delayStats.list.totalDelayedDays') }}</th>
                    <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">{{ $t('delayStats.list.actions') }}</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-secondary-200 bg-white">
                  <tr v-for="member in paginatedTeamMemberStats" :key="member.userId" class="hover:bg-secondary-50">
                    <td class="whitespace-nowrap px-4 py-3 text-sm">
                      <div class="flex items-center">
                        <UserAvatar
                          :name="getUserName(member.userId)"
                          :seed="getUserAvatar(member.userId)"
                          size="md"
                          class="mr-2"
                        />
                        {{ getUserName(member.userId) }}
                      </div>
                    </td>
                    <td class="whitespace-nowrap px-4 py-3 text-sm text-secondary-900">{{ member.totalTasks }}</td>
                    <td class="whitespace-nowrap px-4 py-3 text-sm font-semibold text-danger-600">{{ member.delayedTasks }}</td>
                    <td class="whitespace-nowrap px-4 py-3 text-sm">
                      <span :class="getDelayRateBadgeClass(member.delayRate)" class="inline-flex rounded-full px-2 py-1 text-xs font-medium">
                        {{ Math.round(member.delayRate) }}%
                      </span>
                    </td>
                    <td class="whitespace-nowrap px-4 py-3 text-sm text-warning-600 font-semibold">{{ member.totalDelayedDays }} {{ $t('delayStats.common.days') }}</td>
                    <td class="whitespace-nowrap px-4 py-3 text-sm">
                      <button
                        @click="showMemberDetail(member.userId)"
                        class="text-primary-600 hover:text-primary-800"
                      >
                        {{ $t('delayStats.common.viewDetails') }}
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div v-if="teamMembersWithTasks.length > teamMembersPagination.pageSize" class="mt-4 flex items-center justify-between border-t border-secondary-200 pt-4">
              <div class="text-sm text-secondary-600">
                共 {{ teamMembersWithTasks.length }} 条记录，第 {{ teamMembersPagination.currentPage }} 页
              </div>
              <div class="flex items-center gap-2">
                <select 
                  v-model="teamMembersPagination.pageSize"
                  class="rounded border border-secondary-300 px-2 py-1 text-sm"
                  @change="teamMembersPagination.currentPage = 1"
                >
                  <option :value="10">10 {{ $t('common.pagination.perPage') }}</option>
                  <option :value="20">20 {{ $t('common.pagination.perPage') }}</option>
                  <option :value="50">50 {{ $t('common.pagination.perPage') }}</option>
                </select>
                <button
                  @click="handleTeamMembersPageChange(teamMembersPagination.currentPage - 1)"
                  :disabled="teamMembersPagination.currentPage === 1"
                  class="rounded border border-secondary-300 px-3 py-1 text-sm hover:bg-secondary-50 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {{ $t('common.pagination.previous') }}
                </button>
                <button
                  v-for="page in getTeamMembersPageNumbers()"
                  :key="page"
                  @click="handleTeamMembersPageChange(page)"
                  :class="[
                    'rounded px-3 py-1 text-sm',
                    page === teamMembersPagination.currentPage 
                      ? 'bg-primary-500 text-white' 
                      : 'border border-secondary-300 hover:bg-secondary-50'
                  ]"
                >
                  {{ page }}
                </button>
                <button
                  @click="handleTeamMembersPageChange(teamMembersPagination.currentPage + 1)"
                  :disabled="teamMembersPagination.currentPage === teamMembersTotalPages"
                  class="rounded border border-secondary-300 px-3 py-1 text-sm hover:bg-secondary-50 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {{ $t('common.pagination.next') }}
                </button>
              </div>
            </div>
          </Card>
            </div>
          </Transition>
        </div>
      </div>
    </div>

    <Modal
      :open="showMemberDetailModal"
      @close="showMemberDetailModal = false"
      :title="$t('delayStats.details.memberTaskDetails')"
      size="3xl"
    >
      <div v-if="selectedMemberTasks.length > 0" class="space-y-4">
        <div class="grid grid-cols-2 gap-4">
          <div class="rounded-lg bg-secondary-50 p-4">
            <p class="text-sm font-medium text-secondary-600">{{ $t('delayStats.charts.totalTasks') }}</p>
            <p class="text-2xl font-semibold text-secondary-900">{{ selectedMemberStats.totalTasks }}</p>
          </div>
          <div class="rounded-lg bg-secondary-50 p-4">
            <p class="text-sm font-medium text-secondary-600">{{ $t('delayStats.charts.delayedTasksCount') }}</p>
            <p class="text-2xl font-semibold text-danger-600">{{ selectedMemberStats.delayedTasks }}</p>
          </div>
        </div>
        <div class="overflow-x-auto max-h-96">
          <table class="min-w-full divide-y divide-secondary-200">
            <thead class="bg-secondary-50 sticky top-0">
              <tr>
                <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">{{ $t('delayStats.list.taskName') }}</th>
                <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">{{ $t('delayStats.list.projectName') }}</th>
                <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">{{ $t('delayStats.list.delayedDays') }}</th>
                <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">{{ $t('delayStats.list.severity') }}</th>
                <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">{{ $t('delayStats.list.status') }}</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-secondary-200 bg-white">
              <tr v-for="task in selectedMemberTasks" :key="task.id" class="hover:bg-secondary-50">
                <td class="whitespace-nowrap px-4 py-3 text-sm font-medium text-secondary-900">{{ task.title }}</td>
                <td class="whitespace-nowrap px-4 py-3 text-sm text-secondary-600">{{ getProjectName(task.projectId) }}</td>
                <td class="whitespace-nowrap px-4 py-3 text-sm font-semibold" :class="getDelayTextClass(task.delayedDays || 0)">
                  {{ task.delayedDays || 0 }} {{ $t('delayStats.common.days') }}
                </td>
                <td class="whitespace-nowrap px-4 py-3 text-sm">
                  <span :class="getDelayBadgeClass(task.delayedDays || 0)" class="inline-flex rounded-full px-2 py-1 text-xs font-medium">
                    {{ getDelaySeverityLabel(task.delayedDays || 0) }}
                  </span>
                </td>
                <td class="whitespace-nowrap px-4 py-3 text-sm">
                  <span :class="getStatusBadgeClass(task.status)" class="inline-flex rounded-full px-2 py-1 text-xs font-medium">
                    {{ getStatusLabel(task.status) }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <div v-else class="py-8 text-center">
        <svg class="mx-auto h-12 w-12 text-secondary-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
        </svg>
        <h3 class="mt-2 text-sm font-medium text-secondary-900">{{ $t('delayStats.common.noTasks') }}</h3>
        <p class="mt-1 text-sm text-secondary-500">{{ $t('delayStats.common.noTasksForMember') }}</p>
      </div>
    </Modal>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, onUnmounted, nextTick } from 'vue';
import { useI18n } from 'vue-i18n';
import * as echarts from 'echarts';
import dayjs from 'dayjs';
import MainLayout from '@/components/layout/MainLayout.vue';
import Card from '@/components/common/Card.vue';
import Button from '@/components/common/Button.vue';
import Modal from '@/components/common/Modal.vue';
import UserAvatar from '@/components/common/UserAvatar.vue';
import OrgTreeSelect from '@/components/common/OrgTreeSelect.vue';
import { useProjectStore } from '@/stores/project';
import { useTaskStore } from '@/stores/task';
import { useUserStore } from '@/stores/user';
import { usePermissionStore } from '@/stores/permission';
import { useOrgStore } from '@/stores/org';
import type { Task, OrgNode } from '@/types';

const { t } = useI18n();

const projectStore = useProjectStore();
const taskStore = useTaskStore();
const userStore = useUserStore();
const permissionStore = usePermissionStore();
const orgStore = useOrgStore();

const activeTab = ref<'personal' | 'team'>('personal');
const loading = ref(false);
const showMemberDetailModal = ref(false);
const selectedMemberId = ref<string>('');

const currentUser = computed(() => userStore.currentUser);

// ============ 部门过滤(admin 专用,2026-06-17 新增) ============ //
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

const teamMembersPagination = ref({
  currentPage: 1,
  pageSize: 10
});

const personalDelayDistributionChartRef = ref<HTMLElement>();
const personalProjectDelayChartRef = ref<HTMLElement>();
const teamMemberDelayRateChartRef = ref<HTMLElement>();
const teamMemberDelayCountChartRef = ref<HTMLElement>();

let personalDelayDistributionChartInstance: echarts.ECharts | null = null;
let personalProjectDelayChartInstance: echarts.ECharts | null = null;
let teamMemberDelayRateChartInstance: echarts.ECharts | null = null;
let teamMemberDelayCountChartInstance: echarts.ECharts | null = null;

const accessibleProjects = computed(() => {
  if (permissionStore.currentRole === 'admin' || permissionStore.currentRole === 'project-manager') {
    return projectStore.projects;
  }

  const currentUserId = userStore.currentUserId;
  if (!currentUserId) {
    return [];
  }

  return projectStore.projects.filter(project => {
    const isOwner = project.ownerId === currentUserId;
    const isMember = project.memberIds?.includes(currentUserId) || false;
    return isOwner || isMember;
  });
});

const accessibleProjectIds = computed(() => {
  return accessibleProjects.value.map(p => p.id);
});

const accessibleTasks = computed(() => {
  return taskStore.tasks.filter(t => accessibleProjectIds.value.includes(t.projectId));
});

/** 部门二次过滤后的项目列表(2026-06-17 新增,仅团队 tab 使用) */
const deptFilteredAccessibleProjects = computed(() => {
  // 非 admin 直接用全量(没有部门筛选 UI,行为保持原样)
  if (!currentUser.value || currentUser.value.role !== 'admin') {
    return accessibleProjects.value;
  }
  return accessibleProjects.value.filter(p =>
    effectiveDeptCodes.value.has(p.deptCode ?? null)
  );
});

/** 部门二次过滤后的任务 ID 集合(2026-06-17 新增) */
const deptFilteredAccessibleTasks = computed(() => {
  const projectIds = new Set(deptFilteredAccessibleProjects.value.map(p => p.id));
  return accessibleTasks.value.filter(t => projectIds.has(t.projectId));
});

/** 团队维度专用:从部门过滤后的任务中取某个用户负责的(2026-06-17 新增) */
const getTasksForUserInDept = (userId: string): Task[] => {
  return getTasksForUserImpl(userId, deptFilteredAccessibleTasks.value);
};

/** 任务筛选核心实现(2026-06-17 重构,与个人 tab 解耦,团队 tab 走部门过滤版) */
function getTasksForUserImpl(userId: string, sourceTasks: Task[]): Task[] {
  const userTasks = sourceTasks.filter(t => t.assigneeId === userId);

  const allTaskIds = new Set(userTasks.map(t => t.id));
  const parentTaskIds = new Set(userTasks.filter(t => t.parentTaskId).map(t => t.parentTaskId));
  const leafTaskIds = new Set([...allTaskIds].filter(id => !parentTaskIds.has(id)));
  const leafTasks = userTasks.filter(t => leafTaskIds.has(t.id));

  if (leafTasks.length > 0) {
    return leafTasks;
  }

  const rootTasks = userTasks.filter(t => !t.parentTaskId);
  if (rootTasks.length === 1) {
    return rootTasks;
  }

  return userTasks;
}

const getTasksForUser = (userId: string): Task[] => {
  return getTasksForUserImpl(userId, accessibleTasks.value);
};

const personalTasks = computed(() => {
  const currentUserId = userStore.currentUserId;
  if (!currentUserId) return [];
  return getTasksForUser(currentUserId);
});

const personalDelayedTasks = computed(() => {
  return personalTasks.value.filter(t => (t.delayedDays || 0) > 0 && t.status !== 'done');
});

const personalStats = computed(() => {
  const tasks = personalTasks.value;
  const delayedTasks = personalDelayedTasks.value;

  const totalDelayedDays = delayedTasks.reduce((sum, t) => sum + (t.delayedDays || 0), 0);
  const delayRate = tasks.length > 0 ? (delayedTasks.length / tasks.length) * 100 : 0;

  return {
    totalTasks: tasks.length,
    delayedTasks: delayedTasks.length,
    delayRate,
    totalDelayedDays
  };
});

const teamMembers = computed(() => {
  const currentUserId = userStore.currentUserId;
  if (!currentUserId) return [];

  // 统计参与部门过滤后项目的总成员数(2026-06-17 改为基于 deptFilteredAccessibleProjects)
  const memberSet = new Set<string>();
  deptFilteredAccessibleProjects.value.forEach(p => {
    memberSet.add(p.ownerId);
    if (p.memberIds) {
      p.memberIds.forEach(id => memberSet.add(id));
    }
  });

  return userStore.users.filter(u => memberSet.has(u.id) && u.id !== currentUserId);
});

const teamMemberStats = computed(() => {
  return teamMembers.value.map(member => {
    // 2026-06-17:团队维度走部门过滤版,避免非成员项目中的任务污染统计
    const tasks = getTasksForUserInDept(member.id);
    const delayedTasks = tasks.filter(t => (t.delayedDays || 0) > 0 && t.status !== 'done');
    const totalDelayedDays = delayedTasks.reduce((sum, t) => sum + (t.delayedDays || 0), 0);
    const delayRate = tasks.length > 0 ? (delayedTasks.length / tasks.length) * 100 : 0;

    return {
      userId: member.id,
      totalTasks: tasks.length,
      delayedTasks: delayedTasks.length,
      delayRate,
      totalDelayedDays
    };
  });
});

const teamStats = computed(() => {
  let totalTasks = 0;
  let delayedTasks = 0;
  let totalDelayedDays = 0;

  teamMemberStats.value.forEach(stats => {
    totalTasks += stats.totalTasks;
    delayedTasks += stats.delayedTasks;
    totalDelayedDays += stats.totalDelayedDays;
  });

  const delayRate = totalTasks > 0 ? (delayedTasks / totalTasks) * 100 : 0;

  return {
    totalTasks,
    delayedTasks,
    delayRate,
    totalDelayedDays
  };
});

const teamMembersWithTasks = computed(() => {
  return teamMemberStats.value.filter(m => m.totalTasks > 0);
});

const teamMembersTotalPages = computed(() => {
  return Math.ceil(teamMembersWithTasks.value.length / teamMembersPagination.value.pageSize);
});

const paginatedTeamMemberStats = computed(() => {
  const start = (teamMembersPagination.value.currentPage - 1) * teamMembersPagination.value.pageSize;
  const end = start + teamMembersPagination.value.pageSize;
  return teamMembersWithTasks.value.slice(start, end);
});

const getTeamMembersPageNumbers = () => {
  const pages: number[] = [];
  const maxVisiblePages = 5;
  const currentPage = teamMembersPagination.value.currentPage;
  const total = teamMembersTotalPages.value;

  if (total <= maxVisiblePages) {
    for (let i = 1; i <= total; i++) pages.push(i);
  } else {
    let startPage = Math.max(1, currentPage - 2);
    let endPage = Math.min(total, startPage + maxVisiblePages - 1);
    if (endPage - startPage < maxVisiblePages - 1) {
      startPage = Math.max(1, endPage - maxVisiblePages + 1);
    }
    for (let i = startPage; i <= endPage; i++) pages.push(i);
  }
  return pages;
};

const handleTeamMembersPageChange = (page: number) => {
  if (page >= 1 && page <= teamMembersTotalPages.value) {
    teamMembersPagination.value.currentPage = page;
  }
};

const selectedMemberTasks = computed(() => {
  if (!selectedMemberId.value) return [];
  // 2026-06-17:模态框从团队 tab 打开,数据走部门过滤版保持一致
  return getTasksForUserInDept(selectedMemberId.value);
});

const selectedMemberStats = computed(() => {
  if (!selectedMemberId.value) return { totalTasks: 0, delayedTasks: 0, delayRate: 0, totalDelayedDays: 0 };
  const tasks = getTasksForUserInDept(selectedMemberId.value);
  const delayedTasks = tasks.filter(t => (t.delayedDays || 0) > 0 && t.status !== 'done');
  const totalDelayedDays = delayedTasks.reduce((sum, t) => sum + (t.delayedDays || 0), 0);
  const delayRate = tasks.length > 0 ? (delayedTasks.length / tasks.length) * 100 : 0;

  return {
    totalTasks: tasks.length,
    delayedTasks: delayedTasks.length,
    delayRate,
    totalDelayedDays
  };
});

const getProjectName = (projectId: string) => {
  const project = projectStore.projectById(projectId);
  return project?.name || t('delayStats.common.unknownProject');
};

const getUserName = (userId?: string) => {
  if (!userId) return '-';
  const user = userStore.userById(userId);
  return user?.name || t('delayStats.common.unknown');
};

const getUserAvatar = (userId?: string) => {
  if (!userId) return '';
  const user = userStore.userById(userId);
  return user?.avatar || '';
};

const formatDate = (dateStr: string) => {
  return dayjs(dateStr).format('YYYY-MM-DD');
};

const getDelayTextClass = (days: number) => {
  if (days >= 7) return 'text-danger-600';
  if (days >= 3) return 'text-warning-600';
  return 'text-info-600';
};

const getDelayBadgeClass = (days: number) => {
  if (days >= 7) return 'bg-danger-100 text-danger-800';
  if (days >= 3) return 'bg-warning-100 text-warning-800';
  return 'bg-info-100 text-info-800';
};

const getDelaySeverityLabel = (days: number) => {
  if (days >= 7) return t('delayStats.severity.severe');
  if (days >= 3) return t('delayStats.severity.moderate');
  return t('delayStats.severity.mild');
};

const getDelayRateBadgeClass = (rate: number) => {
  if (rate >= 50) return 'bg-danger-100 text-danger-800';
  if (rate >= 20) return 'bg-warning-100 text-warning-800';
  if (rate > 0) return 'bg-info-100 text-info-800';
  return 'bg-green-100 text-green-800';
};

const getStatusBadgeClass = (status: string) => {
  switch (status) {
    case 'todo':
      return 'bg-secondary-100 text-secondary-800';
    case 'in-progress':
      return 'bg-blue-100 text-blue-800';
    case 'done':
      return 'bg-green-100 text-green-800';
    default:
      return 'bg-secondary-100 text-secondary-800';
  }
};

const getStatusLabel = (status: string) => {
  switch (status) {
    case 'todo':
      return t('delayStats.status.todo');
    case 'in-progress':
      return t('delayStats.status.inProgress');
    case 'done':
      return t('delayStats.status.completed');
    default:
      return status;
  }
};

const initPersonalDelayDistributionChart = () => {
  if (!personalDelayDistributionChartRef.value) return;

  const chart = echarts.init(personalDelayDistributionChartRef.value);

  const critical = personalDelayedTasks.value.filter(t => (t.delayedDays || 0) >= 7).length;
  const warning = personalDelayedTasks.value.filter(t => {
    const days = t.delayedDays || 0;
    return days >= 3 && days < 7;
  }).length;
  const minor = personalDelayedTasks.value.filter(t => {
    const days = t.delayedDays || 0;
    return days > 0 && days < 3;
  }).length;

  const delayData = [
    { value: critical, name: '严重延期（≥7天）', itemStyle: { color: '#dc2626' } },
    { value: warning, name: '中度延期（3-6天）', itemStyle: { color: '#f97316' } },
    { value: minor, name: '轻微延期（<3天）', itemStyle: { color: '#eab308' } }
  ].filter(item => item.value > 0);

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)'
    },
    legend: {
      bottom: '0%',
      left: 'center'
    },
    series: [
      {
        name: t('delayStats.charts.delayedTasks'),
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          formatter: '{b}\n{c} 个'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 16,
            fontWeight: 'bold'
          }
        },
        data: delayData.length > 0 ? delayData : [{ value: 1, name: t('delayStats.common.noData'), itemStyle: { color: '#d1d5db' } }]
      }
    ]
  };

  chart.setOption(option);
  personalDelayDistributionChartInstance = chart;
};

const initPersonalProjectDelayChart = () => {
  if (!personalProjectDelayChartRef.value) return;

  const chart = echarts.init(personalProjectDelayChartRef.value);

  const projectDelayMap = new Map<string, { name: string; count: number; days: number }>();

  personalDelayedTasks.value.forEach(task => {
    const projectName = getProjectName(task.projectId);
    const existing = projectDelayMap.get(task.projectId);
    if (existing) {
      existing.count += 1;
      existing.days += task.delayedDays || 0;
    } else {
      projectDelayMap.set(task.projectId, {
        name: projectName,
        count: 1,
        days: task.delayedDays || 0
      });
    }
  });

  const projectDelayData = Array.from(projectDelayMap.values())
    .sort((a, b) => b.days - a.days)
    .slice(0, 10);

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter: (params: any) => {
        const param = params[0];
        const data = projectDelayData[param.dataIndex];
        return `${data.name}<br/>${t('delayStats.tooltip.projectDelay')}: ${data.count}<br/>${t('delayStats.tooltip.cumulativeDelayedDays')}: ${data.days}`;
      }
    },
    grid: {
      left: '20%',
      right: '15%',
      top: '10%',
      bottom: '10%'
    },
    xAxis: {
      type: 'value',
      name: t('delayStats.charts.totalDelayedDays'),
      nameLocation: 'middle',
      nameGap: 30
    },
    yAxis: {
      type: 'category',
      data: projectDelayData.map(item => item.name),
      inverse: true,
      axisLabel: {
        width: 100,
        overflow: 'truncate',
        ellipsis: '...'
      }
    },
    series: [
      {
        name: t('delayStats.charts.totalDelayedDays'),
        type: 'bar',
        data: projectDelayData.map(item => item.days),
        itemStyle: {
          color: (params: any) => {
            const colors = ['#dc2626', '#f97316', '#eab308', '#3b82f6', '#6366f1'];
            return colors[params.dataIndex % colors.length];
          }
        },
        label: {
          show: true,
          position: 'right',
          formatter: '{c} 天'
        }
      }
    ]
  };

  if (projectDelayData.length === 0) {
    option.series[0].data = [0];
    option.yAxis.data = ['暂无数据'];
  }

  chart.setOption(option);
  personalProjectDelayChartInstance = chart;
};

const initTeamMemberDelayRateChart = () => {
  if (!teamMemberDelayRateChartRef.value) return;

  const chart = echarts.init(teamMemberDelayRateChartRef.value);

  const memberData = teamMemberStats.value
    .filter(m => m.totalTasks > 0)
    .sort((a, b) => b.delayRate - a.delayRate)
    .slice(0, 10);

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter: (params: any) => {
        const param = params[0];
        const data = memberData[param.dataIndex];
        return `${getUserName(data.userId)}<br/>${t('delayStats.tooltip.memberDelayRate')}: ${Math.round(data.delayRate)}%<br/>${t('delayStats.tooltip.totalTasks')}: ${data.totalTasks}<br/>${t('delayStats.tooltip.delayedTasks')}: ${data.delayedTasks}`;
      }
    },
    grid: {
      left: '20%',
      right: '15%',
      top: '10%',
      bottom: '10%'
    },
    xAxis: {
      type: 'value',
      name: `${t('delayStats.charts.delayRate')} (%)`,
      nameLocation: 'middle',
      nameGap: 30,
      max: 100
    },
    yAxis: {
      type: 'category',
      data: memberData.map(m => getUserName(m.userId)),
      inverse: true,
      axisLabel: {
        width: 80,
        overflow: 'truncate',
        ellipsis: '...'
      }
    },
    series: [
      {
        name: t('delayStats.charts.delayRate'),
        type: 'bar',
        data: memberData.map(m => m.delayRate),
        itemStyle: {
          color: (params: any) => {
            const value = params.value;
            if (value >= 50) return '#dc2626';
            if (value >= 20) return '#f97316';
            if (value > 0) return '#eab308';
            return '#22c55e';
          }
        },
        label: {
          show: true,
          position: 'right',
          formatter: (params: any) => `${Math.round(params.value)}%`
        }
      }
    ]
  };

  if (memberData.length === 0) {
    option.series[0].data = [0];
    option.yAxis.data = [t('delayStats.common.noData')];
  }

  chart.setOption(option);
  teamMemberDelayRateChartInstance = chart;
};

const initTeamMemberDelayCountChart = () => {
  if (!teamMemberDelayCountChartRef.value) return;

  const chart = echarts.init(teamMemberDelayCountChartRef.value);

  const memberData = teamMemberStats.value
    .filter(m => m.totalTasks > 0)
    .sort((a, b) => b.delayedTasks - a.delayedTasks)
    .slice(0, 10);

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    legend: {
      data: [t('delayStats.charts.totalTasks'), t('delayStats.charts.delayedTasksCount')],
      bottom: '0%'
    },
    grid: {
      left: '15%',
      right: '10%',
      top: '10%',
      bottom: '15%'
    },
    xAxis: {
      type: 'category',
      data: memberData.map(m => getUserName(m.userId)),
      axisLabel: {
        width: 60,
        overflow: 'truncate',
        ellipsis: '...'
      }
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: t('delayStats.charts.totalTasks'),
        type: 'bar',
        data: memberData.map(m => m.totalTasks),
        itemStyle: { color: '#3b82f6' }
      },
      {
        name: t('delayStats.charts.delayedTasksCount'),
        type: 'bar',
        data: memberData.map(m => m.delayedTasks),
        itemStyle: { color: '#dc2626' }
      }
    ]
  };

  if (memberData.length === 0) {
    option.series[0].data = [0];
    option.series[1].data = [0];
    option.xAxis.data = ['暂无数据'];
  }

  chart.setOption(option);
  teamMemberDelayCountChartInstance = chart;
};

const handleRefresh = async () => {
  loading.value = true;
  try {
    await Promise.all([
      projectStore.loadProjects(),
      taskStore.loadTasks(),
      userStore.loadUsers()
    ]);
    initCharts();
  } finally {
    loading.value = false;
  }
};

const showMemberDetail = (userId: string) => {
  selectedMemberId.value = userId;
  showMemberDetailModal.value = true;
};

const initCharts = () => {
  setTimeout(() => {
    if (activeTab.value === 'personal') {
      initPersonalDelayDistributionChart();
      initPersonalProjectDelayChart();
    } else {
      initTeamMemberDelayRateChart();
      initTeamMemberDelayCountChart();
    }
  }, 100);
};

watch(activeTab, () => {
  teamMembersPagination.value.currentPage = 1;
  initCharts();
});

// 2026-06-17:切换部门 / 含子部门勾选时,部门过滤后的统计数据已变化,
// 但 echarts 是命令式渲染,需要主动重新调用 init 重绘团队图表。
// <Transition> 会随 key 变化销毁并重建图表容器,所以:
//  1) 不能给 <Transition> 加 mode="out-in",否则 leave 期间 ref 仍指向旧容器,
//     init 会把图表挂到旧容器上,新容器出现后就没有图表了;
//  2) 用 flush: 'post' 让 watch 在 DOM patch 之后再触发,此时新容器已挂载、
//     ref 已更新,dispose 检查能识别出旧实例并清掉,然后 init 到新容器上。
watch([selectedDeptCode, includeSubDepts], async () => {
  teamMembersPagination.value.currentPage = 1;
  if (activeTab.value !== 'team') return;
  await nextTick();
  if (teamMemberDelayRateChartInstance && (!teamMemberDelayRateChartRef.value || teamMemberDelayRateChartInstance.getDom() !== teamMemberDelayRateChartRef.value)) {
    teamMemberDelayRateChartInstance.dispose();
    teamMemberDelayRateChartInstance = null;
  }
  if (teamMemberDelayCountChartInstance && (!teamMemberDelayCountChartRef.value || teamMemberDelayCountChartInstance.getDom() !== teamMemberDelayCountChartRef.value)) {
    teamMemberDelayCountChartInstance.dispose();
    teamMemberDelayCountChartInstance = null;
  }
  initTeamMemberDelayRateChart();
  initTeamMemberDelayCountChart();
}, { flush: 'post' });

onMounted(async () => {
  loading.value = true;
  try {
    await Promise.all([
      projectStore.loadProjects(),
      taskStore.loadTasks(),
      userStore.loadUsers(),
      // 2026-06-17:部门过滤需要 org 树,提前加载
      orgStore.loadTree()
    ]);
    initCharts();
  } finally {
    loading.value = false;
  }
});

onUnmounted(() => {
  if (switchTimer) {
    window.clearTimeout(switchTimer);
    switchTimer = null;
  }
  personalDelayDistributionChartInstance?.dispose();
  personalProjectDelayChartInstance?.dispose();
  teamMemberDelayRateChartInstance?.dispose();
  teamMemberDelayCountChartInstance?.dispose();
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

/* 部门切换时下游内容(团队统计/图表/列表)淡入淡出 */
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
</style>
