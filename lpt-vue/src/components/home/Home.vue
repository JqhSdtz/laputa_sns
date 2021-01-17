<template>
	<router-view :key="currentView" id="main-view"></router-view>
	<MainTabBar id="main-bar"></MainTabBar>
	<div v-if="isBusy" id="loading-area" class="sk-chase">
		<div class="sk-chase-dot"></div>
		<div class="sk-chase-dot"></div>
		<div class="sk-chase-dot"></div>
		<div class="sk-chase-dot"></div>
		<div class="sk-chase-dot"></div>
		<div class="sk-chase-dot"></div>
	</div>
</template>

<script>
import MainTabBar from '../tab_bar/MainTabBar'
import lpt from '@/lib/js/laputa'
import 'spinkit/spinkit.min.css'

export default {
	name: "Home",
	components: {
		MainTabBar
	},
	data() {
		const currentUser = lpt.operatorServ.getCurrent();
		return {
			isBusy: false,
			currentView: 'index',
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
			setCurrentView: (currentView) => {
				this.currentView = currentView;
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
	#main-view {
		height: 90%;
	}
	#main-bar {
		height: 10%;
	}
	#loading-area {
		position: fixed;
		right: 5%;
		bottom: 11%;
	}
</style>