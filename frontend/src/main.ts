import { createApp } from 'vue';
import { createPinia } from 'pinia';
import router from './router';
import App from './App.vue';
import i18n from './i18n';
import './assets/styles/main.css';
import { vPermission, vRole, vProjectPermission } from './directives/permission';

const app = createApp(App);
const pinia = createPinia();

app.use(pinia);
app.use(router);
app.use(i18n);

app.directive('permission', vPermission);
app.directive('role', vRole);
app.directive('project-permission', vProjectPermission);

app.mount('#app');
