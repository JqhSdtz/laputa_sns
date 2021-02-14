<template>
	<div style="height: 100%">
		<van-empty v-if="hasEverLoad && isEmpty" description="没有帖子"/>
		<van-pull-refresh ref="pullArea" v-show="hasEverLoad && !isEmpty" v-model="isRefreshing" @refresh="onRefresh"
		                  success-text="刷新成功">
			<van-list class="post-list" @load="loadMore" :offset="listOffset"
			          v-model:loading="isBusy" :finished="finished" finished-text="没有更多了">
				<post-item class="post-item" v-for="post in list" :post-id="post.id" :post-of="postOf"
				           :key="post.id" :class="{post, 'last-post': post.last}"/>
			</van-list>
		</van-pull-refresh>
	</div>
</template>

<script>
import lpt from '@/lib/js/laputa/laputa';
import global from '@/lib/js/global';
import {toRef} from "vue";
import {Toast} from 'vant';
import PostItem from './item/PostItem';
import {createEventBus} from "@/lib/js/global/global-events";

const querior = lpt.createQuerior();
const postListEvents = createEventBus();

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
		userId: String,
		onLoaded: Function
	},
	provide: {
		postListEvents: postListEvents
	},
	components: {
		PostItem
	},
	data() {
		return {
			finished: toRef(querior, 'hasReachedBottom'),
			hasEverLoad: false,
			isEmpty: false,
			list: [],
			listOffset: global.vars.style.tabBarHeight + 10,
			isRefreshing: false,
			isBusy: false
		}
	},
	created() {
		const ref = this;
		this.lptConsumer = lpt.createConsumer();
		this.hasReallyLoaded = false;
		querior.onBusyChange(isBusy => {
			this.$nextTick(() => {
				// 这里单独建一个isBusy属性是为了防止
				// 全局的isBusy变动触发无限下滑组件检测状态导致错误请求
				ref.isBusy = isBusy;
			});
		});
		if (ref.postOf === 'category') {
			this.defaultQueryOption = {
				querior,
				param: {
					queryType: this.sortType,
				},
				data: {
					category_id: this.categoryId
				}
			};
		} else if (ref.postOf === 'creator') {
			this.defaultQueryOption = {
				querior,
				data: {
					creator_id: this.creatorId
				}
			};
		}
		this.loadMore(false);
		global.events.on(['signIn', 'signOut', 'forceRefresh'], (obj, name) => {
			if (name === 'forceRefresh')
				ref.isRefreshing = true;
			ref.onRefresh();
		});
		postListEvents.on(['top', 'unTop'], (param, name) => {
			const isCancel = name === 'unTop';
			const post = param.post;
			let fun;
			let data;
			if (ref.postOf === 'category') {
				fun = lpt.categoryServ.setTopPost;
				data = {
					id: this.categoryId,
					top_post_id: post.id,
					op_comment: param.comment
				};
			} else {
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
				success() {
					Toast.success(isCancel ? '取消成功' : '置顶成功');
					post.is_topped = !isCancel;
					param.callback && param.callback();
				},
				fail(result) {
					Toast.fail(result.message);
				}
			});
		});
		postListEvents.on('delete', (param) => {
			const post = param.post;
			const ref = this;
			lpt.contentServ.delete({
				consumer: this.lptConsumer,
				param: {
					type: lpt.contentType.post
				},
				data: {
					id: post.id,
					op_comment: param.comment
				},
				success() {
					Toast.success('删除成功');
					ref.list.splice(ref.list.indexOf(post), 1);
				},
				fail(result) {
					Toast.fail(result.message);
				}
			});
		});
	},
	unmounted() {
		querior.reset();
	},
	watch: {
		sortType(newValue) {
			this.reset();
			this.defaultQueryOption.param.queryType = newValue;
			this.loadMore(true);
		}
	},
	methods: {
		onRefresh() {
			this.reset();
			this.loadMore(true);
		},
		reset() {
			querior.reset();
			this.hasEverLoad = false;
		},
		loadMore(isRefresh) {
			const ref = this;
			if (!querior.hasReachedBottom) {
				let fun;
				if (this.postOf === 'category') {
					fun = lpt.postServ.queryForCategory;
				} else {
					fun = lpt.postServ.queryForCreator;
				}
				fun({
					...this.defaultQueryOption,
					success(result) {
						global.states.postManager.addList(result.object);
						if (!ref.hasEverLoad) {
							ref.list = result.object;
							ref.isEmpty = ref.list.length === 0;
							ref.hasEverLoad = true;
						} else {
							ref.list = ref.list.concat(result.object);
						}
						if (isRefresh) {
							postListEvents.emit('refreshList');
						}
					},
					fail(result) {
						Toast.fail(result.message);
					},
					complete() {
						if (!ref.hasReallyLoaded) {
							ref.$emit('loaded');
							ref.hasReallyLoaded = true;
						}
						ref.isRefreshing = false;
					}
				});
			}
		}
	}
}
</script>

<style scoped>
.post-list {
	height: 100%;
	overflow-y: visible;
	background-color: #ECECEC;
}

.post-item {
	padding: 0.5rem;
}
</style>