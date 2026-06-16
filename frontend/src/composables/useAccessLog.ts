import { useRouter } from 'vue-router';
import { apiService } from '@/services/api';

let installed = false;

const MENU_PAGE_NAMES = new Set([
  'Dashboard',
  'SimpleDashboard',
  'MyTasks',
  'ProjectList',
  'ProjectNew',
  'ProjectDetail',
  'TaskBoard',
  'GanttView',
  'DelayStats',
  'OvertimeManagement',
  'WeeklyReports',
  'WeeklyReportNew',
  'WeeklyReportDetail',
  'WeeklyReportEdit',
  'Documents',
  'Team',
  'Reports',
  'Settings',
  'SchedulerManagement',
  'AdminAccessHeatmap',
]);

function formatLocalDateTime(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  const y = date.getFullYear();
  const m = pad(date.getMonth() + 1);
  const d = pad(date.getDate());
  const hh = pad(date.getHours());
  const mm = pad(date.getMinutes());
  const ss = pad(date.getSeconds());
  return `${y}-${m}-${d}T${hh}:${mm}:${ss}`;
}

export function useAccessLog() {
  const router = useRouter();

  if (installed) return;
  installed = true;

  router.afterEach((to) => {
    if (!to.name) return;
    const name = String(to.name);
    if (!MENU_PAGE_NAMES.has(name)) return;

    const occurredAt = formatLocalDateTime(new Date());

    apiService.postPageView({
      pagePath: to.fullPath,
      pageName: name,
      occurredAt,
    }).catch(() => {
    });
  });
}
