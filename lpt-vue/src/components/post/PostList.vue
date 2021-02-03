<template>
	<div class="post-list" v-infinite-scroll="loadMore"
	     infinite-scroll-disabled="busy" infinite-scroll-distance="10">
		<a-back-top style="bottom: 4.5rem;" :target="getElement"/>
		<post-item class="post-item" v-for="post in list" :post_id="post.id"
		           :key="post.id" :class="{post, 'last-post': post.last}"></post-item>
	</div>
</template>

<script>
import PostItem from './item/PostItem'
import lpt from '@/lib/js/laputa/laputa'
import infiniteScroll from '@/lib/js/actions/infinite-scroll'
import global from '@/lib/js/global/global-state';

const querior = lpt.createQuerior();

export default {
	name: 'PostList',
	props: {
		categoryId: String,
		sortType: {
			type: String,
			default: 'popular'
		},
		onLoaded: Function,
		userId: String
	},
	components: {
		PostItem
	},
	directives: {
		infiniteScroll
	},
	data() {
		return {
			list: [],
			busy: false
		}
	},
	created() {
		const ref = this;
		querior.onBusyChange(isBusy => {
			this.$nextTick(() => {
				// 这里单独建一个busy属性是为了防止
				// 全局的isBusy变动触发无限下滑组件检测状态导致错误请求
				ref.busy = isBusy;
			});
		});
		this.init();
	},
	unmounted() {
		querior.reset();
	},
	watch: {
		sortType() {
			this.init();
		}
	},
	methods: {
		init() {
			const ref = this;
			querior.reset();
			this.defaultQueryOption = {
				querior,
				param: {
					queryType: this.sortType,
				},
				data: {
					category_id: this.categoryId
				}
			};
			lpt.postServ.queryForCategory({
				...this.defaultQueryOption,
				success(result) {
					ref.list = global.postManager.addList(result.object);
					if (ref.onLoaded) {
						ref.onLoaded(ref.list);
					}
				}
			});
		},
		loadMore() {
			const ref = this;
			if (!querior.hasReachedBottom) {
				lpt.postServ.queryForCategory({
					...this.defaultQueryOption,
					success(result) {
						global.postManager.addList(result.object);
						ref.list = ref.list.concat(result.object);
					},
					fail(result) {
						alert(result.message);
					}
				});
			}
		},
		getElement() {
			return this.$el;
		}
	}
}
</script>

<style scoped>
.post-list {
	height: 100%;
	overflow-y: scroll;
	background-color: #ECECEC;
}
.post-list::-webkit-scrollbar {
	display: none;
}
.post-item {
	padding: 0.5rem;
}
</style>