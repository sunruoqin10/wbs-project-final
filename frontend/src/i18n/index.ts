import { createI18n } from 'vue-i18n';
import messages, { availableLocales } from './locales';

// 从 localStorage 获取保存的语言设置，默认使用中文
const getSavedLocale = (): string => {
  const saved = localStorage.getItem('locale');
  if (saved && (saved === 'zh' || saved === 'ko')) {
    return saved;
  }
  // 尝试从浏览器语言设置中检测
  const browserLang = navigator.language.toLowerCase();
  if (browserLang.startsWith('ko')) {
    return 'ko';
  }
  return 'zh';
};

const i18n = createI18n({
  legacy: false,
  locale: getSavedLocale(),
  fallbackLocale: 'zh',
  messages,
  globalInjection: true
});

// 保存语言设置到 localStorage
export const setLocale = (locale: string) => {
  i18n.global.locale.value = locale;
  localStorage.setItem('locale', locale);
  // 更新 HTML lang 属性
  document.documentElement.lang = locale;
};

export const currentLocale = () => i18n.global.locale.value;

// 导出可用语言列表
export { availableLocales };

export default i18n;
