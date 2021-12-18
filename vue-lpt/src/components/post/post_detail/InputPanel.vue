<template>
	<div>
		<van-action-sheet v-model:show="showCommentPanel" title="发表评论"
		                  :overlay="overlay" :style="panelStyle">
			<div style="position: absolute;right: 3rem;font-size: 1rem;opacity: 0.5;top: 0;line-height: 48px;" 
				 v-text="'可输入' + (250 - (commentInput ? commentInput.length : 0)) + '字'"></div>
			<div ref="commentPanel" class="ant-input-group-wrapper panels">
				<div class="ant-input-wrapper ant-input-group comment-input-wrapper"
				     style="width: 95%; margin-left: 2.5%;">
					<a-mentions ref="commentTextArea" style="font-size: 1rem; background-color: rgb(250,250,250)"
					            v-model:value="commentInput" :maxlength="250" auto-size autofocus
					            :loading="isUserListLoading" @search="onSearchUser"
								placeholder="输入评论">
					    <a-mentions-option v-for="({ id, nick_name, avatar_url }) in userList" :key="id"
							:value="nick_name">
							<img :src="avatar_url" :alt="nick_name" style="width: 20px; margin-right: 8px;">
							<span>{{ nick_name }}</span>
						</a-mentions-option>
					</a-mentions>
					<span class="ant-input-group-addon" @click="sendComment">
						<send-outlined style="font-size: 1.35rem;" :rotate="-45"/>
					</span>
				</div>
			</div>
		</van-action-sheet>
		<van-action-sheet v-model:show="showForwardPanel" title="转发帖子"
		                  :overlay="overlay" :style="panelStyle">
			<div style="position: absolute;right: 3rem;font-size: 1rem;opacity: 0.5;top: 0;line-height: 48px;" 
				 v-text="'可输入' + (250 - (forwardInput ? forwardInput.length : 0)) + '字'"></div>
			<div ref="forwardPanel" class="panels forward-panel">
				<div class="ant-input-wrapper ant-input-group forward-input-wrapper"
				     style="width: 95%; margin-left: 2.5%;">
					<a-mentions ref="forwardTextArea" style="font-size: 1rem; background-color: rgb(250,250,250)"
					            v-model:value="forwardInput" :maxlength="250" auto-size autofocus
								:loading="isUserListLoading" @search="onSearchUser"
					            placeholder="输入评论">
						<a-mentions-option v-for="({ id, nick_name, avatar_url }) in userList" :key="id"
							:value="nick_name">
							<img :src="avatar_url" :alt="nick_name" style="width: 20px; margin-right: 8px;">
							<span>{{ nick_name }}</span>
						</a-mentions-option>
					</a-mentions>
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
		postId: Number,
		overlay: {
			type: Boolean,
			default: true
		}
	},
	inject: {
		postDetailEvents: {
			type: Object
		},
		lptContainer: {
			type: String
		}
	},
	components: {
		SendOutlined
	},
	data() {
		let post;
		if (typeof this.postId !== 'undefined') {
			this.curCommentTargetKey = lpt.postServ.type + '#' + this.postId;
			post = global.states.postManager.get({
				itemId: this.postId
			});
		} else {
			post = {};
		}
		return {
			post: post,
			showCommentPanel: false,
			showForwardPanel: false,
			commentInput: '',
			forwardInput: '',
			isUserListLoading: false,
			panelStyle: {},
			userList: []
		}
	},
	computed: {
		clientWidth() {
			if (this.lptContainer === 'blogDrawer') {
				return global.states.style.drawerWidth;
			} else if (this.lptContainer === 'blogMain') {
				return global.states.style.blogMainWidth;
			} else {
				return global.states.style.bodyWidth;
			}
		}
	},
	created() {
		this.lptConsumer = lpt.createConsumer();
		if (this.postDetailEvents) {
			this.postDetailEvents.on('openForwardPanel', () => {
				this.openForwardPanel();
			});
			this.postDetailEvents.on('openCommentPanel', (param) => {
				this.openCommentPanel(param);
			});
		}
		if (this.commentDetailEvents) {
			this.commentDetailEvents.on('openCommentPanel', (param) => {
				this.openCommentPanel(param);
			});
		}
	},
	mounted() {
		this.panelStyle = {
			width: this.clientWidth + 'px',
			left: this.lptContainer === 'blogMain' ? (global.states.style.blogMainLeft + 'px') : '0'
		};
		if (this.lptContainer === 'blogMain') {
			this.panelStyle.boxShadow = '0 0 10px 6px rgba(0, 0, 0, 0.25)';
			this.panelStyle.paddingBottom = '1.5rem';
		}
	},
	watch: {
		clientWidth(width) {
			this.$nextTick(() => {
				this.panelStyle.width = width;
				const left = this.lptContainer === 'blogMain' ? (global.states.style.blogMainLeft + 'px') : '0';
				this.panelStyle.left = left;
				if (this.showCommentPanel) {
					this.resetSheetWidth('commentPanel');
				}
				if (this.showForwardPanel) {
					this.resetSheetWidth('forwardPanel');
				}
			});
		}
	},
	methods: {
		openForwardPanel() {
			this.showForwardPanel = true;
			this.resetSheetWidth('forwardPanel');
			this.$nextTick(() => {
				if (this.$refs.forwardTextArea)
					this.$refs.forwardTextArea.$el.focus();
			});
		},
		openCommentPanel(param) {
			const preCommentTargetKey = this.curCommentTargetKey;
			if (param) {
				this.curCommentTargetKey = param.type + '#' + param.id;
			} else {
				this.curCommentTargetKey = lpt.postServ.type + '#' + this.postId;
			}
			if (!this.commentInputMap) {
				this.commentInputMap = new Map();
			}
			this.commentInputMap.set(preCommentTargetKey, this.commentInput);
			this.commentInput = this.commentInputMap.get(this.curCommentTargetKey);
			if (!this.commentInput) {
				this.commentInput = param.presetText;
			}
			this.showCommentPanel = true;
			this.resetSheetWidth('commentPanel');
			this.$nextTick(() => {
				if (this.$refs.commentTextArea)
					this.$refs.commentTextArea.$el.focus();
			});
		},
		resetSheetWidth(panelName) {
			// 因为vant框架中action-sheet的宽度是一次性设置的，所以这里要手动修改一下
			const panelElem = this.$refs[panelName];
			if (panelElem) {
				const sheetElem = panelElem.parentNode.parentNode;
				sheetElem.style.width = this.panelStyle.width + 'px';
				sheetElem.style.left = this.panelStyle.left;
			}
		},
		onSearchUser(inputName) {
			this.isUserListLoading = true;
			lpt.userServ.getByNamePattern({
				consumer: this.lptConsumer,
				param: {
					namePattern: inputName + '*'
				},
				success: (result) => {
					this.userList = result.object.map((user) => {
						return {
							id: user.id,
							nick_name: user.nick_name,
							avatar_url: lpt.getUserAvatarUrl(user)
						}
					});
					this.isUserListLoading = false;
				}
			})
		},
		sendForward() {
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
				success: () => {
					this.showForwardPanel = false;
					this.forwardInput = '';
					Toast.success('转发成功');
					const refs = this.$parent.$refs;
					if (refs.forwardList) {
						refs.forwardList.pushForward({
							creator: global.states.curOperator.user,
							create_time: new Date().getTime()
						});
					}
					++this.post.forward_cnt;
				},
				fail(result) {
					Toast.fail(result.message);
				}
			});
		},
		sendComment() {
			if (this.commentInput.length === 0) {
				Toast.fail('请输入评论');
				return
			}
			const strPart = this.curCommentTargetKey.split('#');
			const type = strPart[0];
			const data = {
				content: this.commentInput,
				parent_id: parseInt(strPart[1])
			};
			if (type === lpt.commentServ.level2) {
				// 二级评论的回复除了有上级ID外还有回复的对应二级评论的ID
				data.reply_to_l2_id = parseInt(strPart[2]);
				data.reply_to_user_id = parseInt(strPart[3]);
			}
			lpt.commentServ.create({
				consumer: this.lptConsumer,
				param: {
					type: type
				},
				data: data,
				success: (result) => {
					this.showCommentPanel = false;
					this.commentInput = '';
					Toast.success('评论成功');
					const comment = {
						id: parseInt(result.object),
						creator: global.states.curOperator.user,
						content: data.content,
						create_time: new Date().getTime(),
						like_cnt: 0,
						rights: {
							delete: true
						}
					};
					if (type === lpt.commentServ.level1) {
						// 当前打开了评论列表，且发出的评论是一级评论，即评论对象为帖子
						// 则在评论列表中添加一条
						comment.post_id = data.parent_id;
						comment.l2_cnt = 0;
						comment.poster_rep_cnt = 0;
						comment.entity_type = 'CML1';
						++this.post.comment_cnt;
						global.states.commentL1Manager.add(comment);
						global.states.postManager.add(this.post);
						this.postDetailEvents.emit('publishCommentL1', comment);
					} else if (type === lpt.commentServ.level2) {
						// 当前打开了评论列表，且发出的是二级评论，即评论对象为一级评论
						// 增加该评论所属一级评论的二级评论数量
						comment.entity_type = 'CML2';
						comment.post_id = this.postId;
						comment.l1_id = data.parent_id;
						const l1Comment = global.states.commentL1Manager.get({
							itemId: data.parent_id
						});
						++l1Comment.l2_cnt;
						if (global.states.curOperator.user.id === this.post.creator.id) {
							// 当前用户是帖子创建者，则增加帖子创建者回复数量
							++l1Comment.poster_rep_cnt;
						}
						global.states.commentL2Manager.add(comment);
						this.postDetailEvents.emit('publishCommentL2', comment);
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

:global(.van-popup) {
	transition: width 0.3s cubic-bezier(0.7, 0.3, 0.1, 1);
}

:global(.ant-mentions-dropdown) {
	z-index: 3000
}
</style>