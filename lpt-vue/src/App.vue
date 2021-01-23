<template>
	<a-config-provider :locale="locale" :getPopupContainer="getPopupContainer">
		<router-view v-slot="{ Component }">
			<keep-alive>
				<component :is="Component"/>
			</keep-alive>
		</router-view>
	</a-config-provider>
</template>

<script>
import zhCN from 'ant-design-vue/es/locale/zh_CN';
import {registerCheckSignFailCallback} from '@/lib/js/laputa-vue';
import {Modal} from 'ant-design-vue';

export default {
	name: 'App',
	data() {
		return {
			locale: zhCN,
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
</style>
