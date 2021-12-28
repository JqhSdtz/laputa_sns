<template>
	<a-config-provider :locale="locale" :getPopupContainer="getPopupContainer">
		<base-container name="lptMain">
			<router-view v-slot="{ Component }">
				<keep-alive :exclude="noCacheList">
					<component :is="Component"/>
				</keep-alive>
			</router-view>
			<loading-area id="loading-area"></loading-area>
		</base-container>
	</a-config-provider>
</template>

<script>
import zhCN from 'ant-design-vue/es/locale/zh_CN';
import {registerCheckSignFailCallback} from '@/lib/js/laputa/laputa-vue';
import {Modal} from 'ant-design-vue';
import LoadingArea from '@/components/global/LoadingArea';
import BaseContainer from '@/components/global/container/BaseContainer.vue';
import PromptDialog from '@/components/global/PromptDialog';
import {noCacheList} from '@/modules/lpt/router';

export default {
	name: 'App',
	components: {
		LoadingArea,
		PromptDialog,
		BaseContainer
	},
	data() {
		return {
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
					ref.$router.push({path: '/home/mine'});
				}
			});
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
	font-size: 1rem;
}

body, html {
	width: 100%;
	height: 100%;
}

body {
	margin: 0;
	font-family: -apple-system, BlinkMacSystemFont, Segoe UI, PingFang SC, Hiragino Sans GB, Microsoft YaHei, Helvetica Neue, Helvetica, Arial, sans-serif, Apple Color Emoji, Segoe UI Emoji, Segoe UI Symbol;
}

#loading-area {
	position: fixed;
	left: 5%;
	bottom: 11%;
}

.van-overlay {
	background-color: @overlay-background-color;
}

::-webkit-scrollbar {
	display: none;
}
</style>
