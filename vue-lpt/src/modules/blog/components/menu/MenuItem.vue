<template>
	<div class="item" @click="itemClick">
		<slot></slot>
	</div>
</template>

<script>
import global from '@/lib/js/global';

export default {
	name: 'MenuItem',
	props: {
		path: String,
		alias: String,
		showInDrawer: Boolean
	},
	data() {
		return {}
	},
	computed: {
		isActive() {
			if (!this.path) return false;
			if (this.$route.path == this.path) return true;
			const meta = this.$route.meta;
			if (meta && (meta.drawerPath == this.path || meta.mainPath == this.path)) return true;
			return false;
		}
	},
	methods: {
		itemClick() {
			if (this.showInDrawer) {
				global.states.blog.showDrawer = true;
			}
			if (this.isActive) {
				global.events.emit('forceRefresh');
			} else {
				this.$router.push({
					path: this.path
				});
			}
		}
	}
}
</script>

<style scoped>
.item {
	display: inline-block;
	text-align: center;
	font-size: 14px;
}
</style>