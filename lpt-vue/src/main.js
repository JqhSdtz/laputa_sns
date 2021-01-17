import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { Button, ConfigProvider, Spin } from 'ant-design-vue'
import 'ant-design-vue/lib/button/style'

const app = createApp(App);
app.use(router);
app.use(Button).use(ConfigProvider).use(Spin);
app.mount('#app');


