<template>
	<template v-if="forceReloadFlag">
		<div v-show="showDrawer">
			<div v-show="showPostArea" class="post-area"
			        :class="{'with-scroll-bar': lptContainer === 'blogMain'}"
			        :style="{height: scrollHeight, position: 'relative'}" v-scroll-view>
				<post-item style="padding: 0 0.5rem" :post-id="post.id" :show-bottom="false" :show-full-text="true"/>
				<div v-show="!showCommentDetail" ref="middleBar" id="middle-bar"
				     :style="{width: clientWidth + 'px'}">
					<van-tabs ref="tabs" v-model:active="curTabKey" swipeable sticky lazy-render>
						<van-tab ref="forwardTab" name="forward" :title="'转发 ' + post.forward_cnt">
							<forward-list v-if="curTabKey === 'forward'" ref="forwardList" :post-id="postId"
							              @refresh="init" class="list-area" :fill-parent="$refs.middleBar"/>
						</van-tab>
						<van-tab ref="commentTab" name="comment" :title="'评论 ' + post.comment_cnt">
							<comment-list v-if="curTabKey === 'comment'" ref="commentList" :post-id="postId"
							              sort-type="popular" @refresh="init" class="list-area"
							              :fill-parent="$refs.middleBar"/>
						</van-tab>
						<van-tab ref="likeTab" name="like" :title="'赞 ' + post.like_cnt">
							<like-list v-if="curTabKey === 'like'" ref="likeList" :target-id="parseInt(postId)"
							           @refresh="init" class="list-area" :fill-parent="$refs.middleBar"/>
						</van-tab>
					</van-tabs>
				</div>
				<comment-detail v-if="showCommentDetail" :comment-id="curCommentDetailId"/>
			</div>
			<input-panel :post-id="post.id" :panel-style="panelStyle" :overlay="lptContainer !== 'blogMain'"/>
			<bottom-bar v-show="!showCommentDetail" id="bottom-bar"
			            :style="{height: mainBarHeight + 'px'}"
			            :post-id="post.id"/>
		</div>
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
import {toRef} from "vue";

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
	inject: {
		lptContainer: {
			type: String
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
			panelStyle: {},
			post: this.init(),
			showDrawer: this.lptContainer === 'blogDrawer' ? toRef(global.states.blog, 'showDrawer') : true,
			curTabKey: 'comment',
			showCommentDetail: false,
			showPostArea: true,
			curCommentDetailId: -1
		}
	},
	watch: {
		postId() {
			this.post = this.init();
			this.parseCommand();
			this.forceReload();
		},
		clientWidth(width) {
			this.panelStyle.width = width;
		}
	},
	created() {
		this.parseCommand();
		this.lptConsumer = lpt.createConsumer();
		global.events.on('signIn', () => {
			this.post = this.init();
		});
		this.postDetailEvents.on('openCommentDetail', (param) => {
			this.curCommentDetailId = param.id;
			this.showCommentDetail = true;
		});
		this.postDetailEvents.on('closeCommentDetail', () => {
			this.showCommentDetail = false;
		});
	},
	mounted() {
		this.panelStyle = {
			width: this.clientWidth + 'px',
			left: this.lptContainer === 'blogMain' ? (global.states.style.blogMainLeft + 'px') : '0'
		};
		if (this.lptContainer === 'blogMain') {
			this.panelStyle.boxShadow = '0 0 10px 6px rgba(0, 0, 0, 0.25)';
			this.panelStyle.paddingBottom = '1.5rem';
		}
	},
	computed: {
		scrollHeight() {
			if (this.lptContainer === 'blogMain') {
				return global.states.style.mainHeight + 'px';
			}
			const mainViewHeight = global.states.style.bodyHeight;
			// 底部高度加0.5的padding
			const barHeight = this.mainBarHeight + 10;
			if (this.showCommentDetail) {
				return mainViewHeight + 'px';
			} else {
				return mainViewHeight - barHeight + 'px';
			}
		},
		clientWidth() {
			// 巨坑，深扒van-tabs组件发现是在swipe组件中获取了一个tabs的宽度
			// 但是如果tabs组件不占满屏幕，又没有固定的px值，则返回0
			// tabs组件错乱
			const curTab = this.$refs[this.curTabKey + 'Tab'];
			if (curTab) {
				this.$nextTick(() => curTab.$parent.resize());
				this.$refs.tabs.resize();
			}
			if (this.lptContainer === 'blogDrawer') {
				return global.states.style.drawerWidth;
			} else if (this.lptContainer === 'blogMain') {
				return global.states.style.blogMainWidth;
			} else {
				return global.states.style.bodyWidth;
			}
		}
	},
	methods: {
		setTitle(post) {
			if (this.lptContainer === 'blogDrawer') return;
			global.methods.setTitle({
				contentDesc: post.title || post.customContent || post.content,
				pageDesc: '帖子',
				route: this.$route
			});
		},
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
		init() {
			return global.states.postManager.get({
				itemId: this.postId,
				success: (post) => {
					this.setTitle(post);
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
	padding-top: 1rem;
	background-color: white;
	overflow-y: scroll;
	overflow-x: hidden;
}

.content-area {
	margin-top: 0.5rem;
}

.list-area {
	overflow-y: visible;
}

#middle-bar {
	height: 100%;
	background-color: white;
}

#bottom-bar {
	position: absolute;
	bottom: 0;
	left: 0;
	z-index: 2;
}
</style>