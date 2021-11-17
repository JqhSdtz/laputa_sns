<template>
	<van-empty v-if="hasEverLoad && isEmpty" description="没有消息" />
	<van-pull-refresh v-show="hasEverLoad && !isEmpty" v-model="isRefreshing" @refresh="onPullRefresh"
	                  success-text="刷新成功" style="height: 100%">
		<van-list class="notice-list" @load="loadMore" :fill-parent="$el"
		          v-model:loading="isBusy" :finished="finished" finished-text="没有更多了">
			<a-back-top :style="{bottom: '10px'}" :target="getElement"/>
			<notice-item class="notice-item" v-for="notice in list" :key="notice.id" :notice-id="notice.id"/>
		</van-list>
	</van-pull-refresh>
</template>

<script>
import NoticeItem from './item/NoticeItem';
import lpt from '@/lib/js/laputa/laputa';
import {toRef} from 'vue';
import {Toast} from 'vant';
import global from '@/lib/js/global';

export default {
	name: 'NoticeList',
	components: {
		NoticeItem
	},
	data() {
		this.querior = lpt.createQuerior();
		return {
			curNoticeCount: global.states.curOperator.unread_notice_cnt,
			finished: toRef(this.querior, 'hasReachedBottom'),
			hasEverLoad: false,
			list: [],
			isEmpty: false,
			isRefreshing: false,
			isBusy: false
		}
	},
	inject: {
		lptContainer: {
			type: String
		}
	},
	watch: {
		curNoticeCount(newValue, oldValue) {
			if (newValue !== oldValue) {
				this.reset();
				this.loadMore();
			}
		}
	},
	created() {
		const ref = this;
		this.querior.onBusyChange(isBusy => {
			this.$nextTick(() => {
				ref.isBusy = isBusy;
			});
		});
		this.loadMore();
	},
	activated() {
		if (this.lptConsumer !== 'blogDrawer') {
			global.methods.setTitle({
				pageDesc: '通知',
				route: this.$route
			});
		}
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
			if (!this.querior.hasReachedBottom) {
				lpt.noticeServ.query({
					querior: this.querior,
					success: (result) => {
						global.states.noticeManager.addList(result.object);
						if (!this.hasEverLoad) {
							this.list = result.object;
							this.isEmpty = this.list.length === 0 && this.querior.hasReachedBottom;
							this.hasEverLoad = true;
						} else {
							this.list = this.list.concat(result.object);
						}
					},
					fail(result) {
						Toast.fail(result.message);
					},
					complete: () => {
						this.isRefreshing = false;
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
.notice-list {
	height: 100%;
	overflow-y: visible;
	padding-top: 2rem;
}
</style>