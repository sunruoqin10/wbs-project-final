<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { usePermissionStore } from '@/stores/permission';
import apiService from '@/services/api';
import type { OrgNode, RoleChangeRequest, RoleChangeLog, UserRole, Project } from '@/types';

interface Props {
  visible: boolean;
  userId: string;
  currentRole: string;
  currentManagedDeptCodes?: string[];
  currentManagedCompanyCd?: string;
  /** 员工所属公司代码，作为管辖公司的默认值 */
  userCompanyCd?: string;
  /** 员工所属部门代码，作为管辖部门的默认值 */
  userDeptCode?: string;
  currentManagedProjectIds?: string[];
  /** 当前 role 是否由 HR 同步按 JPSTN_CD 自动推断(2026-06-16) */
  currentRoleAutoInferred?: boolean;
}
const props = withDefaults(defineProps<Props>(), {
  currentManagedDeptCodes: () => [],
  currentManagedCompanyCd: '',
  userCompanyCd: '',
  userDeptCode: '',
  currentManagedProjectIds: () => [],
  currentRoleAutoInferred: false,   // 默认 false(不影响现有调用方)
});
const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void;
  (e: 'success'): void;
}>();

const { t } = useI18n();
const permissionStore = usePermissionStore();

const newRole = ref<string>(props.currentRole);
const managedDeptCodes = ref<string[]>([...props.currentManagedDeptCodes]);
const managedCompanyCd = ref<string>(props.currentManagedCompanyCd);
const managedProjectIds = ref<string[]>([...props.currentManagedProjectIds]);
const reason = ref<string>('');
const loading = ref(false);
const errorMsg = ref('');

const orgTree = ref<OrgNode | null>(null);
const orgLoading = ref(false);
const orgError = ref<string>('');

const history = ref<RoleChangeLog[]>([]);
const historyLoading = ref(false);
const showHistory = ref(false);

const projectsInManagedDepts = ref<Project[]>([]);
const projectsLoading = ref(false);

// ========== 权限(2026-06-12) ==========
// actor: 谁打开这个对话框
// - admin: 任意目标,可改成任意角色
// - dept-pm: 仅本部门目标,只能改成 {project-manager, member, viewer}
const isAdminActor = computed(() => permissionStore.isAdmin());
const isDeptPmActor = computed(() => permissionStore.isDeptProjectManager());
const canChangeRole = computed(() => isAdminActor.value || isDeptPmActor.value);
// 候选角色
const availableRoles = computed<Array<{ value: UserRole; label: string; disabled?: boolean; hint?: string }>>(() => {
  if (isAdminActor.value) {
    return [
      { value: 'admin', label: t('roles.admin') },
      { value: 'dept-project-manager', label: t('roles.deptProjectManager') },
      { value: 'project-manager', label: t('roles.projectManager') },
      { value: 'member', label: t('roles.member') },
      { value: 'viewer', label: t('roles.viewer') }
    ];
  }
  if (isDeptPmActor.value) {
    return [
      { value: 'project-manager', label: t('roles.projectManager') },
      { value: 'member', label: t('roles.member') },
      { value: 'viewer', label: t('roles.viewer') }
    ];
  }
  return [];
});

const showManagedDeptFields = computed(() => newRole.value === 'dept-project-manager');
const showManagedProjectFields = computed(() => newRole.value === 'project-manager');

const flatDepts = computed(() => {
  const result: { code: string; name: string; companyCd?: string }[] = [];
  const walk = (node: OrgNode | null) => {
    if (!node) return;
    result.push({ code: node.code, name: node.name, companyCd: node.companyCd });
    if (node.children?.length) {
      node.children.forEach(walk);
    }
  };
  walk(orgTree.value);
  return result;
});

const managedCompanyName = computed(() => {
  if (!managedCompanyCd.value || !orgTree.value?.children) return managedCompanyCd.value;
  const company = orgTree.value.children.find(c => c.code === managedCompanyCd.value);
  return company?.name || managedCompanyCd.value;
});

const managedDeptNames = computed(() => {
  return managedDeptCodes.value.map(code => {
    const dept = flatDepts.value.find(d => d.code === code);
    return dept?.name || code;
  });
});

const managedProjectNames = computed(() => {
  return managedProjectIds.value.map(id => {
    const p = projectsInManagedDepts.value.find(p => p.id === id);
    return p?.name || id;
  });
});

watch(() => props.visible, async (v) => {
  if (v) {
    newRole.value = props.currentRole;
    managedDeptCodes.value = props.currentManagedDeptCodes.length > 0
      ? [...props.currentManagedDeptCodes]
      : (props.userDeptCode ? [props.userDeptCode] : []);
    managedCompanyCd.value = props.currentManagedCompanyCd || props.userCompanyCd;
    managedProjectIds.value = [...props.currentManagedProjectIds];
    reason.value = '';
    errorMsg.value = '';
    showHistory.value = false;
    await loadOrgTree();
    if (isAdminActor.value) {
      await loadHistory();
    }
    // 若当前角色是 PM,加载他所属部门下的项目供"指派项目"用
    if (props.currentRole === 'project-manager' || newRole.value === 'project-manager') {
      await loadProjectsInActorManagedDepts();
    }
  }
});

const loadOrgTree = async () => {
  orgError.value = '';
  try {
    orgLoading.value = true;
    const tree = await apiService.getOrgTree();
    orgTree.value = tree;
    if (!tree) {
      orgError.value = '后端返回的组织树为空';
    }
  } catch (e: any) {
    console.error('加载组织树失败', e);
    orgError.value = e?.message || '加载组织树失败,请检查后端是否启动';
  } finally {
    orgLoading.value = false;
  }
};

const loadHistory = async () => {
  try {
    historyLoading.value = true;
    history.value = await apiService.getRoleChangeHistory(props.userId);
  } catch (e) {
    console.error('加载历史失败', e);
  } finally {
    historyLoading.value = false;
  }
};

/**
 * 加载 actor 管辖部门下的所有项目(2026-06-12 PM 项目分配用)
 * 数据源:
 *   - admin: 全部项目
 *   - dept-pm: managed_dept_codes 下的项目
 */
const loadProjectsInActorManagedDepts = async () => {
  try {
    projectsLoading.value = true;
    const allProjects = await apiService.getProjects();
    if (isAdminActor.value) {
      projectsInManagedDepts.value = allProjects;
    } else {
      const codes = permissionStore.managedDeptCodes;
      projectsInManagedDepts.value = allProjects.filter(p => p.deptCode && codes.includes(p.deptCode));
    }
  } catch (e) {
    console.error('加载项目列表失败', e);
    projectsInManagedDepts.value = [];
  } finally {
    projectsLoading.value = false;
  }
};

const toggleProject = (projectId: string) => {
  const idx = managedProjectIds.value.indexOf(projectId);
  if (idx >= 0) {
    managedProjectIds.value.splice(idx, 1);
  } else {
    managedProjectIds.value.push(projectId);
  }
};

const isProjectSelected = (projectId: string): boolean => {
  return managedProjectIds.value.includes(projectId);
};

const handleSubmit = async () => {
  if (!canChangeRole.value) {
    errorMsg.value = '您无权修改该用户角色';
    return;
  }
  if (newRole.value === 'dept-project-manager') {
    if (managedDeptCodes.value.length === 0) {
      errorMsg.value = '部门项目负责人必须指定至少一个管辖部门';
      return;
    }
    if (!managedCompanyCd.value) {
      errorMsg.value = '部门项目负责人必须指定管辖公司';
      return;
    }
  }
  errorMsg.value = '';
  loading.value = true;
  try {
    const req: RoleChangeRequest = {
      newRole: newRole.value as UserRole,
      managedDeptCodes: newRole.value === 'dept-project-manager' ? managedDeptCodes.value : undefined,
      managedCompanyCd: newRole.value === 'dept-project-manager' ? managedCompanyCd.value : undefined,
      managedProjectIds: newRole.value === 'project-manager' ? managedProjectIds.value : undefined,
      reason: reason.value || undefined,
    };
    await apiService.changeUserRole(props.userId, req);
    emit('success');
    emit('update:visible', false);
  } catch (e: any) {
    errorMsg.value = e?.message || '角色变更失败';
  } finally {
    loading.value = false;
  }
};

const handleCancel = () => {
  emit('update:visible', false);
};

const formatTime = (iso: string): string => {
  try {
    return new Date(iso).toLocaleString();
  } catch {
    return iso;
  }
};
</script>

<template>
  <div v-if="visible" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" @click.self="handleCancel">
    <div class="w-full max-w-2xl rounded-lg bg-white shadow-xl max-h-[90vh] flex flex-col">
      <!-- Header -->
      <div class="flex items-center justify-between border-b border-secondary-200 px-6 py-4">
        <h3 class="text-lg font-semibold text-secondary-900">
          {{ isAdminActor ? t('team.roleChange.title') : '部门内变更角色' }}
        </h3>
        <button class="text-secondary-400 hover:text-secondary-600" @click="handleCancel">
          <svg class="h-6 w-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M12 12m12 6l-12-12" />
          </svg>
        </button>
      </div>

      <!-- Body -->
      <div class="overflow-y-auto px-6 py-4 space-y-4">
        <div v-if="!canChangeRole" class="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700">
          您无权修改该用户角色
        </div>
        <div class="rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-800">
          {{ t('team.roleChange.warning') }}
        </div>
        <div
          v-if="currentRoleAutoInferred"
          class="rounded-lg border border-blue-200 bg-blue-50 p-3 text-sm text-blue-800"
        >
          <span class="font-medium">{{ t('team.roleSource.autoInferred') }}:</span>
          {{ t('team.roleSource.autoInferredHint') }}
        </div>

        <!-- 新角色 -->
        <div>
          <label class="mb-1 block text-sm font-medium text-secondary-700">
            {{ t('team.roleChange.newRoleLabel') }}
          </label>
          <select
            v-model="newRole"
            :disabled="!canChangeRole"
            class="w-full rounded-lg border border-secondary-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500 disabled:bg-secondary-50"
          >
            <option v-for="r in availableRoles" :key="r.value" :value="r.value">{{ r.label }}</option>
          </select>
          <p v-if="isDeptPmActor" class="mt-1 text-xs text-secondary-500">
            您只能将本部门用户改成"项目经理 / 项目人员 / 观察者",不能改成管理员或其他部门的用户。
          </p>
        </div>

        <!-- 管辖公司(dept-pm 必填) -->
        <div v-if="showManagedDeptFields">
          <label class="mb-1 block text-sm font-medium text-secondary-700">
            {{ t('team.roleChange.managedCompanyCdLabel') }}
          </label>
          <input
            :value="managedCompanyName"
            type="text"
            readonly
            class="w-full rounded-lg border border-secondary-200 bg-secondary-50 px-3 py-2 text-sm text-secondary-600 cursor-not-allowed"
          />
          <p class="mt-1 text-xs text-secondary-400">管辖公司自动设为员工所属公司，不可修改</p>
        </div>

        <!-- 管辖部门(dept-pm 必填) -->
        <div v-if="showManagedDeptFields">
          <label class="mb-1 block text-sm font-medium text-secondary-700">
            {{ t('team.roleChange.managedDeptCodesLabel') }}
          </label>
          <div
            class="w-full rounded-lg border border-secondary-200 bg-secondary-50 px-3 py-2 text-sm text-secondary-600"
          >
            <template v-if="managedDeptNames.length === 0">-</template>
            <template v-else>
              <span v-for="(name, i) in managedDeptNames" :key="i">
                {{ name }}<span v-if="i < managedDeptNames.length - 1">, </span>
              </span>
            </template>
          </div>
          <p class="mt-1 text-xs text-secondary-400">管辖部门自动设为员工所属部门，不可修改</p>
        </div>

        <!-- 管辖项目(PM 角色多选,2026-06-12) -->
        <div v-if="showManagedProjectFields">
          <label class="mb-1 block text-sm font-medium text-secondary-700">
            管辖项目(PM 权限范围)
          </label>
          <p class="mb-2 text-xs text-secondary-500">
            勾选该 PM 可管理的项目;PM 之间互不可见,每个 PM 只看到自己勾选的项目
          </p>
          <div
            v-if="projectsLoading"
            class="rounded-lg border border-secondary-200 bg-secondary-50 px-3 py-2 text-sm text-secondary-500"
          >
            加载项目列表中...
          </div>
          <div
            v-else-if="projectsInManagedDepts.length === 0"
            class="rounded-lg border border-secondary-200 bg-secondary-50 px-3 py-2 text-sm text-secondary-500"
          >
            您管辖的部门下暂无项目
          </div>
          <div
            v-else
            class="max-h-48 overflow-y-auto rounded-lg border border-secondary-200 bg-white p-2"
          >
            <label
              v-for="p in projectsInManagedDepts"
              :key="p.id"
              class="flex items-center gap-2 rounded px-2 py-1.5 text-sm hover:bg-secondary-50 cursor-pointer"
            >
              <input
                type="checkbox"
                :checked="isProjectSelected(p.id)"
                @change="toggleProject(p.id)"
                class="rounded border-secondary-300 text-primary-600"
              />
              <span class="text-secondary-900">{{ p.name }}</span>
              <span class="text-xs text-secondary-400">({{ p.deptCode }})</span>
            </label>
          </div>
          <p v-if="managedProjectNames.length > 0" class="mt-1 text-xs text-secondary-500">
            已选: {{ managedProjectNames.join('、') }}
          </p>
        </div>

        <!-- 变更原因 -->
        <div>
          <label class="mb-1 block text-sm font-medium text-secondary-700">
            {{ t('team.roleChange.reasonLabel') }}
          </label>
          <textarea
            v-model="reason"
            :placeholder="t('team.roleChange.reasonPlaceholder')"
            rows="3"
            class="w-full rounded-lg border border-secondary-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
          />
        </div>

        <div v-if="errorMsg" class="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700">
          {{ errorMsg }}
        </div>

        <!-- 变更历史(仅 admin 可见) -->
        <div v-if="isAdminActor">
          <button
            type="button"
            class="text-sm text-primary-600 hover:text-primary-700"
            @click="showHistory = !showHistory"
          >
            {{ showHistory ? '隐藏' : '查看' }}{{ t('team.roleChange.historyTitle') }}
          </button>
          <div v-if="showHistory" class="mt-2">
            <div v-if="historyLoading" class="py-2 text-sm text-secondary-500">加载中…</div>
            <div v-else-if="history.length === 0" class="py-2 text-sm text-secondary-500">
              {{ t('team.roleChange.noHistory') }}
            </div>
            <table v-else class="w-full text-xs">
              <thead class="bg-secondary-50 text-secondary-600">
                <tr>
                  <th class="px-2 py-1 text-left">{{ t('team.roleChange.changedAt') }}</th>
                  <th class="px-2 py-1 text-left">{{ t('team.roleChange.changedBy') }}</th>
                  <th class="px-2 py-1 text-left">{{ t('team.roleChange.oldRole') }}</th>
                  <th class="px-2 py-1 text-left">{{ t('team.roleChange.newRole') }}</th>
                  <th class="px-2 py-1 text-left">{{ t('team.roleChange.reason') }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="h in history" :key="h.id" class="border-t border-secondary-100">
                  <td class="px-2 py-1">{{ formatTime(h.changedAt) }}</td>
                  <td class="px-2 py-1 font-mono">{{ h.changedBy }}</td>
                  <td class="px-2 py-1">{{ h.oldRole }}</td>
                  <td class="px-2 py-1">{{ h.newRole }}</td>
                  <td class="px-2 py-1">{{ h.reason || '-' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- Footer -->
      <div class="flex justify-end gap-2 border-t border-secondary-200 px-6 py-4">
        <button
          type="button"
          class="rounded-lg border border-secondary-300 px-4 py-2 text-sm font-medium text-secondary-700 hover:bg-secondary-50"
          @click="handleCancel"
        >
          取消
        </button>
        <button
          type="button"
          :disabled="loading || !canChangeRole"
          class="rounded-lg bg-primary-600 px-4 py-2 text-sm font-medium text-white hover:bg-primary-700 disabled:opacity-50"
          @click="handleSubmit"
        >
          {{ loading ? '提交中…' : t('team.roleChange.confirm') }}
        </button>
      </div>
    </div>
  </div>
</template>
