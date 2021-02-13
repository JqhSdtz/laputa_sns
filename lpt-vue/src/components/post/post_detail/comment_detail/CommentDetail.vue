<template>
	<div class="comment-area" style="padding-top: 1rem" :style="{height: scrollHeight, position: 'relative'}">
		<div style="width: 100%; background-color: white; display: inline-block;">
			<div style="display: inline-block; float: left;  margin-left: 1rem">评论详情</div>
			<van-icon name="cross" style="float: right; margin-right: 1rem" @click="closeCommentDetail"/>
		</div>
		<div style="margin: 0.75rem 0.5rem 0 0.5rem">
			<comment-item :comment-id="comment.id"/>
		</div>
		<div style="height: 5px; width: 100%; background-color: #ECECEC"/>
		<div style="width: 100%; margin: 0.5rem 1rem; font-size: 0.85rem">共{{ comment.l2_cnt }}条回复</div>
		<sub-comment-list ref="commentList" class="sub-comment-list" :parent-id="comment.id" @refresh="onRefresh"/>
	</div>
</template>

<script>
import lpt from "@/lib/js/laputa/laputa";
import global from "@/lib/js/global";
import {Toast} from "vant";
import CommentItem from '../comment/item/CommentItem';
import SubCommentList from './SubCommentList';


export default {
	name: 'CommentDetail',
	props: {
		commentId: Number
	},
	components: {
		CommentItem,
		SubCommentList
	},
	inject: {
		postDetailEvents: {
			type: Object
		}
	},
	data() {
		return {
			mainBarHeight: global.vars.style.postDetailBarHeight,
			comment: global.states.commentL1Manager.get(this.commentId)
		}
	},
	created() {
		this.lptConsumer = lpt.createConsumer();
		this.init();
	},
	computed: {
		scrollHeight() {
			const mainViewHeight = document.body.clientHeight;
			// 底部高度加0.5的padding
			const barHeight = this.mainBarHeight + 10;
			return mainViewHeight - barHeight + 'px';
		}
	},
	methods: {
		onRefresh() {
			this.init();
		},
		init() {
			lpt.commentServ.get({
				consumer: this.lptConsumer,
				param: {
					commentId: this.commentId,
					type: lpt.commentServ.level1
				},
				success(result) {
					global.states.commentL1Manager.add(result.object);
				},
				fail(result) {
					Toast.fail(result.message);
				}
			});
		},
		closeCommentDetail() {
			this.postDetailEvents.emit('closeCommentDetail');
		}
	}
}
</script>

<style scoped>
.comment-area {
	overflow-y: scroll;
}

.comment-area::-webkit-scrollbar {
	display: none;
}

.sub-comment-list {
	margin-top: 0.75rem;
}
</style>