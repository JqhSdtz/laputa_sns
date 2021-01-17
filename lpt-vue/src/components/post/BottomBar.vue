<template>
	<a-row class="bottom-bar" justify="space-between">
		<a-col class="icon-col" span="6">
			<share-alt-outlined class="icon"/>
			<span class="cnt">{{post.forward_cnt}}</span>
		</a-col>
		<a-col class="icon-col" span="6">
			<comment-outlined class="icon"/>
			<span class="cnt">{{post.comment_cnt}}</span>
		</a-col>
		<a-col class="icon-col" span="6">
			<like-filled v-if="post.liked_by_viewer" class="icon" style="color: red"/>
			<like-outlined v-else class="icon"/>
			<span class="cnt">{{post.like_cnt}}</span>
		</a-col>
	</a-row>
</template>

<script>
import {  LikeOutlined, LikeFilled,
	CommentOutlined, ShareAltOutlined } from '@ant-design/icons-vue';

export default {
	name: 'BottomBar',
	props: {
		post: Object
	},
	components: {
		LikeOutlined,
		LikeFilled,
		CommentOutlined,
		ShareAltOutlined
	},
	data() {
		return {}
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