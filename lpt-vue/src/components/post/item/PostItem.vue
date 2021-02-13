<template>
	<div class="post-item">
		<top-bar class="top-bar" :post-id="post.id"></top-bar>
		<content-area class="content-area" :post="post"></content-area>
		<bottom-bar class="bottom-bar" :post-id="post.id" :show-actions="true"></bottom-bar>
	</div>
</template>

<script>
import TopBar from './parts/TopBar.vue';
import ContentArea from './parts/ContentArea';
import BottomBar from './parts/BottomBar';
import global from '@/lib/js/global';

export default {
	name: 'PostItem',
	props: {
		postId: Number
	},
	data() {
		return {
			post: global.states.postManager.get(this.postId)
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