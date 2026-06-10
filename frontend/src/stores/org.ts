import { defineStore } from 'pinia';
import { ref } from 'vue';
import type { OrgNode } from '@/types';
import apiService from '@/services/api';

/**
 * 组织架构 Pinia store
 * 缓存 /api/orgs/tree 的响应，避免每次进入 Team.vue 都重新请求
 */
export const useOrgStore = defineStore('org', () => {
  const tree = ref<OrgNode | null>(null);
  const loaded = ref(false);
  const loading = ref(false);

  /**
   * 加载组织树（已加载则直接返回）
   */
  async function loadTree(force = false): Promise<OrgNode | null> {
    if (loaded.value && !force) return tree.value;
    if (loading.value) return tree.value;
    loading.value = true;
    try {
      const data = await apiService.getOrgTree();
      tree.value = data;
      loaded.value = true;
      return data;
    } finally {
      loading.value = false;
    }
  }

  function reset() {
    tree.value = null;
    loaded.value = false;
  }

  return { tree, loaded, loading, loadTree, reset };
});
