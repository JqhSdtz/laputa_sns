<template>
	<div id="main-view" :style="{height: mainViewHeight, position: 'relative'}">
		<main-view :key="currentView"></main-view>
	</div>
	<main-tab-bar id="main-bar" :style="{height: mainBarHeight}"></main-tab-bar>
</template>

<script>

import lpt from '@/lib/js/laputa/laputa'
import MainTabBar from './tab_bar/MainTabBar'
import MainView from './MainView'
import remHelper from '@/lib/js/uitls/rem-helper'

export default {
	name: 'Home',
	components: {
		MainView,
		MainTabBar
	},
	data() {
		const currentUser = lpt.operatorServ.getCurrent();
		const mainView = {
			currentView: 'index'
		}
		// 底部固定4rem，mainView现算一个px值
		this.mainViewHeight = (document.body.clientHeight - remHelper.remToPx(4)) + 'px';
		this.mainBarHeight = '4rem';
		return {
			...mainView,
			hasLoggedIn: typeof global.user !== 'undefined',
			user: currentUser ? currentUser : {
				nick_name: '点击登录'
			}
		}
	},
	provide() {
		return {
			refreshMainView: () => {
				this.currentView = new Date().getTime();
			}
		}
	},
	methods: {

	}
}
</script>

<style scoped>

</style>