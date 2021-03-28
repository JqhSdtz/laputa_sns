<template>
	<div>
		<van-action-sheet v-model:show="showCommentPanel" title="发表评论">
			<div ref="commentPanel" class="ant-input-group-wrapper panels">
				<div class="ant-input-wrapper ant-input-group comment-input-wrapper"
				     style="width: 95%; margin-left: 2.5%;">
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
				<div class="ant-input-wrapper ant-input-group forward-input-wrapper"
				     style="width: 95%; margin-left: 2.5%;">
					<a-textarea ref="forwardTextArea" style="font-size: 1rem; background-color: rgb(250,250,250)"
					            v-model:value="forwardInput" :maxlength="250" auto-size
					            autofocus placeholder="输入评论"/>
					<span class="ant-input-group-addon" @click="sendForward">
						<send-outlined style="font-size: 1.35rem;" :rotate="-45"/>
					</span>
				</div>
			</div>
		</van-action-sheet>
	</div>
</template>

<script>
import {SendOutlined} from '@ant-design/icons-vue';
import lpt from "@/lib/js/laputa/laputa";
import {Toast} from "vant";
import global from "@/lib/js/global";

export default {
	name: 'InputPanel',
	props: {
		postId: Number
	},
	inject: {
		postDetailEvents: {
			type: Object
		}
	},
	components: {
		SendOutlined
	},
	data() {
		this.curCommentTargetKey = lpt.commentServ.level1 + '#' + this.postId;
		const post = global.states.postManager.get({
			itemId: this.postId
		});
		return {
			post: post,
			showCommentPanel: false,
			showForwardPanel: false,
			commentInput: '',
			forwardInput: ''
		}
	},
	created() {
		this.lptConsumer = lpt.createConsumer();
		const ref = this;
		this.postDetailEvents.on('openForwardPanel', () => {
			ref.openForwardPanel();
		});
		this.postDetailEvents.on('openCommentPanel', (param) => {
			ref.openCommentPanel(param);
		});
	},
	methods: {
		openForwardPanel() {
			const ref = this;
			this.showForwardPanel = true;
			this.$nextTick(() => {
				if (ref.$refs.forwardTextArea)
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
				if (ref.$refs.commentTextArea)
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
							const comment = global.states.commentL1Manager.get({
								itemId: parentId
							});
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
.panels {
	padding-top: 0.5rem;
	padding-bottom: 0.35rem;
}

.comment-input-wrapper {
	padding-bottom: 0.2rem;
}
</style>