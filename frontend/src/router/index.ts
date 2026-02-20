import { createRouter, createWebHistory } from 'vue-router';
import type { RouteRecordRaw } from 'vue-router';
import { useUserStore } from '@/stores/user';
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
    meta: { titleKey: 'routes.projectList' }
  },
  {
    path: '/projects/new',
    name: 'ProjectNew',
    component: () => import('@/views/ProjectDetail.vue'),
    meta: { titleKey: 'routes.projectNew' }
  },
  {
    path: '/projects/:id',
    name: 'ProjectDetail',
    component: () => import('@/views/ProjectDetail.vue'),
    meta: { titleKey: 'routes.projectDetail' }
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
    path: '/team',
    name: 'Team',
    component: () => import('@/views/Team.vue'),
    meta: { titleKey: 'routes.team' }
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
    meta: { titleKey: 'routes.settings' }
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

// 白名单：不需要认证的路由
const whiteList = ['/login', '/test'];

router.beforeEach((to, from, next) => {
  const titleKey = to.meta.titleKey as string;
  const title = titleKey ? i18n.global.t(titleKey) : i18n.global.t('app.name');
  document.title = `${title} - WBS`;

  const userStore = useUserStore();

  // 检查是否需要认证
  if (whiteList.includes(to.path)) {
    // 白名单路由直接放行
    // 如果已登录且访问登录页，重定向到仪表盘
    if (to.path === '/login' && userStore.token) {
      next('/dashboard');
    } else {
      next();
    }
  } else {
    // 需要认证的路由
    if (userStore.token) {
      // 已登录，放行
      next();
    } else {
      // 未登录，重定向到登录页
      next({
        path: '/login',
        query: { redirect: to.fullPath } // 保存原始路径以便登录后跳转
      });
    }
  }
});

export default router;
