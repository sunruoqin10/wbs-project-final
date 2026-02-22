<template>
  <slot v-if="hasPermission" />
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { usePermissionStore } from '@/stores/permission';

const props = defineProps<{
  permission?: string | string[];
  projectId?: string;
  projectPermission?: string;
  role?: string | string[];
  requireAll?: boolean;
}>();

const permissionStore = usePermissionStore();

const hasPermission = computed(() => {
  if (props.role) {
    const roles = Array.isArray(props.role) ? props.role : [props.role];
    return roles.includes(permissionStore.currentRole);
  }

  if (props.projectId && props.projectPermission) {
    return permissionStore.hasProjectPermission(props.projectId, props.projectPermission);
  }

  if (props.permission) {
    if (Array.isArray(props.permission)) {
      if (props.requireAll) {
        return permissionStore.hasAllPermissions(props.permission);
      }
      return permissionStore.hasAnyPermission(props.permission);
    }
    return permissionStore.hasPermission(props.permission);
  }

  return true;
});
</script>
