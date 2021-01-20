<template>
	<div class="post-item">
		<top-bar class="top-bar" :post="post"></top-bar>
		<content-area class="content-area" :post="post"></content-area>
		<bottom-bar class="bottom-bar" :post="post"></bottom-bar>
	</div>
</template>

<script>
import TopBar from './parts/TopBar.vue'
import ContentArea from './parts/ContentArea'
import BottomBar from './parts/BottomBar'

export default {
	name: 'PostItem',
	props: {
		post: Object
	},
	components: {
		TopBar,
		ContentArea,
		BottomBar
	},
}
</script>

<style scoped>
.post-item {
	background-color: white;
	margin-bottom: 1rem;
}
</style>