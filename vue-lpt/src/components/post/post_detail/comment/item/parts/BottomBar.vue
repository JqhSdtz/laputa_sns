<template>
	<a-row class="bottom-bar" justify="space-between">
		<template v-if="comment.entity_type === 'CML1'">
			<a-col class="icon-col" span="6" offset="11">
				<comment-outlined class="icon"  v-check-sign="{click: openCommentPanel}"/>
				<span class="cnt">{{ comment.l2_cnt }}</span>
			</a-col>
			<a-col class="icon-col" span="6">
				<span  v-check-sign="{click: changeLike}">
					<like-filled v-if="comment.liked_by_viewer" class="icon" style="color: red"/>
					<like-outlined v-else class="icon"/>
				</span>
				<span class="cnt">{{ comment.like_cnt }}</span>
			</a-col>
		</template>
		<template v-else>
			<a-col class="icon-col" span="6" offset="17">
				<span v-check-sign="{click: changeLike}">
					<like-filled v-if="comment.liked_by_viewer" class="icon" style="color: red"/>
					<like-outlined v-else class="icon"/>
				</span>
				<span class="cnt">{{ comment.like_cnt }}</span>
			</a-col>
		</template>
		<a-col span="1" v-if="showActions" class="actions-icon">
			<van-popover v-model:show="showPopover" :actions="actions" @select="doAction" placement="bottom-end">
				<template v-slot:reference>
					<ellipsis-outlined :rotate="90"/>
				</template>
			</van-popover>
		</a-col>
	</a-row>
</template>

<script>
import {CommentOutlined, LikeFilled, LikeOutlined, EllipsisOutlined} from '@ant-design/icons-vue';
import global from '@/lib/js/global';
import lpt from '@/lib/js/laputa/laputa';
import {Toast} from "vant";

export default {
	name: 'BottomBar',
	props: {
		type: String,
		commentId: Number,
		showActions: Boolean
	},
	inject: {
		postDetailEvents: {
			type: Object
		},
		commentListEvents: {
			type: Object
		}
	},
	components: {
		LikeOutlined,
		LikeFilled,
		CommentOutlined,
		EllipsisOutlined
	},
	data() {
		let comment;
		if (this.type === lpt.commentServ.level1) {
			comment = global.states.commentL1Manager.get({
				itemId: this.commentId
			});
		} else {
			comment = global.states.commentL2Manager.get({
				itemId: this.commentId
			});
		}
		return {
			comment: comment,
			showPopover: false,
			actions: this.initActions(comment)
		}
	},
	created() {
		this.lptConsumer = lpt.createConsumer();
		const ref = this;
		if (this.commentListEvents) {
			this.commentListEvents.on('refreshList', () => {
				ref.actions = ref.initActions(ref.comment);
			});
		}
	},
	methods: {
		initActions(comment) {
			const actions = [];
			if (comment.rights) {
				const rights = comment.rights;
				if (rights.be_topped) {
					const isTopped = comment.is_topped;
					actions.push({
						id: isTopped ? 'unTop' : 'top',
						text: isTopped ? '取消置顶' : '置顶'
					});
				}
				if (rights.delete) {
					actions.push({
						id: 'delete',
						text: '删除'
					});
				}
			}
			actions.push({
				id: 'report',
				text: '举报'
			});
			return actions;
		},
		doAction(action) {
			if (!this.commentListEvents)
				return;
			const ref = this;
			const curUserId = global.states.curOperator.user.id;
			if (action.id === 'top' || action.id === 'unTop') {
				this.commentListEvents.emit(action.id, {
					comment: this.comment,
					callback() {
						ref.actions = ref.initActions(ref.comment);
					}
				});
			} else if (action.id === 'delete') {
				if (this.comment.creator.id === curUserId) {
					// 删自己的，不需要理由
					this.commentListEvents.emit(action.id, {
						comment: this.comment
					});
				} else {
					// 否则需要输入理由
					global.methods.prompt({
						onConfirm(value) {
							ref.commentListEvents.emit(action.id, {
								comment: ref.comment,
								op_comment: value
							});
						}
					});
				}
			} else if (action.id === 'report') {
				this.$router.push({
					path: '/report'
				});
			}
		},
		openCommentPanel() {
			if (this.postDetailEvents) {
				this.postDetailEvents.emit('openCommentPanel', {
					type: lpt.commentServ.level2,
					id: this.comment.id
				});
			}
		},
		changeLike() {
			const ref = this;
			const isLiked = this.comment.liked_by_viewer;
			const fun = isLiked ? lpt.likeServ.unlike : lpt.likeServ.like;
			const commentLevel = this.comment.entity_type === 'CML1' ? lpt.contentType.commentL1 : lpt.contentType.commentL2;
			fun({
				consumer: this.lptConsumer,
				ignoreGlobalBusyChange: true,
				param: {
					type: commentLevel
				},
				data: {
					target_id: this.comment.id
				},
				success() {
					ref.comment.liked_by_viewer = !isLiked;
					ref.comment.like_cnt += isLiked ? -1 : 1;
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
	padding: 0 1rem;
	position: relative;
}

.icon-col {
	text-align: center;
}

.cnt {
	margin-left: 0.5rem;
}

.icon {
	font-size: 1rem;
}
</style>