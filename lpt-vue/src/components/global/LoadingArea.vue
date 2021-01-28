<template>
	<div v-if="isBusy">
		<a-spin />
	</div>
</template>

<script>
import global from '@/lib/js/global-state';

export default {
	name: 'LoadingArea',
	computed: {
		isBusy: () => global.isBusy.value
	}
}
</script>

<style scoped>

</style>