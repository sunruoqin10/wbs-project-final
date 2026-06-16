<template>
  <div
    v-if="props.visible"
    class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
    @click.self="handleCancel"
  >
    <div class="w-full max-w-2xl rounded-lg bg-white shadow-xl max-h-[90vh] flex flex-col">
      <!-- Header -->
      <div class="flex items-center justify-between border-b border-secondary-200 px-6 py-4">
        <h3 class="text-lg font-semibold text-secondary-900">
          {{ t('handover.deptPmTitle') }}
        </h3>
        <button
          class="rounded p-1 text-secondary-400 hover:bg-secondary-100 hover:text-secondary-600"
          @click="handleCancel"
        >
          <svg class="h-6 w-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <!-- Body -->
      <div class="overflow-y-auto px-6 py-4 space-y-4">
        <div v-if="loading" class="py-4 text-sm text-secondary-500">
          {{ t('common.loading') }}
        </div>
        <template v-else>
          <div class="rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-800">
            {{ t('handover.outgoingLabel') }}: {{ preview?.outgoing?.name || '-' }}
          </div>

          <!-- 部门范围 -->
          <div>
            <label class="mb-2 block text-sm font-medium text-secondary-700">
              {{ t('handover.deptScope') }}
            </label>
            <div
              v-if="(preview?.candidateDeptCodes || []).length === 0"
              class="rounded-lg border border-secondary-200 bg-secondary-50 px-3 py-2 text-sm text-secondary-500"
            >
              -
            </div>
            <div
              v-else
              class="max-h-48 overflow-y-auto rounded-lg border border-secondary-200 bg-white p-2"
            >
              <label
                v-for="code in preview?.candidateDeptCodes"
                :key="code"
                class="flex items-center gap-2 rounded px-2 py-1.5 text-sm hover:bg-secondary-50 cursor-pointer"
              >
                <input
                  type="checkbox"
                  :checked="isDeptSelected(code)"
                  @change="toggleDept(code)"
                  class="rounded border-secondary-300 text-primary-600"
                />
                <span class="text-secondary-900">{{ code }}</span>
              </label>
            </div>
            <p v-if="selectedDeptCodes.length > 0" class="mt-1 text-xs text-secondary-500">
              {{ t('handover.selectedCount', { count: selectedDeptCodes.length }) }}
            </p>
          </div>

          <!-- 继任者 -->
          <div>
            <label class="mb-1 block text-sm font-medium text-secondary-700">
              {{ t('handover.successorLabel') }}
            </label>
            <select
              v-model="successorId"
              class="w-full rounded-lg border border-secondary-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
            >
              <option value="">{{ t('handover.successorPlaceholder') }}</option>
              <option
                v-for="opt in props.successorOptions"
                :key="opt.value"
                :value="opt.value"
              >
                {{ opt.label }}
              </option>
            </select>
          </div>

          <!-- 原因 -->
          <div>
            <label class="mb-1 block text-sm font-medium text-secondary-700">
              {{ t('handover.reasonLabel') }}
            </label>
            <textarea
              v-model="reason"
              :placeholder="t('handover.reasonPlaceholder')"
              rows="3"
              class="w-full rounded-lg border border-secondary-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
            />
          </div>

          <div v-if="errorMsg" class="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700">
            {{ errorMsg }}
          </div>
        </template>
      </div>

      <!-- Footer -->
      <div class="flex justify-end gap-2 border-t border-secondary-200 px-6 py-4">
        <button
          type="button"
          class="rounded-lg border border-secondary-300 px-4 py-2 text-sm font-medium text-secondary-700 hover:bg-secondary-50"
          @click="handleCancel"
        >
          {{ t('common.cancel') }}
        </button>
        <button
          type="button"
          :disabled="loading || submitting"
          class="rounded-lg bg-primary-600 px-4 py-2 text-sm font-medium text-white hover:bg-primary-700 disabled:opacity-50"
          @click="handleConfirm"
        >
          {{ submitting ? t('common.loading') : t('common.confirm') }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { handoverService } from '@/services/handoverService';
import type { HandoverPreviewResponse, DeptPmHandoverResponse } from '@/types/handover';

const { t } = useI18n();

/**
 * Dept-PM 交接对话框(2026-06-16 PM/Dept-PM 变更方案)
 *
 * Props:
 *   - visible           : 控制显示
 *   - userId            : 离任 Dept-PM 用户 ID
 *   - successorOptions  : 候选继任者下拉列表 [{ value, label }]
 *
 * Emits:
 *   - update:visible    : 请求父组件关闭
 *   - success           : 提交成功,带 DeptPmHandoverResponse
 */
const props = defineProps<{
  visible: boolean;
  userId: string;
  successorOptions: Array<{ value: string; label: string }>;
}>();

const emit = defineEmits<{
  (e: 'update:visible', v: boolean): void;
  (e: 'success', r: DeptPmHandoverResponse): void;
}>();

const loading = ref(false);
const submitting = ref(false);
const errorMsg = ref('');
const preview = ref<HandoverPreviewResponse | null>(null);
const selectedDeptCodes = ref<string[]>([]);
const successorId = ref<string>('');
const reason = ref<string>('');

watch(
  () => props.visible,
  async v => {
    if (!v) return;
    errorMsg.value = '';
    successorId.value = '';
    reason.value = '';
    selectedDeptCodes.value = [];
    preview.value = null;
    loading.value = true;
    try {
      const r = await handoverService.preview(props.userId, 'DEPT_PM_HANDOVER');
      preview.value = r;
      // 默认勾选所有 candidateDeptCodes
      selectedDeptCodes.value = [...(r?.candidateDeptCodes || [])];
    } catch (e: any) {
      errorMsg.value = e?.message || t('handover.errorMessage');
    } finally {
      loading.value = false;
    }
  }
);

const isDeptSelected = (code: string): boolean => {
  return selectedDeptCodes.value.includes(code);
};

const toggleDept = (code: string) => {
  const idx = selectedDeptCodes.value.indexOf(code);
  if (idx >= 0) {
    selectedDeptCodes.value.splice(idx, 1);
  } else {
    selectedDeptCodes.value.push(code);
  }
};

const handleCancel = () => {
  emit('update:visible', false);
};

const handleConfirm = async () => {
  if (submitting.value) return;
  if (!successorId.value) {
    errorMsg.value = t('handover.validation.needSuccessor');
    return;
  }
  if (selectedDeptCodes.value.length === 0) {
    errorMsg.value = t('handover.validation.needDept');
    return;
  }
  errorMsg.value = '';
  submitting.value = true;
  try {
    const r = await handoverService.handoverDeptPm(props.userId, {
      handoverType: 'DEPT_PM_HANDOVER',
      successorUserId: successorId.value,
      deptCodes: [...selectedDeptCodes.value],
      reason: reason.value || undefined,
    });
    emit('success', r);
    emit('update:visible', false);
  } catch (e: any) {
    errorMsg.value = e?.message || t('handover.errorMessage');
  } finally {
    submitting.value = false;
  }
};
</script>