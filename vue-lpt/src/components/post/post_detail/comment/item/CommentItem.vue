<template>
	<div class="comment-item">
		<top-bar class="top-bar" :comment="comment"/>
		<content-area class="content-area" :comment="comment"/>
		<bottom-bar v-if="showBottom" class="bottom-bar" :type="type" :comment-id="comment.id" :show-actions="showActions"/>
		<sub-comment-area v-if="showSubArea" class="sub-comment-area" :comment="comment"/>
		<van-divider style="margin: 5px 0"/>
	</div>
</template>

<script>
import global from "@/lib/js/global";
import TopBar from './parts/TopBar';
import ContentArea from './parts/ContentArea';
import BottomBar from './parts/BottomBar';
import SubCommentArea from './parts/SubCommentArea';
import lpt from '@/lib/js/laputa/laputa';

export default {
	name: 'CommentItem',
	props: {
		commentId: Number,
		showBottom: {
			type: Boolean,
			default: true
		},
		showActions: Boolean,
		type: {
			type: String,
			default: lpt.commentServ.level1
		},
		showSubArea: {
			type: Boolean,
			default: false
		}
	},
	inject: {
		postDetailEvents: {
			type: Object
		}
	},
	components: {
		TopBar,
		ContentArea,
		BottomBar,
		SubCommentArea
	},
	data() {
		let comment;
		if (this.type === lpt.commentServ.level1) {
			comment = global.states.commentL1Manager.get({
				itemId: this.commentId
			})
		} else {
			comment = global.states.commentL2Manager.get({
				itemId: this.commentId
			})
		}
		return {
			comment
		};
	},
	created() {
		if (this.postDetailEvents) {
			this.postDetailEvents.on('publishCommentL2', (l2Comment) => {
				if (l2Comment.l1_id !== this.comment.id) return;
				if (!this.comment.preview_l2_list) this.comment.preview_l2_list = [];
				this.comment.preview_l2_list.push(l2Comment);
			});
		}
	}
}
</script>

<style scoped>
.comment-item {
	background-color: white;
}

.content-area {
	margin-top: 0.5rem;
}

.sub-comment-area {
	margin-top: 0.5rem;
}
</style>