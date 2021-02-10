<template>
	<div>
		<van-empty v-if="hasEverLoad && isEmpty" description="没有评论" />
		<van-pull-refresh v-show="hasEverLoad && !isEmpty" v-model="isRefreshing" @refresh="onPullRefresh" success-text="刷新成功">
			<van-list class="comment-list" @load="loadMore" :offset="listOffset"
			          v-model:loading="isBusy" :finished="finished" finished-text="没有更多了">
				<div style="width: 100%; background-color: white; display: inline-block; margin-bottom: -0.35rem">
					<button class="ant-btn fake-btn" style="float: left; margin-left: 1rem">
						<span>{{sortType === 'popular' ? '热门评论' : '最新评论'}}</span>
					</button>
					<a-button type="link" style="float: right; margin-right: 1rem" @click="changeSortType">
						<OrderedListOutlined/>
						<span>{{sortType === 'popular' ? '按热度' : '按时间'}}</span>
					</a-button>
				</div>
				<a-back-top :style="{bottom: listOffset + 'px'}" :target="getElement"/>
				<comment-item class="comment-item" v-for="comment in list" :comment-id="comment.id"
				              :key="comment.id"/>
			</van-list>
		</van-pull-refresh>
	</div>
</template>

<script>
import CommentItem from './item/CommentItem';
import {OrderedListOutlined} from '@ant-design/icons-vue';
import lpt from '@/lib/js/laputa/laputa';
import global from '@/lib/js/global';
import {toRef} from "vue";

const querior = lpt.createQuerior();

export default {
	name: 'CommentList',
	props: {
		postId: String,
		onBusyChange: Function,
		onLoaded: Function,
		onRefresh: Function
	},
	components: {
		CommentItem,
		OrderedListOutlined
	},
	data() {
		return {
			sortType: 'popular',
			finished: toRef(querior, 'hasReachedBottom'),
			isEmpty: false,
			hasEverLoad: false,
			list: [],
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
				type: lpt.commentServ.level1,
				queryType: this.sortType,
			},
			data: {
				post_id: this.postId
			}
		};
		this.loadMore();
		global.events.on('login', () => {
			ref.reset();
			ref.loadMore();
		});
	},
	unmounted() {
		querior.reset();
	},
	methods: {
		changeSortType() {
			if (this.sortType === 'popular') {
				this.sortType = 'latest';
			} else {
				this.sortType = 'popular';
			}
			this.reset();
			this.defaultQueryOption.param.queryType = this.sortType;
			this.loadMore();
		},
		onPullRefresh() {
			this.reset();
			this.loadMore();
			this.$emit('refresh');
		},
		reset() {
			querior.reset();
			this.hasEverLoad = false;
		},
		pushComment(comment) {
			this.list.unshift(comment);
		},
		loadMore() {
			console.log('load comment list!');
			const ref = this;
			if (!querior.hasReachedBottom) {
				lpt.commentServ.query({
					...this.defaultQueryOption,
					success(result) {
						global.states.commentL1Manager.addList(result.object);
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
.comment-list {
	height: 100%;
	overflow-y: visible;
	background-color: #ECECEC;
}

.comment-item {
	padding: 0.5rem;
}

.fake-btn {
	border: none;
	pointer-events: none;
}
</style>