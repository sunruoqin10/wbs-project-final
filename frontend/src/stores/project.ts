import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import type { Project } from '@/types';
import apiService from '@/services/api';
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

  // 加载所有项目
  const loadProjects = async () => {
    if (loaded.value) return;
    try {
      loading.value = true;
      projects.value = await apiService.getProjects();
      loaded.value = true;
    } catch (error) {
      // API 调用失败时使用 mock 数据（开发阶段）
      console.warn('API 未连接，使用 mock 数据:', error);
      projects.value = mockProjects;
      loaded.value = true;
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

  const updateProject = async (id: string, data: Partial<Project>) => {
    try {
      const updatedProject = await apiService.updateProject(id, data);
      const index = projects.value.findIndex(p => p.id === id);
      if (index !== -1) {
        projects.value[index] = updatedProject;
      }
      if (currentProject.value?.id === id) {
        currentProject.value = updatedProject;
      }
      return updatedProject;
    } catch (error) {
      console.error('Failed to update project:', error);
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
    setCurrentProject,
    createProject,
    updateProject,
    deleteProject,
    setFilters
  };
});
