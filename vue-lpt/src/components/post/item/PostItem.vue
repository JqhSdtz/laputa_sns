<template>
	<div class="post-item">
		<top-bar class="top-bar" :post-id="post.id" :is-top-post="isTopPost"/>
		<content-area class="content-area" :post="post" :show-full-text="showFullText">
			<div v-if="post.ori_post" style="box-shadow: 0 -1.5px 10px rgba(100, 100, 100, 0.2);">
				<post-item :show-bottom="false" :post-id="post.ori_post.id"/>
			</div>
		</content-area>
		<bottom-bar v-if="showBottom" class="bottom-bar" :post-id="post.id" :post-of="postOf" :show-actions="true"/>
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
		showBottom: {
			type: Boolean,
			default: true
		},
		showFullText: Boolean,
		postId: Number,
		postOf: String,
		isTopPost: Boolean
	},
	data() {
		return {
			post: global.states.postManager.get({
				itemId: this.postId
			})
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