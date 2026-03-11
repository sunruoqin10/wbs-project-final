<template>
  <div class="space-y-4">
    <div
      v-for="comment in comments"
      :key="comment.id"
      class="flex gap-3 rounded-lg bg-secondary-50 p-4"
    >
      <img
        :src="getAvatarUrl(comment.userId)"
        :alt="getUserName(comment.userId)"
        class="h-10 w-10 flex-shrink-0 rounded-full"
      />
      <div class="flex-1 min-w-0">
        <div class="flex items-center justify-between mb-1">
          <span class="font-medium text-secondary-900">{{ getUserName(comment.userId) }}</span>
          <div class="flex items-center gap-2">
            <span class="text-xs text-secondary-500">{{ formatTime(comment.createdAt) }}</span>
            <button
              v-if="canDelete(comment)"
              @click="$emit('delete', comment.id)"
              class="text-xs text-secondary-400 hover:text-danger-600"
              :title="$t('common.delete')"
            >
              {{ $t('common.delete') }}
            </button>
          </div>
        </div>
        <p class="text-sm text-secondary-700">{{ comment.content }}</p>
      </div>
    </div>
    <div v-if="comments.length === 0" class="py-8 text-center text-secondary-500">
      {{ $t('weeklyReports.comments.noComments') }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
import type { WeeklyReportComment } from '@/types';
import { useUserStore } from '@/stores/user';
import dayjs from 'dayjs';
import 'dayjs/locale/zh-cn';

dayjs.locale('zh-cn');

const { t } = useI18n();
const userStore = useUserStore();

const props = defineProps<{
  comments: WeeklyReportComment[];
}>();

const emit = defineEmits<{
  delete: [commentId: string];
}>();

const canDelete = (comment: WeeklyReportComment) => {
  return userStore.currentUserId === comment.userId || userStore.currentRole === 'admin';
};

const getAvatarUrl = (userId: string) => {
  const user = userStore.users.find(u => u.id === userId);
  return user?.avatar || '/default-avatar.png';
};

const getUserName = (userId: string) => {
  const user = userStore.users.find(u => u.id === userId);
  return user?.name || t('common.unknown');
};

const formatTime = (time: string) => {
  if (!time) return '';
  return dayjs(time).format('YYYY-MM-DD HH:mm');
};
</script>
