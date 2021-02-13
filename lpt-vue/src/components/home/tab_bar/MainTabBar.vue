<template>
	<div class="tab-bar">
		<tab-bar-item path="/home/index">
			<template v-slot:normal>
				<ri-home3-line class="icon icon-normal"/>
				<div class="text text-normal">首页</div>
			</template>
			<template v-slot:active>
				<ri-home3-fill class="icon icon-active"/>
				<div class="text text-active">首页</div>
			</template>
		</tab-bar-item>
		<tab-bar-item path="/home/news">
			<template v-slot:normal>
				<ri-apps-line class="icon icon-normal"/>
				<div class="text text-normal">动态</div>
			</template>
			<template v-slot:active>
				<ri-apps-fill class="icon icon-active"/>
				<div class="text text-active">动态</div>
			</template>
		</tab-bar-item>
		<van-icon id="plus-icon" name="add-o" size="3.75rem" color="forestgreen"/>
		<tab-bar-item path="/home/community">
			<template v-slot:normal>
				<ri-dashboard-line class="icon icon-normal"/>
				<div class="text text-normal">社区</div>
			</template>
			<template v-slot:active>
				<ri-dashboard-fill class="icon icon-active"/>
				<div class="text text-active">社区</div>
			</template>
		</tab-bar-item>
		<tab-bar-item path="/home/mine">
			<template v-slot:normal>
				<ri-user3-line class="icon icon-normal"/>
				<div class="text text-normal">我的</div>
			</template>
			<template v-slot:active>
				<ri-user3-fill class="icon icon-active"/>
				<div class="text text-active">我的</div>
			</template>
		</tab-bar-item>
	</div>
</template>

<script>
import TabBarItem from './TabBarItem';
import {
	RiHome3Line, RiAppsLine, RiDashboardLine, RiUser3Line,
	RiHome3Fill, RiAppsFill, RiDashboardFill, RiUser3Fill
} from '@/assets/icons/remix-icon';

export default {
	name: 'MainTabBar',
	components: {
		TabBarItem,
		RiHome3Line,
		RiAppsLine,
		RiDashboardLine,
		RiUser3Line,
		RiHome3Fill,
		RiAppsFill,
		RiDashboardFill,
		RiUser3Fill
	}
}
</script>

<style scoped>
.tab-bar {
	height: 100%;
	display: flex;
	position: fixed;
	left: 0;
	right: 0;
	bottom: 0;
	box-shadow: 0 -2px 14px rgba(100, 100, 100, 0.2);
	background-color: #F7F7F7;
}

.icon {
	margin-top: 0.25rem;
	font-size: 1.75rem;
}

.text {
	font-size: 0.9rem;
}

.icon-active {
	color: forestgreen;
}

.text-active {
	color: forestgreen;
}

#plus-icon {
	margin: -0.35rem 0.5rem 0;
}
</style>