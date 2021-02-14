<template>
	<div class="top-bar">
		<div class="ava-div" style="display: inline-block; float: left">
			<img class="ava" :src="avatarUrl" @click="showUserHomePage"/>
		</div>
		<div class="time-and-name" style="display: inline-block; float: left">
			<p class="name" @click="showUserHomePage">{{ post.creator.nick_name }}</p>
			<p v-if="typeof post.create_time !== 'undefined'" class="time">{{ beforeTime }}</p>
		</div>
		<div v-if="post.is_topped" class="topped-tag" style="display: inline-block; float: left">
			<van-tag  type="primary">
				置顶
			</van-tag>
		</div>
	</div>
</template>

<script>
import lpt from '@/lib/js/laputa/laputa';
import TimeAgo from 'javascript-time-ago';
import zh from 'javascript-time-ago/locale/zh';
import global from "@/lib/js/global";

TimeAgo.addLocale(zh);
const timeAgo = new TimeAgo('zh-CN');

export default {
	name: 'TopBar',
	props: {
		postId: Number
	},
	data() {
		const post = global.states.postManager.get(this.postId);
		return {
			post: post
		}
	},
	computed: {
		avatarUrl() {
			if (this.post.creator) {
				return lpt.getUserAvatarUrl(this.post.creator);
			} else {
				return '';
			}
		},
		beforeTime() {
			if (this.post.create_time) {
				return timeAgo.format(this.post.create_time);
			} else {
				return '';
			}
		}
	},
	methods: {
		showUserHomePage() {
			this.$router.push({
				name: 'homePage',
				params: {
					userId: this.post.creator.id
				}
			});
		}
	}
}
</script>

<style>
.top-bar {
	height: 3rem;
}

.top-bar .ava {
	border-radius: 100%;
	margin: 0.5rem 1rem;
	width: 2.5rem;
	height: 2.5rem;
}

.top-bar .time-and-name, .topped-tag {
	margin-top: 0.52rem;
}

.top-bar p {
	text-align: left;
	margin: 0;
}

.top-bar .name {
	font-weight: bold;
}

.top-bar .time {
	margin-left: 0.15rem;
	font-size: 0.75rem;
}

.topped-tag {
	margin-left: 1rem;
}

</style>
