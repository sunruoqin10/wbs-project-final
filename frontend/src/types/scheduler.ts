export interface SchedulerConfig {
  id: string;
  name: string;
  description: string;
  cronExpression: string;
  enabled: boolean;
  lastRunTime?: string;
  nextRunTime?: string;
  createdAt: string;
  updatedAt: string;
}
