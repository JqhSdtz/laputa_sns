<template>
	<div class="item" @click="itemClick">
		<div v-if="!isActive">
			<slot name="normal"></slot>
		</div>
		<div v-else>
			<slot name="active"></slot>
		</div>
	</div>
</template>

<script>
import global from '@/lib/js/global';

export default {
	name: 'MenuItem',
	props: {
		path: String,
		alias: String
	},
	data() {
		return {}
	},
	computed: {
		isActive() {
			const path = this.$route.path;
			if (path.indexOf(this.path) > -1) {
				return true;
			} else if (this.alias && path.indexOf(this.alias) > -1) {
				return true;
			}
			return false;
		}
	},
	methods: {
		itemClick() {
			if (this.isActive) {
				global.events.emit('forceRefresh');
			} else {
				this.$router.push(this.path);
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