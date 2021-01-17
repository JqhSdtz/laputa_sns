<template>
	<div class="bottom-bar">

	</div>
</template>

<script>

export default {
	name: 'BottomBar',
	props: {
		post: Object
	},
	data() {

		return {}
	}
}
</script>

<style scoped>
	.bottom-bar {
		background-color: rgb(247, 247, 247);
		padding: 0.5rem 1rem;
		height: 1.5rem;
		position: relative;
		border-top: 1px solid rgba(0, 0, 0, .125);
	}
</style>