<template>
  <div class="language-switcher" ref="switcherRef">
    <button
      @click="toggleDropdown"
      class="language-button"
      :class="{ 'is-open': isOpen }"
      :title="currentLocaleInfo?.name"
    >
      <span class="flag">{{ currentLocaleInfo?.flag }}</span>
      <span class="lang-name">{{ currentLocaleInfo?.name }}</span>
      <svg class="chevron" :class="{ 'rotate-180': isOpen }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
      </svg>
    </button>

    <transition name="dropdown">
      <div v-if="isOpen" class="dropdown-menu">
        <div class="dropdown-header">
          <span class="dropdown-title">{{ t('languageSwitcher.selectLanguage') }}</span>
        </div>
        <div class="dropdown-content">
          <button
            v-for="locale in availableLocales"
            :key="locale.code"
            @click="selectLocale(locale.code)"
            class="locale-option"
            :class="{ 'is-active': currentLocale === locale.code }"
          >
            <span class="locale-flag">{{ locale.flag }}</span>
            <span class="locale-name">{{ locale.name }}</span>
            <svg v-if="currentLocale === locale.code" class="check-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
            </svg>
          </button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useI18n } from 'vue-i18n';
import { availableLocales, setLocale } from '@/i18n';

const { t, locale } = useI18n();

const isOpen = ref(false);
const switcherRef = ref<HTMLElement | null>(null);

const currentLocale = computed(() => locale.value);

const currentLocaleInfo = computed(() => {
  return availableLocales.find(l => l.code === currentLocale.value);
});

const toggleDropdown = () => {
  isOpen.value = !isOpen.value;
};

const selectLocale = (localeCode: string) => {
  setLocale(localeCode);
  isOpen.value = false;
};

// 点击外部关闭下拉菜单
const handleClickOutside = (event: MouseEvent) => {
  if (switcherRef.value && !switcherRef.value.contains(event.target as Node)) {
    isOpen.value = false;
  }
};

onMounted(() => {
  document.addEventListener('click', handleClickOutside);
});

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside);
});
</script>

<style scoped>
.language-switcher {
  position: relative;
  display: inline-block;
}

.language-button {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
  white-space: nowrap;
}

.language-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

.language-button:active {
  transform: translateY(0);
}

.language-button.is-open {
  background: linear-gradient(135deg, #764ba2 0%, #667eea 100%);
  box-shadow: 0 6px 20px rgba(118, 75, 162, 0.4);
}

.flag {
  font-size: 1.25rem;
  line-height: 1;
}

.lang-name {
  font-size: 0.875rem;
}

.chevron {
  width: 1rem;
  height: 1rem;
  transition: transform 0.3s ease;
}

.chevron.rotate-180 {
  transform: rotate(180deg);
}

/* 下拉菜单样式 */
.dropdown-menu {
  position: absolute;
  top: calc(100% + 0.5rem);
  right: 0;
  min-width: 200px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  z-index: 1000;
}

.dropdown-header {
  padding: 1rem 1.25rem 0.75rem;
  border-bottom: 1px solid #e5e7eb;
  background: linear-gradient(to bottom, #f9fafb, white);
}

.dropdown-title {
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #6b7280;
}

.dropdown-content {
  padding: 0.5rem;
}

.locale-option {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  width: 100%;
  padding: 0.75rem 1rem;
  background: transparent;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
}

.locale-option:hover {
  background: #f3f4f6;
}

.locale-option.is-active {
  background: linear-gradient(135deg, #eef2ff 0%, #e0e7ff 100%);
}

.locale-option.is-active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 60%;
  background: linear-gradient(to bottom, #667eea, #764ba2);
  border-radius: 0 2px 2px 0;
}

.locale-flag {
  font-size: 1.125rem;
  line-height: 1;
}

.locale-name {
  flex: 1;
  text-align: left;
  font-size: 0.875rem;
  font-weight: 500;
  color: #1f2937;
}

.locale-option.is-active .locale-name {
  color: #667eea;
  font-weight: 600;
}

.check-icon {
  width: 1rem;
  height: 1rem;
  color: #667eea;
  flex-shrink: 0;
}

/* 下拉动画 */
.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.dropdown-enter-from {
  opacity: 0;
  transform: translateY(-10px) scale(0.95);
}

.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-10px) scale(0.95);
}

/* 响应式 */
@media (max-width: 640px) {
  .language-button {
    padding: 0.5rem 0.75rem;
  }

  .lang-name {
    display: none;
  }

  .dropdown-menu {
    min-width: 180px;
  }
}
</style>
