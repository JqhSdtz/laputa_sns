<template>
	<div v-if="isBusy" style="z-index: 3002">
		<a-spin :size="size"/>
	</div>
</template>

<script>
import global from '@/lib/js/global';

export default {
	name: 'LoadingArea',
	props: {
		size: {
			type: String,
			default: 'default'
		}
	},
	computed: {
		isBusy: () => global.states.isBusy.value
	}
}
</script>

<style scoped>

</style>