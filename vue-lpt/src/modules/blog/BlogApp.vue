<template>
	<a-config-provider :locale="locale" :getPopupContainer="getPopupContainer">
		<router-view v-slot="{ Component }">
			<keep-alive :exclude="noCacheList">
				<component :is="Component"/>
			</keep-alive>
		</router-view>
		<loading-area id="loading-area"></loading-area>
		<a-drawer :visible="showDrawer" placement="left" :closable="false"
		          @close="showDrawer = false" width="33%">
			<router-view v-slot="{ Component }" name="leftDrawer">
				<keep-alive :exclude="noCacheList">
					<component :is="Component"/>
				</keep-alive>
			</router-view>
		</a-drawer>
		<float-menu id="main-bar"/>
		<prompt-dialog ref="prompt"/>
	</a-config-provider>
</template>

<script>
import zhCN from 'ant-design-vue/es/locale/zh_CN';
import {registerCheckSignFailCallback} from '@/lib/js/laputa/laputa-vue';
import {Modal} from 'ant-design-vue';
import FloatMenu from '@/modules/blog/components/menu/FloatMenu'
import LoadingArea from '@/components/global/LoadingArea';
import PromptDialog from '@/components/global/PromptDialog';
import {noCacheList} from '@/modules/lpt/router';
import global from '@/lib/js/global';

export default {
	name: 'App',
	components: {
		FloatMenu,
		LoadingArea,
		PromptDialog
	},
	watch: {
		$route() {
			this.checkRoute();
		},
		showDrawer(newValue) {
			global.states.blog.showDrawer = newValue;
		}
	},
	data() {
		return {
			showDrawer: false,
			locale: zhCN,
			noCacheList
		};
	},
	created() {
		this.checkRoute();
		registerCheckSignFailCallback(() => {
			// 注册全局未登录提示
			const ref = this;
			Modal.confirm({
				title: '是否登录？',
				// icon: createVNode(ExclamationCircleOutlined),
				content: '登录打开新世界！',
				onOk() {
					ref.$router.push({name: 'signIn'});
				}
			});
		});
		global.events.on('menuClick',(param) => {
			if (param.name === 'mine') {
				this.showDrawer = true;
			}
		});
	},
	mounted() {
		global.methods.prompt = this.$refs.prompt.prompt;
		global.states.style.lptWidth = document.body.clientWidth * 0.33;
	},
	methods: {
		checkRoute() {
			if (this.$route.name !== 'home') {
				this.showDrawer = true;
			} else {
				this.showDrawer = false;
			}
		},
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
}

#loading-area {
	position: fixed;
	left: 5%;
	bottom: 11%;
}

.ant-drawer-body {
	padding: 0;
}
</style>
