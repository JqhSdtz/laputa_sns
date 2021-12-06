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
			<a-drawer id="blog-drawer" :visible="showDrawer" placement="left" :closable="false"
			          @close="showDrawer = false" :width="drawerWidth">
				<router-view v-slot="{ Component }" name="leftDrawer">
					<keep-alive :exclude="noCacheList">
						<component :is="Component"/>
					</keep-alive>
				</router-view>
				<prompt-dialog v-if="drawerMounted" ref="drawerPrompt" teleport="#blog-drawer .ant-drawer-content-wrapper"/>
			</a-drawer>
		</drawer-container>
		<right-outlined id="drawer-switcher" class="with-transition" ref="drawerSwitcher"
		                :rotate="drawerWidth === '75%' && showDrawer ? 180 : 0"
		                @click="onDrawerSwitcherClick" @mouseenter="onDrawerSwitcherMouseEnter" @mouseleave="onDrawerSwitcherMouseLeave"/>
		<!-- <float-menu/> -->
		<fixed-menu/>
		<tab-strip/>
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
import FixedMenu from '@/modules/blog/components/menu/fixed/FixedMenu';
import TabStrip from '@/modules/blog/components/tabs/TabStrip';
import MainContainer from '@/modules/blog/components/container/MainContainer';
import DrawerContainer from '@/modules/blog/components/container/DrawerContainer';
import LoadingArea from '@/components/global/LoadingArea';
import PromptDialog from '@/components/global/PromptDialog';
import {noCacheList} from '@/modules/lpt/router';
import global from '@/lib/js/global';
import {toRef} from 'vue';
import description from './description';
import lpt from '@/lib/js/laputa/laputa';
import remHelper from '@/lib/js/uitls/rem-helper';

export default {
	name: 'App',
	components: {
		FixedMenu,
		TabStrip,
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
			drawerMounted: false,
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
				this.drawerMounted = true;
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
					ref.$router.push({path: '/home/mine'});
				}
			});
		});
	},
	mounted() {
		if (remHelper.isMobile()) {
			Modal.confirm({
				title: '是否前往移动版？',
				content: '移动版更适合您的屏幕大小',
				onOk: () => {
					const baseMobUrl = 'https://lpt.jqh.zone';
					const defaultMobUrl = baseMobUrl + '/category_detail/' + lpt.categoryServ.rootCategoryId;
					const route = this.$route;
					if (!route || !route.meta) {
						window.location.href = defaultMobUrl;
					} else {
						// 判断访问的是哪种页面（目录详情、帖子详情、目录首页），从而跳转到对应的移动版页面
						let url = defaultMobUrl;
						const mainPath = route.meta.fullMainPath;
						const params = route.params;
						if (mainPath.indexOf('category_detail') >= 0 || mainPath.indexOf('index') >= 0) {
							url = baseMobUrl + '/category_detail/' + params.blogCategoryId;
						} else if (mainPath.indexOf('post_detail') >= 0) {
							url = baseMobUrl + '/post_detail/' + params.blogPostId;
						}
						window.location.href = url;
					}
				}
			});
		}
		document.body.style.backgroundImage = this.backgroundImage;
		global.methods.prompt = this.$refs.prompt.prompt;
		global.methods.getPrompt = (container) => {
			if (container === 'blogDrawer') {
				return this.$refs.drawerPrompt.prompt;
			} else {
				return this.$refs.prompt.prompt;
			}
		};
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

#drawer-switcher {
	position: absolute;
	bottom: 50%; 
	left: 0; 
	font-size: 2rem; 
	z-index: 1001;
	color: rgba(21, 23, 26, 0.25);
}

#drawer-switcher:hover {
	color: rgba(21, 23, 26, 1);
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
	box-shadow: inset 0 0 6px rgba(0, 0, 0, 0.3);
	border-radius: 10px;
	background-color: #F5F5F5;
}

.with-scroll-bar::-webkit-scrollbar-thumb {
	border-radius: 10px;
	-webkit-box-shadow: inset 0 0 6px rgba(0, 0, 0, .3);
	box-shadow: inset 0 0 6px rgba(0, 0, 0, .3);
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

.van-picker-column {
	// vant是移动端框架，靠滑动不靠滚动，在blog环境下开启滚动
	overflow-y: scroll;
}

#blog-drawer .van-overlay {
	background-color: rgba(0, 0, 0, 0.45);
	position: absolute;
}

#blog-drawer .van-popup {
	position: absolute;
}
</style>
