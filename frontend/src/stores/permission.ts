import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import type { Permission, UserRole } from '@/types';
import apiService from '@/services/api';
import { useUserStore } from './user';
import { useProjectStore } from './project';

export const usePermissionStore = defineStore('permission', () => {
  const permissions = ref<Permission[]>([]);
  const loading = ref(false);

  const userStore = useUserStore();
  const projectStore = useProjectStore();

  const rolePermissions: Record<UserRole, string[]> = {
    'admin': [
      'user:view', 'user:create', 'user:edit', 'user:delete',
      'project:create', 'project:view', 'project:delete',
      'settings:view', 'settings:edit',
      'project:edit', 'project:manage_members',
      'task:create', 'task:edit', 'task:delete', 'task:assign',
      'overtime:create', 'overtime:approve', 'overtime:view'
    ],
    'project-manager': [
      'user:view',
      'project:create', 'project:view',
      'project:edit', 'project:manage_members',
      'task:create', 'task:edit', 'task:delete', 'task:assign',
      'overtime:create', 'overtime:approve', 'overtime:view'
    ],
    'member': [
      'project:create', 'project:view',
      'task:create', 'task:edit',
      'overtime:create', 'overtime:view'
    ]
  };

  const currentRole = computed((): UserRole => {
    const role = userStore.currentUser?.role;
    if (role === 'admin' || role === 'project-manager' || role === 'member') {
      return role;
    }
    return 'member';
  });

  const currentPermissions = computed(() => {
    return rolePermissions[currentRole.value] || [];
  });

  const hasPermission = (permissionCode: string): boolean => {
    if (currentRole.value === 'admin') {
      return true;
    }
    return currentPermissions.value.includes(permissionCode);
  };

  const hasAnyPermission = (permissionCodes: string[]): boolean => {
    return permissionCodes.some(code => hasPermission(code));
  };

  const hasAllPermissions = (permissionCodes: string[]): boolean => {
    return permissionCodes.every(code => hasPermission(code));
  };

  const isProjectOwner = (projectId: string): boolean => {
    const project = projectStore.projectById(projectId);
    if (!project) return false;
    return project.ownerId === userStore.currentUserId;
  };

  const isProjectMember = (projectId: string): boolean => {
    const project = projectStore.projectById(projectId);
    if (!project) return false;
    return project.ownerId === userStore.currentUserId;
  };

  const hasProjectPermission = (projectId: string, permissionCode: string): boolean => {
    if (currentRole.value === 'admin') {
      return true;
    }
    if (isProjectOwner(projectId)) {
      return true;
    }
    if (isProjectMember(projectId)) {
      const memberPermissions = [
        'task:create', 'task:edit', 'task:delete',
        'overtime:create', 'overtime:view'
      ];
      return memberPermissions.includes(permissionCode);
    }
    return false;
  };

  const canEditProject = (projectId: string): boolean => {
    return currentRole.value === 'admin' || isProjectOwner(projectId);
  };

  const canDeleteProject = (projectId: string): boolean => {
    return currentRole.value === 'admin' || isProjectOwner(projectId);
  };

  const canManageProjectMembers = (projectId: string): boolean => {
    return currentRole.value === 'admin' || isProjectOwner(projectId);
  };

  const canCreateTask = (projectId: string): boolean => {
    return hasProjectPermission(projectId, 'task:create');
  };

  const canEditTask = (projectId: string, assigneeId?: string): boolean => {
    if (currentRole.value === 'admin') return true;
    if (isProjectOwner(projectId)) return true;
    if (isProjectMember(projectId)) return true;
    return false;
  };

  const canDeleteTask = (projectId: string, assigneeId?: string): boolean => {
    if (currentRole.value === 'admin') return true;
    if (isProjectOwner(projectId)) return true;
    if (assigneeId && assigneeId === userStore.currentUserId) return true;
    return false;
  };

  const canApproveOvertime = (projectId: string): boolean => {
    return currentRole.value === 'admin' || isProjectOwner(projectId);
  };

  const canEditUser = (userId: string): boolean => {
    if (currentRole.value === 'admin') return true;
    return userId === userStore.currentUserId;
  };

  const canDeleteUser = (): boolean => {
    return currentRole.value === 'admin';
  };

  const loadPermissions = async () => {
    try {
      loading.value = true;
      const data = await apiService.getPermissions();
      permissions.value = data;
    } catch (error) {
      console.error('Failed to load permissions:', error);
    } finally {
      loading.value = false;
    }
  };

  return {
    permissions,
    loading,
    currentRole,
    currentPermissions,
    hasPermission,
    hasAnyPermission,
    hasAllPermissions,
    isProjectOwner,
    isProjectMember,
    hasProjectPermission,
    canEditProject,
    canDeleteProject,
    canManageProjectMembers,
    canCreateTask,
    canEditTask,
    canDeleteTask,
    canApproveOvertime,
    canEditUser,
    canDeleteUser,
    loadPermissions
  };
});
