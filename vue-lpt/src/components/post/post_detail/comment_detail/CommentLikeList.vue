<template>
	<div class="comment-area" style="padding-top: 1rem" :style="{height: scrollHeight, position: 'relative'}">
		<div style="width: 100%; background-color: white; display: inline-block;">
			<div style="display: inline-block; float: left;  margin-left: 1rem">评论详情</div>
		</div>
		<div style="margin: 0.75rem 0.5rem 0 0.5rem">
			<comment-item :type="type" :comment-id="parseInt(commentId)"/>
		</div>
		<div style="height: 5px; width: 100%; background-color: #ECECEC"/>
		<div style="width: 100%; margin: 0.5rem 1rem; font-size: 0.85rem">共{{ comment.like_cnt }}个赞</div>
		<like-list ref="likeList" :type="contentType" :target-id="parseInt(commentId)" @refresh="onRefresh"
				style="height: 100%; overflow-y: visible; margin-top: 0.75rem;"/>
	</div>
</template>

<script>
import CommentItem from '../comment/item/CommentItem';
import LikeList from '../../post_detail/like/LikeList';
import global from '@/lib/js/global';
import lpt from '@/lib/js/laputa/laputa';
import {Toast} from 'vant';

export default {
	name: 'CommentLikeList',
	props: {
		type: {
			type: String,
			default: lpt.commentServ.level1
		},
		commentId: String
	},
	components: {
		CommentItem,
		LikeList
	},
	data() {
		const isLevel1 = this.type === lpt.commentServ.level1;
		const getter = isLevel1 ? lpt.commentServ.getDefaultCommentL1
						: lpt.commentServ.getDefaultCommentL2;
		const contentType = isLevel1 ? lpt.contentType.commentL1 : lpt.contentType.commentL2;
		return {
			contentType,
			comment: getter(parseInt(this.commentId))
		}
	},
	created() {
		this.lptConsumer = lpt.createConsumer();
		this.init();
	},
	computed: {
		scrollHeight() {
			const mainViewHeight = document.body.clientHeight;
			return mainViewHeight + 'px';
		}
	},
	methods: {
		onRefresh() {
			this.init();
		},
		init() {
			const ref = this;
			lpt.commentServ.get({
				consumer: this.lptConsumer,
				param: {
					commentId: this.commentId,
					type: this.type
				},
				success(result) {
					if (ref.type === lpt.commentServ.level1) {
						global.states.commentL1Manager.add(result.object);
					} else {
						global.states.commentL2Manager.add(result.object);
					}
					ref.comment = result.object;
				},
				fail(result) {
					Toast.fail(result.message);
				}
			});
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
</style>