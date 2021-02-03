<template>
	<div class="post-item">
		<top-bar class="top-bar" :post="post"></top-bar>
		<content-area class="content-area" :post="post"></content-area>
		<bottom-bar class="bottom-bar" :post_id="post.id"></bottom-bar>
	</div>
</template>

<script>
import TopBar from './parts/TopBar.vue';
import ContentArea from './parts/ContentArea';
import BottomBar from './parts/BottomBar';
import global from '@/lib/js/global/global-state';

export default {
	name: 'PostItem',
	props: {
		post_id: Number
	},
	data() {
		return {
			post: global.postManager.get(this.post_id)
		};
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
.content-area {
	margin-top: 0.5rem;
}
</style>