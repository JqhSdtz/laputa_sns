<template>
	<div class="tab-bar-item" @click="itemClick">
		<div v-if="!isActive" style="height: 100%">
			<slot name="normal"></slot>
		</div>
		<div v-else style="height: 100%">
			<slot name="active"></slot>
		</div>
	</div>
</template>

<script>

export default {
	name: 'TabBarItem',
	props: {
		path: String,
	},
	inject: ['setCurrentView'],
	data() {
		return {}
	},
	computed: {
		isActive() {
			return this.$route.path.indexOf(this.path) !== -1;
		}
	},
	methods: {
		itemClick() {
			if (this.isActive) {
				// 点击当前菜单项时给router-view设置一个时间戳作为key，可以强制刷新
				this.setCurrentView(new Date().getTime());
			} else {
				this.$router.push(this.path);
			}
		}
	}
}
</script>

<style scoped>
.tab-bar-item {
	flex: 1;
	text-align: center;
	height: 5rem;
	font-size: 14px;
}
</style>