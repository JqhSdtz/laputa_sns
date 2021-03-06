<template>
	<a-config-provider :locale="locale" :getPopupContainer="getPopupContainer">
		<router-view v-slot="{ Component }">
			<keep-alive :exclude="noCacheList">
				<component :is="Component"/>
			</keep-alive>
		</router-view>
		<loading-area id="loading-area"></loading-area>
	</a-config-provider>
	<prompt-dialog ref="prompt"/>
</template>

<script>
import zhCN from 'ant-design-vue/es/locale/zh_CN';
import {registerCheckSignFailCallback} from '@/lib/js/laputa/laputa-vue';
import {Modal} from 'ant-design-vue';
import LoadingArea from '@/components/global/LoadingArea';
import PromptDialog from '@/components/global/PromptDialog';
import {noCacheList} from '@/modules/lpt/router';
import global from '@/lib/js/global';

export default {
	name: 'App',
	components: {
		LoadingArea,
		PromptDialog
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
					ref.$router.push({name: 'signIn'});
				}
			});
		});
	},
	mounted() {
		global.methods.prompt = this.$refs.prompt.prompt;
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
}

#loading-area {
	position: fixed;
	left: 5%;
	bottom: 11%;
}
</style>
