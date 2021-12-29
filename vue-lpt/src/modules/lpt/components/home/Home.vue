<template>
	<div id="main-view" :style="{height: mainViewHeight, position: 'relative'}">
		<router-view v-slot="{ Component }">
			<keep-alive>
				<component :is="Component"/>
			</keep-alive>
		</router-view>
	</div>
	<main-tab-bar id="main-bar" :style="{height: mainBarHeight}"></main-tab-bar>
</template>

<script>

import lpt from '@/lib/js/laputa/laputa'
import MainTabBar from './tab_bar/MainTabBar'
import global from "@/lib/js/global";

export default {
	name: 'Home',
	components: {
		MainTabBar
	},
	data() {
		const currentUser = lpt.operatorServ.getCurrent();
		return {
			hasLoggedIn: typeof global.states.user !== 'undefined',
			user: currentUser ? currentUser : {
				nick_name: '点击登录'
			}
		}
	},
	computed: {
		mainViewHeight() {
			return (global.states.style.bodyHeight - global.states.style.tabBarHeight) + 'px';
		},
		mainBarHeight() {
			return global.states.style.tabBarHeight + 'px';
		}
	}
}
</script>

<style scoped>

</style>