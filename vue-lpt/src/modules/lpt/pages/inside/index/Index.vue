<template>
	<template v-if="forceReloadFlag">
		<div class="main-area" :class="{'with-scroll-bar': lptContainer === 'blogMain'}"
		     :style="{height: scrollHeight, position: 'relative'}" keep-scroll-top v-scroll-view>
			<van-search v-model="searchValue" @search="onSearch" placeholder="请输入搜索关键词">
				<template v-slot:right-icon>
					<van-checkbox v-model="enableBoolMode">多关键字</van-checkbox>
				</template>
			</van-search>
			<sort-type-selector v-if="postListLoaded" v-model:sort-type="sortType"/>
			<a-back-top :style="{bottom: (mainBarHeight + 10) + 'px'}" :target="getElement"/>
			<post-list ref="postList" :category-id="category.id" :top-post-id="category.top_post_id"
			           :sort-type="sortType" @loaded="onPostListLoaded"/>
		</div>
	</template>
</template>

<script>
import PostList from '@/components/post/post_list/PostList';
import global from '@/lib/js/global';
import lpt from '@/lib/js/laputa/laputa';
import {Toast} from 'vant';
import SortTypeSelector from '@/components/post/post_list/SortTypeSelector';

export default {
	name: 'Index',
	components: {
		SortTypeSelector,
		PostList
	},
	props: {
		categoryId: {
			type: String,
			default: lpt.categoryServ.rootCategoryId.toString()
		}
	},
	inject: {
		lptContainer: {
			type: String
		}
	},
	data() {
		const category = global.states.categoryManager.get({
			itemId: parseInt(this.categoryId),
			fail(result) {
				Toast.fail(result.message);
			}
		});
		return {
			category,
			enableBoolMode: false,
			searchValue: '',
			mainBarHeight: global.vars.style.tabBarHeight,
			sortType: global.states.pages.index.sortType,
			postListLoaded: false
		}
	},
	watch: {
		categoryId() {
			this.category = global.states.categoryManager.get({
				itemId: parseInt(this.categoryId),
				fail(result) {
					Toast.fail(result.message);
				}
			});
			this.forceReload();
		}
	},
	computed: {
		scrollHeight() {
			const mainViewHeight = document.body.clientHeight;
			// 底部高度加0.5的padding
			let barHeight = this.mainBarHeight;
			if (global.vars.env === 'blog') {
				barHeight = global.vars.blog.style.mainViewOffsetBottom;
			}
			return mainViewHeight - barHeight + 'px';
		}
	},
	created() {
		this.lptConsumer = lpt.createConsumer();
	},
	methods: {
		onSearch() {
			this.$router.push({
				path: '/search_index',
				query: {
					value: this.searchValue,
					mode: this.enableBoolMode ? 'bool' : 'natrl'
				}
			});
		},
		onPostListLoaded() {
			this.postListLoaded = true;
		},
		getElement() {
			return this.$el;
		}
	}
}
</script>

<style scoped>
.main-area {
	overflow-y: scroll;
}
</style>