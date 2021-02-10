<template>
	<div class="comment-item">
		<top-bar class="top-bar" :comment="comment"/>
		<content-area class="content-area" :comment="comment"/>
		<bottom-bar class="bottom-bar" :comment-id="comment.id"/>
		<sub-comment-area class="sub-comment-area" :comment="comment"/>
		<van-divider style="margin: 5px 0"/>
	</div>
</template>

<script>
import global from "@/lib/js/global";
import TopBar from './parts/TopBar';
import ContentArea from './parts/ContentArea';
import BottomBar from './parts/BottomBar';
import SubCommentArea from './parts/SubCommentArea';

export default {
	name: 'CommentItem',
	props: {
		commentId: Number
	},
	components: {
		TopBar,
		ContentArea,
		BottomBar,
		SubCommentArea
	},
	data() {
		return {
			comment: global.states.commentL1Manager.get(this.commentId)
		};
	},
}
</script>

<style scoped>
.comment-item {
	background-color: white;
}
.content-area {
	margin-top: 0.5rem;
}
</style>