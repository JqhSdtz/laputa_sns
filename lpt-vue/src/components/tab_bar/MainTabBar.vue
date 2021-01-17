<template>
	<div class="tab-bar">
		<tab-bar-item path="/index">
			<template v-slot:normal>
				<img src="@/assets/img/tab_bar/home.svg" alt="">
				<div>首页</div>
			</template>
			<template v-slot:active>
				<img src="@/assets/img/tab_bar/home_active.svg" alt="">
				<div style="color: forestgreen">首页</div>
			</template>
		</tab-bar-item>
		<tab-bar-item path="/news">
			<template v-slot:normal>
				<img src="@/assets/img/tab_bar/category.svg" alt="">
				<div>动态</div>
			</template>
			<template v-slot:active>
				<img src="@/assets/img/tab_bar/category_active.svg" alt="">
				<div style="color: forestgreen">动态</div>
			</template>
		</tab-bar-item>
		<tab-bar-item path="/community">
			<template v-slot:normal>
				<img src="@/assets/img/tab_bar/shop_cart.svg" alt="">
				<div>社区</div>
			</template>
			<template v-slot:active>
				<img src="@/assets/img/tab_bar/shop_cart_active.svg" alt="">
				<div style="color: forestgreen">社区</div>
			</template>
		</tab-bar-item>
		<tab-bar-item path="/mine">
			<template v-slot:normal>
				<img src="@/assets/img/tab_bar/profile.svg" alt="">
				<div>我的</div>
			</template>
			<template v-slot:active>
				<img src="@/assets/img/tab_bar/profile_active.svg" alt="">
				<div style="color: forestgreen">我的</div>
			</template>
		</tab-bar-item>
	</div>
</template>

<script>
import TabBarItem from '@/components/tab_bar/TabBarItem';

export default {
	name: 'MainTabBar',
	components: {
		TabBarItem
	}
}
</script>

<style scoped>
img {
	margin: 0.1rem 0;
	height: 70%;
	vertical-align: middle;
}
.tab-bar-item {
	height: 100%;
}
.tab-bar {
	height: 100%;
	display: flex;
	position: fixed;
	left: 0;
	right: 0;
	bottom: 0;
	box-shadow: 0 -3px 10px rgba(100, 100, 100, 0.2);
	background-color: #F7F7F7;
}
</style>