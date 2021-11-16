<template>
	<a-config-provider :locale="locale" :getPopupContainer="getPopupContainer">
		<main-container>
			<div id="main-view" style="position: absolute; height: 98%; top: 2%; width: 80%; left: 10%">
				<router-view ref="mainView" v-slot="{ Component }">
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
		<loading-area id="loading-area" :size="showDrawer ? 'default' : 'large'"></loading-area>
		<drawer-container>
			<a-drawer :visible="showDrawer" placement="left" :closable="false"
			          @close="showDrawer = false" :width="drawerWidth">
				<router-view v-slot="{ Component }" name="leftDrawer">
					<keep-alive :exclude="noCacheList">
						<component :is="Component"/>
					</keep-alive>
				</router-view>
			</a-drawer>
		</drawer-container>
		<right-outlined ref="drawerSwitcher" class="with-transition"
		                :rotate="drawerWidth === '75%' && showDrawer ? 180 : 0"
		                style="position: fixed; bottom: 50%; left: 0; font-size: 2rem; z-index: 1001"
		                @click="onDrawerSwitcherClick" @mouseenter="onDrawerSwitcherMouseEnter" @mouseleave="onDrawerSwitcherMouseLeave"/>
		<float-menu id="main-bar"/>
		<fixed-menu/>
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
import FloatMenu from '@/modules/blog/components/menu/float/FloatMenu';
import FixedMenu from '@/modules/blog/components/menu/fixed/FixedMenu';
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
		FixedMenu,
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
			drawerWidth: '33%',
			backgroundImage: description.backgroundImage,
			showDrawer: toRef(global.states.blog, 'showDrawer'),
			locale: zhCN,
			noCacheList
		};
	},
	watch: {
		showDrawer(isShow) {
			const drawerSwitcher = this.$refs.drawerSwitcher;
			this.$nextTick(() => {
				if (!isShow) {
					drawerSwitcher.style.left = '0';
					drawerSwitcher.style.background = 'none';
				} else {
					drawerSwitcher.style.background = 'white';
					drawerSwitcher.style.left = this.drawerWidth;
					// setTimeout(() => drawerSwitcher.style.left = this.drawerWidth, 300);
				}
			});
		}
	},
	computed: {
		drawerWidthNum() {
			const num = parseFloat(this.drawerWidth);
			return num / 100;
		}
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
		document.body.style.backgroundImage = this.backgroundImage;
		global.methods.prompt = this.$refs.prompt.prompt;
		global.methods.setDrawerWidth = this.setDrawerWidth;
		window.addEventListener('resize', () => {
			this.onResize();
		});
		this.onResize();
	},
	methods: {
		onResize() {
			global.states.style.drawerWidth = document.body.clientWidth * this.drawerWidthNum;
			global.states.style.mainHeight = document.body.clientHeight * 0.98;
			global.states.style.blogMainWidth = document.body.clientWidth * 0.8;
			global.states.style.blogMainLeft = document.body.clientWidth * 0.1;
		},
		getPopupContainer(el, dialogContext) {
			if (dialogContext) {
				return dialogContext.getDialogWrap();
			} else {
				return document.body;
			}
		},
		onDrawerSwitcherMouseEnter() {
			const drawerSwitcher = this.$refs.drawerSwitcher;
			drawerSwitcher.style.opacity = 1;
		},
		onDrawerSwitcherMouseLeave() {
			const drawerSwitcher = this.$refs.drawerSwitcher;
			drawerSwitcher.style.opacity = 0.75;
		},
		onDrawerSwitcherClick() {
			if (!this.showDrawer) {
				this.showDrawer = true;
			} else {
				this.setDrawerWidth();
			}
		},
		setDrawerWidth(width) {
			if (this.drawerWidth === width) return;
			if (width) {
				this.drawerWidth = width;
			} else {
				if (this.drawerWidth === '75%') this.drawerWidth = '33%';
				else this.drawerWidth = '75%';
			}
			const drawerSwitcher = this.$refs.drawerSwitcher;
			this.onResize();
			this.$nextTick(() => {
				drawerSwitcher.style.left = this.drawerWidth;
			});
		}
	}
}
</script>

<style lang="less">
#app {
	height: 100%;
	font-size: 1rem;
}

.ant-drawer-body {
	font-size: 1rem;
}

.van-tab {
	font-size: 1rem;
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
	left: 3%;
	bottom: 7%;
}

.ant-drawer-body {
	padding: 0;
}

.ant-badge {
	font-size: inherit;
}

.ant-drawer-content-wrapper {
	transition: width 0.3s cubic-bezier(0.7, 0.3, 0.1, 1), transform 0.3s  cubic-bezier(0.7, 0.3, 0.1, 1);
}

.post-list {
	background: none !important;
}

#main-view {
	background-color: rgba(255, 255, 255, 0.5);
	box-shadow: 0 0 10px 6px rgba(0, 0, 0, 0.25);
	// backdrop-filter: blur(20px);会影响fixed元素布局
}

.with-transition {
	transition: left 0.3s cubic-bezier(0.7, 0.3, 0.1, 1);
}

.width-transition {
	transition: width 0.3s cubic-bezier(0.7, 0.3, 0.1, 1);
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

.ant-modal-wrap, .ant-modal-mask, .van-popup {
	z-index: 3000;
}

.ant-drawer-left.ant-drawer-open .ant-drawer-content-wrapper {
	box-shadow: 0 0 10px 12px rgb(0 0 0 / 25%) !important;
}
</style>
