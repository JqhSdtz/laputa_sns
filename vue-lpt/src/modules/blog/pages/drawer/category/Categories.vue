<template>
	<van-pull-refresh ref="pullArea" v-model="isRefreshing" @refresh="onRefresh"
	                  style="height: 100%" success-text="刷新成功">
		<div class="main-area">
			<div v-if="recentVisitList.length > 0">
				<van-cell title="最近访问" is-link @click="showRecentVisitPopup = true"/>
				<van-cell style="overflow-x: scroll;"
				          @click="showRecentVisitPopup = true">
					<div style="white-space: nowrap;">
						<div v-for="obj in recentVisitList" :key="obj.category.id" style="display: inline-block; position: relative; width: 4.5rem; vertical-align:top;">
							<category-grid-item :category-id="obj.category.id" style="width: 4rem" size="2.5rem"
							                    @click.capture.stop="showCategory(obj.category)">
								<pushpin-filled v-if="obj.pinned" class="pin-icon" :rotate="-45"/>
							</category-grid-item>
						</div>
					</div>
				</van-cell>
			</div>
			<van-popup v-model:show="showRecentVisitPopup" round :closeable="true" style="width: 80%; padding: 1.5rem">
				<van-grid :column-num="4" :border="false">
					<van-grid-item v-for="obj in recentVisitList" :key="obj.category.id">
						<category-grid-item :category-id="obj.category.id" size="2.5rem" @click.capture.stop="showCategory(obj.category)">
							<pushpin-filled v-if="obj.pinned" class="pin-icon popup-pin-icon" :rotate="-45"/>
						</category-grid-item>
					</van-grid-item>
				</van-grid>
			</van-popup>
			<category-path style="margin-left: 1rem;font-size: 1rem" :path-list="baseCategory.path_list"/>
			<van-grid style="margin-top: 1rem;" :column-num="3" :border="false">
				<van-grid-item v-for="category in categoryList" :key="category.id">
					<category-grid-item :category-id="category.id"
					                    style="font-size: 0.85rem"
					                    size="4.5rem"
					                    :is-link-name="true"
					                    link-title="点击访问该目录"
					                    :click-img="() => showSubCategory(category)"
										:click-name="() => showCategory(category)"/>
				</van-grid-item>
			</van-grid>
		</div>
	</van-pull-refresh>
</template>

<script>
import lpt from '@/lib/js/laputa/laputa';
import {Toast} from 'vant';
import CategoryGridItem from '@/components/category/item/CategoryGridItem';
import CategoryPath from '@/components/category/CategoryPath';
import global from '@/lib/js/global';
import {
	PushpinFilled
} from '@ant-design/icons-vue';

export default {
	name: 'Categories',
	components: {
		CategoryGridItem,
		CategoryPath,
		PushpinFilled
	},
	inject: {
		lptContainer: {
			type: String
		}
	},
	data() {
		const baseCategoryId = lpt.categoryServ.rootCategoryId;
		return {
			baseCategoryId,
			baseCategory: lpt.categoryServ.getDefaultCategory(baseCategoryId),
			isRefreshing: false,
			categoryList: [],
			recentVisitList: [],
			showRecentVisitPopup: false
		}
	},
	watch: {
		$route() {
			this.showRecentVisitPopup = false;
		}
	},
	computed: {
		clientWidth() {
			// 巨坑，深扒van-tabs组件发现是在swipe组件中获取了一个tabs的宽度
			// 但是如果tabs组件不占满屏幕，又没有固定的px值，则返回0
			// tabs组件错乱
			if (this.lptContainer === 'blogDrawer') {
				return global.states.style.drawerWidth;
			} else {
				return global.states.style.bodyWidth;
			}
		}
	},
	created() {
		const ref = this;
		this.lptConsumer = lpt.createConsumer();
		global.events.on(['signIn', 'signOut'], () => {
			ref.init();
		});
		this.init();
	},
	activated() {
		if (!this.hasActived) {
			this.hasActived = true;
		} else {
			this.initRecentVisitList();
		}
	},
	methods: {
		onRefresh() {
			this.init();
			this.lptConsumer.onBusyChange((isBusy) => {
				this.isRefreshing = isBusy;
			});
		},
		init() {
			this.initCategoryList();
			this.initRecentVisitList();
		},
		initCategoryList() {
			global.states.categoryManager.get({
				itemId: this.baseCategoryId,
				filter: res => res.sub_list,
				success: (result) => {
					this.baseCategory = result;
					this.categoryList = this.baseCategory.sub_list;
				},
				fail(result) {
					Toast.fail(result.message);
				}
			});
		},
		initRecentVisitList() {
			const ref = this;
			lpt.userServ.getRecentVisit({
				success(result) {
					ref.recentVisitList = result.object;
					result.object.forEach(obj => {
						const pinned = obj.score > 9000000000000;
						obj.pinned = pinned;
						obj.category.pinned = pinned;
						obj.category = global.states.categoryManager.add(obj.category);
					});
				},
				fail(result) {
					Toast.fail(result.message);
				}
			});
		},
		showSubCategory(category) {
			if (!category.is_leaf) {
				this.baseCategoryId = category.id;
				this.initCategoryList();
			} else {
				this.showCategory(category);
			}
		},
		showCategory(category) {
			this.$router.push({
				path: '/blog/index/' + category.id
			});
			global.states.blog.showDrawer = false;
		}
	}
}
</script>

<style scoped>
.main-area {
	height: 100%;
	overflow-y: scroll;
}

.main-area::-webkit-scrollbar {
	display: none;
}

:global(.van-cell__value) {
	overflow: visible;
}

.pin-icon {
	position: absolute;
	right: 0;
	top: 0;
}

.popup-pin-icon {
	right: -1rem;
	top: -0.25rem;
}
</style>