import { createRouter, createWebHistory } from 'vue-router';
import type { RouteRecordRaw } from 'vue-router';
import { useUserStore } from '@/stores/user';
import { usePermissionStore } from '@/stores/permission';
import i18n from '@/i18n';

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { titleKey: 'routes.login' }
  },
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/test',
    name: 'Test',
    component: () => import('@/views/Test.vue'),
    meta: { titleKey: 'routes.test' }
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/Dashboard.vue'),
    meta: { titleKey: 'routes.dashboard' }
  },
  {
    path: '/simple-dashboard',
    name: 'SimpleDashboard',
    component: () => import('@/views/SimpleDashboard.vue'),
    meta: { titleKey: 'routes.simpleDashboard' }
  },
  {
    path: '/projects',
    name: 'ProjectList',
    component: () => import('@/views/ProjectList.vue'),
    meta: { titleKey: 'routes.projectList', permission: 'project:view' }
  },
  {
    path: '/projects/new',
    name: 'ProjectNew',
    component: () => import('@/views/ProjectDetail.vue'),
    meta: { titleKey: 'routes.projectNew', permission: 'project:create' }
  },
  {
    path: '/projects/:id',
    name: 'ProjectDetail',
    component: () => import('@/views/ProjectDetail.vue'),
    meta: { titleKey: 'routes.projectDetail', permission: 'project:view' }
  },
  {
    path: '/projects/:id/tasks',
    name: 'TaskBoard',
    component: () => import('@/views/TaskBoard.vue'),
    meta: { titleKey: 'routes.taskBoard' }
  },
  {
    path: '/projects/:id/gantt',
    name: 'GanttView',
    component: () => import('@/views/GanttView.vue'),
    meta: { titleKey: 'routes.ganttView' }
  },
  {
    path: '/delay-stats',
    name: 'DelayStats',
    component: () => import('@/views/DelayStats.vue'),
    meta: { title: '项目延期统计' }
  },
  {
    path: '/overtime',
    name: 'OvertimeManagement',
    component: () => import('@/views/OvertimeManagement.vue'),
    meta: { title: '项目加班管理', permission: 'overtime:view' }
  },
  {
    path: '/team',
    name: 'Team',
    component: () => import('@/views/Team.vue'),
    meta: { titleKey: 'routes.team', permission: 'user:view' }
  },
  {
    path: '/reports',
    name: 'Reports',
    component: () => import('@/views/Reports.vue'),
    meta: { titleKey: 'routes.reports' }
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('@/views/Settings.vue'),
    meta: { titleKey: 'routes.settings', permission: 'settings:view' }
  },
  {
    path: '/forbidden',
    name: 'Forbidden',
    component: () => import('@/views/Forbidden.vue'),
    meta: { title: '无权限' }
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

const whiteList = ['/login', '/test', '/forbidden'];

router.beforeEach((to, from, next) => {
  const titleKey = to.meta.titleKey as string;
  const title = titleKey ? i18n.global.t(titleKey) : i18n.global.t('app.name');
  document.title = `${title} - WBS`;

  const userStore = useUserStore();
  const permissionStore = usePermissionStore();

  if (whiteList.includes(to.path)) {
    if (to.path === '/login' && userStore.token) {
      next('/dashboard');
    } else {
      next();
    }
  } else {
    if (userStore.token) {
      const requiredPermission = to.meta.permission as string | undefined;
      
      if (requiredPermission) {
        if (permissionStore.hasPermission(requiredPermission)) {
          next();
        } else {
          next('/forbidden');
        }
      } else {
        next();
      }
    } else {
      next({
        path: '/login',
        query: { redirect: to.fullPath }
      });
    }
  }
});

export default router;
