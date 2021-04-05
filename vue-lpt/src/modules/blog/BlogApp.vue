<template>
	<a-config-provider :locale="locale" :getPopupContainer="getPopupContainer">
		<main-container>
			<div style="position: absolute; height: 96%; top: 2%; width: 80%; left: 10%">
				<router-view id="main-view" ref="mainView" v-slot="{ Component }"
				             :style="{marginBottom: mainViewOffsetBottom + 'px'}">
					<keep-alive :exclude="noCacheList">
						<component :is="Component"/>
					</keep-alive>
				</router-view>
			</div>
		</main-container>
		<div style="position: absolute; top: 100%; width: 100%; padding: 1rem; background-color: white">
			<p style="text-align: center; font-size: 0.85rem">鲁ICP备19009966号</p>
			<p style="text-align: center; font-size: 0.85rem">jqhsdtz@foxmail.com</p>
		</div>
		<loading-area id="loading-area"></loading-area>
		<drawer-container>
			<a-drawer :visible="showDrawer" placement="left" :closable="false"
			          @close="showDrawer = false" width="33%">
				<router-view v-slot="{ Component }" name="leftDrawer">
					<keep-alive :exclude="noCacheList">
						<component :is="Component"/>
					</keep-alive>
				</router-view>
			</a-drawer>
		</drawer-container>
		<div id="right-hidden-side" style="position: fixed; left: 0; height: 100%; width: 3rem;">
			<right-outlined style="position: absolute; bottom: 50%; font-size: 2rem" @click="showDrawer = true"/>
		</div>
		<float-menu id="main-bar"/>
		<prompt-dialog ref="prompt"/>
	</a-config-provider>
</template>

<script>
import zhCN from 'ant-design-vue/es/locale/zh_CN';
import {registerCheckSignFailCallback} from '@/lib/js/laputa/laputa-vue';
import {Modal} from 'ant-design-vue';
import {
	RightOutlined
} from '@ant-design/icons-vue';
import FloatMenu from '@/modules/blog/components/menu/FloatMenu';
import MainContainer from '@/modules/blog/components/container/MainContainer';
import DrawerContainer from '@/modules/blog/components/container/DrawerContainer';
import LoadingArea from '@/components/global/LoadingArea';
import PromptDialog from '@/components/global/PromptDialog';
import {noCacheList} from '@/modules/lpt/router';
import global from '@/lib/js/global';
import {toRef} from 'vue';
import description from './description';

export default {
	name: 'App',
	components: {
		FloatMenu,
		LoadingArea,
		PromptDialog,
		MainContainer,
		DrawerContainer,
		RightOutlined
	},
	provide: {
		lptContainer: 'default'
	},
	data() {
		return {
			coverUrl: description.cover.url,
			mainViewOffsetBottom: global.vars.blog.style.mainViewOffsetBottom,
			showDrawer: toRef(global.states.blog, 'showDrawer'),
			locale: zhCN,
			noCacheList
		};
	},
	created() {
		registerCheckSignFailCallback(() => {
			// 注册全局未登录提示
			const ref = this;
			Modal.confirm({
				title: '是否登录？',
				// icon: createVNode(ExclamationCircleOutlined),
				content: '登录打开新世界！',
				onOk() {
					ref.$router.push({path: '/sign_in'});
				}
			});
		});
	},
	mounted() {
		document.body.style.backgroundImage = `url(${this.coverUrl})`;
		global.methods.prompt = this.$refs.prompt.prompt;
		global.states.style.drawerWidth = document.body.clientWidth * 0.33;
		global.states.style.blogMainWidth = this.$refs.mainView.clientWidth;
		window.addEventListener('resize', () => {
			global.states.style.drawerWidth = document.body.clientWidth * 0.33;
		});
	},
	methods: {
		getPopupContainer(el, dialogContext) {
			if (dialogContext) {
				return dialogContext.getDialogWrap();
			} else {
				return document.body;
			}
		}
	}
}
</script>

<style lang="less">
#app {
	height: 100%;
}

body, html {
	width: 100%;
	height: 100%;
}

body {
	margin: 0;
	height: 100%;
	width: 100%;
	background-size: cover;
	background-position: bottom;
}

body::-webkit-scrollbar {
	display: none;
}

#loading-area {
	position: fixed;
	left: 5%;
	bottom: 11%;
}

.ant-drawer-body {
	padding: 0;
}

.post-list {
	background: none !important;
}

#main-view {
	background-color: rgba(255, 255, 255, 0.5);
}

div:not(.with-scroll-bar)::-webkit-scrollbar {
	display: none;
}

.with-scroll-bar::-webkit-scrollbar {
	width: 5px;
	height: 16px;
	background-color: #F5F5F5;
}

.with-scroll-bar::-webkit-scrollbar-track {
	-webkit-box-shadow: inset 0 0 6px rgba(0, 0, 0, 0.3);
	border-radius: 10px;
	background-color: #F5F5F5;
}

.with-scroll-bar::-webkit-scrollbar-thumb {
	border-radius: 10px;
	-webkit-box-shadow: inset 0 0 6px rgba(0, 0, 0, .3);
	background-color: #555;
}

.van-pull-refresh {
	user-select: inherit !important;
}

.van-swipe {
	cursor: inherit !important;
	user-select: inherit !important;
}

.ant-modal-wrap, .ant-modal-mask {
	z-index: 3000;
}
</style>
