<template>
	<div id="main-view" :style="{height: mainViewHeight}">
		<main-view :key="currentView"></main-view>
	</div>
	<main-tab-bar id="main-bar" :style="{height: mainBarHeight}"></main-tab-bar>
	<loading-area id="loading-area" :isBusy="isBusy"></loading-area>
</template>

<script>

import lpt from '@/lib/js/laputa'
import MainTabBar from './tab_bar/MainTabBar'
import MainView from './MainView'
import LoadingArea from './hide_areas/LoadingArea'
import remHelper from '@/lib/js/rem-helper'

export default {
	name: 'Home',
	components: {
		LoadingArea,
		MainView,
		MainTabBar
	},
	data() {
		const currentUser = lpt.operatorServ.getCurrent();
		const loadingArea = {
			isBusy: false
		};
		const mainView = {
			currentView: 'index'
		}
		// 底部固定4rem，mainView现算一个px值
		this.mainViewHeight = (document.body.clientHeight - remHelper.remToPx(4)) + 'px';
		this.mainBarHeight = '4rem';
		return {
			...loadingArea,
			...mainView,
			hasLoggedIn: typeof global.user !== 'undefined',
			user: currentUser ? currentUser : {
				nick_name: '点击登录'
			}
		}
	},
	provide() {
		return {
			setGlobalBusy: (isBusy) => {
				this.isBusy = isBusy;
			},
			refreshMainView: () => {
				this.currentView = new Date().getTime();
			}
		}
	},
	methods: {
		login() {

		}
	}
}
</script>

<style scoped>
#loading-area {
	position: fixed;
	right: 5%;
	bottom: 11%;
}
</style>