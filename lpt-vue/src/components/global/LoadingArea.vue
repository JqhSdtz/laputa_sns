<template>
	<div v-if="isBusy">
		<a-spin />
	</div>
</template>

<script>
import global from '@/lib/js/global';

export default {
	name: 'LoadingArea',
	computed: {
		isBusy: () => global.states.isBusy.value
	}
}
</script>

<style scoped>

</style>