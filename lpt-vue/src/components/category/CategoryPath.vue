<template>
	<div style="word-wrap:break-word;">
		<span v-for="(path, idx) in pathList" :key="path.id">
			<span v-if="idx !== 0">/</span>
			<span @click="showCategoryDetail(path)">{{ path.name }}</span>
		</span>
	</div>
</template>

<script>
export default {
	name: 'CategoryPath',
	props: {
		pathList: Array
	},
	methods: {
		showCategoryDetail(path) {
			this.$router.push({
				name: 'categoryDetail',
				params: {
					categoryId: path.id
				}
			});
		}
	}
}
</script>

<style scoped>

</style>