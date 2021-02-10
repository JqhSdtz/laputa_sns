<template>
	<div class="post-area" style="padding-top: 1rem" :style="{height: getScrollHeight(), position: 'relative'}">
		<div style="margin: 0 0.5rem">
			<div class="post-item">
				<top-bar class="top-bar" :post="post"></top-bar>
				<content-area class="content-area" :post="post"></content-area>
			</div>
		</div>
		<div v-show="!showCommentDetail" id="middle-bar" style="width: 100%;">
			<van-tabs v-model:active="curTabKey" swipeable sticky lazy-render>
				<van-tab name="forward" :title="'转发 ' + post.forward_cnt">
					<forward-list v-if="curTabKey === 'forward'" ref="forwardList" :post-id="postId" @refresh="onRefresh"/>
				</van-tab>
				<van-tab name="comment" :title="'评论 ' + post.comment_cnt" style="height: 100%">
					<comment-list v-if="curTabKey === 'comment'" ref="commentList" :post-id="postId"
					              sort-type="popular" @refresh="onRefresh"/>
				</van-tab>
				<van-tab name="like" :title="'赞 ' + post.like_cnt">
					<like-list v-if="curTabKey === 'like'" ref="likeList" :post-id="postId" @refresh="onRefresh"/>
				</van-tab>
			</van-tabs>
		</div>
	</div>
	<bottom-bar style="position: fixed; bottom: 0;" :style="{height: mainBarHeight + 'px'}" :post-id="post.id"/>
</template>

<script>
import lpt from '@/lib/js/laputa/laputa';
import global from '@/lib/js/global';
import {createEventBus} from '@/lib/js/global/global-events';
import {Toast} from 'vant';
import TopBar from '@/components/post/item/parts/TopBar';
import ContentArea from '@/components/post/item/parts/ContentArea';
import BottomBar from './BottomBar';
import CommentList from './comment/CommentList';
import ForwardList from "@/components/post/post_detail/forward/ForwardList";
import LikeList from "@/components/post/post_detail/like/LikeList";

const localEvents = createEventBus();

export default {
	name: 'PostDetail',
	props: {
		postId: String
	},
	provide: {
		localEvents: localEvents
	},
	components: {
		TopBar,
		ContentArea,
		BottomBar,
		CommentList,
		ForwardList,
		LikeList
	},
	data() {
		return {
			mainBarHeight: global.vars.style.postDetailBarHeight,
			post: global.states.postManager.get(this.postId),
			curTabKey: 'comment',
			showCommentDetail: false
		}
	},
	created() {
		const ref = this;
		this.lptConsumer = lpt.createConsumer();
		this.init();
		global.events.on('login', () => {
			ref.init();
		});
		localEvents.on('openCommentDetail', (param) => {
			console.log(param);
		});
	},
	methods: {
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
		},
		getScrollHeight() {
			const mainViewHeight = document.body.clientHeight;
			// 底部高度加0.5的padding
			const barHeight = this.mainBarHeight + 10;
			return mainViewHeight - barHeight + 'px';
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
</style>