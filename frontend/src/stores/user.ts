import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import type { User } from '@/types';
import apiService from '@/services/api';
import { users as mockUsers } from '@/data/users';
import { useProjectStore } from './project';
import { useOrgStore } from './org';

export const useUserStore = defineStore('user', () => {
  const users = ref<User[]>([]);
  const currentUserId = ref<string | null>(null);
  const currentUserLoaded = ref(false);
  const token = ref<string | null>(null);

  const currentUser = computed(() => {
    if (!currentUserId.value) return null;
    return users.value.find(u => u.id === currentUserId.value) || null;
  });

  // 解析用户数据，处理 skills / managedDeptCodes 字段的 JSON 字符串
  const parseUserData = (user: any): User => {
    // 处理 skills 字段：如果是 JSON 字符串则解析为数组
    if (user.skills && typeof user.skills === 'string') {
      try {
        user.skills = JSON.parse(user.skills);
      } catch (e) {
        console.warn('Failed to parse skills JSON:', user.skills, e);
        user.skills = [];
      }
    }
    // 确保 skills 是数组
    if (!Array.isArray(user.skills)) {
      user.skills = [];
    }
    // 角色管理 v2:managedDeptCodes 也是 JSON 字符串
    if (user.managedDeptCodes && typeof user.managedDeptCodes === 'string') {
      try {
        user.managedDeptCodes = JSON.parse(user.managedDeptCodes);
      } catch (e) {
        console.warn('Failed to parse managedDeptCodes JSON:', user.managedDeptCodes, e);
        user.managedDeptCodes = [];
      }
    }
    if (user.managedDeptCodes && !Array.isArray(user.managedDeptCodes)) {
      user.managedDeptCodes = [];
    }
    return user as User;
  };

  // 加载所有用户（首次加载，如果已加载则跳过）
  const loadUsers = async () => {
    if (currentUserLoaded.value) return;
    await refreshUsers();
    currentUserLoaded.value = true;
  };

  // 2026-06-13: 切走旧用户前清空 users 列表,避免 mockUsers 回退路径污染
  // (API 失败时 refreshUsers 会把 mockUsers 写进 users.value,登录态切走不清理会带到下一会话)
  const clearUsers = () => {
    users.value = [];
    currentUserLoaded.value = false;
  };

  // 强制刷新用户数据（始终从 API 重新获取）
  const refreshUsers = async () => {
    try {
      const rawUsers = await apiService.getUsers();
      // 解析每个用户的 skills 字段
      users.value = rawUsers.map(parseUserData);
    } catch (error) {
      // API 调用失败时保持现有数据
      console.warn('API 未连接，使用 mock 数据:', error);
      if (users.value.length === 0) {
        users.value = mockUsers;
      }
    }
  };

  const userById = (id: string) => {
    return users.value.find(u => u.id === id);
  };

  const getUsersByIds = (ids?: string[] | null) => {
    if (!ids || !Array.isArray(ids) || ids.length === 0) {
      return [];
    }
    return users.value.filter(u => ids.includes(u.id));
  };

  const addUser = async (userData: Omit<User, 'id'>) => {
    try {
      const newUser = await apiService.createUser(userData);
      // 解析返回的用户数据（处理 skills 字段）
      const parsedUser = parseUserData(newUser);
      users.value.push(parsedUser);
      return parsedUser;
    } catch (error) {
      console.error('Failed to add user:', error);
      throw error;
    }
  };

  const updateUser = async (id: string, userData: Partial<User>) => {
    try {
      const updatedUser = await apiService.updateUser(id, userData);
      // 解析返回的用户数据（处理 skills 字段）
      const parsedUser = parseUserData(updatedUser);
      const index = users.value.findIndex(u => u.id === id);
      if (index !== -1) {
        users.value[index] = parsedUser;
      }
      return parsedUser;
    } catch (error) {
      console.error('Failed to update user:', error);
      throw error;
    }
  };

  const deleteUser = async (id: string) => {
    try {
      await apiService.deleteUser(id);
      const index = users.value.findIndex(u => u.id === id);
      if (index !== -1) {
        users.value.splice(index, 1);
      }
      return true;
    } catch (error) {
      console.error('Failed to delete user:', error);
      throw error;
    }
  };

  // 设置当前登录用户和Token
  const setCurrentUser = (user: User, userToken?: string) => {
    // 2026-06-13: 切换用户前清空依赖旧用户范围的 store,
    // 避免 next 用户短暂命中上次的 projects.value / org.tree / users
    const projectStore = useProjectStore();
    const orgStore = useOrgStore();
    projectStore.clearProjects();
    orgStore.reset();
    clearUsers();
    // 解析用户数据（处理 skills 字段）
    const parsedUser = parseUserData(user);
    currentUserId.value = parsedUser.id;
    // 如果用户不在列表中，添加到列表
    if (!users.value.find(u => u.id === parsedUser.id)) {
      users.value.push(parsedUser);
    }
    // 保存Token
    if (userToken) {
      token.value = userToken;
      localStorage.setItem('auth_token', userToken);
    }
    // 保存当前用户ID到localStorage
    localStorage.setItem('current_user_id', parsedUser.id);
  };

  // 登出
  const logout = () => {
    // 2026-06-13: 登出时清空依赖旧用户范围的 store,防止下一用户登录后命中上次的缓存
    const projectStore = useProjectStore();
    const orgStore = useOrgStore();
    projectStore.clearProjects();
    orgStore.reset();
    clearUsers();
    currentUserId.value = null;
    token.value = null;
    localStorage.removeItem('auth_token');
    localStorage.removeItem('current_user_id');
  };

  // 从localStorage恢复Token和用户信息
  const restoreAuth = async () => {
    const savedToken = localStorage.getItem('auth_token');
    const savedUserId = localStorage.getItem('current_user_id');
    
    if (savedToken && savedUserId) {
      token.value = savedToken;
      currentUserId.value = savedUserId;
    }
  };

  // 初始化时恢复认证信息
  restoreAuth();

  return {
    users,
    currentUserId,
    currentUser,
    currentUserLoaded,
    token,
    loadUsers,
    refreshUsers,
    userById,
    getUsersByIds,
    addUser,
    updateUser,
    deleteUser,
    setCurrentUser,
    logout,
    restoreAuth,
    clearUsers
  };
});
