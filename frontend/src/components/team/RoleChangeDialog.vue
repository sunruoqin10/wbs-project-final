<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { usePermissionStore } from '@/stores/permission';
import apiService from '@/services/api';
import type { OrgNode, RoleChangeRequest, RoleChangeLog, UserRole } from '@/types';

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
}
const props = withDefaults(defineProps<Props>(), {
  currentManagedDeptCodes: () => [],
  currentManagedCompanyCd: '',
  userCompanyCd: '',
  userDeptCode: '',
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
const reason = ref<string>('');
const loading = ref(false);
const errorMsg = ref('');

const orgTree = ref<OrgNode | null>(null);
const orgLoading = ref(false);
const orgError = ref<string>('');

const history = ref<RoleChangeLog[]>([]);
const historyLoading = ref(false);
const showHistory = ref(false);

// 候选部门(扁平化 org 树,getOrgTree 返回单根节点,递归走 children)
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

const showManagedFields = computed(() => newRole.value === 'dept-project-manager');

// 管辖公司显示名称（从组织树顶层节点查找）
const managedCompanyName = computed(() => {
  if (!managedCompanyCd.value || !orgTree.value?.children) return managedCompanyCd.value;
  const company = orgTree.value.children.find(c => c.code === managedCompanyCd.value);
  return company?.name || managedCompanyCd.value;
});

// 管辖部门显示名称（从组织树查找）
const managedDeptNames = computed(() => {
  return managedDeptCodes.value.map(code => {
    const dept = flatDepts.value.find(d => d.code === code);
    return dept?.name || code;
  });
});

const isAdmin = computed(() => permissionStore.isAdmin());

watch(() => props.visible, async (v) => {
  if (v) {
    newRole.value = props.currentRole;
    managedDeptCodes.value = props.currentManagedDeptCodes.length > 0
      ? [...props.currentManagedDeptCodes]
      : (props.userDeptCode ? [props.userDeptCode] : []);
    managedCompanyCd.value = props.currentManagedCompanyCd || props.userCompanyCd;
    reason.value = '';
    errorMsg.value = '';
    showHistory.value = false;
    await loadOrgTree();
    if (isAdmin.value) {
      await loadHistory();
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

const handleSubmit = async () => {
  if (!isAdmin.value) {
    errorMsg.value = '仅管理员可修改角色';
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
        <h3 class="text-lg font-semibold text-secondary-900">{{ t('team.roleChange.title') }}</h3>
        <button class="text-secondary-400 hover:text-secondary-600" @click="handleCancel">
          <svg class="h-6 w-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <!-- Body -->
      <div class="overflow-y-auto px-6 py-4 space-y-4">
        <div v-if="!isAdmin" class="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700">
          仅管理员可修改角色
        </div>
        <div class="rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-800">
          {{ t('team.roleChange.warning') }}
        </div>

        <!-- 新角色 -->
        <div>
          <label class="mb-1 block text-sm font-medium text-secondary-700">
            {{ t('team.roleChange.newRoleLabel') }}
          </label>
          <select
            v-model="newRole"
            :disabled="!isAdmin"
            class="w-full rounded-lg border border-secondary-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500 disabled:bg-secondary-50"
          >
            <option value="admin">{{ t('roles.admin') }}</option>
            <option value="dept-project-manager">{{ t('roles.deptProjectManager') }}</option>
            <option value="project-manager" disabled>{{ t('roles.projectManager') }} (已废弃)</option>
            <option value="member">{{ t('roles.member') }}</option>
            <option value="viewer">{{ t('roles.viewer') }}</option>
          </select>
        </div>

        <!-- 管辖公司 -->
        <div v-if="showManagedFields">
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

        <!-- 管辖部门 -->
        <div v-if="showManagedFields">
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

        <!-- 变更历史 -->
        <div>
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
          :disabled="loading || !isAdmin"
          class="rounded-lg bg-primary-600 px-4 py-2 text-sm font-medium text-white hover:bg-primary-700 disabled:opacity-50"
          @click="handleSubmit"
        >
          {{ loading ? '提交中…' : t('team.roleChange.confirm') }}
        </button>
      </div>
    </div>
  </div>
</template>
