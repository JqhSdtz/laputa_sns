// TODO 按照官方实例未能实现样式的按需引入

import description from './description';
import {createApp} from 'vue';
import LptApp from './LptApp.vue';
import router from './router';
import {Button, ConfigProvider, Spin, Row, Col, BackTop, Form, Mentions, Input, Upload,
    Popover, Badge} from 'ant-design-vue';
import {Tab, Tabs, Overlay, List, PullRefresh, Divider, Empty, ActionSheet, Cell, Tag, Icon,
    Popover as VPopOver, Dialog, Image as VanImage, Uploader, Form as VanForm, Field, Cascader,
    Button as VanButton, Popup, Switch, Search, Checkbox, Grid, GridItem, DatetimePicker,
    NavBar} from 'vant';
import remHelper from '@/lib/js/uitls/rem-helper';
import global from "@/lib/js/global";
import globalMixins from '@/lib/js/global/global-mixins';
import globalDirectives from '@/lib/js/global/global-directives';
import globalVariables from '@/lib/js/global/global-vars';
import JsonViewer from 'vue3-json-viewer';
import VMdPreview from '@kangc/v-md-editor/lib/preview';
import githubTheme from '@kangc/v-md-editor/lib/theme/github.js';
import '@kangc/v-md-editor/lib/style/preview.css';
import '@kangc/v-md-editor/lib/theme/style/github.css';
import markdownItKatex from '@iktakahiro/markdown-it-katex';
import '@iktakahiro/markdown-it-katex/node_modules/katex/dist/katex.min.css';
// 引入所有语言包
import hljs from 'highlight.js';

hljs.registerAliases('cpp', {languageName: 'cpp'});
VMdPreview.use(githubTheme, {
    Hljs: hljs,
    extend(md) {
        md.use(markdownItKatex);
    }
});

// 设置rem单位的相对大小
remHelper.initRem({
    pcWidth: 1024,
    mobWidth: 414,
    responsive: true
});
global.events.emit('remInitialized');

globalVariables.env = 'lpt';
const app = createApp(LptApp);

app.use(router);
app.use(JsonViewer);
app.use(VMdPreview);

const antdUseList = [Button, ConfigProvider, Spin, Row, Col, BackTop, Form, Mentions, Input, Upload,
    Popover, Badge];
antdUseList.forEach(item => {
    app.use(item);
});

const vantUseList = [Tab, Tabs, Overlay, List, PullRefresh, Divider, Empty, ActionSheet, Cell, Tag, Icon,
    VPopOver, Dialog, VanImage, Uploader, VanForm, Field, Cascader, VanButton, Popup, Switch, Search,
    Checkbox, Grid, GridItem, DatetimePicker, NavBar];
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

global.methods.setOption({
    moduleTitle: description.title
});

app.mount('#app');