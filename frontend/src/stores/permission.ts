import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import type { Permission, UserRole } from '@/types';
import apiService from '@/services/api';
import { useUserStore } from './user';
import { useProjectStore } from './project';

/**
 * 权限 Store（角色管理 v2 重写）
 *
 * 4 角色 × 5 档数据范围：
 * - admin               : 全员
 * - dept-project-manager: 我所管理部门
 * - project-manager     : 我 owner 的项目
 * - member              : 我参与的项目（可改自己任务进度）
 * - viewer              : 我参与的项目（只读）
 */
export const usePermissionStore = defineStore('permission', () => {
  const permissions = ref<Permission[]>([]);
  const loading = ref(false);

  const userStore = useUserStore();
  const projectStore = useProjectStore();

  /**
   * 5 个角色 × 权限码 矩阵
   * 静态表 + 后端 PermissionService 双重校验
   */
  const rolePermissions: Record<UserRole, string[]> = {
    'admin': [
      'user:view', 'user:create', 'user:edit', 'user:delete', 'user:change-role',
      'project:create', 'project:view', 'project:edit', 'project:delete', 'project:manage_members',
      'settings:view', 'settings:edit',
      'hr-sync:view', 'hr-sync:execute',
      'task:create', 'task:edit', 'task:edit-progress', 'task:delete', 'task:assign',
      'overtime:create', 'overtime:approve', 'overtime:view',
      'weekly-report:create', 'weekly-report:edit', 'weekly-report:delete', 'weekly-report:view', 'weekly-report:approve',
      'document:create', 'document:edit', 'document:delete', 'document:view'
    ],
    'dept-project-manager': [
      'user:view',
      'project:create', 'project:view', 'project:edit', 'project:delete', 'project:manage_members',
      'settings:view',
      'task:create', 'task:edit', 'task:edit-progress', 'task:delete', 'task:assign',
      'overtime:create', 'overtime:approve', 'overtime:view',
      'weekly-report:create', 'weekly-report:edit', 'weekly-report:delete', 'weekly-report:view', 'weekly-report:approve',
      'document:create', 'document:edit', 'document:delete', 'document:view'
    ],
    'project-manager': [
      'user:view',
      'project:view', 'project:edit', 'project:manage_members',
      'settings:view',
      'task:create', 'task:edit', 'task:edit-progress', 'task:delete', 'task:assign',
      'overtime:create', 'overtime:approve', 'overtime:view',
      'weekly-report:create', 'weekly-report:edit', 'weekly-report:view', 'weekly-report:approve',
      'document:create', 'document:edit', 'document:view'
    ],
    'member': [
      'project:view',
      'settings:view',
      // 成员的核心权限：仅自己 assignee 的任务可改 progress
      'task:edit-progress',
      'overtime:create', 'overtime:view',
      'weekly-report:create', 'weekly-report:edit', 'weekly-report:view',
      'document:create', 'document:view'
    ],
    'viewer': [
      'project:view',
      'settings:view',
      'overtime:view',
      'weekly-report:view',
      'document:view'
    ]
  };

  const currentRole = computed((): UserRole => {
    const role = userStore.currentUser?.role as UserRole | undefined;
    if (role === 'admin' || role === 'dept-project-manager' || role === 'project-manager'
        || role === 'member' || role === 'viewer') {
      return role;
    }
    return 'member';
  });

  const currentPermissions = computed(() => {
    return rolePermissions[currentRole.value] || [];
  });

  /**
   * 用户的 managedDeptCodes（仅 dept-project-manager 有效）
   */
  const managedDeptCodes = computed((): string[] => {
    return userStore.currentUser?.managedDeptCodes || [];
  });

  const managedCompanyCd = computed((): string | undefined => {
    return userStore.currentUser?.managedCompanyCd;
  });

  // ============ 基础权限判断 ============

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

  // ============ 数据范围判断 ============

  const isAdmin = (): boolean => currentRole.value === 'admin';
  const isDeptProjectManager = (): boolean => currentRole.value === 'dept-project-manager';
  const isProjectManager = (): boolean => currentRole.value === 'project-manager';
  const isMember = (): boolean => currentRole.value === 'member';
  const isViewer = (): boolean => currentRole.value === 'viewer';

  /**
   * 是否管理指定 deptCode
   */
  const isDeptManager = (deptCode: string | undefined): boolean => {
    if (!deptCode) return false;
    if (currentRole.value !== 'dept-project-manager') return false;
    return managedDeptCodes.value.includes(deptCode);
  };

  /**
   * 是否为该项目的 owner
   */
  const isProjectOwner = (projectId: string): boolean => {
    const project = projectStore.projectById(projectId);
    if (!project) return false;
    return project.ownerId === userStore.currentUserId;
  };

  /**
   * 是否为该项目的成员
   * 修复旧实现:isProjectMember 与 isProjectOwner 等价的问题
   */
  const isProjectMember = (projectId: string): boolean => {
    const project = projectStore.projectById(projectId);
    if (!project || !project.memberIds) return false;
    return project.memberIds.includes(userStore.currentUserId || '');
  };

  /**
   * 是否能查看该项目（综合判断：数据范围）
   * 规则：admin 任意;dept-pm 看所管部门项目;pm 看 owner 项目;member/viewer 看参与项目
   */
  const canViewProject = (projectId: string): boolean => {
    if (isAdmin()) return true;
    if (isProjectOwner(projectId)) return true;
    if (isProjectMember(projectId)) return true;
    const project = projectStore.projectById(projectId);
    if (isDeptProjectManager() && project && isDeptManager(project.deptCode)) return true;
    return false;
  };

  /**
   * 是否能编辑该项目
   * 规则：admin 任意;dept-pm(部门内);pm(owner);其他不可
   */
  const canEditProject = (projectId: string): boolean => {
    if (isAdmin()) return true;
    if (isProjectOwner(projectId)) return true;
    const project = projectStore.projectById(projectId);
    if (isDeptProjectManager() && project && isDeptManager(project.deptCode)) return true;
    return false;
  };

  /**
   * 是否能删除该项目
   */
  const canDeleteProject = (projectId: string): boolean => {
    return canEditProject(projectId);
  };

  /**
   * 是否能管理项目成员
   */
  const canManageProjectMembers = (projectId: string): boolean => {
    return canEditProject(projectId);
  };

  /**
   * 是否能创建项目
   */
  const canCreateProject = (): boolean => {
    if (isAdmin()) return true;
    if (isDeptProjectManager()) {
      return managedDeptCodes.value.length > 0;
    }
    return false;
  };

  /**
   * 是否能创建任务
   */
  const canCreateTask = (projectId: string): boolean => {
    return canEditProject(projectId);
  };

  /**
   * 是否能编辑任务(任意字段)
   */
  const canEditTask = (projectId: string): boolean => {
    return canEditProject(projectId);
  };

  /**
   * 是否能编辑任务进度
   * 规则：admin / dept-pm(部门内) / pm(owner) 全部允许
   *      member: 仅当 task.assigneeId == self
   *      viewer: 不允许
   */
  const canEditTaskProgress = (projectId: string, assigneeId?: string): boolean => {
    if (isAdmin()) return true;
    if (canEditProject(projectId)) return true;
    if (isMember() && assigneeId && assigneeId === userStore.currentUserId) return true;
    return false;
  };

  /**
   * 是否能删除任务
   */
  const canDeleteTask = (projectId: string, _assigneeId?: string): boolean => {
    return canEditTask(projectId);
  };

  /**
   * 是否能审批周报
   */
  const canApproveWeeklyReport = (projectId: string): boolean => {
    return canEditProject(projectId);
  };

  /**
   * 是否能审批加班
   */
  const canApproveOvertime = (projectId: string): boolean => {
    if (isAdmin()) return true;
    if (isProjectOwner(projectId)) return true;
    const project = projectStore.projectById(projectId);
    if (isDeptProjectManager() && project && isDeptManager(project.deptCode)) return true;
    return false;
  };

  /**
   * 是否能编辑用户
   */
  const canEditUser = (userId: string): boolean => {
    if (isAdmin()) return true;
    return userId === userStore.currentUserId;
  };

  /**
   * 是否能删除用户
   */
  const canDeleteUser = (): boolean => {
    return isAdmin();
  };

  /**
   * 是否能修改其他用户的角色
   */
  const canChangeUserRole = (): boolean => {
    return isAdmin();
  };

  const canCreateDocument = (projectId: string): boolean => {
    return canEditProject(projectId);
  };

  const canEditDocument = (projectId: string, documentOwnerId?: string): boolean => {
    if (canEditProject(projectId)) return true;
    if (documentOwnerId && documentOwnerId === userStore.currentUserId) return true;
    return false;
  };

  const canDeleteDocument = (projectId: string, documentOwnerId?: string): boolean => {
    return canEditDocument(projectId, documentOwnerId);
  };

  const canAccessHrSync = (): boolean => {
    return isAdmin();
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
    managedDeptCodes,
    managedCompanyCd,
    hasPermission,
    hasAnyPermission,
    hasAllPermissions,
    isAdmin,
    isDeptProjectManager,
    isProjectManager,
    isMember,
    isViewer,
    isDeptManager,
    isProjectOwner,
    isProjectMember,
    canViewProject,
    canEditProject,
    canDeleteProject,
    canManageProjectMembers,
    canCreateProject,
    canCreateTask,
    canEditTask,
    canEditTaskProgress,
    canDeleteTask,
    canApproveWeeklyReport,
    canApproveOvertime,
    canEditUser,
    canDeleteUser,
    canChangeUserRole,
    canCreateDocument,
    canEditDocument,
    canDeleteDocument,
    canAccessHrSync,
    loadPermissions
  };
});
