<template>
	<a-config-provider :getPopupContainer="getPopupContainer">
		<Home/>
	</a-config-provider>
</template>

<script>
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import Home from './components/home/Home'

export default {
	name: 'App',
	components: {
		Home
	},
	data() {
		return {
			locale: zhCN,
		};
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

<style>
#app {
	height: 100%;
	font-family: Avenir, Helvetica, Arial, sans-serif;
	-webkit-font-smoothing: antialiased;
	-moz-osx-font-smoothing: grayscale;
	text-align: center;
	color: #2c3e50;
}

body, html {
	width: 100%;
	height: 100%;
}

body {
	margin: 0;
}
</style>
