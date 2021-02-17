<template>
	<div style="height: 100%;">
		<van-empty v-if="hasEverLoad && isEmpty" description="没有评论" />
		<van-pull-refresh style="height: 100%" v-show="hasEverLoad && !isEmpty" v-model="isRefreshing" @refresh="onPullRefresh" success-text="刷新成功">
			<van-list class="comment-list" @load="loadMore" :offset="listOffset" :fill-parent="fillParent || $el"
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
				              :key="comment.id" :show-sub-area="true" :show-actions="true"/>
			</van-list>
		</van-pull-refresh>
	</div>
</template>

<script>
import CommentItem from './item/CommentItem';
import {OrderedListOutlined} from '@ant-design/icons-vue';
import lpt from '@/lib/js/laputa/laputa';
import global from '@/lib/js/global';
import {Toast} from "vant";
import {toRef} from "vue";
import {createEventBus} from "@/lib/js/global/global-events";

export default {
	name: 'CommentList',
	props: {
		fillParent: Object,
		postId: String,
		onBusyChange: Function,
		onLoaded: Function,
		onRefresh: Function
	},
	components: {
		CommentItem,
		OrderedListOutlined
	},
	provide() {
		return {
			commentListEvents: this.commentListEvents
		}
	},
	data() {
		this.querior = lpt.createQuerior();
		this.commentListEvents = createEventBus();
		return {
			sortType: 'popular',
			finished: toRef(this.querior, 'hasReachedBottom'),
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
		this.lptConsumer = lpt.createConsumer();
		this.querior.onBusyChange(isBusy => {
			this.$nextTick(() => {
				ref.isBusy = isBusy;
			});
		});
		this.defaultQueryOption = {
			querior: this.querior,
			param: {
				type: lpt.commentServ.level1,
				queryType: this.sortType,
			},
			data: {
				post_id: this.postId
			}
		};
		this.loadMore(false);
		this.commentListEvents.on(['top', 'unTop'], (param, name) => {
			const isCancel = name === 'unTop';
			const comment = param.comment;
			lpt.postServ.setTopComment({
				consumer: this.lptConsumer,
				param: {
					isCancel: isCancel
				},
				data: {
					id: this.postId,
					top_comment_id: comment.id,
					op_comment: param.op_comment
				},
				success() {
					Toast.success(isCancel ? '取消成功' : '置顶成功');
					comment.is_topped = !isCancel;
					param.callback && param.callback();
				},
				fail(result) {
					Toast.fail(result.message);
				}
			});
		});
		this.commentListEvents.on('delete', (param) => {
			const comment = param.comment;
			const ref = this;
			lpt.contentServ.delete({
				consumer: this.lptConsumer,
				param: {
					type: lpt.contentType.commentL1
				},
				data: {
					id: comment.id,
					op_comment: param.op_comment
				},
				success() {
					Toast.success('删除成功');
					ref.list.splice(ref.list.indexOf(comment), 1);
				},
				fail(result) {
					Toast.fail(result.message);
				}
			});
		});
	},
	unmounted() {
		this.querior.reset();
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
			this.loadMore(true);
		},
		onPullRefresh() {
			this.reset();
			this.loadMore(true);
			this.$emit('refresh');
		},
		reset() {
			this.querior.reset();
			this.hasEverLoad = false;
		},
		pushComment(comment) {
			this.list.unshift(comment);
		},
		loadMore(isRefresh) {
			const ref = this;
			if (!this.querior.hasReachedBottom) {
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
						if (isRefresh) {
							ref.commentListEvents.emit('refreshList');
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