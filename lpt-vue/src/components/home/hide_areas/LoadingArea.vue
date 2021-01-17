<template>
	<div v-if="isBusy">
		<a-spin />
	</div>
</template>

<script>

export default {
	name: 'LoadingArea',
	props: {
		isBusy: Boolean
	}
}
</script>

<style scoped>

</style>