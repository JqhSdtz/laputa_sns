<template>
	<div class="post-list">
		<van-empty v-if="hasEverLoad && isEmpty" description="没有帖子"/>
		<van-pull-refresh ref="pullArea" v-show="hasEverLoad && !isEmpty" v-model="isRefreshing" @refresh="onRefresh"
		    :disabled="disablePullRefresh" success-text="刷新成功">
			<van-list ref="list" class="post-list" @load="loadMore" :offset="listOffset"
			    :fill-parent="$el" v-model:loading="isBusy" :finished="finished" finished-text="没有更多了">
				<slot :post-list="list" :top-post-id="topPostId" :post-of="postOf">
					<post-item class="post-item" v-for="(post, index) in list" 
						:class="{post, 'last-post': post.last}"
						:style="itemStyle && itemStyle(post, index)"
						:post-id="post.id" 
						:post-of="postOf"
					    :key="post.id" 
						:is-top-post="post.id === topPostId"/>
				</slot>
			</van-list>
		</van-pull-refresh>
	</div>
</template>

<script>
import lpt from '@/lib/js/laputa/laputa';
import global from '@/lib/js/global';
import {toRef} from "vue";
import {Toast} from 'vant';
import PostItem from '../item/PostItem';
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
		// 是否在refresh的时候清除原来的列表，用于防止相册中img元素onload事件不触发
		clearOnRefresh: Boolean,
		itemStyle: Function,
		topPostId: Number,
		userId: String,
		postList: Object,
		onLoaded: Function,
		customLoadProcess: Function,
		onBatchProcessed: Function
	},
	emits: ['refresh', 'finish'],
	provide() {
		return {
			postListEvents: this.postListEvents
		}
	},
	inject: {
		lptContainer: {
			type: String
		}
	},
	components: {
		PostItem
	},
	data() {
		this.querior = lpt.createQuerior();
		this.postListEvents = createEventBus();
		const hasPostList = typeof this.postList !== 'undefined';
		return {
			// 如果提前指定了postList，就直接设置为已经加载完成
			finished: hasPostList ? true : toRef(this.querior, 'hasReachedBottom'),
			hasEverLoad: hasPostList,
			disablePullRefresh: hasPostList,
			isEmpty: false,
			list: this.postList || [],
			isRefreshing: false,
			isBusy: false
		}
	},
	computed: {
		listOffset() {
			return global.states.style.tabBarHeight + 10;
		}
	},
	watch: {
		sortType(newValue) {
			this.reset();
			this.defaultQueryOption.param.queryType = newValue;
			// 切换排序方式时先清空列表，防止原本就存在的元素位置异常
			this.list.splice(0);
			this.loadMore(true);
		},
		postList() {
			this.list = this.postList;
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
		if (!this.finished) {
			this.loadMore(false);
		}
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
		global.events.on(['signIn', 'signOut', 'forceRefresh', 'addFollow'], (obj, name) => {
			if (name === 'forceRefresh')
				this.isRefreshing = true;
			if (name === 'addFollow' && this.postOf !== 'news') return;
			this.onRefresh();
		});
		const belongToThisList = (post) => {
			return (this.postOf === 'category' && post.type_str === 'public'
				&& this.categoryId === post.category_id)
				|| (this.postOf === 'creator' && this.creatorId === post.creator_id);
		}
		global.events.on('createPost', (post) => {
			if (belongToThisList(post)) {
				this.isEmpty = false;
				this.list.unshift(post);
				this.increasePostCntOfCategory(this.categoryId, 1, true);
			}
		});
		global.events.on('deletePost', (post) => {
			if (belongToThisList(post)) {
				this.list.splice(this.list.findIndex(_post => _post.id === post.id), 1);
				if (this.list.length === 0) {
					this.isEmpty = true;
				}
            	this.increasePostCntOfCategory(this.categoryId, -1, true);
			}
		});
		global.events.on('updateCategory', (param) => {
			if (belongToThisList(param.post)) {
				this.list.splice(this.list.findIndex(_post => _post.id === param.post.id), 1);
				this.increasePostCntOfCategory(this.categoryId, -1, true);
				this.increasePostCntOfCategory(param.category.id, 1, true);
			}
		});
	},
	unmounted() {
		this.querior.reset();
	},
	methods: {
		onRefresh() {
			this.reset();
			if (this.clearOnRefresh) {
				this.list.splice(0);
			}
			this.loadMore(true);
		},
		reset() {
			this.querior.reset();
			this.hasEverLoad = false;
		},
		increasePostCntOfCategory(categoryId, delta, increaseParent) {
			// 变更当前目录路径上的所有目录的贴数
			global.states.categoryManager.get({
				itemId: categoryId,
				filter: (item) => item.path_list && item.path_list.length,
				success: (category) => {
					if (increaseParent) {
						const pathList = category.path_list;
						for (let i = 0; i < pathList.length; ++i) {
							const itemId = pathList[i].id;
							// justLocal是因为如果从远程获取，post_cnt就是最新的，不需要做改变
							// 所以仅获取本地的，并且有post_cnt字段的
							global.states.categoryManager.get({
								itemId: itemId,
								justLocal: true,
								success: (pCategory) => {
									if (typeof pCategory.post_cnt !== 'undefined') {
										pCategory.post_cnt += delta;
									}
								}
							});
						}
					} else {
						if (typeof category.post_cnt !== 'undefined') {
							category.post_cnt += delta;
						}
					}
				}
			});
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
							this.isEmpty = this.list.length === 0 && this.querior.hasReachedBottom;
							this.hasEverLoad = true;
						} else {
							this.list = this.list.concat(result.object);
						}
						if (isRefresh) {
							this.$emit('refresh');
							this.postListEvents.emit('refreshList');
						}
						if (this.querior.hasReachedBottom) {
							this.$emit('finish');
						}
					},
					fail(result) {
						Toast.fail(result.message);
					},
					complete: () => {
						const fun = () => {
							if (!this.hasReallyLoaded) {
								this.$emit('loaded');
								this.hasReallyLoaded = true;
							}
							this.isRefreshing = false;
						}
						if (this.curLoadPromise) {
							this.curLoadPromise.finally(() => {
								fun();
							});
						} else {
							fun();
						}
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