<template>
	<div style="height: 100%;">
		<van-empty v-if="hasEverLoad && isEmpty" description="没有转发" />
		<van-pull-refresh style="height: 100%" v-show="hasEverLoad && !isEmpty" v-model="isRefreshing" @refresh="onPullRefresh" success-text="刷新成功">
			<van-list class="forward-list" @load="loadMore" :offset="listOffset" :fill-parent="fillParent || $el"
			          v-model:loading="isBusy" :finished="finished" finished-text="没有更多了">
				<a-back-top :style="{bottom: listOffset + 'px'}" :target="getElement"/>
				<forward-item class="forward-item" v-for="forward in list" :forward="forward"
				              :key="forward.id"/>
			</van-list>
		</van-pull-refresh>
	</div>
</template>

<script>
import ForwardItem from './item/ForwardItem';
import {Toast} from "vant";
import {toRef} from 'vue';
import global from '@/lib/js/global';
import lpt from '@/lib/js/laputa/laputa';

export default {
	name: 'ForwardList',
	props: {
		fillParent: Object,
		postId: String,
		onBusyChange: Function,
		onLoaded: Function,
		onRefresh: Function
	},
	components: {
		ForwardItem
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
	computed: {
		listOffset() {
			return global.states.style.postDetailBarHeight + 10;
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
				sup_id: this.postId
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
			this.$emit('refresh');
		},
		reset() {
			this.querior.reset();
			this.hasEverLoad = false;
		},
		pushForward(forward) {
			if (this.isEmpty) this.isEmpty = false;
			this.list.unshift(forward);
		},
		loadMore() {
			if (!this.querior.hasReachedBottom) {
				lpt.forwardServ.query({
					...this.defaultQueryOption,
					success: (result) => {
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
.forward-list {
	height: 100%;
	overflow-y: visible;
	background-color: #ECECEC;
}

.forward-item {
	padding: 0.5rem;
}
</style>