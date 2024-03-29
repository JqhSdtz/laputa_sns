<template>
	<template v-if="forceReloadFlag">
		<div class="main-area" :class="{'with-scroll-bar': lptContainer === 'blogMain'}"
		     :style="{height: scrollHeight, position: 'relative'}" keep-scroll-top v-scroll-view>
			<van-search v-model="searchValue" @search="onSearch" placeholder="请输入搜索关键词">
				<template v-slot:right-icon>
					<van-checkbox v-model="enableBoolMode" style="font-size: 0.9rem">多关键字</van-checkbox>
				</template>
			</van-search>
			<sort-type-selector v-if="postListLoaded" v-model:sort-type="sortType"
				:button-style="{right: sortTypeRight + 'px'}"/>
			<a-back-top :style="{bottom: (mainBarHeight + 10) + 'px', left: backTopLeft + 'px'}" 
				:target="getElement"/>
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
import remHelper from '@/lib/js/uitls/rem-helper';
import {toRef} from 'vue';

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
			success: (result) => {
				this.setTitle(result);
			},
			fail(result) {
				Toast.fail(result.message);
			}
		});
		const sortType = localStorage.getItem('sortTypeIndex' + this.categoryId) || 'popular';
		return {
			category,
			enableBoolMode: false,
			searchValue: '',
			mainBarHeight: toRef(global.states.style, 'tabBarHeight'),
			sortType: sortType,
			postListLoaded: false
		}
	},
	watch: {
		categoryId() {
			this.category = global.states.categoryManager.get({
				itemId: parseInt(this.categoryId),
				success: (result) => {
					this.setTitle(result);
				},
				fail(result) {
					Toast.fail(result.message);
				}
			});
			this.forceReload();
		},
		sortType(value) {
			localStorage.setItem('sortTypeIndex' + this.categoryId, value);
		}
	},
	computed: {
		scrollHeight() {
			if (this.lptContainer === 'blogMain') {
				return global.states.style.mainHeight + 'px';
			}
			const mainViewHeight = global.states.style.bodyHeight;
			// 底部高度加0.5的padding
			let barHeight = this.mainBarHeight;
			return mainViewHeight - barHeight + 'px';
		},
		sortTypeRight() {
			let right = remHelper.remToPx(1);
			if (this.lptContainer === 'blogMain')
				right += global.states.style.blogMainLeft;
			return right;
		},
		backTopLeft() {
			let left = 30;
			if (this.lptContainer === 'blogMain')
				left += global.states.style.blogMainLeft;
			return left;
		}
	},
	activated() {
		this.setTitle(this.category);
	},
	methods: {
		setTitle(category) {
			if (this.lptContainer === 'blogDrawer') return;
			if (category.id !== lpt.categoryServ.rootCategoryId) {
				global.methods.setTitle({
					contentDesc: category.name,
					pageDesc: '主页',
					route: this.$route
				});
			} else {
				global.methods.setTitle({
					pageDesc: '首页',
					route: this.$route
				});
			}
		},
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

:global(.van-field__control) {
	font-size: 0.9rem;
}
</style>