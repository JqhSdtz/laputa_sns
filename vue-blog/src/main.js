// TODO 按照官方实例未能实现样式的按需引入
import 'ant-design-vue/dist/antd.less';

import {createApp} from 'vue';
import App from './App.vue';
import router from './router';
import {Button, Affix} from 'ant-design-vue';

import remHelper from '@/lib/js/uitls/rem-helper';
import globalMixins from '@/lib/js/global/global-mixins';
import globalDirectives from '@/lib/js/global/global-directives';

// 设置rem单位的相对大小
remHelper.initRem({
    pcWidth: 1024,
    mobWidth: 414
});

const app = createApp(App);

app.use(router);

const antdUseList = [Button, Affix];
antdUseList.forEach(item => {
    app.use(item);
});

// 添加全局自定义指令
globalDirectives.forEach(item => {
    app.directive(item.name, item.handler);
});

// 添加全局混入选项
globalMixins.forEach(mixin => {
    app.mixin(mixin);
});

app.mount('#app');