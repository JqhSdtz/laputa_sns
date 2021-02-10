<template>
	<div style="height: 100%">
		<van-empty v-if="hasEverLoad && isEmpty" description="没有帖子"/>
		<van-pull-refresh ref="pullArea" v-show="hasEverLoad && !isEmpty" v-model="isRefreshing" @refresh="onRefresh"
		                  success-text="刷新成功">
			<van-list class="post-list" @load="loadMore" :offset="listOffset"
			          v-model:loading="isBusy" :finished="finished" finished-text="没有更多了">
				<post-item class="post-item" v-for="post in list" :post-id="post.id"
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

const querior = lpt.createQuerior();

export default {
	name: 'PostList',
	props: {
		categoryId: String,
		sortType: {
			type: String,
			default: 'popular'
		},
		userId: String,
		onLoaded: Function
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
		this.hasReallyLoaded = false;
		querior.onBusyChange(isBusy => {
			this.$nextTick(() => {
				// 这里单独建一个isBusy属性是为了防止
				// 全局的isBusy变动触发无限下滑组件检测状态导致错误请求
				ref.isBusy = isBusy;
			});
		});
		this.defaultQueryOption = {
			querior,
			param: {
				queryType: this.sortType,
			},
			data: {
				category_id: this.categoryId
			}
		};
		this.loadMore();
		global.events.on(['login', 'forceRefresh'], (obj, name) => {
			if (name === 'forceRefresh')
				ref.isRefreshing = true;
			ref.onRefresh();
		});
	},
	unmounted() {
		querior.reset();
	},
	watch: {
		sortType(newValue) {
			this.reset();
			this.defaultQueryOption.param.queryType = newValue;
			this.loadMore();
		}
	},
	methods: {
		onRefresh() {
			this.reset();
			this.loadMore();
		},
		reset() {
			querior.reset();
			this.hasEverLoad = false;
		},
		loadMore() {
			const ref = this;
			if (!querior.hasReachedBottom) {
				lpt.postServ.queryForCategory({
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