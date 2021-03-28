<template>
	<div style="word-wrap:break-word;">
		<span v-for="(path, idx) in pathList" :key="path.id">
			<span v-if="idx !== 0">/</span>
			<span @click="showCategoryDetail(path)">{{ path.name }}</span>
		</span>
	</div>
</template>

<script>
import global from '@/lib/js/global';

export default {
	name: 'CategoryPath',
	props: {
		pathList: Array
	},
	watch: {
		pathList() {
			console.log(this.pathList)
		}
	},
	methods: {
		showCategoryDetail(path) {
			if (global.vars.env === 'blog') {
				global.states.blog.showDrawer = true;
			}
			this.$router.push({
				path: '/category_detail/' + path.id
			});
		}
	}
}
</script>

<style scoped>

</style>