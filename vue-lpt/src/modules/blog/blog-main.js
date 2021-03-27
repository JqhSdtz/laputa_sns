// TODO 按照官方实例未能实现样式的按需引入
import 'ant-design-vue/dist/antd.less';

import {createApp} from 'vue';
import BlogApp from './BlogApp.vue';
import router from './router';
import {ConfigProvider, Button, Row, Col, Spin, Form, Input, Badge, BackTop, Drawer, Popover} from 'ant-design-vue';
import {
    Tab, Tabs, Overlay, List, PullRefresh, Divider, Empty, ActionSheet, Cell, Tag, Icon,
    Popover as VPopOver, Dialog, Image as VanImage, Button as VanButton,
    Popup, Switch, Search, Checkbox, Grid, GridItem, DatetimePicker
} from 'vant';
import remHelper from '@/lib/js/uitls/rem-helper';
import globalMixins from '@/lib/js/global/global-mixins';
import globalDirectives from '@/lib/js/global/global-directives';
import globalVariables from '@/lib/js/global/global-vars';
import lpt from '@/lib/js/laputa/laputa';
import JsonViewer from 'vue3-json-viewer';
import VMdPreview from '@kangc/v-md-editor/lib/preview';
import '@kangc/v-md-editor/lib/style/preview.css';
import githubTheme from '@kangc/v-md-editor/lib/theme/github.js';
import axios from "axios";

// 设置rem单位的相对大小
remHelper.initRem({
    pcWidth: 1024,
    mobWidth: 414,
    responsive: true
});

const app = createApp(BlogApp);

app.use(router);
app.use(JsonViewer);
VMdPreview.use(githubTheme);
app.use(VMdPreview);

const antdUseList = [ConfigProvider, Button, Row, Col, Spin, Form, Input, Badge, BackTop,
    Popover, Drawer];
antdUseList.forEach(item => {
    app.use(item);
});

const vantUseList = [Tab, Tabs, Overlay, List, PullRefresh, Divider, Empty, ActionSheet, Cell, Tag, Icon,
    VPopOver, Dialog, VanImage, VanButton, Popup, Switch, Search,
    Checkbox, Grid, GridItem, DatetimePicker];
vantUseList.forEach(item => {
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

globalVariables.env = 'blog';

axios.get('/static/blog/description.json').then((result) => {
    globalVariables.blog.desc = result.data;
    lpt.categoryServ.rootCategoryId = result.data.rootCategoryId;
    app.mount('#app');
});