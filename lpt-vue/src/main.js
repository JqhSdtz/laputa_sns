// TODO 按照官方实例未能实现样式的按需引入
import 'ant-design-vue/dist/antd.less';

import {createApp} from 'vue';
import App from './App.vue';
import router from './router';
import {Button, ConfigProvider, Spin, Row, Col, BackTop, Form, Input, Upload,
    Popover} from 'ant-design-vue';
import {Tab, Tabs, Overlay, List, PullRefresh, Divider, Empty, ActionSheet, Cell, Tag, Icon,
    Popover as VPopOver, Dialog, Image as VanImage, Uploader, Form as VanForm, Field, Cascader,
    Button as VanButton, Popup, Switch} from 'vant';
import remHelper from '@/lib/js/uitls/rem-helper';
import {customDirectionList} from '@/lib/js/laputa/laputa-vue';
import globalMixins from '@/lib/js/global/global-mixins';

// 设置rem单位的相对大小
remHelper.initRem({
    pcWidth: 1024,
    mobWidth: 414
});

const app = createApp(App);
const antdUseList = [router, Button, ConfigProvider, Spin, Row, Col, BackTop, Form, Input, Upload,
    Popover];
antdUseList.forEach(item => {
    app.use(item);
});

const vantUseList = [Tab, Tabs, Overlay, List, PullRefresh, Divider, Empty, ActionSheet, Cell, Tag, Icon,
    VPopOver, Dialog, VanImage, Uploader, VanForm, Field, Cascader, VanButton, Popup, Switch];
vantUseList.forEach(item => {
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