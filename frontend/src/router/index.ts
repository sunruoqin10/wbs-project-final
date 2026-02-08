import { createRouter, createWebHistory } from 'vue-router';
import type { RouteRecordRaw } from 'vue-router';
import { useUserStore } from '@/stores/user';

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/test',
    name: 'Test',
    component: () => import('@/views/Test.vue'),
    meta: { title: '测试页面' }
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/Dashboard.vue'),
    meta: { title: '仪表盘' }
  },
  {
    path: '/simple-dashboard',
    name: 'SimpleDashboard',
    component: () => import('@/views/SimpleDashboard.vue'),
    meta: { title: '仪表盘（简化版）' }
  },
  {
    path: '/projects',
    name: 'ProjectList',
    component: () => import('@/views/ProjectList.vue'),
    meta: { title: '项目列表' }
  },
  {
    path: '/projects/new',
    name: 'ProjectNew',
    component: () => import('@/views/ProjectDetail.vue'),
    meta: { title: '创建项目' }
  },
  {
    path: '/projects/:id',
    name: 'ProjectDetail',
    component: () => import('@/views/ProjectDetail.vue'),
    meta: { title: '项目详情' }
  },
  {
    path: '/projects/:id/tasks',
    name: 'TaskBoard',
    component: () => import('@/views/TaskBoard.vue'),
    meta: { title: '任务看板' }
  },
  {
    path: '/projects/:id/gantt',
    name: 'GanttView',
    component: () => import('@/views/GanttView.vue'),
    meta: { title: '甘特图' }
  },
  {
    path: '/team',
    name: 'Team',
    component: () => import('@/views/Team.vue'),
    meta: { title: '团队成员' }
  },
  {
    path: '/reports',
    name: 'Reports',
    component: () => import('@/views/Reports.vue'),
    meta: { title: '报表统计' }
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('@/views/Settings.vue'),
    meta: { title: '设置' }
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

// 白名单：不需要认证的路由
const whiteList = ['/login', '/test'];

router.beforeEach((to, from, next) => {
  document.title = `${to.meta.title || 'WBS项目管理系统'} - WBS`;

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
