import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { Button, ConfigProvider, Spin, Row, Col} from 'ant-design-vue'
import 'ant-design-vue/dist/antd.less'
import remHelper from '@/lib/js/rem-helper'
import { checkSignDirection } from '@/lib/js/laputa-vue'

remHelper.initRem({
    pcWidth: 1024,
    mobWidth: 414
});

const app = createApp(App);
const useList = [router, Button, ConfigProvider, Spin, Row, Col];

useList.forEach(item => {
    app.use(item);
});

app.directive('checkSign', checkSignDirection);

app.mount('#app');