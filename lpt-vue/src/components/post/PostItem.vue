<template>
	<div class="post-item">
		<top-bar class="top-bar" :post="post"></top-bar>
		<content-area class="content-area" :post="post"></content-area>
		<bottom-bar class="bottom-bar" :post="post"></bottom-bar>
	</div>
</template>

<script>
import TopBar from './TopBar.vue'
import ContentArea from './ContentArea'
import BottomBar from './BottomBar'

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
	border: 1px solid rgba(0, 0, 0, .125);
}
</style>