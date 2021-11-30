<template>
	<template v-if="forceReloadFlag">
		<div v-show="showDrawer">
			<div class="category-area"
			    ref="categoryArea"
			    style="background-color: white"
			    :class="{'with-scroll-bar': lptContainer === 'blogMain'}"
			    :style="{height: scrollHeight, position: 'relative'}" 
				v-scroll-view>
				<div ref="categoryInfoArea" class="main-area">
					<div style="width: 100%; background-size: cover;"
					     :style="{backgroundImage: `linear-gradient(to bottom, rgba(255, 255, 255, 0), rgba(255, 255, 255, 1)), url(${coverImgUrl})`,
					     height: coverHeight + 'px'}"/>
					<div style="margin-top: -2rem">
						<img :src="iconImgUrl" id="icon-img"/>
						<span style="margin-left: 1rem; display: inline-block">
						<p style="font-size: 1.5rem; font-weight: bold;margin-bottom: 0"
						   :class="{'link-name': lptContainer === 'blogDrawer'}"
						   :title="lptContainer === 'blogDrawer' ? '点击访问该目录' : ''"
						   @click="onCategoryNameClick">
							{{ category.name }}
						</p>
						<p>帖数:{{ category.post_cnt }}</p>
					</span>
						<!-- action-bar使用float布局会影响tabs组件的正常加载。。。未找到原因 -->
						<action-bar style="position: absolute; right: 0; margin-top: 2.5rem;" :category-id="category.id"/>
					</div>
					<category-path style="margin-left: 1rem;" :path-list="category.path_list"/>
					<ellipsis style="margin: 0.5rem 1rem" :content="categoryIntro" :rows="2"/>
				</div>
				<!-- 放到tab里面会改变viewport，影响position:fixed的效果，使其相对于tab页固定
						所以添加两个selector，根据tab页是否黏住判断分别显示两个不同的selector -->
				<sort-type-selector class="sort-type-selector" v-if="postListLoaded && isTabFixed"
				                    v-show="curTabKey === 'postList'"
				                    :button-style="{right: 'auto', left: sortTypeLeft + 'px'}" :auto-hide="isTabFixed" 
									:offset="categoryDetailType === 'gallery' ? '5.75rem' : '4rem'"
				                    :hide-offset-base="mainAreaHeight" v-model:sort-type="sortType"/>
				<float-publish-button v-if="hasPublishRight && postListLoaded"
				                      :style="{left: floatPublishLeft + 'px'}" :auto-hide="isTabFixed"
				                      :category="category" :hide-offset-base="mainAreaHeight"/>
				<a-back-top style="bottom: 2rem;" :style="{left: backTopLeft + 'px'}" :target="getElement"/>
				<div ref="middleBar" id="category-middle-bar" :style="{width: clientWidth + 'px'}">
					<van-tabs ref="tabs" v-model:active="curTabKey" :width="clientWidth"
					          swipeable sticky lazy-render @scroll="onScroll">
						<van-tab ref="postListTab" name="postList" title="帖子" :width="clientWidth">
							<sort-type-selector class="sort-type-selector" v-if="postListLoaded && !isTabFixed"
							                    :button-style="{right: 'auto', left: insideSortTypeLeft  + 'px'}" :auto-hide="false"
							                    :offset="categoryDetailType === 'gallery' ? '3rem' : '0.75rem'"
							                    v-model:sort-type="sortType"/>
							<post-list v-if="categoryDetailType === 'plain'" ref="postList" :category-id="category.id"
							           :post-of="'category'" :top-post-id="category.top_post_id" :sort-type="sortType"
							           @refresh="init" @loaded="onPostListLoaded" :fill-parent="$refs.middleBar"/>
							<gallery-list v-if="categoryDetailType === 'gallery'" ref="postList" :category-id="category.id"
							        :post-of="'category'" 
									:top-post-id="category.top_post_id" 
									:sort-type="sortType"
							        @refresh="init"
									@loaded="onPostListLoaded" 
									:fill-parent="$refs.middleBar"/>
						</van-tab>
						<van-tab ref="subCategoryTab" name="subCategory" title="分区">
							<div>
								<van-empty v-if="!category.sub_list || category.sub_list.length === 0" description="没有分区" />
								<van-grid v-else :column-num="3" :border="false">
									<van-grid-item v-for="subCategory in category.sub_list" :key="subCategory.id">
										<category-grid-item :category-id="subCategory.id"/>
									</van-grid-item>
								</van-grid>
							</div>
						</van-tab>
					</van-tabs>
				</div>
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
import GalleryList from '@/components/post/post_list/GalleryList';
import CategoryGridItem from '@/components/category/item/CategoryGridItem';
import SortTypeSelector from '@/components/post/post_list/SortTypeSelector';
import FloatPublishButton from '@/components/post/post_list/FloatPublishButton';
import Ellipsis from '@/components/global/Ellipsis';
import ActionBar from "@/modules/lpt/pages/outside/category/detail/parts/ActionBar";
import CategoryPath from '@/components/category/CategoryPath';
import {toRef} from "vue";

const typeReg = /tp:([a-zA-Z]*)#/;

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
		GalleryList,
		SortTypeSelector
	},
	inject: {
		lptContainer: {
			type: String
		}
	},
	provide() {
		return {
			categoryDetailEvents: this.categoryDetailEvents
		}
	},
	data() {
		this.categoryDetailEvents = createEventBus();
		const sortType = localStorage.getItem('sortTypeCategoryDetail' + this.categoryId) || 'popular';
		return {
			category: global.states.categoryManager.get({
				itemId: this.categoryId
			}),
			showDrawer: this.lptContainer === 'blogDrawer' ? toRef(global.states.blog, 'showDrawer') : true,
			mainAreaHeight: 0,
			coverHeight: this.lptContainer === 'blogMain' ? 400 : 200,
			isTabFixed: false,
			curTabKey: 'postList',
			sortType: sortType,
			categoryDetailType: '',
			categoryIntro: '',
			postListLoaded: false
		}
	},
	watch: {
		sortType(value) {
			localStorage.setItem('sortTypeCategoryDetail' + this.categoryId, value);
		},
		categoryId() {
			this.category = this.init();
			this.isTabFixed = false;
			this.postListLoaded = false;
			// this.curTabKey = 'postList';
			this.forceReload({
				afterReload: () => {
					this.bindScrollTop({
						id: 'categoryDetail-' + this.categoryId,
						el: this.$refs.categoryArea
					});
				}
			});
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
			return global.states.style.mainHeight + 'px';
		},
		backTopLeft() {
			let left = 30;
			if (this.lptContainer === 'blogMain')
				left += global.states.style.blogMainLeft;
			return left;
		},
		floatPublishLeft() {
			let left = this.clientWidth - 60;
			if (this.lptContainer === 'blogMain')
				left += global.states.style.blogMainLeft;
			return left;
		},
		sortTypeLeft() {
			if (this.lptContainer === 'blogMain') {
				return this.insideSortTypeLeft + global.states.style.blogMainLeft;
			} else {
				return this.insideSortTypeLeft;
			}
		},
		insideSortTypeLeft() {
			return this.clientWidth - (this.lptContainer === 'blogMain' ? 130 : 100);
		},
		clientWidth() {
			const curTab = this.$refs[this.curTabKey + 'Tab'];
			if (curTab) {
				this.$nextTick(() => curTab.$parent.resize());
				this.$refs.tabs.resize();
			}
			if (this.lptContainer === 'blogDrawer') {
				return global.states.style.drawerWidth;
			} else if (this.lptContainer === 'blogMain') {
				return global.states.style.blogMainWidth;
			} else {
				return global.states.style.bodyWidth;
			}
		},
		hasPublishRight() {
			if (!this.category.is_leaf || !this.category.allow_user_post) {
				return false;
			} else if (typeof this.category.allow_post_level !== 'undefined') {
				const this_level = this.category.rights && this.category.rights.this_level;
				return this_level && this_level >= this.category.allow_post_level;
			} else {
				return true;
			}
		}
	},
	created() {
		this.category = this.init();
	},
	mounted() {
		this.bindScrollTop({
			id: 'categoryDetail-' + this.categoryId,
			el: this.$refs.categoryArea
		});
	},
	methods: {
		setTitle(category) {
			if (this.lptContainer === 'blogDrawer') return;
			let pageDesc;
			if (this.categoryDetailType === 'plain') {
				pageDesc = '目录';
			} else if (this.categoryDetailType === 'gallery') {
				pageDesc = '相册';
			}
			global.methods.setTitle({
				contentDesc: category.name,
				pageDesc: pageDesc,
				route: this.$route
			});
		},
		resolveIntroChange(category) {
			if (!category.intro || !typeReg.test(category.intro)) {
				this.categoryDetailType = 'plain';
			} else {
				this.categoryDetailType = category.intro.match(typeReg)[1];
				this.categoryIntro = category.intro.replace(typeReg, '');
			}
		},
		onScroll({scrollTop, isFixed}) {
			if (this.$refs.categoryInfoArea) {
				this.mainAreaHeight = this.$refs.categoryInfoArea.clientHeight;
			}
			this.isTabFixed = isFixed;
		},
		init() {
			return global.states.categoryManager.get({
				itemId: this.categoryId,
				filter: (res) => res.isFull,
				success: (category) => {
					this.resolveIntroChange(category);
					this.setTitle(category);
					global.events.emit('visitCategory', category);
					if (!category.sub_list || category.sub_list.length === 0) {
						// 没有子目录则自动跳转到帖子列表
						this.curTabKey = 'postList';
					}
				},
				fail(result) {
					Toast.fail(result.message);
				}
			});
		},
		onPostListLoaded() {
			this.postListLoaded = true;
		},
		onCategoryNameClick() {
			if (this.lptContainer === 'blogDrawer') {
				this.$router.push({
					path: '/blog/category_detail/' + this.category.id
				});
				global.states.blog.showDrawer = false;
			}
		},
		getElement() {
			return this.$refs.categoryArea;
		}
	}
}
</script>

<style scoped>
:global(.van-tab__pane) {
	height: 100%;
}

.link-name {
	cursor: pointer;
}

.link-name:hover {
	text-decoration: underline;
}

.category-area {
	overflow-y: scroll;
	overflow-x: hidden;
}

#category-area::-webkit-scrollbar {
	display: none;
}

.sort-type-selector {
	right: auto
}

.main-area {
	position: relative;
}

#icon-img {
	border-radius: 0.5rem;
	width: 4rem;
	height: 4rem;
	margin-left: 1rem;
	margin-top: -2.5rem;
}

#category-middle-bar {
	height: 100%;
	background-color: white;
}
</style>