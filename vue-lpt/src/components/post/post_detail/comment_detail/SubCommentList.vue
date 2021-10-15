<template>
	<div>
		<van-empty v-if="hasEverLoad && isEmpty" description="没有评论" />
		<van-pull-refresh v-show="hasEverLoad && !isEmpty" v-model="isRefreshing" @refresh="onPullRefresh" success-text="刷新成功">
			<van-list class="comment-list" @load="loadMore" :offset="listOffset" :fill-parent="$el"
			          v-model:loading="isBusy" :finished="finished" finished-text="没有更多了">
				<a-back-top :style="{bottom: listOffset + 'px'}" :target="getElement"/>
				<comment-item class="comment-item" v-for="comment in list" :comment-id="comment.id"
				              :type="commentType" :key="comment.id" :show-actions="true"/>
			</van-list>
		</van-pull-refresh>
	</div>
</template>

<script>
import CommentItem from '../comment/item/CommentItem';
import lpt from '@/lib/js/laputa/laputa';
import global from '@/lib/js/global';
import {Toast} from "vant";
import {toRef} from "vue";
import {createEventBus} from "@/lib/js/global/global-events";

export default {
	name: 'SubCommentList',
	props: {
		parentId: Number,
		onBusyChange: Function,
		onLoaded: Function,
		onRefresh: Function
	},
	components: {
		CommentItem
	},
	provide() {
		return {
			commentListEvents: this.commentListEvents
		}
	},
	inject: {
		postDetailEvents: {
			type: Object
		}
	},
	data() {
		this.querior = lpt.createQuerior();
		this.commentListEvents = createEventBus();
		return {
			commentType: lpt.commentServ.level2,
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
				type: lpt.commentServ.level2,
				queryType: 'popular'
			},
			data: {
				parent_id: this.parentId
			}
		};
		this.loadMore(false);
		this.commentListEvents.on('delete', (param) => {
			const comment = param.comment;
			const ref = this;
			lpt.contentServ.delete({
				consumer: this.lptConsumer,
				param: {
					type: lpt.contentType.commentL2
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
		this.postDetailEvents.on('publishCommentL2', (l2Comment) => {
			if (l2Comment.l1_id !== this.parentId) return;
			this.pushComment(l2Comment);
		});
	},
	unmounted() {
		this.querior.reset();
	},
	methods: {
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
			if (this.isEmpty) this.isEmpty = false;
			this.list.unshift(comment);
		},
		loadMore(isRefresh) {
			if (!this.querior.hasReachedBottom) {
				lpt.commentServ.query({
					...this.defaultQueryOption,
					success: (result) => {
						global.states.commentL2Manager.addList(result.object);
						if (!this.hasEverLoad) {
							this.list = result.object;
							this.isEmpty = this.list.length === 0 && this.querior.hasReachedBottom;
							this.hasEverLoad = true;
						} else {
							this.list = this.list.concat(result.object);
						}
						if (isRefresh) {
							this.commentListEvents.emit('refreshList');
						}
					},
					fail(result) {
						Toast.fail(result.message);
					},
					complete() {
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
.comment-list {
	height: 100%;
	overflow-y: visible;
	background-color: #ECECEC;
}

.comment-item {
	padding: 0.5rem;
}
</style>