<template>
	<a-row class="bottom-bar" justify="space-between">
		<a-col class="icon-col" span="6" @click="showPostDetail">
			<share-alt-outlined class="icon"/>
			<span class="cnt">{{ post.forward_cnt }}</span>
		</a-col>
		<a-col class="icon-col" span="6" @click="showPostDetail">
			<comment-outlined class="icon"/>
			<span class="cnt">{{ post.comment_cnt }}</span>
		</a-col>
		<a-col class="icon-col" span="6">
			<span v-check-sign="{click: changeLike}">
				<like-filled v-if="post.liked_by_viewer" class="icon" style="color: red"/>
				<like-outlined v-else class="icon"/>
			</span>
			<span class="cnt">{{ post.like_cnt }}</span>
		</a-col>
		<a-col span="1" v-if="showActions" class="actions-icon">
			<post-actions :post-id="postId" :post-of="postOf"/>
		</a-col>
	</a-row>
</template>

<script>
import {
	LikeOutlined, LikeFilled,
	CommentOutlined, ShareAltOutlined
} from '@ant-design/icons-vue';
import PostActions from './PostActions';
import lpt from '@/lib/js/laputa/laputa';
import global from '@/lib/js/global';
import {Toast} from 'vant';


export default {
	name: 'BottomBar',
	props: {
		postId: Number,
		postOf: String,
		showActions: Boolean,
		onShowPostDetail: Function
	},
	inject: {
		lptContainer: {
			type: String
		}
	},
	components: {
		LikeOutlined,
		LikeFilled,
		CommentOutlined,
		ShareAltOutlined,
		PostActions
	},
	data() {
		const post = global.states.postManager.get({
			itemId: this.postId
		});
		return {
			post: post
		}
	},
	created() {
		this.lptConsumer = lpt.createConsumer();
	},
	methods: {
		showPostDetail() {
			const res = this.onShowPostDetail && this.onShowPostDetail();
			if (res === false) {
				return;
			}
			if (this.lptContainer === 'blogMain') {
				this.$router.push({
					path: '/blog/post_detail/' + this.post.id
				});
			} else {
				this.$router.push({
					path: '/post_detail/' + this.post.id
				});
			}
		},
		changeLike() {
			const ref = this;
			const isLiked = this.post.liked_by_viewer;
			const fun = isLiked ? lpt.likeServ.unlike : lpt.likeServ.like;
			fun({
				consumer: this.lptConsumer,
				ignoreGlobalBusyChange: true,
				param: {
					type: lpt.contentType.post
				},
				data: {
					target_id: this.post.id
				},
				success() {
					ref.post.liked_by_viewer = !isLiked;
					ref.post.like_cnt += isLiked ? -1 : 1;
				},
				fail(result) {
					Toast.fail(result.message);
				}
			})
		}
	}
}
</script>

<style scoped>
.bottom-bar {
	padding: 0.5rem 1rem;
	position: relative;
}

.icon-col {
	text-align: center;
}

.cnt {
	margin-left: 0.5rem;
}

.icon {
	font-size: 1rem;
}
</style>