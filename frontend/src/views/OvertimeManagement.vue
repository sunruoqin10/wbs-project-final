<template>
  <MainLayout>
    <div class="space-y-6">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-secondary-900">项目加班管理</h1>
          <p class="mt-1 text-sm text-secondary-600">记录和管理项目成员的加班情况</p>
        </div>
        <div class="flex items-center gap-2">
          <Button variant="secondary" @click="handleExportToExcel">
            <svg class="mr-2 h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
            </svg>
            导出Excel
          </Button>
          <Button variant="primary" @click="handleAddOvertime">
            <svg class="mr-2 h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
            </svg>
            新增加班
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
              个人加班信息
            </div>
          </button>
          <button
            v-if="isManagerOrAdmin"
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
              管辖成员加班信息
            </div>
          </button>
        </nav>
      </div>

      <div v-if="activeTab === 'personal'" class="space-y-6">
        <div class="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
          <Card>
            <div class="flex items-center">
              <div class="rounded-lg bg-blue-100 p-3">
                <svg class="h-6 w-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
              <div class="ml-4">
                <p class="text-sm font-medium text-secondary-600">本月加班时长</p>
                <p class="text-2xl font-semibold text-blue-600">{{ personalStats.thisMonthHours }} 小时</p>
              </div>
            </div>
          </Card>

          <Card>
            <div class="flex items-center">
              <div class="rounded-lg bg-green-100 p-3">
                <svg class="h-6 w-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                </svg>
              </div>
              <div class="ml-4">
                <p class="text-sm font-medium text-secondary-600">加班次数</p>
                <p class="text-2xl font-semibold text-green-600">{{ personalStats.totalRecords }} 次</p>
              </div>
            </div>
          </Card>

          <Card>
            <div class="flex items-center">
              <div class="rounded-lg bg-purple-100 p-3">
                <svg class="h-6 w-6 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                </svg>
              </div>
              <div class="ml-4">
                <p class="text-sm font-medium text-secondary-600">调休累计</p>
                <p class="text-2xl font-semibold text-purple-600">{{ personalTotalCompTimeoff }} 小时</p>
              </div>
            </div>
          </Card>

          <Card>
            <div class="flex items-center">
              <div class="rounded-lg bg-red-100 p-3">
                <svg class="h-6 w-6 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
              <div class="ml-4">
                <p class="text-sm font-medium text-secondary-600">加班费累计</p>
                <p class="text-2xl font-semibold text-red-600">{{ personalTotalPayHours }} 小时</p>
              </div>
            </div>
          </Card>
        </div>

        <div class="grid grid-cols-1 gap-6 lg:grid-cols-2">
          <Card>
            <template #header>
              <h3 class="text-lg font-semibold text-secondary-900">加班趋势（最近30天）</h3>
            </template>
            <div class="h-80" ref="personalTrendChartRef"></div>
          </Card>

          <Card>
            <template #header>
              <h3 class="text-lg font-semibold text-secondary-900">项目加班分布</h3>
            </template>
            <div class="h-80" ref="personalDistributionChartRef"></div>
          </Card>
        </div>

        <Card>
          <template #header>
            <div class="flex flex-wrap items-center gap-4">
              <div class="flex items-center gap-2">
                <label class="text-sm text-secondary-600">项目：</label>
                <select v-model="personalFilters.projectId" class="rounded border border-secondary-300 px-3 py-1 text-sm">
                  <option value="">全部项目</option>
                  <option v-for="project in accessibleProjects" :key="project.id" :value="project.id">
                    {{ project.name }}
                  </option>
                </select>
              </div>
              <div class="flex items-center gap-2">
                <label class="text-sm text-secondary-600">状态：</label>
                <select v-model="personalFilters.status" class="rounded border border-secondary-300 px-3 py-1 text-sm">
                  <option value="">全部状态</option>
                  <option value="pending">待审批</option>
                  <option value="approved">已通过</option>
                  <option value="rejected">已拒绝</option>
                </select>
              </div>
              <div class="flex items-center gap-2">
                <label class="text-sm text-secondary-600">类型：</label>
                <select v-model="personalFilters.overtimeType" class="rounded border border-secondary-300 px-3 py-1 text-sm">
                  <option value="">全部类型</option>
                  <option value="weekday">工作日加班</option>
                  <option value="weekend">周末加班</option>
                  <option value="holiday">节假日加班</option>
                </select>
              </div>
              <div class="flex items-center gap-2">
                <label class="text-sm text-secondary-600">日期范围：</label>
                <input
                  v-model="personalFilters.startDate"
                  type="date"
                  class="rounded border border-secondary-300 px-3 py-1 text-sm"
                />
                <span class="text-secondary-500">至</span>
                <input
                  v-model="personalFilters.endDate"
                  type="date"
                  class="rounded border border-secondary-300 px-3 py-1 text-sm"
                />
              </div>
            </div>
          </template>
          <div class="overflow-x-auto">
            <table class="min-w-full divide-y divide-secondary-200">
              <thead class="bg-secondary-50">
                <tr>
                  <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500 cursor-pointer hover:bg-secondary-100" @click="handlePersonalSort('overtimeDate')">
                    日期
                    <span v-if="personalSort.field === 'overtimeDate'" class="ml-1">
                      {{ personalSort.order === 'asc' ? '↑' : '↓' }}
                    </span>
                  </th>
                  <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">项目</th>
                  <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">关联任务</th>
                  <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">时间段</th>
                  <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500 cursor-pointer hover:bg-secondary-100" @click="handlePersonalSort('hours')">
                    时长
                    <span v-if="personalSort.field === 'hours'" class="ml-1">
                      {{ personalSort.order === 'asc' ? '↑' : '↓' }}
                    </span>
                  </th>
                  <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">类型</th>
                  <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">补偿方式</th>
                  <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">加班事由</th>
                  <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500 cursor-pointer hover:bg-secondary-100" @click="handlePersonalSort('status')">
                    状态
                    <span v-if="personalSort.field === 'status'" class="ml-1">
                      {{ personalSort.order === 'asc' ? '↑' : '↓' }}
                    </span>
                  </th>
                  <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">操作</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-secondary-200 bg-white">
                <tr v-if="filteredPersonalRecords.length === 0">
                  <td colspan="10" class="px-4 py-8 text-center text-sm text-secondary-500">
                    暂无加班记录
                  </td>
                </tr>
                <tr v-for="record in filteredPersonalRecords" :key="record.id" class="hover:bg-secondary-50">
                  <td class="whitespace-nowrap px-4 py-3 text-sm text-secondary-600">{{ formatDate(record.overtimeDate) }}</td>
                  <td class="whitespace-nowrap px-4 py-3 text-sm text-secondary-900">{{ getProjectName(record.projectId) }}</td>
                  <td class="px-4 py-3 text-sm text-secondary-600">{{ getTaskName(record.taskId) }}</td>
                  <td class="whitespace-nowrap px-4 py-3 text-sm text-secondary-600">{{ record.startTime }} - {{ record.endTime }}</td>
                  <td class="whitespace-nowrap px-4 py-3 text-sm font-semibold text-secondary-900">{{ record.hours }} 小时</td>
                  <td class="whitespace-nowrap px-4 py-3 text-sm">
                    <span :class="getTypeBadgeClass(record.overtimeType)" class="inline-flex rounded-full px-2 py-1 text-xs font-medium">
                      {{ getTypeLabel(record.overtimeType) }}
                    </span>
                  </td>
                  <td class="whitespace-nowrap px-4 py-3 text-sm text-secondary-600">{{ getCompensationLabel(record.compensationType) }}</td>
                  <td class="px-4 py-3 text-sm text-secondary-900 max-w-xs truncate" :title="record.reason">{{ record.reason }}</td>
                  <td class="whitespace-nowrap px-4 py-3 text-sm">
                    <span :class="getStatusBadgeClass(record.status)" class="inline-flex rounded-full px-2 py-1 text-xs font-medium">
                      {{ getStatusLabel(record.status) }}
                    </span>
                  </td>
                  <td class="whitespace-nowrap px-4 py-3 text-sm">
                    <div class="flex items-center gap-2">
                      <button
                        v-if="canEdit(record)"
                        @click="handleEdit(record)"
                        class="text-secondary-600 hover:text-secondary-800"
                        title="编辑"
                      >
                        编辑
                      </button>
                      <button
                        v-if="canDelete(record)"
                        @click="handleDelete(record)"
                        class="text-danger-600 hover:text-danger-800"
                        title="删除"
                      >
                        删除
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          
          <div v-if="personalPagination.total > 0" class="mt-4 flex items-center justify-between border-t border-secondary-200 pt-4">
            <div class="text-sm text-secondary-600">
              共 {{ personalPagination.total }} 条记录，第 {{ personalPagination.currentPage }} 页
            </div>
            <div class="flex items-center gap-2">
              <select 
                v-model="personalPagination.pageSize" 
                @change="handlePersonalPageSizeChange(personalPagination.pageSize)"
                class="rounded border border-secondary-300 px-2 py-1 text-sm"
              >
                <option :value="10">10条/页</option>
                <option :value="20">20条/页</option>
                <option :value="50">50条/页</option>
                <option :value="100">100条/页</option>
              </select>
              <button
                @click="handlePersonalPageChange(personalPagination.currentPage - 1)"
                :disabled="personalPagination.currentPage === 1"
                class="rounded border border-secondary-300 px-3 py-1 text-sm hover:bg-secondary-50 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                上一页
              </button>
              <button
                v-for="page in getPersonalPageNumbers()"
                :key="page"
                @click="handlePersonalPageChange(page)"
                :class="[
                  'rounded px-3 py-1 text-sm',
                  page === personalPagination.currentPage 
                    ? 'bg-primary-500 text-white' 
                    : 'border border-secondary-300 hover:bg-secondary-50'
                ]"
              >
                {{ page }}
              </button>
              <button
                @click="handlePersonalPageChange(personalPagination.currentPage + 1)"
                :disabled="personalPagination.currentPage === personalTotalPages"
                class="rounded border border-secondary-300 px-3 py-1 text-sm hover:bg-secondary-50 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                下一页
              </button>
            </div>
          </div>
        </Card>
      </div>

      <div v-if="activeTab === 'team'" class="space-y-6">
        <div class="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-5">
          <Card>
            <div class="flex items-center">
              <div class="rounded-lg bg-blue-100 p-3">
                <svg class="h-6 w-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
              <div class="ml-4">
                <p class="text-sm font-medium text-secondary-600">本月加班时长</p>
                <p class="text-2xl font-semibold text-blue-600">{{ teamStats.thisMonthHours }} 小时</p>
              </div>
            </div>
          </Card>

          <Card>
            <div class="flex items-center">
              <div class="rounded-lg bg-green-100 p-3">
                <svg class="h-6 w-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                </svg>
              </div>
              <div class="ml-4">
                <p class="text-sm font-medium text-secondary-600">加班人数</p>
                <p class="text-2xl font-semibold text-green-600">{{ teamStats.thisMonthPeople }} 人</p>
              </div>
            </div>
          </Card>

          <Card>
            <div class="flex items-center">
              <div class="rounded-lg bg-orange-100 p-3">
                <svg class="h-6 w-6 text-orange-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
              <div class="ml-4">
                <p class="text-sm font-medium text-secondary-600">待审批数量</p>
                <p class="text-2xl font-semibold text-orange-600">{{ teamStats.pendingApprovals }}</p>
              </div>
            </div>
          </Card>

          <Card>
            <div class="flex items-center">
              <div class="rounded-lg bg-purple-100 p-3">
                <svg class="h-6 w-6 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                </svg>
              </div>
              <div class="ml-4">
                <p class="text-sm font-medium text-secondary-600">调休累计</p>
                <p class="text-2xl font-semibold text-purple-600">{{ teamTotalCompTimeoff }} 小时</p>
              </div>
            </div>
          </Card>

          <Card>
            <div class="flex items-center">
              <div class="rounded-lg bg-red-100 p-3">
                <svg class="h-6 w-6 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
              <div class="ml-4">
                <p class="text-sm font-medium text-secondary-600">加班费累计</p>
                <p class="text-2xl font-semibold text-red-600">{{ teamTotalPayHours }} 小时</p>
              </div>
            </div>
          </Card>
        </div>

        <div class="grid grid-cols-1 gap-6 lg:grid-cols-2">
          <Card>
            <template #header>
              <h3 class="text-lg font-semibold text-secondary-900">加班趋势（最近30天）</h3>
            </template>
            <div class="h-80" ref="teamTrendChartRef"></div>
          </Card>

          <Card>
            <template #header>
              <h3 class="text-lg font-semibold text-secondary-900">项目加班分布</h3>
            </template>
            <div class="h-80" ref="teamDistributionChartRef"></div>
          </Card>
        </div>

        <Card>
          <template #header>
            <div class="flex flex-wrap items-center gap-4">
              <div class="flex items-center gap-2">
                <label class="text-sm text-secondary-600">项目：</label>
                <select v-model="teamFilters.projectId" class="rounded border border-secondary-300 px-3 py-1 text-sm">
                  <option value="">全部项目</option>
                  <option v-for="project in accessibleProjects" :key="project.id" :value="project.id">
                    {{ project.name }}
                  </option>
                </select>
              </div>
              <div class="flex items-center gap-2">
                <label class="text-sm text-secondary-600">人员：</label>
                <select v-model="teamFilters.userId" class="rounded border border-secondary-300 px-3 py-1 text-sm">
                  <option value="">全部人员</option>
                  <option v-for="user in teamUsers" :key="user.id" :value="user.id">
                    {{ user.name }}
                  </option>
                </select>
              </div>
              <div class="flex items-center gap-2">
                <label class="text-sm text-secondary-600">状态：</label>
                <select v-model="teamFilters.status" class="rounded border border-secondary-300 px-3 py-1 text-sm">
                  <option value="">全部状态</option>
                  <option value="pending">待审批</option>
                  <option value="approved">已通过</option>
                  <option value="rejected">已拒绝</option>
                </select>
              </div>
              <div class="flex items-center gap-2">
                <label class="text-sm text-secondary-600">类型：</label>
                <select v-model="teamFilters.overtimeType" class="rounded border border-secondary-300 px-3 py-1 text-sm">
                  <option value="">全部类型</option>
                  <option value="weekday">工作日加班</option>
                  <option value="weekend">周末加班</option>
                  <option value="holiday">节假日加班</option>
                </select>
              </div>
              <div class="flex items-center gap-2">
                <label class="text-sm text-secondary-600">日期范围：</label>
                <input
                  v-model="teamFilters.startDate"
                  type="date"
                  class="rounded border border-secondary-300 px-3 py-1 text-sm"
                />
                <span class="text-secondary-500">至</span>
                <input
                  v-model="teamFilters.endDate"
                  type="date"
                  class="rounded border border-secondary-300 px-3 py-1 text-sm"
                />
              </div>
            </div>
          </template>
          <div class="overflow-x-auto">
            <table class="min-w-full divide-y divide-secondary-200">
              <thead class="bg-secondary-50">
                <tr>
                  <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500 cursor-pointer hover:bg-secondary-100" @click="handleTeamSort('userId')">
                    人员
                    <span v-if="teamSort.field === 'userId'" class="ml-1">
                      {{ teamSort.order === 'asc' ? '↑' : '↓' }}
                    </span>
                  </th>
                  <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500 cursor-pointer hover:bg-secondary-100" @click="handleTeamSort('overtimeDate')">
                    日期
                    <span v-if="teamSort.field === 'overtimeDate'" class="ml-1">
                      {{ teamSort.order === 'asc' ? '↑' : '↓' }}
                    </span>
                  </th>
                  <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">项目</th>
                  <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">项目负责人</th>
                  <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">关联任务</th>
                  <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">时间段</th>
                  <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500 cursor-pointer hover:bg-secondary-100" @click="handleTeamSort('hours')">
                    时长
                    <span v-if="teamSort.field === 'hours'" class="ml-1">
                      {{ teamSort.order === 'asc' ? '↑' : '↓' }}
                    </span>
                  </th>
                  <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">类型</th>
                  <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">补偿方式</th>
                  <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500 cursor-pointer hover:bg-secondary-100" @click="handleTeamSort('status')">
                    状态
                    <span v-if="teamSort.field === 'status'" class="ml-1">
                      {{ teamSort.order === 'asc' ? '↑' : '↓' }}
                    </span>
                  </th>
                  <th class="px-4 py-3 text-left text-xs font-medium uppercase text-secondary-500">操作</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-secondary-200 bg-white">
                <tr v-if="filteredTeamRecords.length === 0">
                  <td colspan="11" class="px-4 py-8 text-center text-sm text-secondary-500">
                    暂无加班记录
                  </td>
                </tr>
                <tr v-for="record in filteredTeamRecords" :key="record.id" class="hover:bg-secondary-50">
                  <td class="whitespace-nowrap px-4 py-3 text-sm text-secondary-900">
                    <div class="flex items-center">
                      <img
                        :src="getUserAvatar(record.userId)"
                        :alt="getUserName(record.userId)"
                        class="mr-2 h-8 w-8 rounded-full"
                      />
                      {{ getUserName(record.userId) }}
                    </div>
                  </td>
                  <td class="whitespace-nowrap px-4 py-3 text-sm text-secondary-600">{{ formatDate(record.overtimeDate) }}</td>
                  <td class="whitespace-nowrap px-4 py-3 text-sm text-secondary-900">{{ getProjectName(record.projectId) }}</td>
                  <td class="whitespace-nowrap px-4 py-3 text-sm text-secondary-600">{{ getProjectOwner(record.projectId) }}</td>
                  <td class="px-4 py-3 text-sm text-secondary-600">{{ getTaskName(record.taskId) }}</td>
                  <td class="whitespace-nowrap px-4 py-3 text-sm text-secondary-600">{{ record.startTime }} - {{ record.endTime }}</td>
                  <td class="whitespace-nowrap px-4 py-3 text-sm font-semibold text-secondary-900">{{ record.hours }} 小时</td>
                  <td class="whitespace-nowrap px-4 py-3 text-sm">
                    <span :class="getTypeBadgeClass(record.overtimeType)" class="inline-flex rounded-full px-2 py-1 text-xs font-medium">
                      {{ getTypeLabel(record.overtimeType) }}
                    </span>
                  </td>
                  <td class="whitespace-nowrap px-4 py-3 text-sm text-secondary-600">{{ getCompensationLabel(record.compensationType) }}</td>
                  <td class="whitespace-nowrap px-4 py-3 text-sm">
                    <span :class="getStatusBadgeClass(record.status)" class="inline-flex rounded-full px-2 py-1 text-xs font-medium">
                      {{ getStatusLabel(record.status) }}
                    </span>
                  </td>
                  <td class="whitespace-nowrap px-4 py-3 text-sm">
                    <div class="flex items-center gap-2">
                      <button
                        v-if="isManagerOrAdmin && record.status === 'pending' && canApprove(record.projectId)"
                        @click="handleApprove(record)"
                        class="text-primary-600 hover:text-primary-800"
                        title="审批"
                      >
                        审批
                      </button>
                      <button
                        @click="handleEdit(record)"
                        class="text-secondary-600 hover:text-secondary-800"
                        title="编辑"
                      >
                        编辑
                      </button>
                      <button
                        @click="handleDelete(record)"
                        class="text-danger-600 hover:text-danger-800"
                        title="删除"
                      >
                        删除
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          
          <div v-if="teamPagination.total > 0" class="mt-4 flex items-center justify-between border-t border-secondary-200 pt-4">
            <div class="text-sm text-secondary-600">
              共 {{ teamPagination.total }} 条记录，第 {{ teamPagination.currentPage }} 页
            </div>
            <div class="flex items-center gap-2">
              <select 
                v-model="teamPagination.pageSize" 
                @change="handleTeamPageSizeChange(teamPagination.pageSize)"
                class="rounded border border-secondary-300 px-2 py-1 text-sm"
              >
                <option :value="10">10条/页</option>
                <option :value="20">20条/页</option>
                <option :value="50">50条/页</option>
                <option :value="100">100条/页</option>
              </select>
              <button
                @click="handleTeamPageChange(teamPagination.currentPage - 1)"
                :disabled="teamPagination.currentPage === 1"
                class="rounded border border-secondary-300 px-3 py-1 text-sm hover:bg-secondary-50 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                上一页
              </button>
              <button
                v-for="page in getTeamPageNumbers()"
                :key="page"
                @click="handleTeamPageChange(page)"
                :class="[
                  'rounded px-3 py-1 text-sm',
                  page === teamPagination.currentPage 
                    ? 'bg-primary-500 text-white' 
                    : 'border border-secondary-300 hover:bg-secondary-50'
                ]"
              >
                {{ page }}
              </button>
              <button
                @click="handleTeamPageChange(teamPagination.currentPage + 1)"
                :disabled="teamPagination.currentPage === teamTotalPages"
                class="rounded border border-secondary-300 px-3 py-1 text-sm hover:bg-secondary-50 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                下一页
              </button>
            </div>
          </div>
        </Card>
      </div>
    </div>

    <OvertimeModal
      :open="showOvertimeModal"
      :record="currentRecord"
      @close="handleOvertimeModalClose"
      @save="handleOvertimeSave"
    />

    <ApprovalModal
      :open="showApprovalModal"
      :record="currentRecord"
      @close="handleApprovalModalClose"
      @approve="handleApprovalSubmit"
      @reject="handleRejectSubmit"
    />
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import * as echarts from 'echarts';
import dayjs from 'dayjs';
import MainLayout from '@/components/layout/MainLayout.vue';
import Card from '@/components/common/Card.vue';
import Button from '@/components/common/Button.vue';
import OvertimeModal from '@/components/overtime/OvertimeModal.vue';
import ApprovalModal from '@/components/overtime/ApprovalModal.vue';
import { useOvertimeStore } from '@/stores/overtime';
import { useProjectStore } from '@/stores/project';
import { useUserStore } from '@/stores/user';
import { useTaskStore } from '@/stores/task';
import { usePermissionStore } from '@/stores/permission';
import type { OvertimeRecord } from '@/types';
import { exportToExcel } from '@/utils/export';

const overtimeStore = useOvertimeStore();
const projectStore = useProjectStore();
const userStore = useUserStore();
const taskStore = useTaskStore();
const permissionStore = usePermissionStore();

const activeTab = ref<'personal' | 'team'>('personal');

const personalTrendChartRef = ref<HTMLElement>();
const personalDistributionChartRef = ref<HTMLElement>();
const teamTrendChartRef = ref<HTMLElement>();
const teamDistributionChartRef = ref<HTMLElement>();

let personalTrendChartInstance: echarts.ECharts | null = null;
let personalDistributionChartInstance: echarts.ECharts | null = null;
let teamTrendChartInstance: echarts.ECharts | null = null;
let teamDistributionChartInstance: echarts.ECharts | null = null;

const showOvertimeModal = ref(false);
const showApprovalModal = ref(false);
const currentRecord = ref<OvertimeRecord | null>(null);

const isAdmin = computed(() => permissionStore.currentRole === 'admin');
const isProjectManager = computed(() => permissionStore.currentRole === 'project-manager');
const isProjectOwner = computed(() => {
  const currentUserId = userStore.currentUserId;
  if (!currentUserId) return false;
  return projectStore.projects.some(p => p.ownerId === currentUserId);
});
const isManagerOrAdmin = computed(() => isAdmin.value || isProjectManager.value || isProjectOwner.value);

const personalFilters = ref({
  projectId: '',
  status: '',
  overtimeType: '',
  startDate: '',
  endDate: ''
});

const teamFilters = ref({
  projectId: '',
  userId: '',
  status: '',
  overtimeType: '',
  startDate: '',
  endDate: ''
});

const personalPagination = ref({
  currentPage: 1,
  pageSize: 20,
  total: 0
});

const teamPagination = ref({
  currentPage: 1,
  pageSize: 20,
  total: 0
});

const personalSort = ref({
  field: 'overtimeDate' as keyof OvertimeRecord,
  order: 'desc' as 'asc' | 'desc'
});

const teamSort = ref({
  field: 'overtimeDate' as keyof OvertimeRecord,
  order: 'desc' as 'asc' | 'desc'
});

const personalRecords = computed(() => {
  const currentUserId = userStore.currentUserId;
  if (!currentUserId) return [];
  return overtimeStore.overtimeRecords.filter(r => r.userId === currentUserId);
});

const managedRecords = computed(() => {
  const currentUserId = userStore.currentUserId;
  if (!currentUserId) return [];
  
  if (isAdmin.value || isProjectManager.value) {
    return overtimeStore.overtimeRecords;
  }
  
  if (isProjectOwner.value) {
    const managedProjectIds = getManagedProjectIds();
    return overtimeStore.overtimeRecords.filter(r => managedProjectIds.includes(r.projectId));
  }
  
  return [];
});

const teamUsers = computed(() => {
  const userIds = new Set(managedRecords.value.map(r => r.userId));
  return userStore.users.filter(u => userIds.has(u.id));
});

const personalApprovedRecords = computed(() => personalRecords.value.filter(r => r.status === 'approved'));
const teamApprovedRecords = computed(() => managedRecords.value.filter(r => r.status === 'approved'));

const personalTotalCompTimeoff = computed(() => {
  return personalApprovedRecords.value
    .filter(r => r.compensationType === 'timeoff')
    .reduce((sum, r) => sum + r.hours, 0);
});

const personalTotalPayHours = computed(() => {
  return personalApprovedRecords.value
    .filter(r => r.compensationType === 'pay')
    .reduce((sum, r) => sum + r.hours, 0);
});

const teamTotalCompTimeoff = computed(() => {
  return teamApprovedRecords.value
    .filter(r => r.compensationType === 'timeoff')
    .reduce((sum, r) => sum + r.hours, 0);
});

const teamTotalPayHours = computed(() => {
  return teamApprovedRecords.value
    .filter(r => r.compensationType === 'pay')
    .reduce((sum, r) => sum + r.hours, 0);
});

const personalStats = computed(() => {
  const records = personalRecords.value;
  const thisMonth = dayjs().format('YYYY-MM');
  const thisMonthRecords = records.filter(r => dayjs(r.overtimeDate).format('YYYY-MM') === thisMonth);
  
  const projectStats = new Map<string, { projectId: string; projectName: string; hours: number; count: number }>();
  records.forEach(r => {
    const project = projectStore.projectById(r.projectId);
    if (project) {
      const existing = projectStats.get(project.id);
      if (existing) {
        existing.hours += r.hours;
        existing.count += 1;
      } else {
        projectStats.set(project.id, {
          projectId: project.id,
          projectName: project.name,
          hours: r.hours,
          count: 1
        });
      }
    }
  });
  
  return {
    totalRecords: records.length,
    totalHours: records.reduce((sum, r) => sum + r.hours, 0),
    totalPeople: 1,
    pendingApprovals: records.filter(r => r.status === 'pending').length,
    thisMonthHours: thisMonthRecords.reduce((sum, r) => sum + r.hours, 0),
    thisMonthPeople: thisMonthRecords.length > 0 ? 1 : 0,
    byType: {
      weekday: records.filter(r => r.overtimeType === 'weekday').reduce((sum, r) => sum + r.hours, 0),
      weekend: records.filter(r => r.overtimeType === 'weekend').reduce((sum, r) => sum + r.hours, 0),
      holiday: records.filter(r => r.overtimeType === 'holiday').reduce((sum, r) => sum + r.hours, 0)
    },
    byProject: Array.from(projectStats.values())
  };
});

const teamStats = computed(() => {
  const records = managedRecords.value;
  const thisMonth = dayjs().format('YYYY-MM');
  const thisMonthRecords = records.filter(r => dayjs(r.overtimeDate).format('YYYY-MM') === thisMonth);
  
  const projectStats = new Map<string, { projectId: string; projectName: string; hours: number; count: number }>();
  const userStats = new Set<string>();
  
  records.forEach(r => {
    const project = projectStore.projectById(r.projectId);
    if (project) {
      const existing = projectStats.get(project.id);
      if (existing) {
        existing.hours += r.hours;
        existing.count += 1;
      } else {
        projectStats.set(project.id, {
          projectId: project.id,
          projectName: project.name,
          hours: r.hours,
          count: 1
        });
      }
    }
    userStats.add(r.userId);
  });
  
  return {
    totalRecords: records.length,
    totalHours: records.reduce((sum, r) => sum + r.hours, 0),
    totalPeople: userStats.size,
    pendingApprovals: records.filter(r => r.status === 'pending').length,
    thisMonthHours: thisMonthRecords.reduce((sum, r) => sum + r.hours, 0),
    thisMonthPeople: new Set(thisMonthRecords.map(r => r.userId)).size,
    byType: {
      weekday: records.filter(r => r.overtimeType === 'weekday').reduce((sum, r) => sum + r.hours, 0),
      weekend: records.filter(r => r.overtimeType === 'weekend').reduce((sum, r) => sum + r.hours, 0),
      holiday: records.filter(r => r.overtimeType === 'holiday').reduce((sum, r) => sum + r.hours, 0)
    },
    byProject: Array.from(projectStats.values())
  };
});

const filteredPersonalRecords = computed(() => {
  let result = [...personalRecords.value];

  if (personalFilters.value.projectId) {
    result = result.filter(r => r.projectId === personalFilters.value.projectId);
  }

  if (personalFilters.value.status) {
    result = result.filter(r => r.status === personalFilters.value.status);
  }

  if (personalFilters.value.overtimeType) {
    result = result.filter(r => r.overtimeType === personalFilters.value.overtimeType);
  }

  if (personalFilters.value.startDate) {
    result = result.filter(r => r.overtimeDate >= personalFilters.value.startDate);
  }

  if (personalFilters.value.endDate) {
    result = result.filter(r => r.overtimeDate <= personalFilters.value.endDate);
  }

  result.sort((a, b) => {
    const aValue = a[personalSort.value.field];
    const bValue = b[personalSort.value.field];
    
    if (aValue === bValue) return 0;
    
    let comparison = 0;
    if (typeof aValue === 'string' && typeof bValue === 'string') {
      comparison = aValue.localeCompare(bValue);
    } else {
      comparison = aValue < bValue ? -1 : 1;
    }
    
    return personalSort.value.order === 'asc' ? comparison : -comparison;
  });

  personalPagination.value.total = result.length;

  const start = (personalPagination.value.currentPage - 1) * personalPagination.value.pageSize;
  const end = start + personalPagination.value.pageSize;
  
  return result.slice(start, end);
});

const filteredTeamRecords = computed(() => {
  let result = [...managedRecords.value];

  if (teamFilters.value.projectId) {
    result = result.filter(r => r.projectId === teamFilters.value.projectId);
  }

  if (teamFilters.value.userId) {
    result = result.filter(r => r.userId === teamFilters.value.userId);
  }

  if (teamFilters.value.status) {
    result = result.filter(r => r.status === teamFilters.value.status);
  }

  if (teamFilters.value.overtimeType) {
    result = result.filter(r => r.overtimeType === teamFilters.value.overtimeType);
  }

  if (teamFilters.value.startDate) {
    result = result.filter(r => r.overtimeDate >= teamFilters.value.startDate);
  }

  if (teamFilters.value.endDate) {
    result = result.filter(r => r.overtimeDate <= teamFilters.value.endDate);
  }

  result.sort((a, b) => {
    const aValue = a[teamSort.value.field];
    const bValue = b[teamSort.value.field];
    
    if (aValue === bValue) return 0;
    
    let comparison = 0;
    if (typeof aValue === 'string' && typeof bValue === 'string') {
      comparison = aValue.localeCompare(bValue);
    } else {
      comparison = aValue < bValue ? -1 : 1;
    }
    
    return teamSort.value.order === 'asc' ? comparison : -comparison;
  });

  teamPagination.value.total = result.length;

  const start = (teamPagination.value.currentPage - 1) * teamPagination.value.pageSize;
  const end = start + teamPagination.value.pageSize;
  
  return result.slice(start, end);
});

const getManagedProjectIds = (): string[] => {
  const currentUserId = userStore.currentUserId;
  if (!currentUserId) return [];
  
  if (isAdmin.value || isProjectManager.value) {
    return projectStore.projects.map(p => p.id);
  }
  
  return projectStore.projects
    .filter(project => project.ownerId === currentUserId)
    .map(p => p.id);
};

const getAccessibleProjectIds = (): string[] => {
  const currentUserId = userStore.currentUserId;
  if (!currentUserId) return [];
  
  if (isAdmin.value) {
    return projectStore.projects.map(p => p.id);
  }
  
  return projectStore.projects.filter(project => {
    const isOwner = project.ownerId === currentUserId;
    const isMember = project.memberIds?.includes(currentUserId) || false;
    return isOwner || isMember;
  }).map(p => p.id);
};

const accessibleProjects = computed(() => {
  const currentUserId = userStore.currentUserId;
  if (!currentUserId) return [];
  
  if (isAdmin.value) {
    return projectStore.projects;
  }
  
  return projectStore.projects.filter(project => {
    const isOwner = project.ownerId === currentUserId;
    const isMember = project.memberIds?.includes(currentUserId) || false;
    return isOwner || isMember;
  });
});

const getUserName = (userId: string) => {
  const user = userStore.userById(userId);
  return user?.name || '未知';
};

const getUserAvatar = (userId: string) => {
  const user = userStore.userById(userId);
  return user?.avatar || 'https://ui-avatars.com/api/?name=User&background=6366f1&color=fff';
};

const getProjectName = (projectId: string) => {
  const project = projectStore.projectById(projectId);
  return project?.name || '未知项目';
};

const getProjectOwner = (projectId: string) => {
  const project = projectStore.projectById(projectId);
  if (!project?.ownerId) return '未设置';
  const owner = userStore.userById(project.ownerId);
  return owner?.name || '未知';
};

const getTaskName = (taskId?: string) => {
  if (!taskId) return '-';
  const task = taskStore.getTaskById(taskId);
  return task?.title || '未知任务';
};

const formatDate = (dateStr: string) => dayjs(dateStr).format('YYYY-MM-DD');

const getTypeLabel = (type: string) => {
  const labels: Record<string, string> = {
    weekday: '工作日',
    weekend: '周末',
    holiday: '节假日'
  };
  return labels[type] || type;
};

const getTypeBadgeClass = (type: string) => {
  const classes: Record<string, string> = {
    weekday: 'bg-blue-100 text-blue-800',
    weekend: 'bg-green-100 text-green-800',
    holiday: 'bg-purple-100 text-purple-800'
  };
  return classes[type] || 'bg-secondary-100 text-secondary-800';
};

const getCompensationLabel = (type?: string) => {
  if (!type) return '-';
  const labels: Record<string, string> = {
    pay: '加班费',
    timeoff: '调休'
  };
  return labels[type] || type;
};

const getStatusLabel = (status: string) => {
  const labels: Record<string, string> = {
    pending: '待审批',
    approved: '已通过',
    rejected: '已拒绝'
  };
  return labels[status] || status;
};

const getStatusBadgeClass = (status: string) => {
  const classes: Record<string, string> = {
    pending: 'bg-orange-100 text-orange-800',
    approved: 'bg-green-100 text-green-800',
    rejected: 'bg-red-100 text-red-800'
  };
  return classes[status] || 'bg-secondary-100 text-secondary-800';
};

const canApprove = (projectId: string) => {
  const project = projectStore.projectById(projectId);
  if (!project) return false;
  
  const currentUserId = userStore.currentUserId;
  if (!currentUserId) return false;
  
  if (isAdmin.value || isProjectManager.value) return true;
  
  return project.ownerId === currentUserId;
};

const canEdit = (record: OvertimeRecord) => {
  if (record.status === 'approved') return false;
  return record.userId === userStore.currentUserId;
};

const canDelete = (record: OvertimeRecord) => {
  if (record.status === 'approved') return false;
  return record.userId === userStore.currentUserId;
};

const personalTotalPages = computed(() => Math.ceil(personalPagination.value.total / personalPagination.value.pageSize));
const teamTotalPages = computed(() => Math.ceil(teamPagination.value.total / teamPagination.value.pageSize));

const getPersonalPageNumbers = () => {
  const pages: number[] = [];
  const maxVisiblePages = 5;
  const currentPage = personalPagination.value.currentPage;
  const total = personalTotalPages.value;

  if (total <= maxVisiblePages) {
    for (let i = 1; i <= total; i++) pages.push(i);
  } else {
    if (currentPage <= 3) {
      for (let i = 1; i <= maxVisiblePages; i++) pages.push(i);
    } else if (currentPage >= total - 2) {
      for (let i = total - maxVisiblePages + 1; i <= total; i++) pages.push(i);
    } else {
      for (let i = currentPage - 2; i <= currentPage + 2; i++) pages.push(i);
    }
  }
  return pages;
};

const getTeamPageNumbers = () => {
  const pages: number[] = [];
  const maxVisiblePages = 5;
  const currentPage = teamPagination.value.currentPage;
  const total = teamTotalPages.value;

  if (total <= maxVisiblePages) {
    for (let i = 1; i <= total; i++) pages.push(i);
  } else {
    if (currentPage <= 3) {
      for (let i = 1; i <= maxVisiblePages; i++) pages.push(i);
    } else if (currentPage >= total - 2) {
      for (let i = total - maxVisiblePages + 1; i <= total; i++) pages.push(i);
    } else {
      for (let i = currentPage - 2; i <= currentPage + 2; i++) pages.push(i);
    }
  }
  return pages;
};

const initPersonalTrendChart = () => {
  if (!personalTrendChartRef.value) return;
  
  if (personalTrendChartInstance) {
    personalTrendChartInstance.dispose();
  }
  
  personalTrendChartInstance = echarts.init(personalTrendChartRef.value);

  const dates: string[] = [];
  const hours: number[] = [];
  for (let i = 29; i >= 0; i--) {
    const date = dayjs().subtract(i, 'day');
    dates.push(date.format('MM-DD'));
    
    const dateStr = date.format('YYYY-MM-DD');
    const dayHours = personalRecords.value
      .filter(r => r.overtimeDate === dateStr && r.status === 'approved')
      .reduce((sum, r) => sum + r.hours, 0);
    hours.push(dayHours);
  }

  const option = {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: dates, axisLabel: { interval: 4, rotate: 45 } },
    yAxis: { type: 'value', name: '小时', minInterval: 1 },
    series: [{
      name: '加班时长',
      type: 'line',
      data: hours,
      smooth: true,
      lineStyle: { color: '#3b82f6', width: 2 },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(59, 130, 246, 0.3)' },
          { offset: 1, color: 'rgba(59, 130, 246, 0.05)' }
        ])
      },
      itemStyle: { color: '#3b82f6' }
    }]
  };

  personalTrendChartInstance.setOption(option);
};

const initPersonalDistributionChart = () => {
  if (!personalDistributionChartRef.value) return;
  
  if (personalDistributionChartInstance) {
    personalDistributionChartInstance.dispose();
  }
  
  personalDistributionChartInstance = echarts.init(personalDistributionChartRef.value);

  const projectData = (personalStats.value.byProject || [])
    .map((item: any) => ({
      projectId: item.projectId,
      projectName: item.projectName,
      hours: Number(item.hours || 0),
      count: Number(item.count || 0)
    }))
    .filter((item: any) => item.hours > 0)
    .sort((a: any, b: any) => b.hours - a.hours)
    .slice(0, 10);

  if (projectData.length === 0) {
    personalDistributionChartInstance.setOption({
      title: { text: '暂无数据', left: 'center', top: 'center', textStyle: { color: '#999', fontSize: 14 } }
    });
    return;
  }

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: any) => {
        const param = params[0];
        const data = projectData[param.dataIndex];
        return `${data.projectName}<br/>加班时长: ${data.hours} 小时<br/>加班次数: ${data.count} 次`;
      }
    },
    grid: { left: '20%', right: '10%', top: '10%', bottom: '10%' },
    xAxis: { type: 'value', name: '小时', nameLocation: 'middle', nameGap: 30 },
    yAxis: { type: 'category', data: projectData.map(item => item.projectName), inverse: true, axisLabel: { width: 100, overflow: 'truncate', ellipsis: '...' } },
    series: [{
      name: '加班时长',
      type: 'bar',
      data: projectData.map(item => item.hours),
      itemStyle: {
        color: (params: any) => {
          const colors = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#06b6d4', '#84cc16', '#f97316', '#6366f1'];
          return colors[params.dataIndex % colors.length];
        }
      },
      label: { show: true, position: 'right', formatter: '{c} 小时' }
    }]
  };

  personalDistributionChartInstance.setOption(option);
};

const initTeamTrendChart = () => {
  if (!teamTrendChartRef.value) return;
  
  if (teamTrendChartInstance) {
    teamTrendChartInstance.dispose();
  }
  
  teamTrendChartInstance = echarts.init(teamTrendChartRef.value);

  const dates: string[] = [];
  const hours: number[] = [];
  for (let i = 29; i >= 0; i--) {
    const date = dayjs().subtract(i, 'day');
    dates.push(date.format('MM-DD'));
    
    const dateStr = date.format('YYYY-MM-DD');
    const dayHours = managedRecords.value
      .filter(r => r.overtimeDate === dateStr && r.status === 'approved')
      .reduce((sum, r) => sum + r.hours, 0);
    hours.push(dayHours);
  }

  const option = {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: dates, axisLabel: { interval: 4, rotate: 45 } },
    yAxis: { type: 'value', name: '小时', minInterval: 1 },
    series: [{
      name: '加班时长',
      type: 'line',
      data: hours,
      smooth: true,
      lineStyle: { color: '#3b82f6', width: 2 },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(59, 130, 246, 0.3)' },
          { offset: 1, color: 'rgba(59, 130, 246, 0.05)' }
        ])
      },
      itemStyle: { color: '#3b82f6' }
    }]
  };

  teamTrendChartInstance.setOption(option);
};

const initTeamDistributionChart = () => {
  if (!teamDistributionChartRef.value) return;
  
  if (teamDistributionChartInstance) {
    teamDistributionChartInstance.dispose();
  }
  
  teamDistributionChartInstance = echarts.init(teamDistributionChartRef.value);

  const projectData = (teamStats.value.byProject || [])
    .map((item: any) => ({
      projectId: item.projectId,
      projectName: item.projectName,
      hours: Number(item.hours || 0),
      count: Number(item.count || 0)
    }))
    .filter((item: any) => item.hours > 0)
    .sort((a: any, b: any) => b.hours - a.hours)
    .slice(0, 10);

  if (projectData.length === 0) {
    teamDistributionChartInstance.setOption({
      title: { text: '暂无数据', left: 'center', top: 'center', textStyle: { color: '#999', fontSize: 14 } }
    });
    return;
  }

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: any) => {
        const param = params[0];
        const data = projectData[param.dataIndex];
        return `${data.projectName}<br/>加班时长: ${data.hours} 小时<br/>加班次数: ${data.count} 次`;
      }
    },
    grid: { left: '20%', right: '10%', top: '10%', bottom: '10%' },
    xAxis: { type: 'value', name: '小时', nameLocation: 'middle', nameGap: 30 },
    yAxis: { type: 'category', data: projectData.map(item => item.projectName), inverse: true, axisLabel: { width: 100, overflow: 'truncate', ellipsis: '...' } },
    series: [{
      name: '加班时长',
      type: 'bar',
      data: projectData.map(item => item.hours),
      itemStyle: {
        color: (params: any) => {
          const colors = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#06b6d4', '#84cc16', '#f97316', '#6366f1'];
          return colors[params.dataIndex % colors.length];
        }
      },
      label: { show: true, position: 'right', formatter: '{c} 小时' }
    }]
  };

  teamDistributionChartInstance.setOption(option);
};

const handleAddOvertime = () => {
  currentRecord.value = null;
  showOvertimeModal.value = true;
};

const handleExportToExcel = () => {
  const accessibleProjectIds = getAccessibleProjectIds();
  let exportData = overtimeStore.overtimeRecords.filter(r => accessibleProjectIds.includes(r.projectId));

  if (!isManagerOrAdmin.value && userStore.currentUserId) {
    exportData = exportData.filter(r => r.userId === userStore.currentUserId);
  }

  if (activeTab.value === 'personal' && personalFilters.value.projectId) {
    exportData = exportData.filter(r => r.projectId === personalFilters.value.projectId);
  }

  if (activeTab.value === 'team' && teamFilters.value.projectId) {
    exportData = exportData.filter(r => r.projectId === teamFilters.value.projectId);
  }

  if (activeTab.value === 'team' && teamFilters.value.userId && isManagerOrAdmin.value) {
    exportData = exportData.filter(r => r.userId === teamFilters.value.userId);
  }

  const columns = [
    { key: 'userName', label: '人员', width: 12 },
    { key: 'projectName', label: '项目', width: 20 },
    { key: 'projectOwner', label: '项目负责人', width: 12 },
    { key: 'taskName', label: '关联任务', width: 20 },
    { key: 'overtimeDate', label: '日期', width: 12 },
    { key: 'timeRange', label: '时间段', width: 15 },
    { key: 'hours', label: '时长', width: 10 },
    { key: 'overtimeType', label: '类型', width: 12 },
    { key: 'compensationType', label: '补偿方式', width: 10 },
    { key: 'reason', label: '加班事由', width: 30 },
    { key: 'status', label: '状态', width: 10 }
  ];

  const formattedData = exportData.map(record => ({
    userName: getUserName(record.userId),
    projectName: getProjectName(record.projectId),
    projectOwner: getProjectOwner(record.projectId),
    taskName: getTaskName(record.taskId),
    overtimeDate: formatDate(record.overtimeDate),
    timeRange: `${record.startTime} - ${record.endTime}`,
    hours: record.hours,
    overtimeType: getTypeLabel(record.overtimeType),
    compensationType: getCompensationLabel(record.compensationType),
    reason: record.reason || '',
    status: getStatusLabel(record.status)
  }));

  exportToExcel(formattedData, columns, '加班记录');
};

const handlePersonalSort = (field: keyof OvertimeRecord) => {
  if (personalSort.value.field === field) {
    personalSort.value.order = personalSort.value.order === 'asc' ? 'desc' : 'asc';
  } else {
    personalSort.value.field = field;
    personalSort.value.order = 'asc';
  }
};

const handleTeamSort = (field: keyof OvertimeRecord) => {
  if (teamSort.value.field === field) {
    teamSort.value.order = teamSort.value.order === 'asc' ? 'desc' : 'asc';
  } else {
    teamSort.value.field = field;
    teamSort.value.order = 'asc';
  }
};

const handlePersonalPageChange = (page: number) => {
  personalPagination.value.currentPage = page;
};

const handleTeamPageChange = (page: number) => {
  teamPagination.value.currentPage = page;
};

const handlePersonalPageSizeChange = (size: number) => {
  personalPagination.value.pageSize = size;
  personalPagination.value.currentPage = 1;
};

const handleTeamPageSizeChange = (size: number) => {
  teamPagination.value.pageSize = size;
  teamPagination.value.currentPage = 1;
};

const handleEdit = (record: OvertimeRecord) => {
  if (record.status === 'approved') {
    alert('已审批通过的加班记录不能编辑');
    return;
  }
  currentRecord.value = record;
  showOvertimeModal.value = true;
};

const handleDelete = async (record: OvertimeRecord) => {
  if (record.status === 'approved') {
    alert('已审批通过的加班记录不能删除');
    return;
  }
  
  if (!confirm('确定要删除这条加班记录吗？')) return;

  try {
    await overtimeStore.deleteOvertimeRecord(record.id);
    await Promise.all([
      overtimeStore.loadOvertimeRecords(),
      overtimeStore.loadStats()
    ]);
    setTimeout(() => {
      if (activeTab.value === 'personal') {
        initPersonalTrendChart();
        initPersonalDistributionChart();
      } else {
        initTeamTrendChart();
        initTeamDistributionChart();
      }
    }, 100);
  } catch (error) {
    console.error('Failed to delete overtime record:', error);
    alert('删除失败，请重试');
  }
};

const handleApprove = (record: OvertimeRecord) => {
  currentRecord.value = record;
  showApprovalModal.value = true;
};

const handleOvertimeModalClose = () => {
  showOvertimeModal.value = false;
  currentRecord.value = null;
};

const handleOvertimeSave = async (data: Partial<OvertimeRecord>) => {
  try {
    if (currentRecord.value) {
      await overtimeStore.updateOvertimeRecord(currentRecord.value.id, data);
    } else {
      await overtimeStore.createOvertimeRecord(data);
    }
    showOvertimeModal.value = false;
    currentRecord.value = null;
    await Promise.all([
      overtimeStore.loadOvertimeRecords(),
      overtimeStore.loadStats()
    ]);
    setTimeout(() => {
      if (activeTab.value === 'personal') {
        initPersonalTrendChart();
        initPersonalDistributionChart();
      } else {
        initTeamTrendChart();
        initTeamDistributionChart();
      }
    }, 100);
  } catch (error) {
    console.error('Failed to save overtime record:', error);
    alert('保存失败，请重试');
  }
};

const handleApprovalModalClose = () => {
  showApprovalModal.value = false;
  currentRecord.value = null;
};

const handleApprovalSubmit = async (recordId: string) => {
  try {
    const approverId = userStore.currentUserId;
    if (!approverId) {
      alert('无法获取当前用户信息');
      return;
    }
    await overtimeStore.approveOvertimeRecord(recordId, approverId);
    showApprovalModal.value = false;
    currentRecord.value = null;
    await Promise.all([
      overtimeStore.loadOvertimeRecords(),
      overtimeStore.loadStats()
    ]);
    setTimeout(() => {
      if (activeTab.value === 'personal') {
        initPersonalTrendChart();
        initPersonalDistributionChart();
      } else {
        initTeamTrendChart();
        initTeamDistributionChart();
      }
    }, 100);
  } catch (error) {
    console.error('Failed to approve overtime record:', error);
    alert('审批失败，请重试');
  }
};

const handleRejectSubmit = async (recordId: string, rejectReason: string) => {
  try {
    const approverId = userStore.currentUserId;
    if (!approverId) {
      alert('无法获取当前用户信息');
      return;
    }
    await overtimeStore.rejectOvertimeRecord(recordId, approverId, rejectReason);
    showApprovalModal.value = false;
    currentRecord.value = null;
    await Promise.all([
      overtimeStore.loadOvertimeRecords(),
      overtimeStore.loadStats()
    ]);
    setTimeout(() => {
      if (activeTab.value === 'personal') {
        initPersonalTrendChart();
        initPersonalDistributionChart();
      } else {
        initTeamTrendChart();
        initTeamDistributionChart();
      }
    }, 100);
  } catch (error) {
    console.error('Failed to reject overtime record:', error);
    alert('拒绝失败，请重试');
  }
};

onMounted(async () => {
  await Promise.all([
    projectStore.loadProjects(),
    userStore.loadUsers(),
    taskStore.loadTasks(),
    overtimeStore.loadOvertimeRecords(),
    overtimeStore.loadStats()
  ]);

  setTimeout(() => {
    initPersonalTrendChart();
    initPersonalDistributionChart();
  }, 100);
});

watch(() => activeTab.value, (newTab) => {
  setTimeout(() => {
    if (newTab === 'personal') {
      initPersonalTrendChart();
      initPersonalDistributionChart();
    } else {
      initTeamTrendChart();
      initTeamDistributionChart();
    }
  }, 100);
});

watch(() => personalFilters.value, () => {
  personalPagination.value.currentPage = 1;
}, { deep: true });

watch(() => teamFilters.value, () => {
  teamPagination.value.currentPage = 1;
}, { deep: true });

watch(() => personalSort.value, () => {
  personalPagination.value.currentPage = 1;
}, { deep: true });

watch(() => teamSort.value, () => {
  teamPagination.value.currentPage = 1;
}, { deep: true });

watch(() => overtimeStore.overtimeRecords, () => {
  setTimeout(() => {
    if (activeTab.value === 'personal') {
      initPersonalTrendChart();
      initPersonalDistributionChart();
    } else {
      initTeamTrendChart();
      initTeamDistributionChart();
    }
  }, 100);
}, { deep: true });

onUnmounted(() => {
  if (personalTrendChartInstance) {
    personalTrendChartInstance.dispose();
    personalTrendChartInstance = null;
  }
  if (personalDistributionChartInstance) {
    personalDistributionChartInstance.dispose();
    personalDistributionChartInstance = null;
  }
  if (teamTrendChartInstance) {
    teamTrendChartInstance.dispose();
    teamTrendChartInstance = null;
  }
  if (teamDistributionChartInstance) {
    teamDistributionChartInstance.dispose();
    teamDistributionChartInstance = null;
  }
});
</script>