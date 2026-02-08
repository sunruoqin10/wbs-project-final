import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import type { User } from '@/types';
import apiService from '@/services/api';
import { users as mockUsers } from '@/data/users';

export const useUserStore = defineStore('user', () => {
  const users = ref<User[]>([]);
  const currentUserId = ref<string | null>(null);
  const currentUserLoaded = ref(false);
  const token = ref<string | null>(null);

  const currentUser = computed(() => {
    if (!currentUserId.value) return null;
    return users.value.find(u => u.id === currentUserId.value) || null;
  });

  // 解析用户数据，处理 skills 字段的 JSON 字符串
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
    return user as User;
  };

  // 加载所有用户
  const loadUsers = async () => {
    if (currentUserLoaded.value) return;
    try {
      const rawUsers = await apiService.getUsers();
      // 解析每个用户的 skills 字段
      users.value = rawUsers.map(parseUserData);
      currentUserLoaded.value = true;
    } catch (error) {
      // API 调用失败时使用 mock 数据（开发阶段）
      console.warn('API 未连接，使用 mock 数据:', error);
      users.value = mockUsers;
      currentUserLoaded.value = true;
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
  };

  // 登出
  const logout = () => {
    currentUserId.value = null;
    token.value = null;
    localStorage.removeItem('auth_token');
  };

  // 从localStorage恢复Token和用户信息
  const restoreAuth = () => {
    const savedToken = localStorage.getItem('auth_token');
    if (savedToken) {
      token.value = savedToken;
      // TODO: 可以通过token获取用户信息
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
    userById,
    getUsersByIds,
    addUser,
    updateUser,
    deleteUser,
    setCurrentUser,
    logout,
    restoreAuth
  };
});
