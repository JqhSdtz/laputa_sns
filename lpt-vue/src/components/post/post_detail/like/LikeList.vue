<template>
	<van-empty v-if="hasEverLoad && isEmpty" description="没有点赞" />
	<van-pull-refresh v-show="hasEverLoad && !isEmpty" v-model="isRefreshing" @refresh="onPullRefresh" success-text="刷新成功">
		<van-list class="like-list" @load="loadMore" :offset="listOffset"
		          v-model:loading="isBusy" :finished="finished" finished-text="没有更多了">
			<a-back-top :style="{bottom: listOffset + 'px'}" :target="getElement"/>
			<like-item class="like-item" v-for="like in list" :like="like"
			              :key="like.id"/>
		</van-list>
	</van-pull-refresh>
</template>

<script>
import LikeItem from './item/LikeItem';
import {toRef} from 'vue';
import global from '@/lib/js/global';
import lpt from '@/lib/js/laputa/laputa';

const querior = lpt.createQuerior();

export default {
	name: 'LikeList',
	props: {
		postId: String,
		onBusyChange: Function,
		onLoaded: Function,
		onRefresh: Function
	},
	components: {
		LikeItem
	},
	data() {
		return {
			finished: toRef(querior, 'hasReachedBottom'),
			hasEverLoad: false,
			list: [],
			isEmpty: false,
			listOffset: global.vars.style.postDetailBarHeight + 10,
			isRefreshing: false,
			isBusy: false
		}
	},
	created() {
		const ref = this;
		querior.onBusyChange(isBusy => {
			this.$nextTick(() => {
				ref.isBusy = isBusy;
			});
		});
		this.defaultQueryOption = {
			querior,
			param: {
				type: lpt.contentType.post
			},
			data: {
				target_id: this.postId
			}
		};
		this.loadMore();
	},
	unmounted() {
		querior.reset();
	},
	methods: {
		onPullRefresh() {
			this.reset();
			this.loadMore();
			this.$emit('refresh');
		},
		reset() {
			querior.reset();
			this.hasEverLoad = false;
		},
		pushLike(like) {
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
			console.log('load like list!');
			const ref = this;
			if (!querior.hasReachedBottom) {
				lpt.likeServ.query({
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
						alert(result.message);
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
.like-list {
	height: 100%;
	overflow-y: visible;
	background-color: #ECECEC;
}

.like-item {
	padding: 0.5rem;
}
</style>