<template>
	<div style="width: 100%; padding-top: 0.5rem; background-color: white">
		<div class="bottom-bar">
			<span class="icon-col" style="width: 31%" v-check-sign="{click: openForwardPanel}">
				<share-alt-outlined class="icon"/>
				<span class="icon-text">转发</span>
			</span>
			<span class="icon-col" style="width: 31%" v-check-sign="{click: openCommentPanel}">
				<comment-outlined class="icon"/>
				<span class="icon-text">评论</span>
			</span>
			<span class="icon-col" style="width: 31%" v-check-sign="{click: changeLike}">
				<span>
					<like-filled v-if="post.liked_by_viewer" class="icon" style="color: red"/>
					<like-outlined v-else class="icon"/>
				</span>
				<span class="icon-text">点赞</span>
			</span>
			<span class="icon-col" style="width: 7%">
				<post-actions :post-id="post.id" placement="top-end"/>
			</span>
		</div>
	</div>
</template>

<script>
import {CommentOutlined, LikeFilled, LikeOutlined, ShareAltOutlined} from '@ant-design/icons-vue';
import PostActions from '@/components/post/item/parts/PostActions';
import {Toast} from 'vant';
import lpt from '@/lib/js/laputa/laputa';
import global from '@/lib/js/global';

export default {
	name: 'BottomBar',
	props: {
		postId: Number
	},
	inject: {
		postDetailEvents: {
			type: Object
		}
	},
	components: {
		LikeOutlined,
		LikeFilled,
		CommentOutlined,
		ShareAltOutlined,
		PostActions
	},
	data() {
		return {
			post: global.states.postManager.get({
				itemId: this.postId
			})
		}
	},
	created() {
		this.lptConsumer = lpt.createConsumer();
	},
	methods: {
		openForwardPanel() {
			this.postDetailEvents.emit('openForwardPanel');
		},
		openCommentPanel() {
			this.postDetailEvents.emit('openCommentPanel', {
				type: lpt.commentServ.level1,
				id: this.postId
			});
		},
		changeLike() {
			const isCancel = this.post.liked_by_viewer;
			const fun = isCancel ? lpt.likeServ.unlike : lpt.likeServ.like;
			fun({
				consumer: this.lptConsumer,
				ignoreGlobalBusyChange: true,
				param: {
					type: lpt.contentType.post
				},
				data: {
					target_id: this.post.id
				},
				success: () => {
					this.post.liked_by_viewer = !isCancel;
					this.post.like_cnt += isCancel ? -1 : 1;
					const refs = this.$parent.$refs;
					if (refs.likeList) {
						if (isCancel) {
							refs.likeList.shiftLike();
						} else {
							refs.likeList.pushLike({
								creator: global.states.curOperator.user,
								create_time: new Date().getTime()
							});
						}
					}
				},
				fail(result) {
					Toast.fail(result.message);
				}
			})
		}
	}
}
</script>

<style scoped>
.bottom-bar {
	width: 100%;
}

.icon-col {
	display: inline-block;
	text-align: center;
}

.icon-text {
	margin-left: 0.5rem
}

.icon {
	font-size: 1.25rem;
}

:global(.van-tabs__line) {
	z-index: 0;
}
</style>