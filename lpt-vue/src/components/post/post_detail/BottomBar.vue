<template>
	<div style="width: 100%; padding-top: 0.5rem; background-color: white">
		<van-action-sheet v-model:show="showCommentPanel" title="发表评论">
			<div ref="commentPanel" class="ant-input-group-wrapper panels" style="padding-top: 1rem">
				<div class="ant-input-wrapper ant-input-group comment-input-wrapper" style="width: 95%; margin-left: 2.5%;">
					<a-textarea ref="commentTextArea" style="font-size: 1rem; background-color: rgb(250,250,250)"
					            v-model:value="commentInput" :maxlength="250" auto-size
					            autofocus placeholder="输入评论"/>
					<span class="ant-input-group-addon" @click="sendComment">
						<send-outlined style="font-size: 1.35rem;" :rotate="-45"/>
					</span>
				</div>
			</div>
		</van-action-sheet>

		<van-action-sheet v-model:show="showForwardPanel" title="转发帖子">
			<div ref="forwardPanel" class="panels forward-panel">
<!--				<div style="width: 100%; display: inline-block">-->
<!--					<a-button type="link" style="float: right; margin-right: 1rem">立即转发</a-button>-->
<!--				</div>-->
<!--				<a-textarea ref="forwardTextArea" style="font-size: 1rem;width: 95%; margin-left: 2.5%"-->
<!--				            v-model:value="forwardInput" auto-size-->
<!--				            autofocus placeholder="输入评论"/>-->
				<div class="ant-input-wrapper ant-input-group forward-input-wrapper" style="width: 95%; margin-left: 2.5%;">
					<a-textarea ref="forwardTextArea" style="font-size: 1rem; background-color: rgb(250,250,250)"
					            v-model:value="forwardInput" :maxlength="250" auto-size
					            autofocus placeholder="输入评论"/>
					<span class="ant-input-group-addon" @click="sendForward">
						<send-outlined style="font-size: 1.35rem;" :rotate="-45"/>
					</span>
				</div>
			</div>
		</van-action-sheet>

		<div class="bottom-bar" v-show="!showCommentPanel && !showForwardPanel">
			<span class="icon-col" v-check-sign="true" @click="openForwardPanel">
				<share-alt-outlined class="icon"/>
				<span class="icon-text">转发</span>
			</span>
			<span class="icon-col" v-check-sign="true" @click="openCommentPanel">
				<comment-outlined class="icon"/>
				<span class="icon-text">评论</span>
			</span>
			<span class="icon-col" v-check-sign="true" @click="changeLike">
				<span>
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
import {Toast} from 'vant';
import lpt from '@/lib/js/laputa/laputa';
import global from '@/lib/js/global';

export default {
	name: 'BottomBar',
	props: {
		postId: Number
	},
	inject: {
		localEvents: {
			type: Object
		}
	},
	components: {
		LikeOutlined,
		LikeFilled,
		CommentOutlined,
		ShareAltOutlined,
		SendOutlined
	},
	data() {
		this.curCommentTargetKey = lpt.commentServ.level1 + '#' + this.postId;
		return {
			post: global.states.postManager.get(this.postId),
			showCommentPanel: false,
			showForwardPanel: false,
			commentInput: '',
			forwardInput: ''
		}
	},
	created() {
		this.lptConsumer = lpt.createConsumer();
		const ref = this;
		this.localEvents.on('openCommentPanel', (param) => {
			ref.openCommentPanel(param);
		});
	},
	methods: {
		changeLike() {
			const ref = this;
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
				success() {
					ref.post.liked_by_viewer = !isCancel;
					ref.post.like_cnt += isCancel ? -1 : 1;
					const refs = ref.$parent.$refs;
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
		},
		openForwardPanel() {
			const ref = this;
			this.showForwardPanel = true;
			this.$nextTick(() => {
				ref.$refs.forwardTextArea.$el.focus();
			});
		},
		openCommentPanel(param) {
			const preCommentTargetKey = this.curCommentTargetKey;
			if (param) {
				this.curCommentTargetKey = param.type + '#' + param.id;
			} else {
				this.curCommentTargetKey = lpt.commentServ.level1 + '#' + this.postId;
			}
			if (!this.commentInputMap) {
				this.commentInputMap = new Map();
			}
			this.commentInputMap.set(preCommentTargetKey, this.commentInput);
			this.commentInput = this.commentInputMap.get(this.curCommentTargetKey);
			const ref = this;
			this.showCommentPanel = true;
			this.$nextTick(() => {
				ref.$refs.commentTextArea.$el.focus();
			});
		},
		sendForward() {
			const ref = this;
			if (this.forwardInput.length === 0) {
				Toast.fail('请输入评论');
				return;
			}
			lpt.forwardServ.create({
				consumer: this.lptConsumer,
				data: {
					sup_id: this.postId,
					content: this.forwardInput
				},
				success() {
					ref.showForwardPanel = false;
					Toast.success('转发成功');
					const refs = ref.$parent.$refs;
					if (refs.forwardList) {
						refs.forwardList.pushForward({
							creator: global.states.curOperator.user,
							create_time: new Date().getTime()
						});
					}
				},
				fail(result) {
					Toast.fail(result.message);
				}
			});
		},
		sendComment() {
			const ref = this;
			if (this.commentInput.length === 0) {
				Toast.fail('请输入评论');
				return
			}
			const strPart = this.curCommentTargetKey.split('#');
			const type = strPart[0];
			const parentId = strPart[1];
			lpt.commentServ.create({
				consumer: this.lptConsumer,
				param: {
					type: type
				},
				data: {
					parent_id: parentId,
					content: this.commentInput
				},
				success(result) {
					ref.showCommentPanel = false;
					Toast.success('评论成功');
					const refs = ref.$parent.$refs;
					if (refs.commentList) {
						if (type === lpt.commentServ.level1) {
							// 当前打开了评论列表，且发出的评论是一级评论
							// 则在评论列表中添加一条
							const comment = {
								id: result.object,
								creator: global.states.curOperator.user,
								content: ref.commentInput,
								create_time: new Date().getTime()
							};
							global.states.commentL1Manager.add(comment);
							refs.commentList.pushComment(comment);
						} else if (type === lpt.commentServ.level2) {
							// 当前打开了评论列表，且发出的是二级评论
							// 增加该评论所属一级评论的二级评论数量
							const comment = global.states.commentL1Manager.get(parentId);
							++comment.l2_cnt;
							if (global.states.curOperator.user.id === ref.post.creator.id) {
								// 当前用户是帖子创建者，则增加帖子创建者回复数量
								++comment.poster_rep_cnt;
							}
						}
					}
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
.bottom-bar {
	width: 100%;
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
	bottom: 0.2rem;
}

.forward-panel {
	padding-top: 0.5rem;
	padding-bottom: 0.2rem;
}

.comment-input-wrapper {
	padding-bottom: 0.2rem;
}

:global(.van-tabs__line) {
	z-index: 0;
}
</style>