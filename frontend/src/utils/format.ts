import dayjs from 'dayjs';
import 'dayjs/locale/zh-cn';
import relativeTime from 'dayjs/plugin/relativeTime';

dayjs.locale('zh-cn');
dayjs.extend(relativeTime);

export const formatDate = (date: string | Date, format = 'YYYY-MM-DD') => {
  return dayjs(date).format(format);
};

export const formatDateTime = (date: string | Date) => {
  return dayjs(date).format('YYYY-MM-DD HH:mm');
};

export const formatRelativeTime = (date: string | Date) => {
  return dayjs(date).fromNow();
};

export const isDueSoon = (date: string | Date, days = 2) => {
  const now = dayjs();
  const dueDate = dayjs(date);
  const diff = dueDate.diff(now, 'day');
  return diff >= 0 && diff <= days;
};

export const isOverdue = (date: string | Date) => {
  return dayjs(date).isBefore(dayjs(), 'day');
};

export const getProgressColor = (progress: number) => {
  if (progress < 25) return 'bg-danger-500';
  if (progress < 50) return 'bg-warning-500';
  if (progress < 75) return 'bg-info-500';
  return 'bg-accent-500';
};
