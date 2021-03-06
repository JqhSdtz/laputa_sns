<template>
	<div style="height: 100%">
		<van-empty v-if="hasEverLoad && isEmpty" description="没有帖子"/>
		<van-pull-refresh ref="pullArea" v-show="hasEverLoad && !isEmpty" v-model="isRefreshing" @refresh="onRefresh"
		                  success-text="刷新成功">
			<van-list ref="list" class="post-list" @load="loadMore" :offset="listOffset"
			          :fill-parent="$el" v-model:loading="isBusy" :finished="finished" finished-text="没有更多了">
				<slot :post-list="list" :top-post-id="topPostId" :post-of="postOf">
					<post-item class="post-item" v-for="post in list" :post-id="post.id" :post-of="postOf"
					           :key="post.id" :is-top-post="post.id === topPostId"
					           :class="{post, 'last-post': post.last}"/>
				</slot>
			</van-list>
		</van-pull-refresh>
		<teleport to="body">
			<prompt-dialog ref="categoryPrompt">
				<template v-if="showSelectedCategory" v-slot:tip>
					<category-grid-item :category-id="curSelectedCategory.id"/>
				</template>
			</prompt-dialog>
		</teleport>
	</div>
</template>

<script>
import lpt from '@/lib/js/laputa/laputa';
import global from '@/lib/js/global';
import {toRef} from "vue";
import {Toast} from 'vant';
import PostItem from '../item/PostItem';
import PromptDialog from '@/components/global/PromptDialog';
import CategoryGridItem from "@/components/category/item/CategoryGridItem";
import {createEventBus} from "@/lib/js/global/global-events";

export default {
	name: 'PostList',
	props: {
		postOf: {
			type: String,
			default: 'category'
		},
		creatorId: {
			type: Number
		},
		categoryId: {
			type: Number,
			default: 0
		},
		sortType: {
			type: String,
			default: 'popular'
		},
		topPostId: Number,
		userId: String,
		onLoaded: Function,
		customLoadProcess: Function,
		onBatchProcessed: Function
	},
	emits: ['refresh'],
	provide() {
		return {
			postListEvents: this.postListEvents
		}
	},
	components: {
		PostItem,
		PromptDialog,
		CategoryGridItem
	},
	data() {
		this.querior = lpt.createQuerior();
		this.postListEvents = createEventBus();
		return {
			finished: toRef(this.querior, 'hasReachedBottom'),
			showSelectedCategory: false,
			curSelectedCategory: lpt.categoryServ.getDefaultCategory(-1),
			hasEverLoad: false,
			isEmpty: false,
			list: [],
			listOffset: global.vars.style.tabBarHeight + 10,
			isRefreshing: false,
			isBusy: false
		}
	},
	watch: {
		sortType(newValue) {
			this.reset();
			this.defaultQueryOption.param.queryType = newValue;
			this.loadMore(true);
		}
	},
	created() {
		this.lptConsumer = lpt.createConsumer();
		this.hasReallyLoaded = false;
		this.querior.onBusyChange(isBusy => {
			this.$nextTick(() => {
				// 这里单独建一个isBusy属性是为了防止
				// 全局的isBusy变动触发无限下滑组件检测状态导致错误请求
				if (isBusy) {
					// 变为繁忙状态，直接改变
					this.isBusy = isBusy;
				} else {
					// 结束繁忙状态，要判断是否有自定义处理过程
					// 如果有，则要判断自定义处理过程是否执行完毕
					if (this.curLoadPromise) {
						this.curLoadPromise.finally(() => {
							this.isBusy = isBusy;
						});
					} else {
						this.isBusy = isBusy;
					}
				}
			});
		});
		if (this.postOf === 'category') {
			this.defaultQueryOption = {
				querior: this.querior,
				param: {
					queryType: this.sortType,
				},
				data: {
					category_id: this.categoryId
				}
			};
		} else if (this.postOf === 'creator') {
			this.defaultQueryOption = {
				querior: this.querior,
				data: {
					creator_id: this.creatorId
				}
			};
		} else if (this.postOf === 'news') {
			this.defaultQueryOption = {
				querior: this.querior
			};
		}
		this.loadMore(false);
		global.events.on(['signIn', 'signOut', 'forceRefresh'], (obj, name) => {
			if (name === 'forceRefresh')
				this.isRefreshing = true;
			this.onRefresh();
		});
		this.postListEvents.on(['top', 'unTop'], (param, name) => {
			const isCancel = name === 'unTop';
			const post = param.post;
			let fun;
			let data;
			if (this.postOf === 'category') {
				fun = lpt.categoryServ.setTopPost;
				data = {
					id: this.categoryId,
					top_post_id: post.id,
					op_comment: param.comment
				};
			}
			if (this.postOf === 'creator') {
				fun = lpt.userServ.setTopPost;
				data = {
					top_post_id: post.id
				};
			}
			fun({
				consumer: this.lptConsumer,
				param: {
					isCancel: isCancel
				},
				data: data,
				success: () => {
					Toast.success(isCancel ? '取消成功' : '置顶成功');
					post.is_topped = !isCancel;
					if (this.postOf === 'category') {
						const category = global.states.categoryManager.get({
							itemId: this.categoryId
						});
						if (category) {
							category.top_post_id = isCancel ? -1 : post.id;
						}
					} else if (this.postOf === 'creator') {
						const user = global.states.userManager.get({
							itemId: this.creatorId
						});
						if (user) {
							user.top_post_id = isCancel ? -1 : post.id;
						}
					}
					param.callback && param.callback();
				},
				fail(result) {
					Toast.fail(result.message);
				}
			});
		});
		this.postListEvents.on('delete', (param) => {
			const post = param.post;
			lpt.contentServ.delete({
				consumer: this.lptConsumer,
				param: {
					type: lpt.contentType.post
				},
				data: {
					id: post.id,
					op_comment: param.comment
				},
				success: () => {
					Toast.success('删除成功');
					this.list.splice(this.list.findIndex(_post => _post.id === post.id), 1);
				},
				fail(result) {
					Toast.fail(result.message);
				}
			});
		});
		this.postListEvents.on('updateCategory', (param) => {
			const prompt = this.$refs.categoryPrompt.prompt;
			prompt({
				title: '输入目标目录的ID',
				placeholder: ' ',
				onValidate: (value) => {
					return global.states.categoryManager.get({
						itemId: value,
						getPromise: true
					}).then((category) => {
						this.curSelectedCategory = category;
					}).catch(() => {
						return Promise.reject(`ID为"${value}"的目录不存在`);
					});
				},
				onConfirm: (value) => {
					this.showSelectedCategory = true;
					return prompt({
						inputType: 'none',
						title: '迁移目录',
						tipMessage: '确认迁移到该目录？',
						onFinish: () => this.$nextTick(() => this.showSelectedCategory = false),
						onValidate: () => true,
						onConfirm: () => {
							lpt.postServ.setCategory({
								consumer: this.lptConsumer,
								data: {
									id: param.post.id,
									category_id: value
								},
								success: () => {
									Toast.success('迁移目录成功');
									// ref.list.splice(ref.list.indexOf(param.post), 1);
								},
								fail(result) {
									Toast.fail(result.message);
								}
							});
						}
					});
				}
			});
		});
	},
	unmounted() {
		this.querior.reset();
	},
	methods: {
		onRefresh() {
			this.reset();
			this.loadMore(true);
		},
		reset() {
			this.querior.reset();
			this.hasEverLoad = false;
		},
		loadMore(isRefresh) {
			if (!this.querior.hasReachedBottom) {
				let fun;
				if (this.postOf === 'category') {
					fun = lpt.postServ.queryForCategory;
				} else if (this.postOf === 'creator') {
					fun = lpt.postServ.queryForCreator;
				} else if (this.postOf === 'news') {
					fun = lpt.newsServ.query;
				}
				fun({
					...this.defaultQueryOption,
					success: (result) => {
						if (this.postOf === 'news') {
							result.object = result.object.filter(obj => obj.content)
								.map(obj => obj.content);
						}
						const promiseList = [];
						if (this.customLoadProcess) {
							result.object.forEach((post) => {
								promiseList.push(this.customLoadProcess(post));
							});
							this.curLoadPromise = Promise.all(promiseList)
								.then(() => this.onBatchProcessed && this.onBatchProcessed(result.object, this.list));
						}
						global.states.postManager.addList(result.object);
						if (!this.hasEverLoad) {
							this.list = result.object;
							this.isEmpty = this.list.length === 0;
							this.hasEverLoad = true;
						} else {
							this.list = this.list.concat(result.object);
						}
						if (isRefresh) {
							this.$emit('refresh');
							this.postListEvents.emit('refreshList');
						}
					},
					fail(result) {
						Toast.fail(result.message);
					},
					complete: () => {
						if (!this.hasReallyLoaded) {
							this.$emit('loaded');
							this.hasReallyLoaded = true;
						}
						this.isRefreshing = false;
					}
				});
			}
		}
	}
}
</script>

<style scoped>
.post-list {
	height: 100% !important;
	overflow-y: visible;
	background-color: #ECECEC;
}

.post-item {
	padding: 0.5rem;
}
</style>