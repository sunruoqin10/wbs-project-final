import { defineStore } from 'pinia';
import { ref } from 'vue';

export const useUiStore = defineStore('ui', () => {
  const sidebarCollapsed = ref(false);
  const modalOpen = ref(false);
  const loading = ref(false);

  const toggleSidebar = () => {
    sidebarCollapsed.value = !sidebarCollapsed.value;
  };

  const setSidebarCollapsed = (collapsed: boolean) => {
    sidebarCollapsed.value = collapsed;
  };

  const openModal = () => {
    modalOpen.value = true;
  };

  const closeModal = () => {
    modalOpen.value = false;
  };

  const setLoading = (value: boolean) => {
    loading.value = value;
  };

  return {
    sidebarCollapsed,
    modalOpen,
    loading,
    toggleSidebar,
    setSidebarCollapsed,
    openModal,
    closeModal,
    setLoading
  };
});
