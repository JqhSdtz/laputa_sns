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
							<category-grid-item :category-id="obj.category.id" style="width: 4rem" size="2.5rem">
								<pushpin-filled v-if="obj.pinned" class="pin-icon" :rotate="-45"/>
							</category-grid-item>
						</div>
					</div>
				</van-cell>
			</div>
			<van-cell is-link title="详情页查看" :to="'/category_detail/' + rootCategoryId"/>
			<van-popup v-model:show="showRecentVisitPopup" round :closeable="true" style="width: 80%; padding: 1.5rem">
				<van-grid :column-num="4" :border="false">
					<van-grid-item v-for="obj in recentVisitList" :key="obj.category.id">
						<category-grid-item :category-id="obj.category.id" size="2.5rem">
							<pushpin-filled v-if="obj.pinned" class="pin-icon popup-pin-icon" :rotate="-45"/>
						</category-grid-item>
					</van-grid-item>
				</van-grid>
			</van-popup>
			<van-grid style="margin-top: 1rem;" :column-num="3" :border="false">
				<van-grid-item v-for="category in categoryList" :key="category.id">
					<category-grid-item :category-id="category.id"/>
				</van-grid-item>
			</van-grid>
		</div>
	</van-pull-refresh>
</template>

<script>
import lpt from '@/lib/js/laputa/laputa';
import {Toast} from 'vant';
import CategoryGridItem from '@/components/category/item/CategoryGridItem';
import global from '@/lib/js/global';
import {
	PushpinFilled
} from '@ant-design/icons-vue';

export default {
	name: 'Community',
	components: {
		CategoryGridItem,
		PushpinFilled
	},
	data() {
		return {
			rootCategoryId: lpt.categoryServ.rootCategoryId,
			isRefreshing: false,
			categoryList: [],
			recentVisitList: [],
			showRecentVisitPopup: false
		}
	},
	inject: {
		lptContainer: {
			type: String
		}
	},
	watch: {
		$route() {
			this.showRecentVisitPopup = false;
		}
	},
	created() {
		this.lptConsumer = lpt.createConsumer();
		global.events.on(['signIn', 'signOut'], () => {
			this.init();
		});
		this.init();
		if (this.lptConsumer !== 'blogDrawer') {
			global.methods.setTitle({
				pageDesc: '社区'
			});
		}
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
			const ref = this;
			lpt.categoryServ.get({
				consumer: this.lptConsumer,
				param: {
					id: this.rootCategoryId
				},
				success(result) {
					const category = global.states.categoryManager.add(result.object);
					ref.categoryList = category.sub_list;
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