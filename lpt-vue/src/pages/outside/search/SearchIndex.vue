<template>
	<van-tabs v-model:active="curTabKey" swipeable lazy-render>

	</van-tabs>
</template>

<script>
export default {
	name: 'SearchIndex',
	data() {
		return {
			curTabKey: 'post'
		}
	}
}
</script>

<style scoped>

</style>