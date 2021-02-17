<template>
	<div id="main-area" :style="{height: scrollHeight, position: 'relative'}">
		<van-empty v-if="hasEverLoad && isEmpty" description="没有关注"/>
		<van-pull-refresh v-show="hasEverLoad && !isEmpty" v-model="isRefreshing" @refresh="onPullRefresh"
		                  success-text="刷新成功" style="height: 100%">
			<van-list class="user-list" @load="loadMore" :offset="10"
			          v-model:loading="isBusy" :finished="hasEverLoad" finished-text="没有更多了">
				<a-back-top :style="{bottom: '10px'}" :target="getElement"/>
				<user-item class="user-item" v-for="obj in list" :key="obj.id" :user="obj.target"/>
			</van-list>
		</van-pull-refresh>
	</div>
</template>

<script>
import lpt from '@/lib/js/laputa/laputa';
import UserItem from './item/UserItem';
import {toRef} from 'vue';
import {Toast} from 'vant';

export default {
	name: 'FollowingList',
	props: {
		userId: String
	},
	components: {
		UserItem
	},
	data() {
		return {
			hasEverLoad: false,
			list: [],
			isEmpty: false,
			isRefreshing: false,
			isBusy: false
		}
	},
	computed: {
		scrollHeight() {
			const mainViewHeight = document.body.clientHeight;
			return mainViewHeight + 'px';
		}
	},
	created() {
		this.lptConsumer = lpt.createConsumer();
		this.loadMore();
	},
	methods: {
		onPullRefresh() {
			this.reset();
			this.loadMore();
		},
		reset() {
			this.hasEverLoad = false;
		},
		loadMore() {
			const ref = this;
			lpt.followServ.getFollowing({
				consumer: this.lptConsumer,
				param: {
					userId: parseInt(this.userId)
				},
				success(result) {
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
					ref.isRefreshing = false;
				}
			});
		},
		getElement() {
			return this.$el;
		}
	}
}
</script>

<style scoped>
.user-list {
	height: 100%;
	overflow-y: visible;
}

#main-area {
	height: 100%;
	overflow-y: scroll;
}

#main-area::-webkit-scrollbar {
	display: none;
}
</style>