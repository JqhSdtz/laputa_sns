import {createApp} from 'vue';
import App from './App.vue';
import router from './router';
import {Button, ConfigProvider, Spin, Row, Col, BackTop, Form, Input, Upload, Popover, Tabs} from 'ant-design-vue';
import 'ant-design-vue/dist/antd.less';
import remHelper from '@/lib/js/uitls/rem-helper';
import {customDirectionList} from '@/lib/js/laputa/laputa-vue';
import globalMixins from '@/lib/js/global/global-mixins';
import {initLaputa} from '@/lib/js/laputa/laputa-vue';

initLaputa();

// 设置rem单位的相对大小
remHelper.initRem({
    pcWidth: 1024,
    mobWidth: 414
});

const app = createApp(App);
const useList = [router, Button, ConfigProvider, Spin, Row, Col, BackTop, Form, Input, Upload, Popover, Tabs];
useList.forEach(item => {
    app.use(item);
});

// 添加全局自定义指令
customDirectionList.forEach(item => {
    app.directive(item.name, item.handler);
});

// 添加全局混入选项
globalMixins.forEach(mixin => {
   app.mixin(mixin);
});

app.mount('#app');