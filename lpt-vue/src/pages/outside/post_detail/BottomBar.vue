<template>
	<div style="width: 100%; margin-bottom: 0.2rem">
		<div ref="commentPanel" class="ant-input-group-wrapper panels" v-show="showCommentPanel"
		     v-click-outside="onClickCommentPanelOutSide">
			<div class="ant-input-wrapper ant-input-group" style="width: 95%; margin-left: 2.5%;">
				<a-textarea ref="commentTextArea" style="font-size: 1rem; background-color: rgb(250,250,250)"
				            v-model:value="commentInput" auto-size
				            autofocus placeholder="输入评论"/>
				<span class="ant-input-group-addon">
					<send-outlined style="font-size: 1.35rem;" :rotate="-45"/>
				</span>
			</div>
		</div>

		<div ref="forwardPanel" class="panels" v-show="showForwardPanel"
		     v-click-outside="onClickForwardPanelOutSide">
			<div style="width: 100%; display: inline-block">
				<button class="ant-btn fake-btn" style="float: left; margin-left: 1rem">
					<span>转发帖子</span>
				</button>
				<a-button type="link" style="float: right; margin-right: 1rem">立即转发</a-button>
			</div>
			<a-textarea ref="forwardTextArea" style="font-size: 1rem;width: 95%; margin-left: 2.5%" v-model:value="forwardInput" auto-size
			            autofocus placeholder="输入评论"/>
		</div>

		<div class="bottom-bar" v-show="!showCommentPanel && !showForwardPanel">
			<span class="icon-col" @click="toggleForwardPanel">
				<share-alt-outlined class="icon"/>
				<span class="icon-text">转发</span>
			</span>
			<span class="icon-col" @click="toggleCommentPanel">
				<comment-outlined class="icon"/>
				<span class="icon-text">评论</span>
			</span>
			<span class="icon-col">
				<span v-check-sign="true" @click="changeLike">
					<like-filled v-if="post.liked_by_viewer" class="icon" style="color: red"/>
					<like-outlined v-else class="icon"/>
				</span>
				<span class="icon-text">点赞</span>
			</span>
		</div>
	</div>
</template>

<script>
import {CommentOutlined, LikeFilled, LikeOutlined, SendOutlined, ShareAltOutlined} from '@ant-design/icons-vue';
import lpt from '@/lib/js/laputa/laputa';
import global from '@/lib/js/global/global-state';

export default {
	name: 'BottomBar',
	props: {
		post_id: Number
	},
	components: {
		LikeOutlined,
		LikeFilled,
		CommentOutlined,
		ShareAltOutlined,
		SendOutlined
	},
	data() {
		return {
			post: global.postManager.get(this.post_id),
			showCommentPanel: false,
			showForwardPanel: false,
			commentInput: '',
			forwardInput: ''
		}
	},
	created() {
		this.lptConsumer = lpt.createConsumer();
	},
	methods: {
		changeLike() {
			const ref = this;
			const isLiked = this.post.liked_by_viewer;
			const fun = isLiked ? lpt.likeServ.unlike : lpt.likeServ.like;
			fun({
				consumer: this.lptConsumer,
				ignoreGlobalBusyChange: true,
				param: {
					type: lpt.contentType.post
				},
				data: {
					target_id: this.post.id
				},
				success() {
					ref.post.liked_by_viewer = !isLiked;
					ref.post.like_cnt += isLiked ? -1 : 1;
				}
			})
		},
		toggleCommentPanel() {
			this.showCommentPanel = !this.showCommentPanel;
			if (this.showCommentPanel && this.$refs.commentTextArea) {
				const ref = this;
				this.$nextTick(() => {
					ref.$refs.commentTextArea.$el.focus();
				});
			}
		},
		toggleForwardPanel() {
			this.showForwardPanel = !this.showForwardPanel;
			if (this.showForwardPanel && this.$refs.forwardTextArea) {
				const ref = this;
				this.$nextTick(() => {
					ref.$refs.forwardTextArea.$el.focus();
				});
			}
		},
		onClickCommentPanelOutSide() {
			if (this.showCommentPanel) {
				this.showCommentPanel = false;
			}
		},
		onClickForwardPanelOutSide() {
			if (this.showForwardPanel) {
				this.showForwardPanel = false;
			}
		}
	}
}
</script>

<style scoped>
.bottom-bar {
	width: 100%;
	padding-bottom: 1rem;
}

.icon-col {
	display: inline-block;
	width: 33.33%;
	text-align: center;
}

.icon-text {
	margin-left: 0.5rem
}

.icon {
	font-size: 1.25rem;
}

.panels {
	box-shadow: 0 -3px 10px rgba(100, 100, 100, 0.2);
}

.fake-btn {
	border: none;
	pointer-events: none;
}
</style>