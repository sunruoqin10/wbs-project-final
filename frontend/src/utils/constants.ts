export const TASK_STATUS_OPTIONS = [
  { label: '待办', value: 'todo' },
  { label: '进行中', value: 'in-progress' },
  { label: '已完成', value: 'done' }
];

export const TASK_PRIORITY_OPTIONS = [
  { label: '低', value: 'low' },
  { label: '中', value: 'medium' },
  { label: '高', value: 'high' },
  { label: '紧急', value: 'urgent' }
];

export const PROJECT_STATUS_OPTIONS = [
  { label: '计划中', value: 'planning' },
  { label: '进行中', value: 'active' },
  { label: '已完成', value: 'completed' },
  { label: '已暂停', value: 'on-hold' }
];

export const PROJECT_PRIORITY_OPTIONS = [
  { label: '低', value: 'low' },
  { label: '中', value: 'medium' },
  { label: '高', value: 'high' },
  { label: '紧急', value: 'critical' }
];

export const USER_ROLE_OPTIONS = [
  { label: '管理员', value: 'admin' },
  { label: '项目经理', value: 'project-manager' },
  { label: '成员', value: 'member' },
  { label: '观察者', value: 'viewer' }
];
