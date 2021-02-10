<template>
	<a-row class="bottom-bar" justify="space-between">
		<a-col class="icon-col" span="12" offset="6" @click="openCommentPanel">
			<comment-outlined class="icon"/>
			<span class="cnt">{{ comment.l2_cnt }}</span>
			<span v-if="comment.poster_rep_cnt > 0" style="font-size: 0.75rem">
				楼主{{ comment.poster_rep_cnt }}条
			</span>
		</a-col>
		<a-col class="icon-col" span="6">
			<span v-check-sign="true" @click="changeLike">
				<like-filled v-if="comment.liked_by_viewer" class="icon" style="color: red"/>
				<like-outlined v-else class="icon"/>
			</span>
			<span class="cnt">{{ comment.like_cnt }}</span>
		</a-col>
	</a-row>
</template>

<script>
import {CommentOutlined, LikeFilled, LikeOutlined} from "@ant-design/icons-vue";
import global from "@/lib/js/global";
import lpt from "@/lib/js/laputa/laputa";

export default {
	name: 'BottomBar',
	props: {
		commentId: Number
	},
	inject: {
		localEvents: {
			type: Object
		}
	},
	components: {
		LikeOutlined,
		LikeFilled,
		CommentOutlined
	},
	data() {
		return {
			comment: global.states.commentL1Manager.get(this.commentId)
		}
	},
	created() {
		this.lptConsumer = lpt.createConsumer();
	},
	methods: {
		openCommentPanel() {
			this.localEvents.emit('openCommentPanel', {
				type: lpt.commentServ.level2,
				id: this.comment.id
			});
		},
		changeLike() {
			const ref = this;
			const isLiked = this.comment.liked_by_viewer;
			const fun = isLiked ? lpt.likeServ.unlike : lpt.likeServ.like;
			fun({
				consumer: this.lptConsumer,
				ignoreGlobalBusyChange: true,
				param: {
					type: lpt.contentType.commentL1
				},
				data: {
					target_id: this.comment.id
				},
				success() {
					ref.comment.liked_by_viewer = !isLiked;
					ref.comment.like_cnt += isLiked ? -1 : 1;
				}
			})
		}
	}
}
</script>

<style scoped>
.bottom-bar {
	padding: 0 1rem;
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