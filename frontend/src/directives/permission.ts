import { Directive, DirectiveBinding } from 'vue';
import { usePermissionStore } from '@/stores/permission';

export const vPermission: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding<string | string[]>) {
    const permissionStore = usePermissionStore();
    const value = binding.value;

    let hasPermission = false;

    if (typeof value === 'string') {
      hasPermission = permissionStore.hasPermission(value);
    } else if (Array.isArray(value)) {
      if (binding.modifiers.all) {
        hasPermission = permissionStore.hasAllPermissions(value);
      } else {
        hasPermission = permissionStore.hasAnyPermission(value);
      }
    }

    if (!hasPermission) {
      el.style.display = 'none';
      el.setAttribute('data-permission-hidden', 'true');
    }
  },

  updated(el: HTMLElement, binding: DirectiveBinding<string | string[]>) {
    const permissionStore = usePermissionStore();
    const value = binding.value;

    let hasPermission = false;

    if (typeof value === 'string') {
      hasPermission = permissionStore.hasPermission(value);
    } else if (Array.isArray(value)) {
      if (binding.modifiers.all) {
        hasPermission = permissionStore.hasAllPermissions(value);
      } else {
        hasPermission = permissionStore.hasAnyPermission(value);
      }
    }

    if (hasPermission) {
      el.style.display = '';
      el.removeAttribute('data-permission-hidden');
    } else {
      el.style.display = 'none';
      el.setAttribute('data-permission-hidden', 'true');
    }
  }
};

export const vRole: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding<string | string[]>) {
    const permissionStore = usePermissionStore();
    const value = binding.value;
    const currentRole = permissionStore.currentRole;

    let hasRole = false;

    if (typeof value === 'string') {
      hasRole = currentRole === value;
    } else if (Array.isArray(value)) {
      hasRole = value.includes(currentRole);
    }

    if (!hasRole) {
      el.style.display = 'none';
      el.setAttribute('data-role-hidden', 'true');
    }
  },

  updated(el: HTMLElement, binding: DirectiveBinding<string | string[]>) {
    const permissionStore = usePermissionStore();
    const value = binding.value;
    const currentRole = permissionStore.currentRole;

    let hasRole = false;

    if (typeof value === 'string') {
      hasRole = currentRole === value;
    } else if (Array.isArray(value)) {
      hasRole = value.includes(currentRole);
    }

    if (hasRole) {
      el.style.display = '';
      el.removeAttribute('data-role-hidden');
    } else {
      el.style.display = 'none';
      el.setAttribute('data-role-hidden', 'true');
    }
  }
};

export const vProjectPermission: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding<{ projectId: string; permission: string }>) {
    const permissionStore = usePermissionStore();
    const { projectId, permission } = binding.value;

    if (!projectId || !permission) {
      return;
    }

    const hasPermission = permissionStore.hasProjectPermission(projectId, permission);

    if (!hasPermission) {
      el.style.display = 'none';
      el.setAttribute('data-project-permission-hidden', 'true');
    }
  },

  updated(el: HTMLElement, binding: DirectiveBinding<{ projectId: string; permission: string }>) {
    const permissionStore = usePermissionStore();
    const { projectId, permission } = binding.value;

    if (!projectId || !permission) {
      return;
    }

    const hasPermission = permissionStore.hasProjectPermission(projectId, permission);

    if (hasPermission) {
      el.style.display = '';
      el.removeAttribute('data-project-permission-hidden');
    } else {
      el.style.display = 'none';
      el.setAttribute('data-project-permission-hidden', 'true');
    }
  }
};
