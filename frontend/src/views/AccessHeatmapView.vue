<template>
  <MainLayout>
    <div class="space-y-6">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold text-secondary-900">
            {{ $t('admin.accessHeatmap.title') }}
          </h1>
          <p class="mt-1 text-sm text-secondary-600">
            {{ $t('admin.accessHeatmap.description') }}
          </p>
        </div>
        <div class="flex items-center gap-3">
          <label class="text-sm text-secondary-600">
            {{ $t('admin.accessHeatmap.controls.windowLabel') }}
          </label>
          <select
            v-model="windowVal"
            class="rounded border border-secondary-300 px-3 py-1 text-sm"
            @change="fetchData"
          >
            <option value="1d">{{ $t('admin.accessHeatmap.controls.windowOptions.d1') }}</option>
            <option value="7d">{{ $t('admin.accessHeatmap.controls.windowOptions.d7') }}</option>
            <option value="30d">{{ $t('admin.accessHeatmap.controls.windowOptions.d30') }}</option>
            <option value="90d">{{ $t('admin.accessHeatmap.controls.windowOptions.d90') }}</option>
          </select>
          <Button variant="primary" :disabled="loading" @click="fetchData">
            {{
              loading
                ? $t('admin.accessHeatmap.controls.refreshing')
                : $t('admin.accessHeatmap.controls.refresh')
            }}
          </Button>
        </div>
      </div>

      <div v-if="!error" class="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
        <Card padding="md">
          <div class="flex items-center">
            <div class="rounded-lg bg-primary-100 p-3">
              <svg
                class="h-6 w-6 text-primary-600"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
                />
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"
                />
              </svg>
            </div>
            <div class="ml-4">
              <p class="text-sm font-medium text-secondary-600">
                {{ $t('admin.accessHeatmap.kpi.totalVisits') }}
              </p>
              <p class="text-2xl font-semibold text-secondary-900">
                {{ data?.kpi.totalVisits ?? '—' }}
              </p>
              <p v-if="data" class="mt-1 text-xs text-secondary-500">
                {{ $t('admin.accessHeatmap.kpi.totalVisitsSub') }}
              </p>
            </div>
          </div>
        </Card>

        <Card padding="md">
          <div class="flex items-center">
            <div class="rounded-lg bg-accent-100 p-3">
              <svg
                class="h-6 w-6 text-accent-600"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 0h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"
                />
              </svg>
            </div>
            <div class="ml-4 min-w-0 flex-1">
              <p class="text-sm font-medium text-secondary-600">
                {{ $t('admin.accessHeatmap.kpi.topPage') }}
              </p>
              <p class="truncate text-2xl font-semibold text-secondary-900">
                {{ data?.kpi.topPageName ? translatePageName(data.kpi.topPageName) : '—' }}
              </p>
              <p v-if="data?.kpi.topPageTotal != null" class="mt-1 text-xs text-secondary-500">
                {{ $t('admin.accessHeatmap.kpi.topPageSub', { total: data.kpi.topPageTotal }) }}
              </p>
            </div>
          </div>
        </Card>

        <Card padding="md">
          <div class="flex items-center">
            <div class="rounded-lg bg-info-100 p-3">
              <svg
                class="h-6 w-6 text-info-600"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M5 3v4M3 5h4M6 17v4m-2-2h4m5-16l2.286 6.857L21 12l-5.714 2.143L13 21l-2.286-6.857L5 12l5.714-2.143L13 3z"
                />
              </svg>
            </div>
            <div class="ml-4">
              <p class="text-sm font-medium text-secondary-600">
                {{ $t('admin.accessHeatmap.kpi.peakHour') }}
              </p>
              <p class="text-2xl font-semibold text-secondary-900">
                {{
                  data?.kpi.peakHour != null && data?.xAxis?.[data.kpi.peakHour] != null
                    ? peakBucketLabel(data.xAxisUnit ?? 'hour', data.xAxis[data.kpi.peakHour])
                    : '—'
                }}
              </p>
              <p v-if="data?.kpi.peakHourTotal != null" class="mt-1 text-xs text-secondary-500">
                {{ $t('admin.accessHeatmap.kpi.peakHourSub', { total: data.kpi.peakHourTotal }) }}
              </p>
            </div>
          </div>
        </Card>

        <Card padding="md">
          <div class="flex items-center">
            <div class="rounded-lg bg-warning-100 p-3">
              <svg
                class="h-6 w-6 text-warning-600"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 0h2a2 2 0 002-2m-3 4h3m-3 4h3m-6-4h.01M9 16h.01"
                />
              </svg>
            </div>
            <div class="ml-4">
              <p class="text-sm font-medium text-secondary-600">
                {{ $t('admin.accessHeatmap.kpi.pageCount') }}
              </p>
              <p class="text-2xl font-semibold text-secondary-900">
                {{ data?.yAxis?.length ?? '—' }}
              </p>
              <p v-if="data" class="mt-1 text-xs text-secondary-500">
                {{ $t('admin.accessHeatmap.kpi.pageCountSub') }}
              </p>
            </div>
          </div>
        </Card>
      </div>

      <Card padding="none">
        <template #header>
          <h3 class="text-lg font-semibold text-secondary-900">
            {{ $t('admin.accessHeatmap.title') }}
          </h3>
        </template>
        <div v-if="error" class="py-12 text-center">
          <p class="text-danger-600 mb-1">
            {{ $t('admin.accessHeatmap.errors.loadFailed') }}
          </p>
          <p v-if="errorMsg" class="mb-3 text-sm text-secondary-500">
            {{ errorMsg }}
          </p>
          <Button variant="primary" @click="fetchData">
            {{ $t('admin.accessHeatmap.controls.refresh') }}
          </Button>
        </div>
        <div
          v-else-if="loading && !data"
          class="h-96 animate-pulse rounded bg-secondary-50"
        />
        <div v-else ref="chartEl" class="h-[480px] w-full" />
      </Card>

      <div class="text-xs text-secondary-500">
        {{ $t('admin.accessHeatmap.footer.lastUpdated') }}: {{ lastUpdatedStr }} ·
        {{ $t('admin.accessHeatmap.footer.dataRange') }}:
        {{ $t('admin.accessHeatmap.footer.dataRangeValue') }} ·
        {{ $t('admin.accessHeatmap.footer.dataSource') }}:
        {{ $t('admin.accessHeatmap.footer.dataSourceValue') }}
      </div>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import * as echarts from 'echarts';
import { apiService } from '@/services/api';
import type { HeatmapResponse } from '@/types';
import MainLayout from '@/components/layout/MainLayout.vue';
import Card from '@/components/common/Card.vue';
import Button from '@/components/common/Button.vue';

type Window = '1d' | '7d' | '30d' | '90d';

const { t: $t, locale } = useI18n();

const windowVal = ref<Window>('7d');
const data = ref<HeatmapResponse | null>(null);
const loading = ref(false);
const error = ref(false);
const errorMsg = ref('');
const chartEl = ref<HTMLDivElement | null>(null);
let chart: echarts.ECharts | null = null;
const lastUpdatedStr = ref('');

const pad = (n: number) => String(n).padStart(2, '0');

const axisNameKey = (unit: string): string => {
  switch (unit) {
    case 'dayOfWeek': return 'admin.accessHeatmap.chart.dayOfWeekAxis';
    case 'date': return 'admin.accessHeatmap.chart.dateAxis';
    case 'week': return 'admin.accessHeatmap.chart.weekAxis';
    case 'hour':
    default: return 'admin.accessHeatmap.chart.hourAxis';
  }
};

const bucketLabel = (unit: string, raw: string): string => {
  if (!raw) return '';
  if (unit === 'dayOfWeek') {
    return $t(`admin.accessHeatmap.chart.dayOfWeek.${raw}`);
  }
  return raw;
};

// KPI "最热时段" 显示:hour → 06:00,dayOfWeek → 周一,date/week → 原样
const peakBucketLabel = (unit: string, raw: string): string => {
  if (!raw) return '';
  if (unit === 'hour') return raw + ':00';
  return bucketLabel(unit, raw);
};

const translatePageName = (pageName: string): string => {
  if (!pageName) return '';
  const key = pageName.charAt(0).toLowerCase() + pageName.slice(1);
  const fullKey = `routes.${key}`;
  const translated = $t(fullKey);
  if (translated === fullKey || translated === key) {
    return pageName;
  }
  return translated;
};

const fetchData = async () => {
  loading.value = true;
  error.value = false;
  errorMsg.value = '';
  try {
    const r = await apiService.getHeatmap(windowVal.value);
    data.value = r;
    lastUpdatedStr.value = new Date().toLocaleTimeString();
    await nextTick();
    renderChart(r);
  } catch (e: any) {
    error.value = true;
    errorMsg.value = e?.message || String(e);
    data.value = null;
    console.warn('[AccessHeatmapView] getHeatmap failed:', e);
  } finally {
    loading.value = false;
  }
};

const renderChart = (d: HeatmapResponse) => {
  if (!chartEl.value) return;
  if (!chart) chart = echarts.init(chartEl.value);

  const unit = d.xAxisUnit ?? 'hour';
  const xRaw = d.xAxis ?? (unit === 'hour'
    ? Array.from({ length: 24 }, (_, i) => pad(i))
    : []);
  const yPagesRaw = d.yAxis ?? [];
  const yPages = yPagesRaw.map(translatePageName);
  const xCats = xRaw.map((b) => bucketLabel(unit, b));
  const matrix = d.matrix ?? [];

  const seriesData: [string, string, number][] = [];
  matrix.forEach((row, yi) => {
    row.forEach((v, xi) => {
      seriesData.push([xCats[xi] ?? String(xRaw[xi] ?? ''), yPages[yi] ?? yPagesRaw[yi] ?? '', Number(v)]);
    });
  });

  const maxVal = d.maxValue ?? 0;

  chart.setOption(
    {
      tooltip: {
        position: 'top',
        formatter: (p: any) => {
          const catRaw = String(p.data[0]);
          const pageCat = String(p.data[1]);
          const v = p.data[2];

          const pageName = pageCat || '-';
          // p.data[0] 已经是显示标签,这里把 hour 类型补上 ":00" 后缀
          const timeStr = unit === 'hour' ? catRaw + ':00' : catRaw;

          return `${pageName}<br/>${timeStr}<br/>${$t(
            'admin.accessHeatmap.chart.tooltipTotal',
            { total: v },
          )}`;
        },
      },
      grid: { left: 140, right: 30, top: 30, bottom: 60 },
      xAxis: {
        type: 'category',
        data: xCats,
        name: $t(axisNameKey(unit)),
        nameLocation: 'end',
        splitArea: { show: true },
      },
      yAxis: {
        type: 'category',
        data: yPages,
        name: $t('admin.accessHeatmap.chart.pageAxis'),
        nameLocation: 'end',
        splitArea: { show: true },
      },
      visualMap: {
        type: 'piecewise',
        orient: 'horizontal',
        left: 'center',
        bottom: 10,
        pieces: buildPieces(maxVal),
      },
      series: [
        {
          type: 'heatmap',
          data: seriesData,
          label: { show: false },
          emphasis: { itemStyle: { borderColor: '#000', borderWidth: 1 } },
        },
      ],
    },
    true,
  );
};

const buildPieces = (maxVal: number): any[] => {
  const pieces: any[] = [];
  pieces.push({ min: 0, max: 0, color: '#f5f5f5', label: '0' });

  if (maxVal <= 0) return pieces;

  if (maxVal <= 10) {
    pieces.push({ min: 1, max: 5, color: '#bfdbfe', label: '1-5' });
    pieces.push({ min: 6, color: '#1d4ed8', label: '6+' });
  } else if (maxVal <= 100) {
    pieces.push({ min: 1, max: 10, color: '#bfdbfe', label: '1-10' });
    pieces.push({ min: 11, max: 50, color: '#60a5fa', label: '11-50' });
    pieces.push({ min: 51, color: '#1d4ed8', label: '51+' });
  } else {
    pieces.push({ min: 1, max: 20, color: '#bfdbfe', label: '1-20' });
    pieces.push({ min: 21, max: 100, color: '#60a5fa', label: '21-100' });
    pieces.push({ min: 101, color: '#1d4ed8', label: '101+' });
  }
  return pieces;
};

onMounted(() => {
  fetchData();
  window.addEventListener('resize', onResize);
});

// 切换语言时重新渲染图表（Y 轴页面名翻译会随 locale 变化）
watch(locale, () => {
  if (data.value) renderChart(data.value);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize);
  chart?.dispose();
  chart = null;
});

const onResize = () => chart?.resize();
</script>
