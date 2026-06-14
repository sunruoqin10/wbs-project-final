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

  /**
   * 用户的 managedProjectIds（2026-06-14, 仅 project-manager 有效）
   */
  const managedProjectIds = computed((): string[] => {
    return userStore.currentUser?.managedProjectIds || [];
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
   * 是否为该项目的创建者
   */
  const isProjectCreator = (projectId: string): boolean => {
    const project = projectStore.projectById(projectId);
    if (!project) return false;
    return project.createdBy === userStore.currentUserId;
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
   * 规则：admin 任意;创建者 总可看;dept-pm 看所管部门项目;pm 看 owner 项目;member/viewer 看参与项目
   */
  const canViewProject = (projectId: string): boolean => {
    if (isAdmin()) return true;
    if (isProjectCreator(projectId)) return true;
    if (isProjectOwner(projectId)) return true;
    if (isProjectMember(projectId)) return true;
    const project = projectStore.projectById(projectId);
    if (isDeptProjectManager() && project && isDeptManager(project.deptCode)) return true;
    return false;
  };

  /**
   * 是否能编辑该项目
   * 规则：admin 任意;创建者 总可编辑;dept-pm(部门内);pm(owner / managed_projects);其他不可(2026-06-12 PM 加)
   */
  const canEditProject = (projectId: string): boolean => {
    if (isAdmin()) return true;
    if (isProjectCreator(projectId)) return true;
    if (isProjectOwner(projectId)) return true;
    if (isManagedProject(projectId)) return true;
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
   * 规则:admin 任意;dept-pm 需有 managed_dept_codes;pm 需有 dept_code(2026-06-12 PM 加)
   */
  const canCreateProject = (): boolean => {
    if (isAdmin()) return true;
    if (isDeptProjectManager()) {
      return managedDeptCodes.value.length > 0;
    }
    if (isProjectManager()) {
      const me = userStore.currentUser;
      return !!me && !!me.deptCode && me.deptCode.length > 0;
    }
    return false;
  };

  /**
   * PM 是否管理该项目(2026-06-12 PM 角色重新启用)
   * 数据源: project.id IN user.managedProjectIds
   */
  const isManagedProject = (projectId: string): boolean => {
    const me = userStore.currentUser;
    if (!me || me.role !== 'project-manager') return false;
    const ids = me.managedProjectIds || [];
    return ids.includes(projectId);
  };

  /**
   * 任务内容管理(创建/编辑/删除任务标题/描述/负责人/状态/日期):
   * admin + 项目创建者 + 项目负责人(owner) + 项目经理(managed_projects)。
   * 部门项目负责人不能再管任务。
   */
  const canManageTaskContent = (projectId: string): boolean => {
    if (isAdmin()) return true;
    if (isProjectCreator(projectId)) return true;
    if (isProjectOwner(projectId)) return true;
    if (isManagedProject(projectId)) return true;
    return false;
  };

  /**
   * 是否能创建任务
   */
  const canCreateTask = (projectId: string): boolean => {
    return canManageTaskContent(projectId);
  };

  /**
   * 是否能编辑任务(任意字段)
   * 规则：admin / 项目创建者 / 项目负责人(owner) / 任务负责人(任务的 assignee)
   * (2026-06-12:任务负责人可编辑自己的任务)
   */
  const canEditTask = (task: { projectId: string; assigneeId?: string }): boolean => {
    if (isAdmin()) return true;
    if (canManageTaskContent(task.projectId)) return true;
    if (task.assigneeId && task.assigneeId === userStore.currentUserId) return true;
    return false;
  };

  /**
   * 是否能删除任务
   * 规则：admin / 项目创建者 / 项目负责人(owner) / 任务负责人(task.assigneeId)
   * (2026-06-12:任务负责人可删除自己的任务)
   */
  const canDeleteTask = (task: { projectId: string; assigneeId?: string }): boolean => {
    if (isAdmin()) return true;
    if (canManageTaskContent(task.projectId)) return true;
    if (task.assigneeId && task.assigneeId === userStore.currentUserId) return true;
    return false;
  };

  /**
   * 是否能在指定父任务下添加子任务
   * 规则：admin / 项目创建者 / 项目负责人(owner) / 父任务的 assignee
   * (2026-06-12:任务的负责人可为自己的任务添加子任务)
   */
  const canAddSubtask = (parentTask: { projectId: string; assigneeId?: string }): boolean => {
    if (isAdmin()) return true;
    if (canManageTaskContent(parentTask.projectId)) return true;
    if (parentTask.assigneeId && parentTask.assigneeId === userStore.currentUserId) return true;
    return false;
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
   * 是否能审批周报
   */
  const canApproveWeeklyReport = (projectId: string): boolean => {
    return canEditProject(projectId);
  };

  /**
   * 是否能审批加班
   *
   * 2026-06-14 修复:加 submitterId 参数 + dept-pm 分支加 submitter.deptCode 降级。
   * 原实现 100% 依赖 projectStore.projectById(projectId) 能拿到项目,
   * 但团队 tab 渲染瞬间如果 project store 数据还没回来(竞态),
   * dept-pm 的"项目归属部门"判定就拿不到 deptCode,直接 false → 按钮不显示。
   *
   * 降级方案:project 缺失时,改用 submitter.deptCode 判定 —
   * 这与后端 OvertimeService.hasPermission 的 dept-pm 分支(L86-92)语义一致:
   * "提交者 dept 在 managed_dept_codes 内" 也算可见/可审。
   * 优先级:project.deptCode 优先(主规范口径),submitter.deptCode 兜底。
   */
  const canApproveOvertime = (projectId: string, submitterId?: string): boolean => {
    if (isAdmin()) return true;

    // 2026-06-14: 项目经理的加班申请 — 只有同部门 dept-project-manager 能审批,
    // project-owner 也无法审批其他 project-manager 的加班申请
    const submitter = submitterId ? userStore.userById(submitterId) : null;
    const submitterIsProjectManager = submitter?.role === 'project-manager';

    if (submitterIsProjectManager) {
      // 只有同部门 dept-project-manager 或 admin(已在上方返回) 能审批
      return isDeptProjectManager() && !!submitter?.deptCode && isDeptManager(submitter.deptCode);
    }

    // —— 以下是"非项目经理提交者"的正常逻辑 ——

    // project-manager / project-owner: 可以审批自己负责/管理项目的加班记录
    if (isProjectOwner(projectId)) return true;
    // 2026-06-14: 项目经理可通过 managed_project_ids 管辖项目
    if (isProjectManager() && isManagedProject(projectId)) return true;
    // dept-project-manager(2026-06-14): 提交者所属部门在管辖部门范围内即可审批
    if (isDeptProjectManager() && submitter && submitter.deptCode && isDeptManager(submitter.deptCode)) {
      return true;
    }
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
    managedProjectIds,
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
    isProjectCreator,
    isProjectMember,
    isManagedProject,
    canViewProject,
    canEditProject,
    canDeleteProject,
    canManageProjectMembers,
    canCreateProject,
    canManageTaskContent,
    canCreateTask,
    canAddSubtask,
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
