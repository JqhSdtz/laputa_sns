<template>
	<a-row class="top-bar">
		<a-col span="4" class="ava-div">
			<img class="ava" :class="{'def-ava': !comment.creator.raw_avatar}" :src="avatarUrl" @click="showUserHomePage"/>
		</a-col>
		<a-col class="time-and-name">
			<div>
				<p class="name" @click="showUserHomePage" style="display: inline-block">{{ comment.creator.nick_name }}</p>
				<van-tag v-if="comment.creator.id === posterId" type="primary"
				         style="display: inline-block; margin-left: 0.5rem">
					贴主
				</van-tag>
			</div>
			<p v-if="typeof comment.create_time !== 'undefined'" class="time">{{ beforeTime }}</p>
		</a-col>
		<a-col v-if="comment.is_topped" class="topped-tag">
			<van-tag type="primary">
				置顶
			</van-tag>
		</a-col>
	</a-row>
</template>

<script>
import lpt from "@/lib/js/laputa/laputa";
import TimeAgo from "javascript-time-ago";
import zh from 'javascript-time-ago/locale/zh';
import global from "@/lib/js/global";

TimeAgo.addLocale(zh);
const timeAgo = new TimeAgo('zh-CN');

export default {
	name: 'TopBar',
	props: {
		comment: Object
	},
	computed: {
		posterId() {
			const post = global.states.postManager.get({
				itemId: this.comment.post_id
			});
			return post.creator.id;
		},
		avatarUrl() {
			if (this.comment.creator) {
				return lpt.getUserAvatarUrl(this.comment.creator);
			} else {
				return '';
			}
		},
		beforeTime() {
			if (this.comment.create_time) {
				return timeAgo.format(this.comment.create_time);
			} else {
				return '';
			}
		}
	},
	methods: {
		showUserHomePage() {
			this.$router.push({
				path: '/user_home_page/' + this.comment.creator.id
			});
		}
	}
}
</script>

<style scoped>
.top-bar {
	height: 2.5rem;
}

.top-bar .time-and-name, .top-bar .topped-tag {
	margin-top: 0;
}

.top-bar .ava {
	border-radius: 100%;
	margin: 0.2rem 1rem;
	width: 2.2rem;
	height: 2.2rem;
}

.top-bar .def-ava {
	margin: 0.075rem 0.75rem;
	width: 2.45rem;
	height: 2.45rem;
}

.top-bar p {
	text-align: left;
	margin: 0;
}

.top-bar .name {
	font-weight: bold;
	font-size: 0.85rem;
}

.top-bar .time {
	margin-left: 0.15rem;
	font-size: 0.65rem;
}

.topped-tag {
	margin-left: 1rem;
}
</style>