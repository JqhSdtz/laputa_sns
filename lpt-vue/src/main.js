import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { Button, ConfigProvider, Spin, Row, Col} from 'ant-design-vue'
import 'ant-design-vue/dist/antd.less'

const app = createApp(App);
app.use(router);
app.use(Button).use(ConfigProvider).use(Spin).use(Row).use(Col);
app.mount('#app');


