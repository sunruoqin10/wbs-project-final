import type { Statistics } from '@/types';
import { projects } from './projects';
import { tasks } from './tasks';

export const statistics: Statistics = {
  totalProjects: projects.length,
  activeProjects: projects.filter(p => p.status === 'active').length,
  completedProjects: projects.filter(p => p.status === 'completed').length,
  totalTasks: tasks.length,
  completedTasks: tasks.filter(t => t.status === 'done').length,
  inProgressTasks: tasks.filter(t => t.status === 'in-progress').length,
  totalMembers: 6
};
