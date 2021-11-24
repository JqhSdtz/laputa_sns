<template>
	<a-row class="bottom-bar" justify="space-between">
		<a-col class="icon-col" span="6" @click="showPostDetail">
			<share-alt-outlined class="icon"/>
			<span class="cnt">{{ post.forward_cnt }}</span>
		</a-col>
		<a-col class="icon-col" span="6" @click="showPostDetail">
			<comment-outlined class="icon"/>
			<span class="cnt">{{ post.comment_cnt }}</span>
		</a-col>
		<a-col class="icon-col" span="6">
			<span v-check-sign="{click: changeLike}">
				<like-filled v-if="post.liked_by_viewer" class="icon" style="color: red"/>
				<like-outlined v-else class="icon"/>
			</span>
			<span class="cnt">{{ post.like_cnt }}</span>
		</a-col>
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
import {
	LikeOutlined, LikeFilled,
	CommentOutlined, ShareAltOutlined,
	EllipsisOutlined
} from '@ant-design/icons-vue';
import lpt from '@/lib/js/laputa/laputa';
import global from '@/lib/js/global';
import {Dialog, Toast} from 'vant';
import {toRef} from 'vue';

export default {
	name: 'BottomBar',
	props: {
		postId: Number,
		postOf: String,
		showActions: Boolean
	},
	inject: {
		postListEvents: {
			type: Object
		},
		lptContainer: {
			type: String
		}
	},
	components: {
		LikeOutlined,
		LikeFilled,
		CommentOutlined,
		ShareAltOutlined,
		EllipsisOutlined
	},
	data() {
		const post = global.states.postManager.get({
			itemId: this.postId
		});
		return {
			post: post,
			showPopover: false,
			actions: this.initActions(post),
			showDrawer: toRef(global.states.blog, 'showDrawer')
		}
	},
	created() {
		this.lptConsumer = lpt.createConsumer();
		const ref = this;
		if (this.postListEvents) {
			this.postListEvents.on('refreshList', () => {
				ref.actions = ref.initActions(ref.post);
			});
		}
	},
	watch: {
		showDrawer(isShow) {
			if (this.lptContainer === 'blogDrawer' && !isShow) {
				this.showPopover = false;
			}
		}
	},
	methods: {
		initActions(post) {
			const actions = [];
			if (post.rights) {
				const rights = post.rights;
				if (rights.be_topped) {
					const isTopped = post.is_topped;
					actions.push({
						id: isTopped ? 'unTop' : 'top',
						text: isTopped ? '取消置顶' : '置顶'
					});
				}
				if (rights.edit) {
					actions.push({
						id: 'edit',
						text: '编辑'
					});
				}
				if (rights.update_category) {
					actions.push({
						id: 'updateCategory',
						text: '迁移目录'
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
			if (!this.postListEvents)
				return;
			const ref = this;
			const prompt = global.methods.getPrompt(this.lptContainer);
			if (action.id === 'top' || action.id === 'unTop') {
				if (ref.postOf === 'category') {
					// 目录设置置顶贴需要输入理由
					prompt({
						onConfirm(value) {
							ref.postListEvents.emit(action.id, {
								post: ref.post,
								comment: value,
								callback() {
									ref.actions = ref.initActions(ref.post);
								}
							});
						}
					});
				} else {
					// 个人设置置顶帖不需要
					this.postListEvents.emit(action.id, {
						post: this.post,
						callback() {
							ref.actions = ref.initActions(ref.post);
						}
					});
				}
			} else if (action.id === 'delete') {
				if (this.post.creator.id === global.states.curOperator.user.id) {
					Dialog.confirm({
						title: '删帖',
						message: '确认删除该帖？'
					}).then(() => {
						// 删自己的，不需要理由
						this.postListEvents.emit(action.id, {
							post: this.post
						});
					}).catch(() => {
					});
				} else {
					// 否则需要输入理由
					prompt({
						onConfirm(value) {
							ref.postListEvents.emit(action.id, {
								post: ref.post,
								comment: value
							});
						}
					});
				}
			} else if (action.id === 'edit') {
				this.$router.push({
					path: '/publish',
					query: {
						opType: 'edit',
						postId: ref.post.id
					}
				});
			} else if (action.id === 'updateCategory') {
				this.postListEvents.emit(action.id, {
					post: this.post
				});
			} else if (action.id === 'report') {
				this.$router.push({
					path: '/report'
				});
			}
		},
		showPostDetail() {
			if (this.lptContainer === 'blogMain') {
				this.$router.push({
					path: '/blog/post_detail/' + this.post.id
				});
			} else {
				this.$router.push({
					path: '/post_detail/' + this.post.id
				});
			}
		},
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
	padding: 0.5rem 1rem;
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