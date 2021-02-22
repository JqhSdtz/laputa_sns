<template>
	<template v-if="forceReloadFlag">
		<div class="post-area" style="padding-top: 1rem" :style="{height: scrollHeight, position: 'relative'}">
			<div style="margin: 0 0.5rem">
				<post-item class="post-item" :post-id="post.id" :show-bottom="false"/>
			</div>
			<div v-show="!showCommentDetail" ref="middleBar" id="middle-bar" style="width: 100%;height: 100%">
				<van-tabs v-model:active="curTabKey" swipeable sticky lazy-render>
					<van-tab name="forward" :title="'转发 ' + post.forward_cnt">
						<forward-list v-if="curTabKey === 'forward'" ref="forwardList" :post-id="postId"
						              @refresh="onRefresh" class="list-area" :fill-parent="$refs.middleBar"/>
					</van-tab>
					<van-tab name="comment" :title="'评论 ' + post.comment_cnt">
						<comment-list v-if="curTabKey === 'comment'" ref="commentList" :post-id="postId"
						              sort-type="popular" @refresh="onRefresh" class="list-area"
						              :fill-parent="$refs.middleBar"/>
					</van-tab>
					<van-tab name="like" :title="'赞 ' + post.like_cnt">
						<like-list v-if="curTabKey === 'like'" ref="likeList" :target-id="parseInt(postId)"
						           @refresh="onRefresh" class="list-area" :fill-parent="$refs.middleBar"/>
					</van-tab>
				</van-tabs>
			</div>
			<comment-detail v-if="showCommentDetail" :comment-id="curCommentDetailId"/>
		</div>
		<input-panel :post-id="post.id"/>
		<bottom-bar v-show="!showCommentDetail" style="position: fixed; bottom: 0;" :style="{height: mainBarHeight + 'px'}"
		            :post-id="post.id"/>
	</template>
</template>

<script>
import lpt from '@/lib/js/laputa/laputa';
import global from '@/lib/js/global';
import {createEventBus} from '@/lib/js/global/global-events';
import {Toast} from 'vant';
import PostItem from '@/components/post/item/PostItem';
import BottomBar from './BottomBar';
import CommentList from './comment/CommentList';
import ForwardList from "@/components/post/post_detail/forward/ForwardList";
import LikeList from '@/components/post/post_detail/like/LikeList';
import CommentDetail from "@/components/post/post_detail/comment_detail/CommentDetail";
import InputPanel from '@/components/post/post_detail/InputPanel';

export default {
	name: 'PostDetail',
	props: {
		postId: String
	},
	provide() {
		return {
			postDetailEvents: this.postDetailEvents
		}
	},
	components: {
		InputPanel,
		CommentDetail,
		PostItem,
		BottomBar,
		CommentList,
		ForwardList,
		LikeList
	},
	data() {
		this.postDetailEvents = createEventBus();
		return {
			mainBarHeight: global.vars.style.postDetailBarHeight,
			post: global.states.postManager.get(this.postId),
			curTabKey: 'comment',
			showCommentDetail: false,
			curCommentDetailId: -1
		}
	},
	watch: {
		postId(newValue) {
			this.post = global.states.postManager.get(newValue);
			this.forceReload();
		}
	},
	created() {
		this.parseCommand();
		const ref = this;
		this.lptConsumer = lpt.createConsumer();
		this.init();
		global.events.on('signIn', () => {
			ref.init();
		});
		this.postDetailEvents.on('openCommentDetail', (param) => {
			ref.curCommentDetailId = param.id;
			ref.showCommentDetail = true;
		});
		this.postDetailEvents.on('closeCommentDetail', () => {
			ref.showCommentDetail = false;
		});
	},
	computed: {
		scrollHeight() {
			const mainViewHeight = document.body.clientHeight;
			// 底部高度加0.5的padding
			const barHeight = this.mainBarHeight + 10;
			if (this.showCommentDetail) {
				return mainViewHeight + 'px';
			} else {
				return mainViewHeight - barHeight + 'px';
			}
		}
	},
	methods: {
		parseCommand() {
			const query = this.$route.query;
			const command = query.command;
			if (!command)
				return;
			if (command === 'showLikeList') {
				this.curTabKey = 'like';
			} else if (command === 'showForwardList') {
				this.curTabKey = 'forward';
			} else if (command === 'showCommentDetail') {
				this.curCommentDetailId = query.commentId;
			}
		},
		onRefresh() {
			this.init();
		},
		init() {
			lpt.postServ.get({
				consumer: this.lptConsumer,
				param: {
					postId: this.postId
				},
				success(result) {
					// 注意！注释掉的写法是不对的，会失去响应性
					// ref.post = global.states.postManager.add(result.object);
					global.states.postManager.add(result.object);
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
:global(.van-tab__pane) {
	height: 100%;
}

.post-area {
	overflow-y: scroll;
}

.post-area::-webkit-scrollbar {
	display: none;
}

.content-area {
	margin-top: 0.5rem;
}

.list-area {
	overflow-y: visible;
}
</style>