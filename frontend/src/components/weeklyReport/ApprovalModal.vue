<template>
  <div v-if="open" class="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50" @click="$emit('close')">
    <Card class="relative w-full max-w-md mx-4" @click.stop>
      <div class="mb-4">
        <h2 class="text-xl font-bold text-secondary-900">{{ $t('weeklyReports.approve.approve') }}</h2>
      </div>
      <div class="space-y-4">
        <div class="flex gap-2">
          <button
            @click="approved = true"
            :class="[
              'flex-1 rounded-lg px-4 py-2 font-medium transition-colors',
              approved
                ? 'bg-green-600 text-white hover:bg-green-700'
                : 'bg-secondary-100 text-secondary-700 hover:bg-secondary-200'
            ]"
          >
            {{ $t('weeklyReports.approve.approve') }}
          </button>
          <button
            @click="approved = false"
            :class="[
              'flex-1 rounded-lg px-4 py-2 font-medium transition-colors',
              approved
                ? 'bg-secondary-100 text-secondary-700 hover:bg-secondary-200'
                : 'bg-red-600 text-white hover:bg-red-700'
            ]"
          >
            {{ $t('weeklyReports.approve.reject') }}
          </button>
        </div>
        <div>
          <label class="block mb-2 text-sm font-medium text-secondary-700">
            {{ $t('weeklyReports.approve.comment') }}
          </label>
          <textarea
            v-model="approveComment"
            :placeholder="$t('weeklyReports.approve.commentPlaceholder')"
            class="w-full rounded-lg border border-secondary-300 px-3 py-2 text-sm focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
            rows="3"
          />
        </div>
      </div>
      <div class="flex justify-end gap-3 mt-6">
        <Button variant="ghost" @click="$emit('close')">
          {{ $t('common.cancel') }}
        </Button>
        <Button variant="primary" @click="submit">
          {{ $t('weeklyReports.approve.submitApprove') }}
        </Button>
      </div>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';
import Card from '@/components/common/Card.vue';
import Button from '@/components/common/Button.vue';

const { t } = useI18n();

const props = defineProps<{
  open: boolean;
}>();

const emit = defineEmits<{
  close: [];
  submit: [approved: boolean, comment?: string];
}>();

const approved = ref(true);
const approveComment = ref('');

const submit = () => {
  emit('submit', approved.value, approveComment.value || undefined);
  approved.value = true;
  approveComment.value = '';
};
</script>
