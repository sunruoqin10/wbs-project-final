export const required = (value: any) => {
  if (value === null || value === undefined || value === '') {
    return '此项为必填项';
  }
  return true;
};

export const email = (value: string) => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(value)) {
    return '请输入有效的邮箱地址';
  }
  return true;
};

export const minLength = (min: number) => (value: string) => {
  if (value.length < min) {
    return `最少需要 ${min} 个字符`;
  }
  return true;
};

export const maxLength = (max: number) => (value: string) => {
  if (value.length > max) {
    return `最多允许 ${max} 个字符`;
  }
  return true;
};

export const dateRange = (start: string, end: string) => {
  if (new Date(start) > new Date(end)) {
    return '开始日期不能晚于结束日期';
  }
  return true;
};
