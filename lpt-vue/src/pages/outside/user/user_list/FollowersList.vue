<template>
	<van-empty v-if="hasEverLoad && isEmpty" description="没有粉丝" />
	<van-pull-refresh v-show="hasEverLoad && !isEmpty" v-model="isRefreshing" @refresh="onPullRefresh"
	                  success-text="刷新成功" style="height: 100%">
		<van-list class="user-list" @load="loadMore"
		          v-model:loading="isBusy" :finished="finished" finished-text="没有更多了">
			<a-back-top :style="{bottom: '10px'}" :target="getElement"/>
			<user-item class="user-item" v-for="obj in list" :key="obj.id" :user="obj.follower"/>
		</van-list>
	</van-pull-refresh>
</template>

<script>
import lpt from '@/lib/js/laputa/laputa';
import UserItem from './item/UserItem';
import {toRef} from 'vue';
import {Toast} from 'vant';

export default {
	name: 'FollowersList',
	props: {
		userId: String
	},
	components: {
		UserItem
	},
	data() {
		this.querior = lpt.createQuerior();
		return {
			finished: toRef(this.querior, 'hasReachedBottom'),
			hasEverLoad: false,
			list: [],
			isEmpty: false,
			isRefreshing: false,
			isBusy: false
		}
	},
	created() {
		const ref = this;
		this.querior.onBusyChange(isBusy => {
			this.$nextTick(() => {
				ref.isBusy = isBusy;
			});
		});
		this.defaultQueryOption = {
			querior: this.querior,
			data: {
				target_id: parseInt(this.userId)
			}
		};
		this.loadMore();
	},
	unmounted() {
		this.querior.reset();
	},
	methods: {
		onPullRefresh() {
			this.reset();
			this.loadMore();
		},
		reset() {
			this.querior.reset();
			this.hasEverLoad = false;
		},
		loadMore() {
			const ref = this;
			if (!this.querior.hasReachedBottom) {
				lpt.followServ.queryFollower({
					...this.defaultQueryOption,
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
			}
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
</style>