<template>
	<div class="item" :class="{'with-background': withBackground}"
	     @click="itemClick">
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
		showInDrawer: Boolean,
		withBackground: Boolean
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
			this.$router.push({
				path: this.path
			});
		}
	}
}
</script>

<style scoped>
.item {
	display: inline-block;
	text-align: center;
	font-size: 14px;
	cursor: pointer;
	z-index: 2;
}

.with-background {
	background-color: white;
	height: 3.5rem;
	width: 3.5rem;
	box-shadow: 0 0 10px 6px rgba(0, 0, 0, 0.25);
	border-radius: 100%;
}
</style>