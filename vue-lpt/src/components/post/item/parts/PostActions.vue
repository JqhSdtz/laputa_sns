<template>
    <van-popover v-model:show="showPopover" 
        :actions="actions" 
        @select="doAction" 
        :placement="placement">
        <template v-slot:reference>
            <ellipsis-outlined :rotate="90"/>
        </template>
    </van-popover>
</template>

<script>
import {EllipsisOutlined} from '@ant-design/icons-vue';
import global from '@/lib/js/global';
import lpt from '@/lib/js/laputa/laputa';
import {toRef} from 'vue';
import {Dialog, Toast} from 'vant';

export default {
    name: 'PostActions',
    components: {
        EllipsisOutlined
    },
    props: {
        postId: Number,
        postOf: String,
        placement: {
            type: String,
            default: 'bottom-end'
        }
    },
    inject: {
        postListEvents: {
			type: Object
		},
        lptContainer: {
			type: String
		},
        prompts: {
            type: Object
        }
    },
    data() {
        const post = global.states.postManager.get({
			itemId: this.postId
		});
        return {
            post: post,
            actions: this.initActions(post),
            showPopover: false,
            showDrawer: toRef(global.states.blog, 'showDrawer')
        }
    },
    watch: {
		showDrawer(isShow) {
			if (this.lptContainer === 'blogDrawer' && !isShow) {
				this.showPopover = false;
			}
		},
        'post.rights'() {
            this.actions = this.initActions(this.post);
        }
	},
    created() {
        this.postListEvents && this.postListEvents.on('refreshList', () => {
            this.actions = this.initActions(this.post);
        });
        this.lptConsumer = lpt.createConsumer();
    },
    methods: {
        initActions(post) {
			const actions = [];
			if (post.rights) {
				const rights = post.rights;
				if (rights.be_topped && this.postOf) {
                    // 处在帖子列表中的帖子才可以判断置顶的具体操作
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
			const prompt = this.prompts.plainPrompt;
			if (action.id === 'top' || action.id === 'unTop') {
                // 置顶操作交给PostList，因为需要判断帖子所在的列表是目录还是用户主页
				if (this.postOf === 'category') {
					// 目录设置置顶贴需要输入理由
					prompt({
						onConfirm: (value) => {
							this.postListEvents && this.postListEvents.emit(action.id, {
								post: this.post,
								comment: value,
								callback() {
									this.actions = this.initActions(this.post);
								}
							});
						}
					});
				} else {
					// 个人设置置顶帖不需要
                    this.prompts.plainPrompt({
                        title: '置顶',
                        inputType: 'none',
                        tipMessage: '确认置顶该帖？',
                        onConfirm: () => {
                            this.postListEvents && this.postListEvents.emit(action.id, {
                                post: this.post,
                                callback: () => {
                                    this.actions = this.initActions(this.post);
                                }
                            });
                        }
                    });
				}
			} else if (action.id === 'delete') {
				this.doDelete();
			} else if (action.id === 'edit') {
				this.doEdit();
			} else if (action.id === 'updateCategory') {
				this.doUpdateCategory();
			} else if (action.id === 'report') {
				this.doReport();
			}
		},
        doDelete() {
            const fn = (post, comment) => {
                lpt.contentServ.delete({
                    consumer: this.lptConsumer,
                    param: {
                        type: lpt.contentType.post
                    },
                    data: {
                        id: post.id,
                        op_comment: comment
                    },
                    success: () => {
                        Toast.success('删除成功');
                        global.events.emit('deletePost', post);
                    },
                    fail(result) {
                        Toast.fail(result.message);
                    }
                });
            }
            if (this.post.creator.id === global.states.curOperator.user.id) {
                this.prompts.confirm({
                    title: '删帖',
                    message: '确认删除该帖？',
                    onConfirm: () => {
                        // 删自己的，不需要理由
                        fn(this.post);
                    }
                });
            } else {
                // 否则需要输入理由
                const prompt = this.prompts.plainPrompt;
                prompt({
                    onConfirm: (value) => {
                        fn(this.post, value);
                    }
                });
            }
        },
        doEdit() {
            this.$router.push({
                path: '/publish',
                query: {
                    opType: 'edit',
                    postId: this.post.id
                }
            });
        },
        doUpdateCategory() {
            const prompt = this.prompts.categoryPrompt;
			prompt((category) => {
				return {
					inputType: 'none',
					title: '迁移目录',
					tipMessage: '确认迁移到该目录？',
					onValidate: () => true,
					onConfirm: () => {
						lpt.postServ.setCategory({
							consumer: this.lptConsumer,
							data: {
								id: this.post.id,
								category_id: category.id
							},
							success: () => {
								Toast.success('迁移目录成功');
								global.events.emit('updateCategory', {
                                    post: this.post,
                                    category: category
                                });
							},
							fail(result) {
								Toast.fail(result.message);
							}
						});
					}
				}
			});
        },
        doReport() {
            this.$router.push({
                path: '/report'
            });
        }
    }
}
</script>