<template>
  <div class="login-container">
    <!-- 粒子特效背景 -->
    <canvas ref="particleCanvas" class="particle-canvas"></canvas>

    <!-- 登录表单卡片 -->
    <div class="login-card">
      <!-- 语言切换按钮 -->
      <div class="language-switcher-wrapper">
        <LanguageSwitcher />
      </div>

      <!-- Logo和标题 -->
      <div class="login-header">
        <div class="logo">
          <svg class="logo-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
          </svg>
        </div>
        <h1 class="login-title">{{ $t('login.title') }}</h1>
        <p class="login-subtitle">{{ $t('login.subtitle') }}</p>
      </div>

      <!-- 登录表单 -->
      <form @submit.prevent="handleLogin" class="login-form">
        <div class="form-group">
          <div class="input-wrapper">
            <svg class="input-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
            </svg>
            <input
              v-model="loginForm.userId"
              type="text"
              :placeholder="$t('login.userIdPlaceholder')"
              required
              class="form-input"
            />
          </div>
        </div>

        <div class="form-group">
          <div class="input-wrapper">
            <svg class="input-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
            </svg>
            <input
              v-model="loginForm.password"
              :type="showPassword ? 'text' : 'password'"
              :placeholder="$t('login.passwordPlaceholder')"
              required
              class="form-input"
            />
            <button
              type="button"
              @click="showPassword = !showPassword"
              class="password-toggle"
            >
              <svg v-if="!showPassword" class="toggle-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
              </svg>
              <svg v-else class="toggle-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
              </svg>
            </button>
          </div>
        </div>

        <div class="form-options">
          <label class="checkbox-label">
            <input v-model="loginForm.remember" type="checkbox" class="checkbox" />
            <span>{{ $t('login.rememberMe') }}</span>
          </label>
        </div>

        <button type="submit" class="login-button" :disabled="isLoading">
          <span v-if="!isLoading">{{ $t('login.loginButton') }}</span>
          <span v-else class="loading-content">
            <span class="loading-spinner"></span>
            <span class="loading-text">{{ $t('login.loggingIn') }}</span>
          </span>
        </button>

        <!-- 测试账号提示 -->
        <div class="test-account-hint">
          <span>{{ $t('login.testAccount') }}: user001 / 1</span>
        </div>
      </form>

      <!-- 底部链接 -->
      <div class="login-footer">
        <p class="footer-text">
          {{ $t('login.footerText') }}
          <a href="#" class="footer-link">{{ $t('login.termsOfService') }}</a>
          {{ $t('common.and') }}
          <a href="#" class="footer-link">{{ $t('login.privacyPolicy') }}</a>
        </p>
      </div>
    </div>

    <!-- 背景装饰 -->
    <div class="bg-decoration">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import { useI18n } from 'vue-i18n';
import apiService from '@/services/api';
import LanguageSwitcher from '@/components/common/LanguageSwitcher.vue';

const router = useRouter();
const userStore = useUserStore();
const { t } = useI18n();

const particleCanvas = ref<HTMLCanvasElement>();
const showPassword = ref(false);
const isLoading = ref(false);

const loginForm = ref({
  // 测试账号: userId: 'user001', password: '1'
  userId: '',
  password: '',
  remember: false
});

// 粒子系统
let particles: Particle[] = [];
let animationId: number | null = null;

interface Particle {
  x: number;
  y: number;
  vx: number;
  vy: number;
  size: number;
  opacity: number;
}

const initParticles = () => {
  if (!particleCanvas.value) return;

  const canvas = particleCanvas.value;
  const ctx = canvas.getContext('2d');
  if (!ctx) return;

  // 设置canvas尺寸
  canvas.width = window.innerWidth;
  canvas.height = window.innerHeight;

  // 创建粒子
  const particleCount = Math.floor((canvas.width * canvas.height) / 10000);
  particles = [];

  for (let i = 0; i < particleCount; i++) {
    particles.push({
      x: Math.random() * canvas.width,
      y: Math.random() * canvas.height,
      vx: (Math.random() - 0.5) * 0.5,
      vy: (Math.random() - 0.5) * 0.5,
      size: Math.random() * 3 + 1,
      opacity: Math.random() * 0.5 + 0.2
    });
  }

  // 动画循环
  const animate = () => {
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    // 更新和绘制粒子
    particles.forEach((particle, index) => {
      // 更新位置
      particle.x += particle.vx;
      particle.y += particle.vy;

      // 边界检测
      if (particle.x < 0 || particle.x > canvas.width) particle.vx *= -1;
      if (particle.y < 0 || particle.y > canvas.height) particle.vy *= -1;

      // 绘制粒子
      ctx.beginPath();
      ctx.arc(particle.x, particle.y, particle.size, 0, Math.PI * 2);
      ctx.fillStyle = `rgba(100, 200, 255, ${particle.opacity})`;
      ctx.fill();

      // 绘制连线
      particles.slice(index + 1).forEach(otherParticle => {
        const dx = particle.x - otherParticle.x;
        const dy = particle.y - otherParticle.y;
        const distance = Math.sqrt(dx * dx + dy * dy);

        if (distance < 150) {
          ctx.beginPath();
          ctx.moveTo(particle.x, particle.y);
          ctx.lineTo(otherParticle.x, otherParticle.y);
          ctx.strokeStyle = `rgba(100, 200, 255, ${0.15 * (1 - distance / 150)})`;
          ctx.lineWidth = 1;
          ctx.stroke();
        }
      });
    });

    animationId = requestAnimationFrame(animate);
  };

  animate();
};

const handleResize = () => {
  if (particleCanvas.value) {
    particleCanvas.value.width = window.innerWidth;
    particleCanvas.value.height = window.innerHeight;
  }
};

const handleLogin = async () => {
  if (!loginForm.value.userId || !loginForm.value.password) {
    alert(`${t('login.validation.userIdRequired')}${t('common.and')}${t('login.validation.passwordRequired')}`);
    return;
  }

  isLoading.value = true;

  try {
    // 调用后端登录API，返回包含token和user的对象
    const loginResponse = await apiService.login(loginForm.value.userId, loginForm.value.password);

    // 保存用户信息和token到store
    userStore.setCurrentUser(loginResponse.user, loginResponse.token);

    // 登录成功，跳转到仪表盘或重定向路径
    const redirect = router.currentRoute.value.query.redirect as string || '/dashboard';
    router.push(redirect);
  } catch (error: any) {
    alert(error.message || t('login.error.loginFailed'));
  } finally {
    isLoading.value = false;
  }
};

onMounted(() => {
  initParticles();
  window.addEventListener('resize', handleResize);
});

onBeforeUnmount(() => {
  if (animationId !== null) {
    cancelAnimationFrame(animationId);
  }
  window.removeEventListener('resize', handleResize);
});
</script>

<style scoped>
.login-container {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.particle-canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
}

.bg-decoration {
  position: absolute;
  width: 100%;
  height: 100%;
  overflow: hidden;
  z-index: 0;
}

.circle {
  position: absolute;
  border-radius: 50%;
  background: linear-gradient(45deg, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.05));
  backdrop-filter: blur(10px);
  animation: float 20s infinite ease-in-out;
}

.circle-1 {
  width: 400px;
  height: 400px;
  top: -200px;
  left: -200px;
  animation-delay: 0s;
}

.circle-2 {
  width: 300px;
  height: 300px;
  bottom: -150px;
  right: -150px;
  animation-delay: 5s;
}

.circle-3 {
  width: 200px;
  height: 200px;
  top: 50%;
  right: 10%;
  animation-delay: 10s;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-30px) rotate(180deg);
  }
}

.login-card {
  position: relative;
  z-index: 10;
  width: 100%;
  max-width: 420px;
  margin: 20px;
  padding: 48px 40px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  animation: slideUp 0.6s ease-out;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.language-switcher-wrapper {
  position: absolute;
  top: 20px;
  right: 20px;
  z-index: 20;
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  margin-bottom: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  box-shadow: 0 8px 16px rgba(102, 126, 234, 0.3);
}

.logo-icon {
  width: 32px;
  height: 32px;
  color: white;
}

.login-title {
  font-size: 28px;
  font-weight: 700;
  color: #1a202c;
  margin-bottom: 8px;
}

.login-subtitle {
  font-size: 16px;
  color: #718096;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 16px;
  width: 20px;
  height: 20px;
  color: #a0aec0;
  pointer-events: none;
}

.form-input {
  width: 100%;
  padding: 14px 48px;
  font-size: 15px;
  color: #2d3748;
  background: #f7fafc;
  border: 2px solid transparent;
  border-radius: 12px;
  transition: all 0.3s ease;
}

.form-input:focus {
  outline: none;
  background: white;
  border-color: #667eea;
  box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1);
}

.form-input::placeholder {
  color: #a0aec0;
}

.password-toggle {
  position: absolute;
  right: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  color: #a0aec0;
  background: none;
  border: none;
  cursor: pointer;
  transition: color 0.3s ease;
}

.password-toggle:hover {
  color: #667eea;
}

.toggle-icon {
  width: 20px;
  height: 20px;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #718096;
  cursor: pointer;
}

.checkbox {
  width: 16px;
  height: 16px;
  cursor: pointer;
  accent-color: #667eea;
}

.login-button {
  width: 100%;
  padding: 16px;
  font-size: 16px;
  font-weight: 600;
  color: white;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 8px 16px rgba(102, 126, 234, 0.3);
}

.login-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 12px 24px rgba(102, 126, 234, 0.4);
}

.login-button:active:not(:disabled) {
  transform: translateY(0);
}

.login-button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.test-account-hint {
  text-align: center;
  padding: 12px;
  background: #f0f4ff;
  border-radius: 8px;
  font-size: 13px;
  color: #667eea;
  border: 1px dashed #667eea;
}

.loading-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.loading-spinner {
  display: inline-block;
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.loading-text {
  font-size: 16px;
  font-weight: 600;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.login-footer {
  margin-top: 32px;
  text-align: center;
}

.footer-text {
  font-size: 13px;
  color: #a0aec0;
}

.footer-link {
  color: #667eea;
  text-decoration: none;
  font-weight: 500;
}

.footer-link:hover {
  text-decoration: underline;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .login-card {
    margin: 16px;
    padding: 32px 24px;
  }

  .login-title {
    font-size: 24px;
  }

  .circle-1 {
    width: 300px;
    height: 300px;
    top: -150px;
    left: -150px;
  }

  .circle-2 {
    width: 200px;
    height: 200px;
    bottom: -100px;
    right: -100px;
  }
}
</style>
