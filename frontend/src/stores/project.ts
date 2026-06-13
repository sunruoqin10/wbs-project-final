import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import type { Project } from '@/types';
import apiService, { ApiError } from '@/services/api';
import { projects as mockProjects } from '@/data/projects';

export const useProjectStore = defineStore('project', () => {
  const projects = ref<Project[]>([]);
  const currentProject = ref<Project | null>(null);
  const loading = ref(false);
  const loaded = ref(false);

  const filters = ref({
    status: [] as string[],
    search: ''
  });

  // 加载所有项目（首次加载，如果已加载则跳过）
  const loadProjects = async () => {
    if (loaded.value) return;
    try {
      await refreshProjects();
      loaded.value = true;
    } finally {
      loading.value = false;
    }
  };

  // 强制刷新项目数据（始终从 API 重新获取）
  const refreshProjects = async () => {
    try {
      loading.value = true;
      projects.value = await apiService.getProjects();
      // 调试：打印项目数据，检查延期信息
      console.log('=== 加载的项目数据 ===');
      projects.value.forEach(p => {
        console.log(`项目: ${p.name}`, {
          isDelayed: p.isDelayed,
          delayedTasks: p.delayedTasks,
          totalDelayedDays: p.totalDelayedDays
        });
      });
    } catch (error) {
      // API 调用失败时保持现有数据
      console.warn('API 未连接，使用 mock 数据:', error);
      if (projects.value.length === 0) {
        projects.value = mockProjects;
      }
    } finally {
      loading.value = false;
    }
  };

  // Getters
  const activeProjects = computed(() => {
    return projects.value.filter(p => p.status === 'active');
  });

  const completedProjects = computed(() => {
    return projects.value.filter(p => p.status === 'completed');
  });

  const projectById = (id: string) => {
    return projects.value.find(p => p.id === id);
  };

  const filteredProjects = computed(() => {
    let result = projects.value;

    if (filters.value.status.length > 0) {
      result = result.filter(p => filters.value.status.includes(p.status));
    }

    if (filters.value.search) {
      const search = filters.value.search.toLowerCase();
      result = result.filter(p =>
        p.name.toLowerCase().includes(search) ||
        p.description.toLowerCase().includes(search)
      );
    }

    return result;
  });

  // Actions
  const setCurrentProject = (project: Project | null) => {
    currentProject.value = project;
  };

  const createProject = async (data: Partial<Project>) => {
    try {
      const newProject = await apiService.createProject(data);
      projects.value.unshift(newProject);
      return newProject;
    } catch (error) {
      console.error('Failed to create project:', error);
      throw error;
    }
  };

  /**
   * 更新项目
   * @param silent 传 true 时,即使后端返回"无项目编辑权限"也不打 console.error——
   * 适用于"已知某些调用方可能没权限"的场景(如自动补工时、批量迁移)
   * 其它错误照常 console.error。
   */
  const updateProject = async (
    id: string,
    data: Partial<Project>,
    options: { silent?: boolean } = {}
  ) => {
    try {
      const updatedProject = await apiService.updateProject(id, data, { silent: options.silent });
      const index = projects.value.findIndex(p => p.id === id);
      if (index !== -1) {
        projects.value[index] = updatedProject;
      }
      if (currentProject.value?.id === id) {
        currentProject.value = updatedProject;
      }
      return updatedProject;
    } catch (error) {
      // 权限不足时若已 silent,这里也跳过;否则打日志
      const isPermissionError = error instanceof ApiError && error.message === '无项目编辑权限';
      if (!(options.silent && isPermissionError)) {
        console.error('Failed to update project:', error);
      }
      throw error;
    }
  };

  const deleteProject = async (id: string) => {
    try {
      await apiService.deleteProject(id);
      const index = projects.value.findIndex(p => p.id === id);
      if (index !== -1) {
        projects.value.splice(index, 1);
      }
      if (currentProject.value?.id === id) {
        currentProject.value = null;
      }
    } catch (error) {
      console.error('Failed to delete project:', error);
      throw error;
    }
  };

  const setFilters = (newFilters: typeof filters.value) => {
    filters.value = newFilters;
  };

  return {
    projects,
    currentProject,
    loading,
    loaded,
    filters,
    activeProjects,
    completedProjects,
    projectById,
    filteredProjects,
    loadProjects,
    refreshProjects,
    setCurrentProject,
    createProject,
    updateProject,
    deleteProject,
    setFilters
  };
});
