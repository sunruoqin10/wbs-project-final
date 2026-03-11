import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import type { WeeklyReport, WeeklyReportComment, ReportStatus } from '@/types';
import apiService from '@/services/api';

export const useWeeklyReportStore = defineStore('weeklyReport', () => {
  const reports = ref<WeeklyReport[]>([]);
  const currentReport = ref<WeeklyReport | null>(null);
  const loading = ref(false);
  const loaded = ref(false);

  const filters = ref({
    userId: '',
    projectId: '',
    status: '' as ReportStatus | '',
    startDate: '',
    endDate: ''
  });

  const loadReports = async () => {
    try {
      loading.value = true;
      console.log('[周报Store] 开始加载周报数据...');
      const response = await apiService.getWeeklyReports();
      console.log('[周报Store] API返回成功:', response);

      if (Array.isArray(response)) {
        console.log('[周报Store] 收到周报数据，数量:', response.length);
        console.log('[周报Store] 周报详情:', response);
        reports.value = response;
      } else {
        console.warn('[周报Store] API返回的不是数组:', response);
        reports.value = [];
      }
      loaded.value = true;
    } catch (error) {
      console.error('[周报Store] 加载周报失败:', error);
      reports.value = [];
      loaded.value = true;
      throw error;
    } finally {
      loading.value = false;
    }
  };

  const loadReportById = async (id: string) => {
    try {
      loading.value = true;
      const report = await apiService.getWeeklyReportById(id);
      currentReport.value = report;
      return report;
    } catch (error) {
      console.warn('Failed to load weekly report:', error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  const createReport = async (data: Partial<WeeklyReport>) => {
    try {
      loading.value = true;
      const response = await apiService.createWeeklyReport(data);
      if (response.code === 200 && response.data) {
        reports.value.unshift(response.data);
      }
      return response.data;
    } catch (error) {
      console.warn('Failed to create weekly report:', error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  const updateReport = async (id: string, data: Partial<WeeklyReport>) => {
    try {
      loading.value = true;
      const response = await apiService.updateWeeklyReport(id, data);
      if (response.code === 200 && response.data) {
        const index = reports.value.findIndex(r => r.id === id);
        if (index !== -1) {
          reports.value[index] = response.data;
        }
      }
      return response.data;
    } catch (error) {
      console.warn('Failed to update weekly report:', error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  const deleteReport = async (id: string) => {
    try {
      loading.value = true;
      const response = await apiService.deleteWeeklyReport(id);
      if (response.code === 200) {
        reports.value = reports.value.filter(r => r.id !== id);
      }
    } catch (error) {
      console.warn('Failed to delete weekly report:', error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  const submitReport = async (id: string) => {
    try {
      loading.value = true;
      const response = await apiService.submitWeeklyReport(id);
      if (response.code === 200 && response.data) {
        const index = reports.value.findIndex(r => r.id === id);
        if (index !== -1) {
          reports.value[index] = response.data;
        }
      }
      return response.data;
    } catch (error) {
      console.warn('Failed to submit weekly report:', error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  const approveReport = async (id: string, approved: boolean, approveComment?: string) => {
    try {
      loading.value = true;
      const response = await apiService.approveWeeklyReport(id, approved, approveComment);
      if (response.code === 200 && response.data) {
        const index = reports.value.findIndex(r => r.id === id);
        if (index !== -1) {
          reports.value[index] = response.data;
        }
      }
      return response.data;
    } catch (error) {
      console.warn('Failed to approve weekly report:', error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  const loadMyReports = async (userId: string) => {
    try {
      loading.value = true;
      const response = await apiService.getMyWeeklyReports(userId);
      if (response.code === 200 && response.data) {
        reports.value = response.data;
      }
      return response.data;
    } catch (error) {
      console.warn('Failed to load my weekly reports:', error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  const loadProjectReports = async (projectId: string) => {
    try {
      loading.value = true;
      const response = await apiService.getProjectWeeklyReports(projectId);
      if (response.code === 200 && response.data) {
        reports.value = response.data;
      }
      return response.data;
    } catch (error) {
      console.warn('Failed to load project weekly reports:', error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  const loadCurrentWeekReport = async (userId: string) => {
    try {
      loading.value = true;
      const response = await apiService.getCurrentWeekReport(userId);
      if (response.code === 200 && response.data) {
        currentReport.value = response.data;
      }
      return response.data;
    } catch (error) {
      console.warn('Failed to load current week report:', error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  const loadComments = async (reportId: string) => {
    try {
      const response = await apiService.getWeeklyReportComments(reportId);
      if (response.code === 200 && response.data) {
        if (currentReport.value && currentReport.value.id === reportId) {
          currentReport.value.comments = response.data;
        }
      }
      return response.data;
    } catch (error) {
      console.warn('Failed to load comments:', error);
      throw error;
    }
  };

  const addComment = async (reportId: string, content: string) => {
    try {
      const response = await apiService.addWeeklyReportComment(reportId, { content });
      if (response.code === 200 && response.data) {
        if (currentReport.value && currentReport.value.id === reportId) {
          if (!currentReport.value.comments) {
            currentReport.value.comments = [];
          }
          currentReport.value.comments.unshift(response.data);
        }
      }
      return response.data;
    } catch (error) {
      console.warn('Failed to add comment:', error);
      throw error;
    }
  };

  const deleteComment = async (commentId: string) => {
    try {
      const response = await apiService.deleteWeeklyReportComment(commentId);
      if (response.code === 200 && currentReport.value?.comments) {
        currentReport.value.comments = currentReport.value.comments.filter(c => c.id !== commentId);
      }
    } catch (error) {
      console.warn('Failed to delete comment:', error);
      throw error;
    }
  };

  const filteredReports = computed(() => {
    let result = reports.value;

    if (filters.value.userId) {
      result = result.filter(r => r.userId === filters.value.userId);
    }

    if (filters.value.projectId) {
      result = result.filter(r => r.projectId === filters.value.projectId);
    }

    if (filters.value.status) {
      result = result.filter(r => r.status === filters.value.status);
    }

    if (filters.value.startDate) {
      result = result.filter(r => r.weekStart >= filters.value.startDate);
    }

    if (filters.value.endDate) {
      result = result.filter(r => r.weekEnd <= filters.value.endDate);
    }

    return result;
  });

  const setCurrentReport = (report: WeeklyReport | null) => {
    currentReport.value = report;
  };

  const setFilters = (newFilters: typeof filters.value) => {
    filters.value = { ...filters.value, ...newFilters };
  };

  const clearFilters = () => {
    filters.value = {
      userId: '',
      projectId: '',
      status: '' as ReportStatus | '',
      startDate: '',
      endDate: ''
    };
  };

  return {
    reports,
    currentReport,
    loading,
    loaded,
    filters,
    filteredReports,
    loadReports,
    loadReportById,
    createReport,
    updateReport,
    deleteReport,
    submitReport,
    approveReport,
    loadMyReports,
    loadProjectReports,
    loadCurrentWeekReport,
    loadComments,
    addComment,
    deleteComment,
    setCurrentReport,
    setFilters,
    clearFilters
  };
});
