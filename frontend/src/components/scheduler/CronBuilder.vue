<template>
  <div class="space-y-4">
    <div>
      <label class="block text-sm font-medium text-secondary-700 mb-2">{{ $t('scheduler.frequency') }}</label>
      <div class="grid grid-cols-4 gap-2">
        <button
          v-for="option in frequencyOptions"
          :key="option.value"
          @click="selectFrequency(option.value)"
          :class="[
            'rounded-lg border-2 px-4 py-2 text-sm font-medium transition-all',
            frequency === option.value
              ? 'border-primary-500 bg-primary-50 text-primary-700'
              : 'border-secondary-200 text-secondary-600 hover:border-secondary-300'
          ]"
        >
          {{ option.label }}
        </button>
      </div>
    </div>

    <div v-if="frequency === 'weekly'" class="grid grid-cols-7 gap-2">
      <button
        v-for="(label, index) in weekDayLabels"
        :key="index"
        @click="toggleWeekDay(index)"
        :class="[
          'rounded-lg border-2 px-2 py-2 text-xs font-medium text-center transition-all',
          weekDays.includes(index)
            ? 'border-primary-500 bg-primary-50 text-primary-700'
            : 'border-secondary-200 text-secondary-500 hover:border-secondary-300'
        ]"
      >
        {{ label }}
      </button>
    </div>

    <div v-if="frequency === 'monthly'" class="grid grid-cols-7 gap-2">
      <button
        v-for="day in 31"
        :key="day"
        @click="monthDay = day"
        :class="[
          'rounded-lg border-2 px-1 py-2 text-xs font-medium text-center transition-all',
          monthDay === day
            ? 'border-primary-500 bg-primary-50 text-primary-700'
            : 'border-secondary-200 text-secondary-500 hover:border-secondary-300'
        ]"
      >
        {{ day }}
      </button>
    </div>

    <div>
      <label class="block text-sm font-medium text-secondary-700 mb-2">{{ $t('scheduler.execTime') }}</label>
      <div class="flex items-center gap-2">
        <select
          v-model="hour"
          class="rounded-lg border border-secondary-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none"
        >
          <option v-for="h in 24" :key="h - 1" :value="h - 1">
            {{ String(h - 1).padStart(2, '0') }}
          </option>
        </select>
        <span class="text-secondary-500">:</span>
        <select
          v-model="minute"
          class="rounded-lg border border-secondary-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none"
        >
          <option v-for="m in 60" :key="m - 1" :value="m - 1">
            {{ String(m - 1).padStart(2, '0') }}
          </option>
        </select>
        <span class="text-sm text-secondary-500">HH:MM</span>
      </div>
    </div>

    <div class="rounded-lg bg-secondary-50 p-4">
      <div class="flex items-center justify-between">
        <span class="text-sm font-medium text-secondary-700">Cron</span>
        <code class="rounded bg-white px-3 py-1 text-sm font-mono text-primary-700 border border-secondary-200">
          {{ cronExpression }}
        </code>
      </div>
      <p class="mt-2 text-xs text-secondary-500">{{ humanReadable }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();

const props = defineProps<{
  modelValue: string;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void;
}>();

const frequencyOptions = computed(() => [
  { value: 'daily', label: t('scheduler.daily') },
  { value: 'weekly', label: t('scheduler.weekly') },
  { value: 'monthly', label: t('scheduler.monthly') },
  { value: 'custom', label: t('scheduler.custom') },
]);

const weekDayLabels = computed(() => [
  t('scheduler.sun'), t('scheduler.mon'), t('scheduler.tue'),
  t('scheduler.wed'), t('scheduler.thu'), t('scheduler.fri'), t('scheduler.sat'),
]);

const frequency = ref<'daily' | 'weekly' | 'monthly' | 'custom'>('daily');
const hour = ref(9);
const minute = ref(0);
const weekDays = ref<number[]>([1, 2, 3, 4, 5]);
const monthDay = ref(1);
const customCron = ref('');

const cronExpression = computed(() => {
  if (frequency.value === 'custom') {
    return customCron.value || props.modelValue;
  }
  const m = minute.value;
  const h = hour.value;
  if (frequency.value === 'daily') {
    return `0 ${m} ${h} * * ?`;
  }
  if (frequency.value === 'weekly') {
    const days = weekDays.value.length > 0 ? weekDays.value.join(',') : '*';
    return `0 ${m} ${h} ? * ${days}`;
  }
  if (frequency.value === 'monthly') {
    return `0 ${m} ${h} ${monthDay.value} * ?`;
  }
  return props.modelValue;
});

const humanReadable = computed(() => {
  const timeStr = `${String(hour.value).padStart(2, '0')}:${String(minute.value).padStart(2, '0')}`;
  if (frequency.value === 'daily') return `${t('scheduler.everyDay')} ${timeStr}`;
  if (frequency.value === 'weekly') {
    const names = weekDays.value.map((d: number) => weekDayLabels.value[d]).join('、');
    return `${t('scheduler.everyWeek')} ${names} ${timeStr}`;
  }
  if (frequency.value === 'monthly') return `${t('scheduler.everyMonth')} ${monthDay.value}${t('scheduler.dayOfMonth')} ${timeStr}`;
  if (frequency.value === 'custom') return t('scheduler.customCronDesc');
  return '';
});

function selectFrequency(val: string) {
  frequency.value = val as typeof frequency.value;
}

function toggleWeekDay(index: number) {
  const idx = weekDays.value.indexOf(index);
  if (idx === -1) {
    weekDays.value.push(index);
  } else {
    weekDays.value.splice(idx, 1);
  }
}

watch(cronExpression, (val) => {
  emit('update:modelValue', val);
});

watch(() => props.modelValue, (val) => {
  parseCron(val);
}, { immediate: true });

function parseCron(cron: string) {
  if (!cron) return;
  const parts = cron.trim().split(/\s+/);
  if (parts.length < 6) {
    frequency.value = 'custom';
    customCron.value = cron;
    return;
  }
  const [, m, h, dayOfMonth, monthField, dayOfWeek] = parts;
  minute.value = parseInt(m) || 0;
  hour.value = parseInt(h) || 0;

  if (dayOfWeek === '?' && monthField === '*' && dayOfMonth === '*') {
    frequency.value = 'daily';
  } else if (dayOfMonth === '?' && dayOfWeek !== '*') {
    frequency.value = 'weekly';
    weekDays.value = dayOfWeek.split(',').map(Number).filter(n => !isNaN(n));
  } else if (dayOfWeek === '?' && dayOfMonth !== '*' && dayOfMonth !== '?') {
    frequency.value = 'monthly';
    monthDay.value = parseInt(dayOfMonth) || 1;
  } else {
    frequency.value = 'custom';
    customCron.value = cron;
  }
}
</script>
