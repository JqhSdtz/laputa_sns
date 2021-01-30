import {createApp} from 'vue';
import App from './App.vue';
import router from './router';
import {Button, ConfigProvider, Spin, Row, Col, BackTop, Form, Input, Upload} from 'ant-design-vue';
import 'ant-design-vue/dist/antd.less';
import remHelper from '@/lib/js/rem-helper';
import {checkSignDirection, testDirection} from '@/lib/js/laputa-vue';
import globalMixins from '@/lib/js/global-mixins';
import {initLaputa} from '@/lib/js/laputa-vue';

initLaputa();

// 设置rem单位的相对大小
remHelper.initRem({
    pcWidth: 1024,
    mobWidth: 414
});

const app = createApp(App);
const useList = [router, Button, ConfigProvider, Spin, Row, Col, BackTop, Form, Input, Upload];
useList.forEach(item => {
    app.use(item);
});

// 全局添加权限校验指令
const customDirectionList = [
    {
        name: 'checkSign',
        handler: checkSignDirection
    },
    {
        name: 'test',
        handler: testDirection
    }
];
customDirectionList.forEach(item => {
    app.directive(item.name, item.handler);
});

// 添加全局混入选项
globalMixins.forEach(mixin => {
   app.mixin(mixin);
});

app.mount('#app');