<template>
	<a-row class="bottom-bar" justify="space-between">
		<a-col class="icon-col" span="6">
			<share-alt-outlined class="icon"/>
			<span class="cnt">{{ post.forward_cnt }}</span>
		</a-col>
		<a-col class="icon-col" span="6" @click="showPostDetail">
			<comment-outlined class="icon"/>
			<span class="cnt">{{ post.comment_cnt }}</span>
		</a-col>
		<a-col class="icon-col" span="6">
			<span v-check-sign="true" @click="changeLike">
				<like-filled v-if="post.liked_by_viewer" class="icon" style="color: red"/>
				<like-outlined v-else class="icon"/>
			</span>
			<span class="cnt">{{ post.like_cnt }}</span>
		</a-col>
	</a-row>
</template>

<script>
import {
	LikeOutlined, LikeFilled,
	CommentOutlined, ShareAltOutlined
} from '@ant-design/icons-vue';
import lpt from '@/lib/js/laputa/laputa';
import global from "@/lib/js/global/global-state";

export default {
	name: 'BottomBar',
	props: {
		post_id: Number
	},
	components: {
		LikeOutlined,
		LikeFilled,
		CommentOutlined,
		ShareAltOutlined
	},
	data() {
		return {
			post: global.postManager.get(this.post_id)
		}
	},
	created() {
		this.lptConsumer = lpt.createConsumer();
	},
	methods: {
		showPostDetail() {
			this.$router.push({
				name: 'postDetail',
				params: {
					post_id: this.post.id
				}
			});
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