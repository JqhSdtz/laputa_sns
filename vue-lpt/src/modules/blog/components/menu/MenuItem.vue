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
		name: String,
		alias: String
	},
	data() {
		return {}
	},
	computed: {
		isActive() {
			return this.name && this.$route.name == this.name;
		}
	},
	methods: {
		itemClick() {
			if (this.isActive) {
				global.events.emit('forceRefresh');
			} else {
				this.$router.push({
					name: this.name
				});
			}
			global.events.emit('menuClick', {
				name: this.name
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
}
</style>