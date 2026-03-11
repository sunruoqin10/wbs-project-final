<template>
  <MainLayout>
    <div class="max-w-4xl mx-auto space-y-6">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-4">
          <Button variant="ghost" @click="goBack">
            <svg class="mr-2 h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
            </svg>
            {{ $t('common.back') }}
          </Button>
          <h1 class="text-2xl font-bold text-secondary-900">
            {{ isEditing ? $t('weeklyReports.form.editTitle') : $t('weeklyReports.form.createTitle') }}
          </h1>
        </div>
      </div>

      <Card class="p-6">
        <form @submit.prevent="handleSubmit" class="space-y-6">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label class="mb-2 block text-sm font-medium text-secondary-700">
                {{ $t('weeklyReports.form.project') }} *
              </label>
              <select
                v-model="formData.projectId"
                :disabled="isEditing"
                :class="[
                  'w-full rounded-lg border px-3 py-2 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20 transition-colors',
                  isEditing
                    ? 'bg-secondary-50 text-secondary-600 cursor-not-allowed opacity-60 border-secondary-200'
                    : 'border-secondary-200 bg-white'
                ]"
                required
              >
                <option value="">{{ $t('weeklyReports.form.selectProject') }}</option>
                <option v-for="project in availableProjects" :key="project.id" :value="project.id">
                  {{ project.name }}
                </option>
              </select>
            </div>

            <div>
              <label class="mb-2 block text-sm font-medium text-secondary-700">
                {{ $t('weeklyReports.form.week') }} *
              </label>
              <div class="flex gap-2">
                <input
                  v-model="formData.weekStart"
                  type="date"
                  :disabled="isEditing"
                  :class="[
                    'w-full rounded-lg border px-3 py-2 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20',
                    isEditing
                      ? 'bg-secondary-50 text-secondary-600 cursor-not-allowed opacity-60 border-secondary-200'
                      : 'border-secondary-200'
                  ]"
                  required
                  @change="updateWeekEnd"
                />
                <span class="flex items-center text-secondary-500">-</span>
                <input
                  v-model="formData.weekEnd"
                  type="date"
                  :disabled="isEditing"
                  :class="[
                    'w-full rounded-lg border px-3 py-2 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20',
                    isEditing
                      ? 'bg-secondary-50 text-secondary-600 cursor-not-allowed opacity-60 border-secondary-200'
                      : 'border-secondary-200'
                  ]"
                  required
                />
              </div>
              <p class="mt-1 text-xs text-secondary-500">{{ $t('weeklyReports.form.weekHint') }}</p>
            </div>
          </div>

          <div v-if="isEditing" class="rounded-lg bg-info-50 border border-info-200 px-3 py-2">
            <div class="flex gap-2 items-start">
              <svg class="h-4 w-4 text-info-600 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <div class="text-xs text-info-800 leading-relaxed">
                {{ $t('weeklyReports.form.editHint') }}
              </div>
            </div>
          </div>

          <div>
            <label class="mb-2 block text-sm font-medium text-secondary-700">
              {{ $t('weeklyReports.form.completedWork') }} *
            </label>
            <textarea
              v-model="formData.completedWork"
              rows="6"
              :placeholder="$t('weeklyReports.form.completedWorkPlaceholder')"
              class="w-full rounded-lg border border-secondary-200 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20 resize-none"
              required
            ></textarea>
          </div>

          <div>
            <label class="mb-2 block text-sm font-medium text-secondary-700">
              {{ $t('weeklyReports.form.nextWeekPlan') }} *
            </label>
            <textarea
              v-model="formData.nextWeekPlan"
              rows="6"
              :placeholder="$t('weeklyReports.form.nextWeekPlanPlaceholder')"
              class="w-full rounded-lg border border-secondary-200 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20 resize-none"
              required
            ></textarea>
          </div>

          <div>
            <label class="mb-2 block text-sm font-medium text-secondary-700">
              {{ $t('weeklyReports.form.problems') }}
            </label>
            <textarea
              v-model="formData.problems"
              rows="4"
              :placeholder="$t('weeklyReports.form.problemsPlaceholder')"
              class="w-full rounded-lg border border-secondary-200 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500/20 resize-none"
            ></textarea>
          </div>

          <div class="flex gap-3 pt-4 border-t border-secondary-200">
            <Button variant="secondary" @click="goBack">
              {{ $t('common.cancel') }}
            </Button>
            <Button variant="primary" @click="handleSaveDraft" :loading="saving">
              {{ $t('weeklyReports.form.saveDraft') }}
            </Button>
            <Button variant="primary" @click="handleSubmit" :loading="saving">
              {{ $t('weeklyReports.form.submit') }}
            </Button>
          </div>
        </form>
      </Card>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useI18n } from 'vue-i18n';
import MainLayout from '@/components/layout/MainLayout.vue';
import Card from '@/components/common/Card.vue';
import Button from '@/components/common/Button.vue';
import { useWeeklyReportStore } from '@/stores/weeklyReport';
import { useProjectStore } from '@/stores/project';
import { useUserStore } from '@/stores/user';
import type { WeeklyReport } from '@/types';
import dayjs from 'dayjs';
import 'dayjs/locale/zh-cn';

dayjs.locale('zh-cn');

const router = useRouter();
const route = useRoute();
const { t } = useI18n();
const weeklyReportStore = useWeeklyReportStore();
const projectStore = useProjectStore();
const userStore = useUserStore();

const reportId = computed(() => route.params.id as string);
const isEditing = computed(() => !!reportId.value && route.path.includes('/edit'));
const saving = ref(false);

const currentUserId = computed(() => userStore.currentUser?.id || userStore.currentUserId);

const availableProjects = computed(() => {
  if (permissionStore.currentRole === 'admin' || permissionStore.currentRole === 'project-manager') {
    return projectStore.projects;
  }
  return projectStore.projects.filter(p => {
    const isOwner = p.ownerId === currentUserId.value;
    const isMember = p.memberIds?.includes(currentUserId.value) || false;
    return isOwner || isMember;
  });
});

const formData = reactive({
  projectId: '',
  weekStart: '',
  weekEnd: '',
  completedWork: '',
  nextWeekPlan: '',
  problems: ''
});

const permissionStore = computed(() => ({
  currentRole: userStore.currentUser?.role || 'member'
}));

const resetForm = () => {
  formData.projectId = '';
  formData.weekStart = '';
  formData.weekEnd = '';
  formData.completedWork = '';
  formData.nextWeekPlan = '';
  formData.problems = '';
};

const loadReportData = async () => {
  if (isEditing.value) {
    await weeklyReportStore.loadReports();
    const report = weeklyReportStore.currentReport;
    if (report) {
      formData.projectId = report.projectId;
      formData.weekStart = report.weekStart;
      formData.weekEnd = report.weekEnd;
      formData.completedWork = report.completedWork;
      formData.nextWeekPlan = report.nextWeekPlan;
      formData.problems = report.problems || '';
    }
  } else {
    resetForm();
    setDefaultWeekRange();
  }
};

const setDefaultWeekRange = () => {
  const today = dayjs();
  const monday = today.startOf('week');
  const sunday = today.endOf('week');
  
  formData.weekStart = monday.format('YYYY-MM-DD');
  formData.weekEnd = sunday.format('YYYY-MM-DD');
};

const updateWeekEnd = () => {
  if (formData.weekStart) {
    const start = dayjs(formData.weekStart);
    const end = start.add(6, 'day');
    formData.weekEnd = end.format('YYYY-MM-DD');
  }
};

const validateForm = (isDraft: boolean = false): boolean => {
  if (!formData.projectId) {
    alert(t('weeklyReports.form.errors.selectProject'));
    return false;
  }

  if (!formData.weekStart || !formData.weekEnd) {
    alert(t('weeklyReports.form.errors.selectWeek'));
    return false;
  }

  if (!isDraft) {
    if (!formData.completedWork.trim()) {
      alert(t('weeklyReports.form.errors.enterCompletedWork'));
      return false;
    }

    if (!formData.nextWeekPlan.trim()) {
      alert(t('weeklyReports.form.errors.enterNextWeekPlan'));
      return false;
    }
  }

  return true;
};

const handleSaveDraft = async () => {
  if (!validateForm(true)) {
    return;
  }

  saving.value = true;

  try {
    const reportData = {
      ...formData,
      userId: currentUserId.value,
      status: 'draft' as const
    };

    if (isEditing.value) {
      await weeklyReportStore.updateReport(reportId.value, reportData);
      alert(t('messages.success.save'));
    } else {
      const newReport = await weeklyReportStore.createReport(reportData);
      if (newReport) {
        alert(t('messages.success.save'));
        router.push('/weekly-reports');
      }
    }
  } catch (error) {
    console.error('Failed to save draft:', error);
    alert(t('messages.error.save'));
  } finally {
    saving.value = false;
  }
};

const handleSubmit = async () => {
  if (!validateForm(false)) {
    return;
  }

  saving.value = true;

  try {
    const reportData = {
      ...formData,
      userId: currentUserId.value
    };

    if (isEditing.value) {
      await weeklyReportStore.updateReport(reportId.value, reportData);
      await weeklyReportStore.submitReport(reportId.value);
      alert(t('messages.success.submit'));
    } else {
      const newReport = await weeklyReportStore.createReport(reportData);
      if (newReport) {
        await weeklyReportStore.submitReport(newReport.id);
        alert(t('messages.success.submit'));
      }
    }
    router.push('/weekly-reports');
  } catch (error) {
    console.error('Failed to submit report:', error);
    alert(t('messages.error.submit'));
  } finally {
    saving.value = false;
  }
};

const goBack = () => {
  router.back();
};

onMounted(async () => {
  await projectStore.loadProjects();
  await loadReportData();
});
</script>
