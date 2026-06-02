<template>
  <MainLayout>
    <div class="space-y-6">
      <div>
        <h1 class="text-2xl font-bold text-secondary-900">{{ $t('scheduler.title') }}</h1>
        <p class="mt-1 text-sm text-secondary-600">{{ $t('scheduler.subtitle') }}</p>
      </div>

      <div v-if="loading" class="flex items-center justify-center py-12">
        <p class="text-secondary-500">{{ $t('common.loading') }}</p>
      </div>

      <div v-else-if="configs.length === 0" class="text-center py-12 text-secondary-500">
        {{ $t('scheduler.empty') }}
      </div>

      <div v-else class="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <Card v-for="config in configs" :key="config.id">
          <div class="space-y-4">
            <div class="flex items-start justify-between">
              <div>
                <h3 class="text-lg font-semibold text-secondary-900">{{ config.name }}</h3>
                <p class="mt-1 text-sm text-secondary-500">{{ config.description }}</p>
              </div>
              <Badge :variant="config.enabled ? 'success' : 'secondary'">
                {{ config.enabled ? $t('scheduler.running') : $t('scheduler.stopped') }}
              </Badge>
            </div>

            <div class="rounded-lg bg-secondary-50 p-3">
              <div class="flex items-center justify-between">
                <span class="text-sm text-secondary-500">Cron</span>
                <code class="rounded bg-white px-2 py-1 text-sm font-mono text-primary-700 border border-secondary-200">
                  {{ config.cronExpression }}
                </code>
              </div>
            </div>

            <div class="grid grid-cols-2 gap-4 text-sm">
              <div>
                <span class="text-secondary-500">{{ $t('scheduler.lastRun') }}:</span>
                <span class="ml-1 text-secondary-700">{{ formatTime(config.lastRunTime) }}</span>
              </div>
              <div>
                <span class="text-secondary-500">{{ $t('scheduler.nextRun') }}:</span>
                <span class="ml-1 text-secondary-700">{{ formatTime(config.nextRunTime) }}</span>
              </div>
            </div>

            <div class="flex items-center gap-2 pt-2 border-t border-secondary-100">
              <Button
                v-if="config.enabled"
                variant="secondary"
                size="sm"
                @click="handleStop(config.id)"
                :loading="actionLoading === config.id"
              >
                {{ $t('scheduler.stop') }}
              </Button>
              <Button
                v-else
                variant="primary"
                size="sm"
                @click="handleStart(config.id)"
                :loading="actionLoading === config.id"
              >
                {{ $t('scheduler.start') }}
              </Button>
              <Button
                variant="secondary"
                size="sm"
                @click="handleTrigger(config.id)"
                :loading="actionLoading === config.id"
              >
                {{ $t('scheduler.triggerNow') }}
              </Button>
              <Button
                variant="secondary"
                size="sm"
                @click="openEdit(config)"
              >
                {{ $t('scheduler.edit') }}
              </Button>
            </div>
          </div>
        </Card>
      </div>
    </div>

    <Modal :open="editModalOpen" :title="$t('scheduler.edit')" size="lg" @close="editModalOpen = false">
      <div class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-secondary-700 mb-1">{{ $t('scheduler.cronExpression') }}</label>
          <CronBuilder v-model="editForm.cronExpression!" />
        </div>
        <div class="flex justify-end gap-2">
          <Button variant="secondary" @click="editModalOpen = false">{{ $t('common.cancel') }}</Button>
          <Button variant="primary" @click="handleSave" :loading="saving">{{ $t('common.save') }}</Button>
        </div>
      </div>
    </Modal>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import MainLayout from '@/components/layout/MainLayout.vue';
import Card from '@/components/common/Card.vue';
import Button from '@/components/common/Button.vue';
import Badge from '@/components/common/Badge.vue';
import Modal from '@/components/common/Modal.vue';
import CronBuilder from '@/components/scheduler/CronBuilder.vue';
import apiService from '@/services/api';
import type { SchedulerConfig } from '@/types/scheduler';
import dayjs from 'dayjs';

const { t } = useI18n();

const configs = ref<SchedulerConfig[]>([]);
const loading = ref(false);
const actionLoading = ref<string | null>(null);
const editModalOpen = ref(false);
const saving = ref(false);
const editForm = ref<Partial<SchedulerConfig>>({});
const currentEditId = ref('');

function formatTime(time?: string) {
  if (!time) return t('scheduler.never');
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss');
}

async function loadConfigs() {
  loading.value = true;
  try {
    configs.value = await apiService.getSchedulerConfigs();
  } catch (e) {
    console.error('Failed to load scheduler configs', e);
  } finally {
    loading.value = false;
  }
}

async function handleStart(id: string) {
  actionLoading.value = id;
  try {
    await apiService.startScheduler(id);
    await loadConfigs();
  } finally {
    actionLoading.value = null;
  }
}

async function handleStop(id: string) {
  actionLoading.value = id;
  try {
    await apiService.stopScheduler(id);
    await loadConfigs();
  } finally {
    actionLoading.value = null;
  }
}

async function handleTrigger(id: string) {
  actionLoading.value = id;
  try {
    await apiService.triggerScheduler(id);
    await loadConfigs();
  } finally {
    actionLoading.value = null;
  }
}

function openEdit(config: SchedulerConfig) {
  currentEditId.value = config.id;
  editForm.value = {
    cronExpression: config.cronExpression,
    enabled: config.enabled,
  };
  editModalOpen.value = true;
}

async function handleSave() {
  saving.value = true;
  try {
    await apiService.updateSchedulerConfig(currentEditId.value, editForm.value);
    editModalOpen.value = false;
    await loadConfigs();
  } finally {
    saving.value = false;
  }
}

onMounted(() => {
  loadConfigs();
});
</script>
