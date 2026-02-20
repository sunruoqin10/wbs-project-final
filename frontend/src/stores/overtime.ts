import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import type { OvertimeRecord, OvertimeStats } from '@/types';
import apiService from '@/services/api';

export const useOvertimeStore = defineStore('overtime', () => {
  const overtimeRecords = ref<OvertimeRecord[]>([]);
  const currentRecord = ref<OvertimeRecord | null>(null);
  const stats = ref<OvertimeStats | null>(null);
  const loading = ref(false);
  const loaded = ref(false);

  const filters = ref({
    userId: '',
    projectId: '',
    status: '',
    startDate: '',
    endDate: ''
  });

  // 加载加班记录列表
  const loadOvertimeRecords = async (params?: {
    userId?: string;
    projectId?: string;
    status?: string;
    startDate?: string;
    endDate?: string;
  }) => {
    try {
      loading.value = true;
      const records = await apiService.getOvertimeRecords(params);
      overtimeRecords.value = records;
      loaded.value = true;
    } catch (error) {
      console.error('Failed to load overtime records:', error);
      // API 调用失败时使用空数组
      overtimeRecords.value = [];
      loaded.value = true;
    } finally {
      loading.value = false;
    }
  };

  // 加载统计数据
  const loadStats = async (projectId?: string) => {
    try {
      loading.value = true;
      const data = await apiService.getOvertimeStats(projectId);
      stats.value = data;
    } catch (error) {
      console.error('Failed to load overtime stats:', error);
      // API 调用失败时使用默认统计数据
      stats.value = {
        totalRecords: 0,
        totalHours: 0,
        totalPeople: 0,
        pendingApprovals: 0,
        thisMonthHours: 0,
        thisMonthPeople: 0,
        byType: {
          weekday: 0,
          weekend: 0,
          holiday: 0
        },
        byProject: []
      };
    } finally {
      loading.value = false;
    }
  };

  // Getters
  const pendingRecords = computed(() => {
    return overtimeRecords.value.filter(r => r.status === 'pending');
  });

  const approvedRecords = computed(() => {
    return overtimeRecords.value.filter(r => r.status === 'approved');
  });

  const rejectedRecords = computed(() => {
    return overtimeRecords.value.filter(r => r.status === 'rejected');
  });

  const recordsByUser = (userId: string) => {
    return overtimeRecords.value.filter(r => r.userId === userId);
  };

  const recordsByProject = (projectId: string) => {
    return overtimeRecords.value.filter(r => r.projectId === projectId);
  };

  const getRecordById = (id: string) => {
    return overtimeRecords.value.find(r => r.id === id);
  };

  const filteredRecords = computed(() => {
    let result = overtimeRecords.value;

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
      result = result.filter(r => r.overtimeDate >= filters.value.startDate);
    }

    if (filters.value.endDate) {
      result = result.filter(r => r.overtimeDate <= filters.value.endDate);
    }

    return result;
  });

  // Actions
  const setCurrentRecord = (record: OvertimeRecord | null) => {
    currentRecord.value = record;
  };

  const setFilters = (newFilters: typeof filters.value) => {
    filters.value = newFilters;
  };

  // 创建加班记录
  const createOvertimeRecord = async (data: Partial<OvertimeRecord>) => {
    try {
      const newRecord = await apiService.createOvertimeRecord(data);
      overtimeRecords.value.unshift(newRecord);
      return newRecord;
    } catch (error) {
      console.error('Failed to create overtime record:', error);
      throw error;
    }
  };

  // 更新加班记录
  const updateOvertimeRecord = async (id: string, data: Partial<OvertimeRecord>) => {
    try {
      const updatedRecord = await apiService.updateOvertimeRecord(id, data);
      const index = overtimeRecords.value.findIndex(r => r.id === id);
      if (index !== -1) {
        overtimeRecords.value[index] = updatedRecord;
      }
      if (currentRecord.value?.id === id) {
        currentRecord.value = updatedRecord;
      }
      return updatedRecord;
    } catch (error) {
      console.error('Failed to update overtime record:', error);
      throw error;
    }
  };

  // 删除加班记录
  const deleteOvertimeRecord = async (id: string) => {
    try {
      await apiService.deleteOvertimeRecord(id);
      const index = overtimeRecords.value.findIndex(r => r.id === id);
      if (index !== -1) {
        overtimeRecords.value.splice(index, 1);
      }
      if (currentRecord.value?.id === id) {
        currentRecord.value = null;
      }
    } catch (error) {
      console.error('Failed to delete overtime record:', error);
      throw error;
    }
  };

  // 审批加班记录（通过）
  const approveOvertimeRecord = async (id: string, approverId: string) => {
    try {
      const updatedRecord = await apiService.approveOvertimeRecord(id, approverId);
      const index = overtimeRecords.value.findIndex(r => r.id === id);
      if (index !== -1) {
        overtimeRecords.value[index] = updatedRecord;
      }
      if (currentRecord.value?.id === id) {
        currentRecord.value = updatedRecord;
      }
      return updatedRecord;
    } catch (error) {
      console.error('Failed to approve overtime record:', error);
      throw error;
    }
  };

  // 审批加班记录（拒绝）
  const rejectOvertimeRecord = async (id: string, approverId: string, rejectReason: string) => {
    try {
      const updatedRecord = await apiService.rejectOvertimeRecord(id, approverId, rejectReason);
      const index = overtimeRecords.value.findIndex(r => r.id === id);
      if (index !== -1) {
        overtimeRecords.value[index] = updatedRecord;
      }
      if (currentRecord.value?.id === id) {
        currentRecord.value = updatedRecord;
      }
      return updatedRecord;
    } catch (error) {
      console.error('Failed to reject overtime record:', error);
      throw error;
    }
  };

  return {
    overtimeRecords,
    currentRecord,
    stats,
    loading,
    loaded,
    filters,
    pendingRecords,
    approvedRecords,
    rejectedRecords,
    recordsByUser,
    recordsByProject,
    getRecordById,
    filteredRecords,
    loadOvertimeRecords,
    loadStats,
    setCurrentRecord,
    setFilters,
    createOvertimeRecord,
    updateOvertimeRecord,
    deleteOvertimeRecord,
    approveOvertimeRecord,
    rejectOvertimeRecord
  };
});
