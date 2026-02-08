import type { User } from '@/types';

export const users: User[] = [
  {
    id: 'u1',
    name: '张伟',
    email: 'zhangwei@example.com',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=ZhangWei',
    role: 'admin',
    department: '技术部',
    skills: ['Vue.js', 'TypeScript', 'Node.js'],
    joinedAt: '2024-01-15'
  },
  {
    id: 'u2',
    name: '李娜',
    email: 'lina@example.com',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=LiNa',
    role: 'project-manager',
    department: '产品部',
    skills: ['产品设计', '项目管理', 'UI/UX'],
    joinedAt: '2024-02-01'
  },
  {
    id: 'u3',
    name: '王强',
    email: 'wangqiang@example.com',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=WangQiang',
    role: 'member',
    department: '技术部',
    skills: ['React', 'Python', 'Docker'],
    joinedAt: '2024-03-10'
  },
  {
    id: 'u4',
    name: '刘芳',
    email: 'liufang@example.com',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=LiuFang',
    role: 'member',
    department: '设计部',
    skills: ['Figma', 'Photoshop', 'Illustrator'],
    joinedAt: '2024-03-15'
  },
  {
    id: 'u5',
    name: '陈明',
    email: 'chenming@example.com',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=ChenMing',
    role: 'member',
    department: '技术部',
    skills: ['Java', 'Spring Boot', 'MySQL'],
    joinedAt: '2024-04-01'
  },
  {
    id: 'u6',
    name: '赵丽',
    email: 'zhaoli@example.com',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=ZhaoLi',
    role: 'viewer',
    department: '市场部',
    skills: ['市场营销', '数据分析', 'SEO'],
    joinedAt: '2024-04-10'
  }
];
