<template>
	<div style="height: 100%">
		<van-empty v-if="hasEverLoad && isEmpty" description="没有点赞"/>
		<van-pull-refresh style="height: 100%" v-show="hasEverLoad && !isEmpty" v-model="isRefreshing"
		                  @refresh="onPullRefresh" success-text="刷新成功">
			<van-list class="like-list" @load="loadMore" :offset="listOffset" :fill-parent="fillParent || $el"
			          v-model:loading="isBusy" :finished="finished" finished-text="没有更多了">
				<a-back-top :style="{bottom: listOffset + 'px'}" :target="getElement"/>
				<like-item class="like-item" v-for="like in list" :like="like"
				           :key="like.id"/>
			</van-list>
		</van-pull-refresh>
	</div>
</template>

<script>
import LikeItem from './item/LikeItem';
import {Toast} from "vant";
import {toRef} from 'vue';
import global from '@/lib/js/global';
import lpt from '@/lib/js/laputa/laputa';

export default {
	name: 'LikeList',
	props: {
		type: {
			type: String,
			default: lpt.contentType.post
		},
		fillParent: Object,
		targetId: Number,
		onBusyChange: Function,
		onLoaded: Function,
		onRefresh: Function
	},
	components: {
		LikeItem
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
			const listOffset = this.type === lpt.contentType.post
				? global.states.style.postDetailBarHeight + 10 : 10;
			return listOffset;
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
			param: {
				type: this.type
			},
			data: {
				target_id: this.targetId
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
		pushLike(like) {
			if (this.isEmpty) this.isEmpty = false;
			this.list.unshift(like);
		},
		shiftLike() {
			const curUserId = global.states.curOperator.user.id;
			const curList = this.list;
			for (let i = 0; i < curList.length; ++i) {
				const like = curList[i];
				if (like.creator.id === curUserId) {
					curList.splice(i, 1);
					break;
				}
			}
		},
		loadMore() {
			if (!this.querior.hasReachedBottom) {
				lpt.likeServ.query({
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
.like-list {
	height: 100%;
	overflow-y: visible;
	background-color: #ECECEC;
}

.like-item {
	padding: 0.5rem;
}
</style>