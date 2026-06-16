<template>
  <div class="space-y-3">
    <div v-if="loading" class="py-4 text-sm text-secondary-500">
      {{ t('common.loading') }}
    </div>
    <div v-else-if="records.length === 0" class="py-4 text-sm text-secondary-500">
      {{ t('handover.history.empty') }}
    </div>
    <table v-else class="w-full text-sm">
      <thead class="bg-secondary-50 text-secondary-600">
        <tr>
          <th class="px-3 py-2 text-left font-medium">
            {{ t('handover.history.col.createdAt') }}
          </th>
          <th class="px-3 py-2 text-left font-medium">
            {{ t('handover.history.col.handoverType') }}
          </th>
          <th class="px-3 py-2 text-left font-medium">
            {{ t('handover.history.col.projectName') }}
          </th>
          <th class="px-3 py-2 text-left font-medium">
            {{ t('handover.history.col.fromUserName') }}
          </th>
          <th class="px-3 py-2 text-left font-medium">
            {{ t('handover.history.col.toUserName') }}
          </th>
          <th class="px-3 py-2 text-left font-medium">
            {{ t('handover.history.col.reason') }}
          </th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="r in records"
          :key="r.id"
          class="border-t border-secondary-100 hover:bg-secondary-50"
        >
          <td class="px-3 py-2 text-secondary-700">{{ formatTime(r.createdAt) }}</td>
          <td class="px-3 py-2">
            <span class="rounded-full bg-secondary-100 px-2 py-0.5 text-xs text-secondary-700">
              {{ formatType(r.handoverType) }}
            </span>
          </td>
          <td class="px-3 py-2 text-secondary-900">{{ r.projectName || '-' }}</td>
          <td class="px-3 py-2 text-secondary-700">{{ r.fromUserName || '-' }}</td>
          <td class="px-3 py-2 text-secondary-700">{{ r.toUserName || '-' }}</td>
          <td class="px-3 py-2 text-secondary-600">{{ r.reason || '-' }}</td>
        </tr>
      </tbody>
    </table>
    <div v-if="errorMsg" class="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700">
      {{ errorMsg }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { handoverService } from '@/services/handoverService';
import type { ProjectHandoverLogDTO } from '@/types/handover';

/**
 * 某用户的项目交接历史 Tab(2026-06-16 PM/Dept-PM 变更方案)
 * 读取式表格,首次挂载 / userId 变更时拉取。
 */
const props = defineProps<{ userId: string }>();

const { t } = useI18n();

const records = ref<ProjectHandoverLogDTO[]>([]);
const loading = ref(false);
const errorMsg = ref('');

const load = async () => {
  if (!props.userId) return;
  loading.value = true;
  errorMsg.value = '';
  try {
    const r = await handoverService.history(props.userId, { pageSize: 50 });
    records.value = r.records || [];
  } catch (e: any) {
    errorMsg.value = e?.message || t('handover.errorMessage');
    records.value = [];
  } finally {
    loading.value = false;
  }
};

onMounted(load);

watch(
  () => props.userId,
  () => {
    records.value = [];
    load();
  }
);

const formatTime = (iso: string): string => {
  if (!iso) return '-';
  try {
    return new Date(iso).toLocaleString();
  } catch {
    return iso;
  }
};

const formatType = (t: string): string => {
  if (t === 'PM_HANDOVER') return 'PM';
  if (t === 'DEPT_PM_HANDOVER') return 'Dept-PM';
  return t;
};
</script>