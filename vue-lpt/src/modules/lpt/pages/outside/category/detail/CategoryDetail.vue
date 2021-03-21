<template>
	<template v-if="forceReloadFlag && showDrawer">
		<div id="category-area" :style="{height: scrollHeight, position: 'relative'}">
			<div ref="categoryInfoArea" id="main-area">
				<div style="height: 200px; width: 100%; background-size: cover;"
				     :style="{backgroundImage: `linear-gradient(to bottom, rgba(255, 255, 255, 0), rgba(255, 255, 255, 1)), url(${coverImgUrl})`}"/>
				<div style="margin-top: -2rem">
					<img :src="iconImgUrl" id="icon-img"/>
					<span style="margin-left: 1rem; display: inline-block">
						<p style="font-size: 1.5rem; font-weight: bold;margin-bottom: 0">{{ category.name }}</p>
						<p>帖数:{{ category.post_cnt }}</p>
					</span>
					<action-bar style="float: right; margin-top: 1.5rem;" :category-id="category.id"/>
				</div>
				<category-path style="margin-left: 1rem;" :path-list="category.path_list"/>
				<ellipsis style="margin-left: 1rem; margin-top: 0.5rem" :content="category.intro" :rows="2"/>
			</div>
			<!-- 放到tab里面会改变viewport，影响position:fixed的效果，使其相对于tab页固定
			        所以添加两个selector，根据tab页是否黏住判断分别显示两个不同的selector -->
			<sort-type-selector class="sort-type-selector" v-if="postListLoaded && isTabFixed"
			                    v-show="curTabKey === 'postList'"
			                    :button-style="{left: clientWidth - 100 + 'px'}" :auto-hide="isTabFixed" offset="4rem"
			                    :hide-offset-base="mainAreaHeight" v-model:sort-type="sortType"/>
			<float-publish-button v-if="hasPublishRight && postListLoaded"
			                      :style="{left: clientWidth - 60 + 'px'}" :auto-hide="isTabFixed"
			                      :category="category" :hide-offset-base="mainAreaHeight"/>
			<a-back-top style="left: 30px; bottom: 2rem;" :target="getElement"/>
			<div ref="middleBar" id="middle-bar" style="width: 100%;height: 100%">
				<van-tabs v-model:active="curTabKey" swipeable sticky lazy-render @scroll="onScroll">
					<van-tab ref="postArea" name="postList" title="帖子">
						<sort-type-selector class="sort-type-selector" v-if="postListLoaded && !isTabFixed"
						                    :button-style="{left: clientWidth - 100 + 'px'}" :auto-hide="false"
						                    offset="0.75rem"
						                    v-model:sort-type="sortType"/>
						<post-list ref="postList" :category-id="category.id" :post-of="'category'"
						           :top-post-id="category.top_post_id" :sort-type="sortType" @refresh="onRefresh"
						           @loaded="onPostListLoaded" :fill-parent="$refs.middleBar"/>
					</van-tab>
					<van-tab name="subCategory" title="分区">
						<div>
							<van-grid :column-num="3" :border="false">
								<van-grid-item v-for="subCategory in category.sub_list" :key="subCategory.id">
									<category-grid-item :category-id="subCategory.id"/>
								</van-grid-item>
							</van-grid>
						</div>
					</van-tab>
				</van-tabs>
			</div>
		</div>
	</template>
</template>

<script>
import {createEventBus} from '@/lib/js/global/global-events';
import global from '@/lib/js/global';
import lpt from '@/lib/js/laputa/laputa';
import {Toast} from 'vant';
import PostList from '@/components/post/post_list/PostList';
import CategoryGridItem from '@/components/category/item/CategoryGridItem';
import SortTypeSelector from '@/components/post/post_list/SortTypeSelector';
import FloatPublishButton from '@/components/post/post_list/FloatPublishButton';
import Ellipsis from '@/components/global/Ellipsis';
import ActionBar from "@/modules/lpt/pages/outside/category/detail/parts/ActionBar";
import CategoryPath from '@/components/category/CategoryPath';
import {toRef} from "vue";

export default {
	name: 'CategoryDetail',
	props: {
		categoryId: String
	},
	components: {
		CategoryPath,
		ActionBar,
		Ellipsis,
		FloatPublishButton,
		CategoryGridItem,
		PostList,
		SortTypeSelector
	},
	provide() {
		return {
			categoryDetailEvents: this.categoryDetailEvents
		}
	},
	data() {
		this.categoryDetailEvents = createEventBus();
		const category = global.states.categoryManager.get({
			itemId: this.categoryId
		});
		return {
			category,
			showDrawer: toRef(global.states.blog, 'showDrawer'),
			mainAreaHeight: 0,
			isTabFixed: false,
			curTabKey: 'postList',
			sortType: 'popular',
			postListLoaded: false
		}
	},
	watch: {
		categoryId(newValue) {
			this.category = global.states.categoryManager.get({
				itemId: newValue
			});
			this.init();
			this.isTabFixed = false;
			this.postListLoaded = false;
			this.curTabKey = 'postList';
			this.forceReload();
		}
	},
	computed: {
		iconImgUrl() {
			return lpt.getCategoryIconUrl(this.category);
		},
		coverImgUrl() {
			return lpt.getCategoryCoverUrl(this.category);
		},
		scrollHeight() {
			const mainViewHeight = document.body.clientHeight;
			return mainViewHeight + 'px';
		},
		clientWidth() {
			if (global.vars.env === 'lpt') {
				return document.body.clientWidth;
			} else {
				return this.$parent.$el.clientWidth;
			}
		},
		hasPublishRight() {
			if (!this.category.allow_user_post) {
				return false;
			} else if (typeof this.category.allow_post_level !== 'undefined') {
				const permissionMap = global.states.curOperator.permission_map;
				return permissionMap && permissionMap[this.category.id] >= this.category.allow_post_level;
			} else {
				return true;
			}
		}
	},
	created() {
		this.lptConsumer = lpt.createConsumer();
		this.init();
	},
	methods: {
		onRefresh() {
			this.init();
		},
		onScroll({scrollTop, isFixed}) {
			if (this.$refs.categoryInfoArea) {
				this.mainAreaHeight = this.$refs.categoryInfoArea.clientHeight;
			}
			this.isTabFixed = isFixed;
		},
		afterCategoryLoad() {
			this.categoryLoaded = true;
		},
		init() {
			const ref = this;
			lpt.categoryServ.get({
				consumer: this.lptConsumer,
				param: {
					id: this.categoryId
				},
				success(result) {
					global.states.categoryManager.add(result.object);
					ref.$nextTick(() => {
						ref.afterCategoryLoad();
					});
				},
				fail(result) {
					Toast.fail(result.message);
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
:global(.van-tab__pane) {
	height: 100%;
}

#category-area {
	overflow-y: scroll;
}

#category-area::-webkit-scrollbar {
	display: none;
}

.sort-type-selector {
	right: auto
}

#main-area {
	position: relative;
}

#icon-img {
	border-radius: 0.5rem;
	width: 4rem;
	height: 4rem;
	margin-left: 1rem;
	margin-top: -2.5rem;
}
</style>